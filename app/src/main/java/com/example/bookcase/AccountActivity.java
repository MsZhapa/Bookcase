package com.example.bookcase;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.*;
import androidx.credentials.exceptions.*;
import androidx.lifecycle.*;
import com.google.android.libraries.identity.googleid.*;
import com.google.firebase.auth.*;

public class AccountActivity extends AppCompatActivity {
    private AccountModel model;
    private android.os.CancellationSignal request;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_account);
        ((TextView)findViewById(R.id.screenTitle)).setText(R.string.account_title);
        findViewById(R.id.backButton).setOnClickListener(v->{if(!BookcaseDatabase.active(this) || BookcaseDatabase.sessionAllowed(this)) finish();});
        model=new ViewModelProvider(this).get(AccountModel.class);
        findViewById(R.id.googleSignInButton).setOnClickListener(v->signIn());
        findViewById(R.id.activateV2Button).setOnClickListener(v->model.activate());
        findViewById(R.id.signOutButton).setOnClickListener(v->{
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this,AccountActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish();
        });
        model.state.observe(this,result->{
            ((TextView)findViewById(R.id.accountStatus)).setText(result.message);
            findViewById(R.id.googleSignInButton).setEnabled(!result.busy);
            findViewById(R.id.activateV2Button).setEnabled(!result.busy && result.ready);
            ((Button)findViewById(R.id.activateV2Button)).setText(BookcaseDatabase.active(this) ? R.string.open_private_library : R.string.activate_private_library);
            findViewById(R.id.signOutButton).setVisibility(FirebaseAuth.getInstance().getCurrentUser()==null ? android.view.View.GONE : android.view.View.VISIBLE);
            if(result.open) { model.consumed(); startActivity(new Intent(this,WelcomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish(); }
        });
        if(state==null) model.check();
    }
    private void signIn() {
        int resource=getResources().getIdentifier("default_web_client_id","string",getPackageName());
        if(resource==0) { model.error("Google sign-in configuration is unavailable. Update Bookcase and try again."); return; }
        model.busy("Choose the Google account allowed to open your library.");
        byte[] nonceBytes=new byte[32]; new java.security.SecureRandom().nextBytes(nonceBytes);
        String nonce=android.util.Base64.encodeToString(nonceBytes,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
        GetSignInWithGoogleOption google=new GetSignInWithGoogleOption.Builder(getString(resource)).setNonce(nonce).build();
        GetCredentialRequest credentials=new GetCredentialRequest.Builder().addCredentialOption(google).build();
        request=new android.os.CancellationSignal();
        CredentialManager.create(this).getCredentialAsync(this,credentials,request,ContextCompat.getMainExecutor(this),new CredentialManagerCallback<GetCredentialResponse,GetCredentialException>() {
            public void onResult(GetCredentialResponse response) {
                Credential credential=response.getCredential();
                if(credential instanceof CustomCredential && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                    try {
                        String token=GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
                        String payload=new String(android.util.Base64.decode(token.split("\\.")[1],android.util.Base64.URL_SAFE),java.nio.charset.StandardCharsets.UTF_8);
                        if(!nonce.equals(com.google.gson.JsonParser.parseString(payload).getAsJsonObject().get("nonce").getAsString())) throw new IllegalArgumentException("Unexpected sign-in response");
                        model.authenticate(token);
                    }
                    catch(Exception error) { model.error("Could not read the Google sign-in response. Please try again."); }
                } else model.error("Choose a Google account to continue.");
            }
            public void onError(GetCredentialException error) {
                model.error(error instanceof GetCredentialCancellationException ? "Sign-in cancelled. Your library was not changed." : "Google sign-in is unavailable. Add your Google account to this device, check your connection and try again.");
            }
        });
    }
    @Override protected void onDestroy() { if(request!=null) request.cancel(); super.onDestroy(); }
    static final class State {
        final boolean busy,ready,open; final String message;
        State(boolean busy,boolean ready,boolean open,String message) {this.busy=busy;this.ready=ready;this.open=open;this.message=message;}
    }
    public static class AccountModel extends AndroidViewModel {
        final MutableLiveData<State> state=new MutableLiveData<>(new State(false,false,false,"Sign in with Google to open the private V2 library. Other accounts cannot access it. Your original library remains unchanged."));
        private static final java.util.concurrent.Executor worker=java.util.concurrent.Executors.newSingleThreadExecutor();
        private String verifiedUid;
        public AccountModel(Application app) { super(app); }
        void busy(String text) { verifiedUid=null; state.setValue(new State(true,false,false,text)); }
        void error(String text) { verifiedUid=null; state.postValue(new State(false,false,false,text)); }
        void authenticate(String token) {
            busy("Signing in and checking private library access…");
            FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token,null)).addOnSuccessListener(result->check()).addOnFailureListener(error->error("Google sign-in failed. Check your connection and try again."));
        }
        private static class Proof {
            final int count; final boolean activated; final String source,target;
            Proof(int count,boolean activated,String source,String target) { this.count=count;this.activated=activated;this.source=source;this.target=target; }
        }
        private com.google.android.gms.tasks.Task<Proof> verify() {
            return new BackupRepository(BookcaseDatabase.v2()).load().continueWithTask(worker,read->{
                LibraryBackup target=read.getResult(); target.validate();
                return BookcaseDatabase.v2().collection("libraryStatus").document("current").get(com.google.firebase.firestore.Source.SERVER).continueWithTask(worker,status->{
                    com.google.firebase.firestore.DocumentSnapshot receipt=status.getResult();
                    if(!receipt.exists() || !Long.valueOf(2).equals(receipt.getLong("schemaVersion")) || !Boolean.TRUE.equals(receipt.getBoolean("migrationVerified")))
                        throw new IllegalStateException("Private migration is not yet verified");
                    Proof proof=new Proof(target.copies(),Boolean.TRUE.equals(receipt.getBoolean("activated")),receipt.getString("sourceFingerprint"),receipt.getString("targetFingerprint"));
                    if(proof.activated) return com.google.android.gms.tasks.Tasks.forResult(proof);
                    return new BackupRepository(com.google.firebase.firestore.FirebaseFirestore.getInstance()).load().continueWith(worker,original->{
                        MigrationReadiness.verify(false,proof.source,proof.target,BackupJson.fingerprint(original.getResult()),BackupJson.fingerprint(target)); return proof;
                    });
                });
            });
        }
        void check() {
            FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser(); if(user==null) return;
            busy("Verifying private library access…");
            verify().addOnSuccessListener(proof->{
                FirebaseUser current=FirebaseAuth.getInstance().getCurrentUser(); if(current==null || !current.getUid().equals(user.getUid())) return;
                verifiedUid=user.getUid(); state.setValue(new State(false,true,false,"Signed in as "+user.getEmail()+".\n\nPrivate V2 library verified: "+proof.count+" physical copies.\n\n"+(BookcaseDatabase.active(getApplication()) ? "Your private library is active." : "You can now switch to V2. The original library will be retained unchanged.")));
            }).addOnFailureListener(this::failedVerification);
        }
        private void failedVerification(Exception error) {
            verifiedUid=null; boolean denied=false; String detail=null;
            for(Throwable cause=error;cause!=null;cause=cause.getCause()) {
                if(cause instanceof com.google.firebase.firestore.FirebaseFirestoreException && ((com.google.firebase.firestore.FirebaseFirestoreException)cause).getCode()==com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) denied=true;
                if(cause instanceof IllegalStateException) detail=cause.getMessage();
            }
            if(denied) { FirebaseAuth.getInstance().signOut(); error("This Google account does not have access to your library. Choose the account you specified for Bookcase."); }
            else error(detail!=null ? detail : "Could not verify the private library. Check your connection and try signing in again. No database switch was made.");
        }
        void activate() {
            FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
            if(user==null || verifiedUid==null || !verifiedUid.equals(user.getUid())) { error("Sign in and verify access before switching."); return; }
            String approvedUid=user.getUid(); busy("Rechecking the library before switching…");
            verify().continueWithTask(worker,verified->{
                Proof proof=verified.getResult();
                if(proof.activated) return com.google.android.gms.tasks.Tasks.forResult(null);
                return BookcaseDatabase.v2().runTransaction(tx->{
                    com.google.firebase.firestore.DocumentReference ref=BookcaseDatabase.v2().collection("libraryStatus").document("current");
                    com.google.firebase.firestore.DocumentSnapshot current=tx.get(ref);
                    if(!Boolean.TRUE.equals(current.getBoolean("migrationVerified")) || !java.util.Objects.equals(proof.source,current.getString("sourceFingerprint")) || !java.util.Objects.equals(proof.target,current.getString("targetFingerprint"))) throw new IllegalStateException("Migration changed. Verify it again before switching.");
                    if(!Boolean.TRUE.equals(current.getBoolean("activated"))) {
                        java.util.Map<String,Object> fields=new java.util.HashMap<>(); fields.put("activated",true); fields.put("activatedAt",com.google.firebase.firestore.FieldValue.serverTimestamp()); tx.update(ref,fields);
                    }
                    return null;
                });
            }).addOnSuccessListener(unused->{
                FirebaseUser current=FirebaseAuth.getInstance().getCurrentUser(); if(current==null || !approvedUid.equals(current.getUid())) { error("Sign in again to open the private library."); return; }
                BookcaseDatabase.activate(getApplication(),current); state.setValue(new State(false,true,true,"Private library active."));
            }).addOnFailureListener(this::failedVerification);
        }
        void consumed() { State s=state.getValue(); state.setValue(new State(s.busy,s.ready,false,s.message)); }
    }
}
