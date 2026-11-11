#!/usr/bin/env node
'use strict';
// Local emulator only. Unsigned mock identity tokens are NEVER sent to production.
const fs=require('node:fs'),assert=require('node:assert/strict');
const host='http://127.0.0.1:8080',project='demo-bookcase-accounts',root=`${host}/v1/projects/${project}/databases/(default)/documents`;
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
async function main(){
 await allowed('rules compile',request(`${host}/emulator/v1/projects/${project}:securityRules`,null,'PUT',{rules:{files:[{name:'firestore.rules',content:rules}]}}));
 await allowed('clear isolated fixture',request(`${host}/emulator/v1/projects/${project}/databases/(default)/documents`,'owner','DELETE'));
 const alice=jwt('alice@example.test',false,'password','alice'),bob=jwt('bob@example.test',true,'google.com','bob');
 const a='users/alice/',b='users/bob/';
 const work={id:'w',title:'Book',author:'Author',description:''};
 const edition={id:'e',workId:'w',title:'Book',author:'Author',description:'',imageUrl:'',isbn:'',pageCount:100};
 const copy={id:'c',editionId:'e',cubeNumber:1,position:1,read:false,progress:0,lendingStatus:'AVAILABLE',migrationArchive:'original snapshot'};
 await allowed('password account creates own work',commit(alice,[document(a+'works/w',fields(work))]));
 await allowed('Google account creates own work with same ID',commit(bob,[document(b+'works/w',fields({...work,title:'Other book'}))]));
 await allowed('password account reads own list',request(root+'/'+a+'works',alice));
 await allowed('Google account reads own list',request(root+'/'+b+'works',bob));
 for(const [label,token]of [['signed out',null],['another account',bob],['anonymous provider',jwt('alice@example.test',false,'anonymous','alice')],['same email different UID',jwt('alice@example.test',true,'google.com','impostor')]]){
  await denied(label+' cannot read Alice books',request(root+'/'+a+'works',token));
  await denied(label+' cannot write Alice books',commit(token,[document(a+'works/x',fields({...work,id:'x'}))]));
 }
 await denied('Alice cannot read Bob',request(root+'/'+b+'works/w',alice));
 await denied('Alice cannot delete Bob',request(root+'/'+b+'works/w',alice,'DELETE'));
 for(const p of ['works','bookCopies','books','privateConfig/access','libraryStatus/current','users/bob','users'])await denied('no access to '+p,request(root+'/'+p,alice));
 await denied('no self-assigned roles',commit(alice,[document('users/alice',fields({role:'admin'}))]));
 await denied('unknown collection denied',commit(alice,[document(a+'other/x',fields({value:1}))]));
 await denied('work rejects extra fields',commit(alice,[document(a+'works/x',fields({...work,id:'x',ownerUid:'bob'}))]));
 await denied('work rejects missing title',commit(alice,[document(a+'works/x',fields({id:'x',author:'A',description:''}))]));
 await denied('work update rejects missing fields',request(root+'/'+a+'works/w',alice,'PATCH',{fields:fields({id:'w',title:'X'})}));
 await allowed('create own edition',commit(alice,[document(a+'editions/e',fields(edition))]));
 await denied('edition cannot link work in another account',commit(bob,[document(b+'editions/e',fields({...edition,workId:'alice-only'}))]));
 await allowed('create own copy',commit(alice,[document(a+'bookCopies/c',fields(copy))]));
 await denied('copy cannot refer to another account edition',commit(bob,[document(b+'bookCopies/c',fields(copy))]));
 await allowed('update notes and pages',request(root+'/'+a+'bookCopies/c',alice,'PATCH',{fields:fields({...copy,notes:'Note',currentPage:25,readingPageCount:100,progress:25})}));
 await denied('cannot edit archive',request(root+'/'+a+'bookCopies/c',alice,'PATCH',{fields:fields({...copy,migrationArchive:'changed'})}));
 await denied('cannot remove archive',request(root+'/'+a+'bookCopies/c',alice,'PATCH',{fields:fields(Object.fromEntries(Object.entries(copy).filter(([k])=>k!=='migrationArchive')))}));
 await denied('pages cannot exceed total',request(root+'/'+a+'bookCopies/c',alice,'PATCH',{fields:fields({...copy,currentPage:101,readingPageCount:100})}));
 await denied('cube must be valid',request(root+'/'+a+'bookCopies/c',alice,'PATCH',{fields:fields({...copy,cubeNumber:9})}));
 await allowed('create own counter',commit(alice,[document(a+'cubePositions/1',fields({lastPosition:1}))]));
 await allowed('advance own counter',request(root+'/'+a+'cubePositions/1',alice,'PATCH',{fields:fields({lastPosition:2})}));
 await denied('counter cannot decrease',request(root+'/'+a+'cubePositions/1',alice,'PATCH',{fields:fields({lastPosition:1})}));
 const wish={id:'wish',title:'Wanted',author:'',isbn:'',createdAt:Date.now()};
 await allowed('create own wishlist item',commit(alice,[document(a+'wishlist/wish',fields(wish))]));
 await denied('wishlist date immutable',request(root+'/'+a+'wishlist/wish',alice,'PATCH',{fields:fields({...wish,createdAt:1})}));
 await denied('Bob cannot list Alice wishlist',request(root+'/'+a+'wishlist',bob));
 const loan={...fields({id:'loan',bookId:'c',bookCopyId:'c',borrowerName:'Friend',status:'ACTIVE'}),lentDate:{timestampValue:'2026-01-01T00:00:00Z'}};
 await allowed('lend atomically within own account',commit(alice,[document(a+'lendingRecords/loan',loan),{update:{name:`projects/${project}/databases/(default)/documents/${a}bookCopies/c`,fields:fields({...copy,lendingStatus:'LENT',borrowerName:'Friend',activeLoanId:'loan'})}}]));
 await denied('another account cannot read loans',request(root+'/'+a+'lendingRecords',bob));
 await allowed('return loan',request(root+'/'+a+'lendingRecords/loan',alice,'PATCH',{fields:{...loan,...fields({status:'RETURNED'}),returnedDate:{timestampValue:'2026-01-02T00:00:00Z'}}}));
 await denied('returned loan cannot be rewritten',request(root+'/'+a+'lendingRecords/loan',alice,'PATCH',{fields:loan}));
 await allowed('delete own copy',request(root+'/'+a+'bookCopies/c',alice,'DELETE'));
 // Use real emulator authentication for registration/login rather than mock JWTs.
 const authRoot=`http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1`;
 const email='new-'+Date.now()+'@example.test',password='Test-password-123';
 const registered=await allowed('email registration',request(authRoot+'/accounts:signUp?key=fake',null,'POST',{email,password,returnSecureToken:true}));
 await allowed('new account has an empty library',request(root+`/users/${registered.localId}/bookCopies`,registered.idToken));
 await denied('new account cannot read existing Alice books',request(root+'/'+a+'works',registered.idToken));
 const login=await allowed('email/password login',request(authRoot+'/accounts:signInWithPassword?key=fake',null,'POST',{email,password,returnSecureToken:true}));
 assert.equal(login.localId,registered.localId);
 const wrong=await request(authRoot+'/accounts:signInWithPassword?key=fake',null,'POST',{email,password:'Wrong-password',returnSecureToken:true});assert.equal(wrong.status,400);checks++;console.log('PASS wrong password rejected');
 await allowed('password reset request (local emulator only)',request(authRoot+'/accounts:sendOobCode?key=fake',null,'POST',{email,requestType:'PASSWORD_RESET'}));
 console.log(`PASS ${checks} account isolation, validation and authentication checks`);
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
