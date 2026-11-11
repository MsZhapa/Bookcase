package com.example.bookcase;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Real welcome/register/login UI and repositories, exclusively on localhost demo services. */
@RunWith(AndroidJUnit4.class)
public class AccountsEmulatorTest {
 private static <T>T waitFor(Task<T> task)throws Exception{return Tasks.await(task,60,TimeUnit.SECONDS);}
 private static boolean clickText(android.view.accessibility.AccessibilityNodeInfo node,String text){
  if(node==null)return false;
  if(text.contentEquals(node.getText()==null?"":node.getText())){
   android.graphics.Rect bounds=new android.graphics.Rect();node.getBoundsInScreen(bounds);
   if(bounds.isEmpty()||!node.isVisibleToUser())return false;
   long now=android.os.SystemClock.uptimeMillis();
   Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
   instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now,android.view.MotionEvent.ACTION_DOWN,bounds.centerX(),bounds.centerY(),0));
   instrumentation.sendPointerSync(android.view.MotionEvent.obtain(now,now+50,android.view.MotionEvent.ACTION_UP,bounds.centerX(),bounds.centerY(),0));return true;
  }
  for(int c=0;c<node.getChildCount();c++)if(clickText(node.getChild(c),text))return true;
  return false;
 }
 @Test public void registrationLoginLogoutAndAccountIsolation()throws Exception {
  Instrumentation i=InstrumentationRegistry.getInstrumentation();
  FirebaseApp.getInstance().delete();
  FirebaseApp app=FirebaseApp.initializeApp(i.getTargetContext(),new FirebaseOptions.Builder().setApplicationId("1:123456:android:accounts-test").setApiKey("fake-api-key").setProjectId("demo-bookcase-accounts").build());
  FirebaseAuth auth=FirebaseAuth.getInstance(app);auth.useEmulator("127.0.0.1",9099);auth.signOut();
  FirebaseFirestore db=FirebaseFirestore.getInstance(app);db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());db.useEmulator("127.0.0.1",8080);
  java.lang.reflect.Field field=BookcaseDatabase.class.getDeclaredField("database");field.setAccessible(true);field.set(null,db);
  String address="ui-"+UUID.randomUUID()+"@example.test",password="Test-password-123";
  Activity screen=i.startActivitySync(new Intent(i.getTargetContext(),WelcomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  i.runOnMainSync(()->{
   assertNotNull(screen.findViewById(R.id.googleSignInButton));
   ((Button)screen.findViewById(R.id.emailSignInButton)).performClick();
   assertNotNull(((EditText)screen.findViewById(R.id.emailInput)).getError());
   screen.findViewById(R.id.toggleRegisterButton).performClick();
   assertEquals(android.view.View.VISIBLE,screen.findViewById(R.id.confirmPasswordInput).getVisibility());
   ((EditText)screen.findViewById(R.id.emailInput)).setText(address);
   ((EditText)screen.findViewById(R.id.passwordInput)).setText(password);
   ((EditText)screen.findViewById(R.id.confirmPasswordInput)).setText("different");
   screen.findViewById(R.id.emailSignInButton).performClick();
   assertNotNull(((EditText)screen.findViewById(R.id.confirmPasswordInput)).getError());
   ((EditText)screen.findViewById(R.id.confirmPasswordInput)).setText(password);
  });
  Instrumentation.ActivityMonitor cubes=i.addMonitor(CubesActivity.class.getName(),null,false);
  i.runOnMainSync(()->screen.findViewById(R.id.emailSignInButton).performClick());
  Activity first=cubes.waitForActivityWithTimeout(30000);assertNotNull("Registration opens cubes",first);
  String uid=auth.getCurrentUser().getUid();LibraryStore captured=BookcaseDatabase.store();BookRepository books=new BookRepository(captured);
  assertEquals(0,waitFor(books.getBooks(null)).size());
  Book book=new Book("shared-id","First user's book","Author","","",false,0,1);book.setIsbn("");waitFor(books.add(book));
  CubeSettings.save(i.getTargetContext(),new String[]{"First shelf","Cube 2","Cube 3","Cube 4","Cube 5","Cube 6","Cube 7","Cube 8"},3);
  i.runOnMainSync(()->{
   ((android.view.inputmethod.InputMethodManager)first.getSystemService(android.content.Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(first.getWindow().getDecorView().getWindowToken(),0);
   first.findViewById(R.id.libraryMenuButton).performClick();
  });
  // Menu includes sign out; invoke the same menu item through its visible popup.
  boolean clicked=false;for(int attempt=0;attempt<50&&!clicked;attempt++){clicked=clickText(i.getUiAutomation().getRootInActiveWindow(),"Sign out");if(!clicked)android.os.SystemClock.sleep(100);}
  assertTrue("Sign out is available in menu",clicked);i.waitForIdleSync();
  for(int attempt=0;attempt<50&&auth.getCurrentUser()!=null;attempt++)android.os.SystemClock.sleep(100);
  assertNull(auth.getCurrentUser());
  waitFor(auth.createUserWithEmailAndPassword("other-"+UUID.randomUUID()+"@example.test",password));
  assertNotEquals(uid,auth.getCurrentUser().getUid());
  assertEquals(0,waitFor(new BookRepository().getBooks(null)).size());
  assertEquals(2,CubeSettings.columns(i.getTargetContext()));assertEquals("Cube 1",CubeSettings.name(i.getTargetContext(),1));
  try{waitFor(books.getBooks(null));fail("Old account repository must not read after switching");}catch(java.util.concurrent.ExecutionException expected){}
  Book other=new Book("shared-id","Second user's book","Author","","",false,0,1);other.setIsbn("");waitFor(new BookRepository().add(other));
  assertEquals("Second user's book",waitFor(new BookRepository().getBooks(null)).get(0).getTitle());
  auth.signOut();
  Activity loginScreen=i.startActivitySync(new Intent(i.getTargetContext(),AccountActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
  Instrumentation.ActivityMonitor loggedIn=i.addMonitor(CubesActivity.class.getName(),null,false);
  i.runOnMainSync(()->{
   ((EditText)loginScreen.findViewById(R.id.emailInput)).setText(address);
   ((EditText)loginScreen.findViewById(R.id.passwordInput)).setText(password);
   loginScreen.findViewById(R.id.emailSignInButton).performClick();
  });
  assertNotNull("Email login opens cubes",loggedIn.waitForActivityWithTimeout(30000));i.removeMonitor(loggedIn);
  assertEquals(uid,auth.getCurrentUser().getUid());assertEquals("First user's book",waitFor(new BookRepository().getBooks(null)).get(0).getTitle());
  assertEquals(3,CubeSettings.columns(i.getTargetContext()));assertEquals("First shelf",CubeSettings.name(i.getTargetContext(),1));
  auth.signOut();i.removeMonitor(cubes);waitFor(db.terminate());field.set(null,null);
 }
}
