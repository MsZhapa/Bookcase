package com.example.bookcase;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
/** Private screens close when the allowed Google session ends. */
public class BookcaseActivity extends AppCompatActivity {
    private boolean redirecting;
    private final FirebaseAuth.AuthStateListener access=auth->checkAccess();
    @Override protected void onStart() { super.onStart(); checkAccess(); FirebaseAuth.getInstance().addAuthStateListener(access); }
    @Override protected void onStop() { FirebaseAuth.getInstance().removeAuthStateListener(access); super.onStop(); }
    private void checkAccess() {
        if(!redirecting && !BookcaseDatabase.sessionAllowed(this)) {
            redirecting=true; startActivity(new Intent(this,AccountActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish();
        }
    }
}
