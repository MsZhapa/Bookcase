package com.example.bookcase;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.*;
import androidx.credentials.exceptions.*;
import com.google.android.libraries.identity.googleid.*;
import com.google.firebase.auth.*;

/** Public welcome, sign-in and registration screen. */
public class AccountActivity extends AppCompatActivity {
 private android.os.CancellationSignal request;
 private boolean registering=false,working=false,opening=false;
 private EditText email,password,confirmation;
 private final FirebaseAuth.AuthStateListener authState=auth->{if(auth.getCurrentUser()!=null&&!opening)openLibrary();};
 @Override protected void onCreate(Bundle state){
  super.onCreate(state);setContentView(R.layout.activity_account);
  getWindow().setStatusBarColor(android.graphics.Color.parseColor("#F7F3EB"));
  getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#F7F3EB"));
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  email=findViewById(R.id.emailInput);password=findViewById(R.id.passwordInput);confirmation=findViewById(R.id.confirmPasswordInput);
  registering=state!=null&&state.getBoolean("registering");render();
  findViewById(R.id.emailSignInButton).setOnClickListener(v->submit());
  findViewById(R.id.toggleRegisterButton).setOnClickListener(v->{registering=!registering;password.setText("");confirmation.setText("");message("");render();});
  findViewById(R.id.googleSignInButton).setOnClickListener(v->signIn());
  findViewById(R.id.resetPasswordButton).setOnClickListener(v->reset());
 }
 @Override protected void onStart(){super.onStart();FirebaseAuth.getInstance().addAuthStateListener(authState);}
 @Override protected void onStop(){FirebaseAuth.getInstance().removeAuthStateListener(authState);super.onStop();}
 @Override protected void onSaveInstanceState(Bundle out){out.putBoolean("registering",registering);super.onSaveInstanceState(out);}
 private void render(){
  ((TextView)findViewById(R.id.authHeading)).setText(registering?R.string.create_account:R.string.welcome_back);
  ((Button)findViewById(R.id.emailSignInButton)).setText(registering?R.string.create_account:R.string.log_in);
  ((Button)findViewById(R.id.toggleRegisterButton)).setText(registering?R.string.have_account:R.string.need_account);
  confirmation.setVisibility(registering?View.VISIBLE:View.GONE);
  findViewById(R.id.resetPasswordButton).setVisibility(registering?View.GONE:View.VISIBLE);
 }
 private void busy(boolean value){working=value;for(int id:new int[]{R.id.emailSignInButton,R.id.googleSignInButton,R.id.toggleRegisterButton,R.id.resetPasswordButton,R.id.emailInput,R.id.passwordInput,R.id.confirmPasswordInput})findViewById(id).setEnabled(!value);findViewById(R.id.authProgress).setVisibility(value?View.VISIBLE:View.GONE);}
 private void message(String text){if(isFinishing()||isDestroyed())return;busy(false);TextView status=findViewById(R.id.accountStatus);status.setText(text);status.setVisibility(text.isEmpty()?View.GONE:View.VISIBLE);}
 private String address(){return email.getText().toString().trim();}
 private boolean validEmail(){if(!android.util.Patterns.EMAIL_ADDRESS.matcher(address()).matches()){email.setError(getString(R.string.valid_email));return false;}return true;}
 private void submit(){
  if(working||!validEmail())return;String secret=password.getText().toString();
  if(secret.isEmpty()){password.setError(getString(R.string.enter_password));return;}
  if(registering&&secret.length()<6){password.setError(getString(R.string.password_length));return;}
  if(registering&&!secret.equals(confirmation.getText().toString())){confirmation.setError(getString(R.string.password_mismatch));return;}
  message("");busy(true);
  com.google.android.gms.tasks.Task<AuthResult> task=registering?FirebaseAuth.getInstance().createUserWithEmailAndPassword(address(),secret):FirebaseAuth.getInstance().signInWithEmailAndPassword(address(),secret);
  task.addOnSuccessListener(result->openLibrary()).addOnFailureListener(this::failed);
 }
 private void failed(Exception error){
  if(error instanceof FirebaseAuthUserCollisionException)message(getString(R.string.account_exists));
  else if(error instanceof FirebaseAuthWeakPasswordException)message(getString(R.string.password_length));
  else if(error instanceof com.google.firebase.FirebaseNetworkException)message(getString(R.string.auth_connection_error));
  else if(error instanceof com.google.firebase.FirebaseTooManyRequestsException)message(getString(R.string.auth_rate_limit));
  else message(getString(registering?R.string.registration_failed:R.string.login_failed));
 }
 private void reset(){if(working||!validEmail())return;message("");busy(true);FirebaseAuth.getInstance().sendPasswordResetEmail(address()).addOnSuccessListener(v->message(getString(R.string.reset_sent))).addOnFailureListener(this::failed);}
 private void openLibrary(){
  FirebaseUser expected=FirebaseAuth.getInstance().getCurrentUser();
  if(opening||isFinishing()||isDestroyed()||expected==null)return;
  String uid=expected.getUid();opening=true;busy(true);
  BookcaseDatabase.store().collection("bookCopies").limit(1).get(com.google.firebase.firestore.Source.SERVER).addOnSuccessListener(data->{
   if(isFinishing()||isDestroyed())return;
   FirebaseUser current=FirebaseAuth.getInstance().getCurrentUser();
   if(current==null||!uid.equals(current.getUid())){opening=false;if(current!=null)openLibrary();return;}
   password.setText("");confirmation.setText("");
   ((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(password.getWindowToken(),0);
   startActivity(new Intent(this,CubesActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));finish();
  }).addOnFailureListener(error->{opening=false;message(getString(R.string.library_open_failed));});
 }
    private void signIn() {
        int resource=getResources().getIdentifier("default_web_client_id","string",getPackageName());
        if(resource==0) { message("Google sign-in configuration is unavailable. Update Bookcase and try again."); return; }
        message(""); busy(true);
        byte[] nonceBytes=new byte[32]; new java.security.SecureRandom().nextBytes(nonceBytes);
        String nonce=android.util.Base64.encodeToString(nonceBytes,android.util.Base64.URL_SAFE|android.util.Base64.NO_WRAP|android.util.Base64.NO_PADDING);
        GetSignInWithGoogleOption google=new GetSignInWithGoogleOption.Builder(getString(resource)).setNonce(nonce).build();
        GetCredentialRequest credentials=new GetCredentialRequest.Builder().addCredentialOption(google).build();
        request=new android.os.CancellationSignal();
        CredentialManager.create(this).getCredentialAsync(this,credentials,request,ContextCompat.getMainExecutor(this),new CredentialManagerCallback<GetCredentialResponse,GetCredentialException>() {
            public void onResult(GetCredentialResponse response) {
                if(isDestroyed()||isFinishing())return; Credential credential=response.getCredential();
                if(credential instanceof CustomCredential && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                    try {
                        String token=GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
                        String payload=new String(android.util.Base64.decode(token.split("\\.")[1],android.util.Base64.URL_SAFE),java.nio.charset.StandardCharsets.UTF_8);
                        if(!nonce.equals(com.google.gson.JsonParser.parseString(payload).getAsJsonObject().get("nonce").getAsString())) throw new IllegalArgumentException("Unexpected sign-in response");
                        FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token,null)).addOnSuccessListener(result->openLibrary()).addOnFailureListener(thisError->failed(thisError));
                    }
                    catch(Exception error) { message("Could not read the Google sign-in response. Please try again."); }
                } else message("Choose a Google account to continue.");
            }
            public void onError(GetCredentialException error) {
                message(error instanceof GetCredentialCancellationException ? "Sign-in cancelled." : "Google sign-in is unavailable. Add your Google account to this device, check your connection and try again.");
            }
        });
    }

 @Override protected void onDestroy(){if(request!=null)request.cancel();super.onDestroy();}
}
