package com.example.bookcase;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
/** Library screens close on sign-out or an account change. */
public class BookcaseActivity extends AppCompatActivity {
    private boolean redirecting;
    private String sessionUid;
    @Override protected void onCreate(android.os.Bundle state) {super.onCreate(state);com.google.firebase.auth.FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();sessionUid=user==null?null:user.getUid();}
    private final FirebaseAuth.AuthStateListener access=auth->checkAccess();
    @Override protected void onStart() { super.onStart(); checkAccess(); FirebaseAuth.getInstance().addAuthStateListener(access); }
    @Override protected void onStop() { FirebaseAuth.getInstance().removeAuthStateListener(access); super.onStop(); }
    private void checkAccess() {
        if(!redirecting && (!BookcaseDatabase.sessionAllowed(this) || !java.util.Objects.equals(sessionUid,FirebaseAuth.getInstance().getCurrentUser()==null?null:FirebaseAuth.getInstance().getCurrentUser().getUid()))) {
            redirecting=true; startActivity(new Intent(this,AccountActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish();
        }
    }
}
