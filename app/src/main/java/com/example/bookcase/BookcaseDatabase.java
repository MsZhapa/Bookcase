package com.example.bookcase;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

/** One routing point. Private V2 is enabled only after signed-in verification. */
public final class BookcaseDatabase {
    private BookcaseDatabase() {}
    private static FirebaseFirestore privateDatabase;
    public static boolean active(Context c) { return c.getSharedPreferences("bookcase_database",Context.MODE_PRIVATE).getBoolean("v2",false); }
    public static FirebaseFirestore current() {
        return active(FirebaseApp.getInstance().getApplicationContext()) ? v2() : FirebaseFirestore.getInstance();
    }
    public static synchronized FirebaseFirestore v2() {
        if(privateDatabase==null) {
            privateDatabase=FirebaseFirestore.getInstance(FirebaseApp.getInstance(),"books");
            // Avoid retaining another account's private library in an on-disk Firestore cache.
            privateDatabase.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        }
        return privateDatabase;
    }
    static void activate(Context c,FirebaseUser user) {
        if(user==null || !user.isEmailVerified()) throw new IllegalStateException("Sign in before switching");
        c.getSharedPreferences("bookcase_database",Context.MODE_PRIVATE).edit().putBoolean("v2",true).putString("ownerUid",user.getUid()).apply();
    }
    public static boolean sessionAllowed(Context c) {
        if(!active(c)) return false;
        FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser(); boolean google=false;
        if(user!=null) for(UserInfo provider:user.getProviderData()) if("google.com".equals(provider.getProviderId())) google=true;
        return CatalogueIdentity.session(true,c.getSharedPreferences("bookcase_database",Context.MODE_PRIVATE).getString("ownerUid",""),user==null?null:user.getUid(),user!=null && user.isEmailVerified(),google);
    }
}
