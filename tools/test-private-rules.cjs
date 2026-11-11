#!/usr/bin/env node
'use strict';
// Local emulator only. Unsigned mock identity tokens are NEVER sent to production.
const fs=require('node:fs'),assert=require('node:assert/strict');
const host='http://127.0.0.1:8080',project='demo-bookcase-private',root=`${host}/v1/projects/${project}/databases/(default)/documents`;
const rules=fs.readFileSync(require('node:path').join(__dirname,'../firestore.v2.rules'),'utf8');
const owner='owner@example.test';
function jwt(email=owner,verified=true,provider='google.com',uid='owner') {
  const now=Math.floor(Date.now()/1000),encode=value=>Buffer.from(JSON.stringify(value)).toString('base64url');
  return encode({alg:'none',typ:'JWT'})+'.'+encode({iss:`https://securetoken.google.com/${project}`,aud:project,sub:uid,user_id:uid,iat:now,exp:now+3600,auth_time:now,email,email_verified:verified,firebase:{sign_in_provider:provider,identities:{email:[email]}}})+'.';
}
const identity=jwt(),foreign=jwt('other@example.test',true,'google.com','other'),password=jwt(owner,true,'password'),unverified=jwt(owner,false);
async function request(url,token,method='GET',body) {
  const headers={}; if(token) headers.Authorization='Bearer '+token; if(body!==undefined) headers['Content-Type']='application/json';
  const response=await fetch(url,{method,headers,body:body===undefined?undefined:JSON.stringify(body)});
  const text=await response.text(); let data; try {data=JSON.parse(text);} catch {data={text};} return {ok:response.ok,status:response.status,data};
}
function field(value) {
  if(value===null) return {nullValue:null};
  if(typeof value==='string') return {stringValue:value};
  if(typeof value==='boolean') return {booleanValue:value};
  if(typeof value==='number') return {integerValue:String(value)};
  if(Array.isArray(value)) return {arrayValue:{values:value.map(field)}};
  return {mapValue:{fields:Object.fromEntries(Object.entries(value).map(([k,v])=>[k,field(v)]))}};
}
function fields(value) {return field(value).mapValue.fields;}
function backupField(value) {
  switch(value.type) {
    case 'null': return {nullValue:null}; case 'string': return {stringValue:value.value}; case 'boolean': return {booleanValue:value.value}; case 'integer': return {integerValue:value.value}; case 'double': return {doubleValue:value.value};
    case 'timestamp': return {timestampValue:new Date(Number(value.seconds)*1000).toISOString().replace(/\.\d{3}Z$/,'.'+String(value.nanoseconds).padStart(9,'0')+'Z')};
    case 'map': return {mapValue:{fields:Object.fromEntries(Object.entries(value.value).map(([k,v])=>[k,backupField(v)]))}};
    case 'list': return {arrayValue:{values:value.value.map(backupField)}}; default:throw Error('Unsupported fixture field');
  }
}
const document=(path,value)=>({update:{name:`projects/${project}/databases/(default)/documents/${path}`,fields:value},currentDocument:{exists:false}});
const commit=(token,writes)=>request(root+':commit',token,'POST',{writes});
let checks=0;
async function allowed(label,promise) {const result=await promise;assert(result.ok,label+': '+JSON.stringify(result.data));checks++;console.log('PASS '+label);return result.data;}
async function denied(label,promise) {const result=await promise;assert.equal(result.status,403,label+': '+JSON.stringify(result.data));checks++;console.log('PASS '+label);}
async function main() {
  await allowed('rules compile',request(`${host}/emulator/v1/projects/${project}:securityRules`,null,'PUT',{rules:{files:[{name:'firestore.rules',content:rules}]}}));
  await allowed('clear isolated fixture',request(`${host}/emulator/v1/projects/${project}/databases/(default)/documents`,'owner','DELETE'));
  await allowed('administrator configures owner',commit('owner',[document('privateConfig/access',fields({ownerEmail:owner}))]));
  for(const [name,token] of [['signed out',null],['another Google account',foreign],['unverified email',unverified],['password provider',password]]) {
    await denied(name+' cannot list library',request(root+'/bookCopies',token));
    await denied(name+' cannot create records',commit(token,[document('works/forbidden',fields({id:'forbidden',title:'X',author:'A',description:null}))]));
  }
  await allowed('administrator marks verified migration ready',commit('owner',[document('libraryStatus/current',{...fields({schemaVersion:2,migrationVerified:true,initialCopyCount:507,sourceFingerprint:'0'.repeat(64),targetFingerprint:'1'.repeat(64),activated:false}),verifiedAt:{timestampValue:new Date().toISOString()}})]));
  await allowed('owner can read readiness receipt',request(root+'/libraryStatus/current',identity));
  await denied('other account cannot read readiness receipt',request(root+'/libraryStatus/current',foreign));
  await denied('owner cannot forge readiness receipt',request(root+'/libraryStatus/current',identity,'PATCH',{fields:fields({schemaVersion:2,migrationVerified:false})}));
  await denied('owner cannot read access configuration',request(root+'/privateConfig/access',identity));
  await denied('owner cannot change access configuration',request(root+'/privateConfig/access',identity,'PATCH',{fields:fields({ownerEmail:'attacker@example.test'})}));
  await denied('no self-assigned admin role',commit(identity,[document('users/owner',fields({isAdmin:true}))]));
  await denied('undefined collection stays closed',request(root+'/other',identity));
  const receipt=(await request(root+'/libraryStatus/current',identity)).data;
  const activate={...receipt.fields,activated:{booleanValue:true}};
  await denied('another account cannot acknowledge cutover',commit(foreign,[{update:{name:receipt.name,fields:activate},updateTransforms:[{fieldPath:'activatedAt',setToServerValue:'REQUEST_TIME'}]}]));
  await denied('owner cannot change verification fingerprints',commit(identity,[{update:{name:receipt.name,fields:{...activate,sourceFingerprint:{stringValue:'2'.repeat(64)}}},updateTransforms:[{fieldPath:'activatedAt',setToServerValue:'REQUEST_TIME'}]}]));
  await allowed('owner acknowledges verified first cutover',commit(identity,[{update:{name:receipt.name,fields:activate},updateTransforms:[{fieldPath:'activatedAt',setToServerValue:'REQUEST_TIME'}]}]));
  await denied('cutover acknowledgement cannot be reset',request(root+'/libraryStatus/current',identity,'PATCH',{fields:{...receipt.fields,activated:{booleanValue:false}}}));
  const fixturePath=process.argv[2];
  if(fixturePath) {
    const backup=JSON.parse(fs.readFileSync(fixturePath,'utf8'));
    const writes=[]; for(const collection of ['cubePositions','works','editions','lendingRecords','bookCopies','wishlist']) for(const d of backup.collections[collection]) writes.push(document(collection+'/'+d.id,backupField(d.fields).mapValue.fields));
    for(let i=0;i<writes.length;i+=1) {
      const result=await commit(identity,writes.slice(i,i+1));assert(result.ok,'full migrated backup restore chunk '+i+': '+JSON.stringify(result.data));
    }
    checks++;console.log('PASS full migrated backup restored under owner rules ('+writes.length+' records)');
  }
  await allowed('owner creates a valid work',commit(identity,[document('works/testwork',fields({id:'testwork',title:'Work',author:'Author',description:null}))]));
  const edition={id:'testedition',workId:'testwork',title:'Work',author:'Author',description:null,imageUrl:null,isbn:'',pageCount:400};
  await allowed('owner creates valid edition',commit(identity,[document('editions/testedition',fields(edition))]));
  const copy={id:'testcopy',editionId:'testedition',cubeNumber:1,position:0,read:false,progress:0,lendingStatus:'AVAILABLE',notes:''};
  await allowed('owner creates valid physical copy',commit(identity,[document('bookCopies/testcopy',fields(copy))]));
  await allowed('owner reads and lists library',request(root+'/bookCopies',identity));
  await allowed('owner uses existing loan-history query',request(root+':runQuery',identity,'POST',{structuredQuery:{from:[{collectionId:'lendingRecords'}],where:{fieldFilter:{field:{fieldPath:'bookId'},op:'EQUAL',value:{stringValue:'testcopy'}}}}}));
  const patch=(path,value,token=identity)=>request(root+'/'+path,token,'PATCH',{fields:fields(value)});
  await denied('another Google account cannot get copy',request(root+'/bookCopies/testcopy',foreign));
  await denied('another Google account cannot edit copy',patch('bookCopies/testcopy',{...copy,notes:'Stolen'},foreign));
  await denied('another Google account cannot delete copy',request(root+'/bookCopies/testcopy',foreign,'DELETE'));
  await allowed('owner saves notes',patch('bookCopies/testcopy',{...copy,notes:'Personal note'}));
  await denied('oversized notes blocked on update',patch('bookCopies/testcopy',{...copy,notes:'x'.repeat(20001)}));
  await denied('archive cannot be added or changed on update',patch('bookCopies/testcopy',{...copy,migrationArchive:'tampered'}));
  await denied('oversized archive blocked on create',commit(identity,[document('bookCopies/oversize',{...fields(copy),id:{stringValue:'oversize'},migrationArchive:{stringValue:'x'.repeat(200001)}})]));
  await denied('schema pollution blocked',patch('bookCopies/testcopy',{...copy,isAdmin:true}));
  await denied('invalid progress blocked',patch('bookCopies/testcopy',{...copy,progress:101}));
  await denied('invalid page types blocked',patch('bookCopies/testcopy',{...copy,currentPage:'ten'}));
  await denied('page above total blocked',patch('bookCopies/testcopy',{...copy,currentPage:401,readingPageCount:400}));
  await denied('copy ID hijack blocked',patch('bookCopies/testcopy',{...copy,id:'other'}));
  await denied('missing required field blocked',patch('bookCopies/testcopy',Object.fromEntries(Object.entries(copy).filter(([k])=>k!=='editionId'))));
  await denied('orphan edition blocked',patch('editions/testedition',{...edition,workId:'missing'}));
  await denied('invalid URL blocked',patch('editions/testedition',{...edition,imageUrl:'file:///secret'}));
  await denied('enormous metadata blocked',patch('works/testwork',{id:'testwork',title:'x'.repeat(1001),author:'A',description:null}));
  const counter=await request(root+'/cubePositions/8',identity); const old=counter.ok?Number(counter.data.fields.lastPosition.integerValue):0;
  await allowed('owner reserves shelf cursor',patch('cubePositions/8',{lastPosition:old+10}));
  await denied('shelf cursor cannot move backwards',patch('cubePositions/8',{lastPosition:0}));
  await denied('counter cannot hide extra fields',patch('cubePositions/8',{lastPosition:old+11,isAdmin:true}));
  await denied('negative cursor blocked',patch('cubePositions/8',{lastPosition:-1}));
  const wish={id:'testwish',title:'Wanted',author:'Author',isbn:'',createdAt:1};
  await allowed('owner saves wishlist entry',commit(identity,[document('wishlist/testwish',fields(wish))]));
  await denied('wishlist creation time immutable',patch('wishlist/testwish',{...wish,createdAt:2}));
  await denied('wishlist title cannot disappear',patch('wishlist/testwish',{...wish,title:''}));
  await denied('legacy collection is read-only',commit(identity,[document('books/legacy',fields({title:'Legacy',location:1}))]));
  await denied('private subcollection stays closed',request(root+'/works/testwork/private',identity));
  await allowed('owner can delete own wishlist entry',request(root+'/wishlist/testwish',identity,'DELETE'));
  console.log(JSON.stringify({securityChecks:checks,passed:true,project,productionTouched:false}));
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
