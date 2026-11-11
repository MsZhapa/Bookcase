#!/usr/bin/env node
'use strict';
// Administrative copy only. Never deletes, changes rules, or switches the Android app.
const fs=require('node:fs'),path=require('node:path');
const args=process.argv.slice(2), options={};
for(let i=0;i<args.length;i++) {
  if(args[i]==='--apply') options.apply=true;
  else if(['--source','--target','--project','--database'].includes(args[i])) options[args[i].slice(2)]=args[++i];
  else throw new Error('Unknown argument');
}
if(!options.source || !options.target || !options.project || !options.database || options.database==='(default)') throw new Error('Specify source/target backup files, project and a separate database; dry-run is the default');
const names=['works','editions','lendingRecords','books','bookCopies','wishlist','cubePositions'];
const canonical=value=>JSON.stringify(sort(value));
function sort(value) {
  if(Array.isArray(value)) return value.map(sort);
  if(value && typeof value==='object') return Object.fromEntries(Object.keys(value).sort().map(key=>[key,sort(value[key])]));
  return value;
}
function fromField(field) {
  if('nullValue' in field) return {type:'null'};
  if('stringValue' in field) return {type:'string',value:field.stringValue};
  if('booleanValue' in field) return {type:'boolean',value:field.booleanValue};
  if('integerValue' in field) return {type:'integer',value:String(field.integerValue)};
  if('doubleValue' in field) { if(!Number.isFinite(field.doubleValue)) throw new Error('Unsupported number'); return {type:'double',value:field.doubleValue}; }
  if('timestampValue' in field) {
    const match=/^(.*?)(?:\.(\d+))?Z$/.exec(field.timestampValue); if(!match) throw new Error('Unsupported timestamp');
    return {type:'timestamp',seconds:String(Date.parse(match[1]+'Z')/1000),nanoseconds:Number((match[2]||'').padEnd(9,'0'))};
  }
  if('mapValue' in field) return {type:'map',value:Object.fromEntries(Object.entries(field.mapValue.fields||{}).map(([k,v])=>[k,fromField(v)]))};
  if('arrayValue' in field) return {type:'list',value:(field.arrayValue.values||[]).map(fromField)};
  throw new Error('Unsupported source field type');
}
function toField(field) {
  switch(field.type) {
    case 'null': return {nullValue:null};
    case 'string': return {stringValue:field.value};
    case 'boolean': return {booleanValue:field.value};
    case 'integer': return {integerValue:field.value};
    case 'double': return {doubleValue:field.value};
    case 'timestamp': return {timestampValue:new Date(Number(field.seconds)*1000).toISOString().replace(/\.\d{3}Z$/,'.'+String(field.nanoseconds).padStart(9,'0')+'Z')};
    case 'map': return {mapValue:{fields:Object.fromEntries(Object.entries(field.value).map(([k,v])=>[k,toField(v)]))}};
    case 'list': return {arrayValue:{values:field.value.map(toField)}};
    default: throw new Error('Unsupported target field type');
  }
}
async function main() {
  const source=JSON.parse(fs.readFileSync(options.source,'utf8')),target=JSON.parse(fs.readFileSync(options.target,'utf8'));
  for(const data of [source,target]) if(data.application!=='Bookcase' || data.formatVersion!==2 || canonical(Object.keys(data.collections).sort())!==canonical(names.slice().sort())) throw new Error('App-generated version 2 backups are required');
  if(target.collections.books.length) throw new Error('V2 target must contain normalized copies only');
  const sourceIds=[...source.collections.books,...source.collections.bookCopies].map(d=>d.id).sort();
  if(canonical(sourceIds)!==canonical(target.collections.bookCopies.map(d=>d.id).sort()) || new Set(sourceIds).size!==sourceIds.length) throw new Error('Copy identities changed');
  const cli=process.env.BOOKCASE_FIREBASE_TOOLS || path.dirname(require.resolve('firebase-tools/package.json'));
  const account=require(path.join(cli,'lib/auth')).getGlobalDefaultAccount(); if(!account) throw new Error('Sign in with Firebase CLI first');
  await require(path.join(cli,'lib/requireAuth')).requireAuth({...account,project:options.project,nonInteractive:true});
  const client=new (require(path.join(cli,'lib/apiv2')).Client)({urlPrefix:'https://firestore.googleapis.com',apiVersion:'v1'});
  const root=database=>`/projects/${options.project}/databases/${database}/documents`;
  async function snapshot(database) {
    const collections={};
    for(const name of names) {
      const docs=[]; let pageToken;
      do {
        const queryParams={pageSize:1000}; if(pageToken) queryParams.pageToken=pageToken;
        const response=await client.get(`${root(database)}/${name}`,{queryParams});
        for(const doc of response.body.documents||[]) docs.push({id:doc.name.split('/').pop(),fields:{type:'map',value:Object.fromEntries(Object.entries(doc.fields||{}).map(([k,v])=>[k,fromField(v)]))}});
        pageToken=response.body.nextPageToken;
      } while(pageToken);
      collections[name]=docs.sort((a,b)=>a.id<b.id?-1:a.id>b.id?1:0);
    }
    return collections;
  }
  const before=await snapshot('(default)');
  if(canonical(before)!==canonical(source.collections)) throw new Error('Live source differs from the backup. Refresh and review migration before any writes.');
  const targetBefore=await snapshot(options.database),missing=[];
  for(const name of names) {
    const wanted=new Map(target.collections[name].map(d=>[d.id,d]));
    for(const doc of targetBefore[name]) if(!wanted.has(doc.id) || canonical(wanted.get(doc.id))!==canonical(doc)) throw new Error('Target contains unrelated or conflicting records; nothing will be overwritten');
    const existing=new Set(targetBefore[name].map(d=>d.id));
    for(const doc of target.collections[name]) if(!existing.has(doc.id)) missing.push({name,doc});
  }
  console.log(JSON.stringify({mode:options.apply?'create-only':'dry-run',database:options.database,copies:sourceIds.length,recordsToCreate:missing.length,sourceMatchesBackup:true}));
  if(!options.apply) return;
  for(let start=0;start<missing.length;start+=100) {
    const writes=missing.slice(start,start+100).map(({name,doc})=>({update:{name:root(options.database).slice(1)+`/${name}/${doc.id}`,fields:toField(doc.fields).mapValue.fields},currentDocument:{exists:false}}));
    await client.post(`${root(options.database)}:commit`,{writes});
    console.log(JSON.stringify({created:Math.min(start+100,missing.length),total:missing.length}));
  }
  const actual=await snapshot(options.database);
  if(canonical(actual)!==canonical(target.collections)) throw new Error('Target verification failed; do not switch the app');
  if(canonical(await snapshot('(default)'))!==canonical(before)) throw new Error('Source changed during copy; target is not ready for switching');
  const report={database:options.database,copies:sourceIds.length,recordCounts:Object.fromEntries(names.map(n=>[n,actual[n].length])),targetVerified:true,sourceUnchanged:true,appSwitched:false};
  fs.writeFileSync(path.join(path.dirname(options.target),'bookcase-v2-cloud-verification.json'),JSON.stringify(report,null,2)+'\n'); console.log(JSON.stringify(report));
}
main().catch(error=>{console.error(error.message||'Migration stopped');process.exitCode=1;});
