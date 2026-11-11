package com.example.bookcase;

import com.google.android.gms.tasks.*;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;

/** Source reads only. All writes are create-only and target a separate Firestore instance. */
public final class MigrationRepository {
    private static final java.util.concurrent.Executor WORKER=java.util.concurrent.Executors.newSingleThreadExecutor();
    private final FirebaseFirestore source,target;
    public MigrationRepository() { this(FirebaseFirestore.getInstance(),BookcaseDatabase.v2()); }
    MigrationRepository(FirebaseFirestore source,FirebaseFirestore target) {
        if(source==target) throw new IllegalArgumentException("Migration requires a separate database"); this.source=source; this.target=target;
    }
    public Task<MigrationPlan> preview() { return new BackupRepository(source).load().continueWith(WORKER,read -> new MigrationPlan(read.getResult())); }
    public Task<Void> stage(MigrationPlan approved,BackupRepository.Progress progress) {
        if(!approved.ready()) return Tasks.forException(new IllegalStateException("Resolve migration blockers first"));
        return new BackupRepository(source).load().continueWithTask(WORKER,read -> {
            if(!BackupJson.fingerprint(read.getResult()).equals(approved.sourceFingerprint)) throw new IllegalStateException("Current library changed. Refresh the migration preview and save a new backup.");
            return new BackupRepository(target).load();
        }).continueWithTask(WORKER,read -> {
            LibraryBackup current=read.getResult();
            for(String collection:LibraryBackup.COLLECTIONS) for(String id:current.collections.get(collection).keySet())
                if(!approved.target.collections.get(collection).containsKey(id)) throw new IllegalStateException("The separate database contains unrelated records. Migration will not overwrite or delete them.");
            RestorePlan restore=new RestorePlan(approved.target,current);
            if(!restore.canRestore()) throw new IllegalStateException("The separate database contains conflicting records. Review it before retrying.");
            return new BackupRepository(target).restore(approved.target,restore,progress);
        }).continueWithTask(WORKER,done -> { done.getResult(); return new BackupRepository(target).load(); })
          .continueWithTask(WORKER,read -> { approved.verify(read.getResult()); return new BackupRepository(source).load(); })
          .continueWith(WORKER,read -> {
              if(!approved.sourceFingerprint.equals(BackupJson.fingerprint(read.getResult()))) throw new IllegalStateException("V2 copy was created, but the current library changed during migration. It is not ready for switching. Refresh the preview.");
              return null;
          });
    }
}
