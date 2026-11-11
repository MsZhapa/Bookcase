package com.example.bookcase;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

/** All accounts use the same UID-scoped library structure. */
public final class BookcaseDatabase {
    private BookcaseDatabase() {}
    private static FirebaseFirestore database;
    public static FirebaseFirestore current() { return v2(); }
    public static LibraryStore store() { return new LibraryStore(current()); }
    public static synchronized FirebaseFirestore v2() {
        if(database==null) {
            database=FirebaseFirestore.getInstance(FirebaseApp.getInstance(),"books");
            database.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        }
        return database;
    }
    public static boolean active(Context c) { return sessionAllowed(c); }
    public static boolean sessionAllowed(Context c) { return FirebaseAuth.getInstance().getCurrentUser()!=null; }
}
