package com.example.bookcase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.Task;

/** A repository captures its account path once, so an account switch cannot redirect pending writes. */
public final class LibraryStore {
    private final FirebaseFirestore db;
    private final String prefix;
    public LibraryStore(FirebaseFirestore db) {
        this(db, FirebaseApp.DEFAULT_APP_NAME.equals(db.getApp().getName()) ? currentUid(db) : null);
    }
    public LibraryStore(FirebaseFirestore db,String uid) {
        this.db=db;
        if(uid!=null && (uid.isEmpty() || uid.contains("/"))) throw new IllegalArgumentException("Invalid account");
        prefix=uid==null ? "" : "users/"+uid+"/";
    }
    private static String currentUid(FirebaseFirestore db) {
        FirebaseUser user=FirebaseAuth.getInstance(db.getApp()).getCurrentUser();
        return user==null ? "_signed_out_" : user.getUid();
    }
    public CollectionReference collection(String name) { return db.collection(prefix+name); }
    public DocumentReference document(String name) { return db.document(prefix+name); }
    public <T> Task<T> runTransaction(Transaction.Function<T> work) { return db.runTransaction(work); }
}
