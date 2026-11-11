package com.example.bookcase;

import com.google.android.gms.tasks.*;
import com.google.firebase.firestore.*;
import java.util.*;

/** Server-only snapshots and resumable, create-only restore transactions. */
public final class BackupRepository {
    private static final java.util.concurrent.Executor WORKER=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final LibraryStore db;
    public BackupRepository() { this(BookcaseDatabase.current()); }
    public BackupRepository(FirebaseFirestore db) { this(new LibraryStore(db)); }
    public BackupRepository(LibraryStore db) { this.db=db; }
    public Task<LibraryBackup> load() {
        List<Task<QuerySnapshot>> reads=new ArrayList<>();
        for(String name:LibraryBackup.COLLECTIONS) reads.add(db.collection(name).get(Source.SERVER));
        return Tasks.whenAllSuccess(reads).continueWith(WORKER,done -> {
            done.getResult(); LibraryBackup data=new LibraryBackup();
            for(int i=0;i<reads.size();i++) for(DocumentSnapshot doc:reads.get(i).getResult()) data.collections.get(LibraryBackup.COLLECTIONS.get(i)).put(doc.getId(),doc.getData());
            return data;
        });
    }
    public interface Progress { void saved(int count); }
    public Task<Integer> restore(LibraryBackup backup,RestorePlan approved,Progress progress) {
        if(!approved.canRestore()) return Tasks.forException(new IllegalStateException("Resolve conflicts before restoring"));
        return load().continueWithTask(WORKER,read -> {
            LibraryBackup current=read.getResult(); RestorePlan fresh=new RestorePlan(backup,current);
            if(!fresh.canRestore() || !fresh.currentFingerprint.equals(approved.currentFingerprint))
                throw new IllegalStateException("Library changed since the preview. Select the backup again to review it.");
            if(fresh.missing.isEmpty() && fresh.counterChanges==0) return Tasks.forResult(0);
            // Reserve positions first. Ordinary app additions/moves use the same counters.
            // Any later counter change stops remaining chunks instead of risking a collision.
            Map<String,Map<String,Object>> reserved=new TreeMap<>();
            Task<Integer> chain=db.runTransaction(tx -> {
                List<DocumentSnapshot> counters=new ArrayList<>();
                for(int i=1;i<=8;i++) counters.add(tx.get(db.collection("cubePositions").document(""+i)));
                for(int i=1;i<=8;i++) {
                    DocumentSnapshot doc=counters.get(i-1); Map<String,Object> old=doc.exists() ? doc.getData() : null;
                    if(!BackupJson.same(current.get("cubePositions",""+i),old)) throw changed();
                }
                for(int i=1;i<=8;i++) {
                    DocumentSnapshot doc=counters.get(i-1); Map<String,Object> value=new TreeMap<>(); if(doc.exists()) value.putAll(doc.getData());
                    long old=LibraryBackup.number(value.get("lastPosition"),0), target=fresh.counters[i-1];
                    if(target>old) { value.put("lastPosition",target); tx.set(doc.getReference(),Collections.singletonMap("lastPosition",target),SetOptions.merge()); }
                    reserved.put(""+i,doc.exists() || target>old ? value : null);
                }
                return 0;
            });
            // Single-record transactions stay within private archive rule-expression limits and allow retry.
            // Earlier chunks contain works/editions/history before copies refer to them.
            for(int start=0;start<fresh.missing.size();start+=1) {
                List<String> chunk=new ArrayList<>(fresh.missing.subList(start,Math.min(start+1,fresh.missing.size())));
                chain=chain.continueWithTask(WORKER,previous -> {
                    int completed=previous.getResult();
                    return db.runTransaction(tx -> {
                        Map<String,DocumentSnapshot> reads=new LinkedHashMap<>();
                        for(int i=1;i<=8;i++) reads.put("cubePositions/"+i,tx.get(db.collection("cubePositions").document(""+i)));
                        Set<String> keys=new LinkedHashSet<>(chunk);
                        for(String key:chunk) {
                            String[] path=key.split("/",2); Map<String,Object> fields=backup.get(path[0],path[1]);
                            if(path[0].equals("bookCopies")) {
                                String edition=LibraryBackup.text(fields.get("editionId")); keys.add("editions/"+edition);
                                keys.add("works/"+LibraryBackup.text(backup.get("editions",edition).get("workId")));
                            }
                            if(path[0].equals("editions")) keys.add("works/"+LibraryBackup.text(fields.get("workId")));
                            if(path[0].equals("books") || path[0].equals("bookCopies")) {
                                String loan=LibraryBackup.text(fields.get("activeLoanId")); if(!loan.isEmpty()) keys.add("lendingRecords/"+loan);
                                keys.add((path[0].equals("books") ? "bookCopies/" : "books/")+path[1]);
                            }
                        }
                        for(String key:keys) reads.put(key,tx.get(db.document(key)));
                        for(int i=1;i<=8;i++) { DocumentSnapshot doc=reads.get("cubePositions/"+i); if(!BackupJson.same(reserved.get(""+i),doc.exists()?doc.getData():null)) throw changed(); }
                        for(String key:keys) {
                            String[] path=key.split("/",2); Map<String,Object> expected=backup.get(path[0],path[1]); DocumentSnapshot doc=reads.get(key);
                            if(expected==null) { if(doc.exists()) throw changed(); }
                            else if(doc.exists()) { if(!BackupJson.same(expected,doc.getData())) throw changed(); }
                            else if(!chunk.contains(key)) throw changed();
                        }
                        int added=0;
                        for(String key:chunk) { DocumentSnapshot doc=reads.get(key); if(!doc.exists()) { String[] path=key.split("/",2); tx.set(doc.getReference(),backup.get(path[0],path[1])); added++; } }
                        return completed+added;
                    }).continueWith(WORKER,saved -> { int count=saved.getResult(); progress.saved(count); return count; });
                });
            }
            return chain;
        });
    }
    private static IllegalStateException changed() { return new IllegalStateException("Library changed during restore. Completed additions are safe. Select the backup again to review and resume."); }
}
