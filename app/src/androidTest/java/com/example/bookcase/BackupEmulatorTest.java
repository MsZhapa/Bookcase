package com.example.bookcase;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.firebase.*;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.*;
import static org.junit.Assert.*;

/** Requires the local Firestore emulator. No test can use the production Firebase app. */
@RunWith(AndroidJUnit4.class)
public class BackupEmulatorTest {
    private FirebaseApp app;
    private FirebaseFirestore db;
    private BackupRepository repository;
    private static <T> T waitFor(Task<T> task) throws Exception { return Tasks.await(task,60,TimeUnit.SECONDS); }
    @Before public void setUp() {
        String project="demo-bookcase-"+UUID.randomUUID().toString().substring(0,8);
        app=FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                new FirebaseOptions.Builder().setApplicationId("1:123456:android:backup-test").setProjectId(project).setApiKey("fake-emulator-key").build(),project);
        db=FirebaseFirestore.getInstance(app);
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        db.useEmulator("10.0.2.2",8080);
        repository=new BackupRepository(db);
    }
    @After public void tearDown() throws Exception { if(db!=null) waitFor(db.terminate()); if(app!=null) app.delete(); }
    static Map<String,Object> fields(Object... pairs) { Map<String,Object> out=new TreeMap<>(); for(int i=0;i<pairs.length;i+=2) out.put((String)pairs[i],pairs[i+1]); return out; }
    private static LibraryBackup fixture(int copies) {
        LibraryBackup b=new LibraryBackup(); b.cubeNames[4]="Travel"; b.columns=3;
        for(int i=0;i<copies;i++) b.collections.get("books").put(String.format(java.util.Locale.ROOT,"old%04d",i),fields("title","Test book "+i,"author","Author","location",1L,"position",(long)i+1,"progress",0L,"read",false,"isbn","9780306406157"));
        b.collections.get("works").put("w",fields("id","w","title","A book"));
        b.collections.get("editions").put("e",fields("id","e","workId","w","title","A book","isbn","9780306406157"));
        for(int i=0;i<2;i++) b.collections.get("bookCopies").put("copy"+i,fields("id","copy"+i,"editionId","e","cubeNumber",5L,"position",12L+i,"notes","Keep my notes","currentPage",100L,"readingPageCount",400L,"progress",25L,"read",false));
        b.get("bookCopies","copy0").putAll(fields("activeLoanId","loan","lendingStatus","LENT","borrowerName","Friend","lentDate",new Timestamp(1700000000,123456000)));
        b.collections.get("lendingRecords").put("loan",fields("bookId","copy0","bookCopyId","copy0","status","ACTIVE","lentDate",new Timestamp(1700000000,123456000)));
        WishItem wish=WishItem.create("Wanted","Author",""); b.collections.get("wishlist").put(wish.getId(),fields("id",wish.getId(),"title",wish.getTitle(),"author",wish.getAuthor(),"isbn","","createdAt",1L));
        return b;
    }
    @Test public void restoresLargeLibraryAndReimportIsIdempotent() throws Exception {
        LibraryBackup original=fixture(507); StringWriter file=new StringWriter(); BackupJson.write(file,original);
        LibraryBackup backup=BackupJson.read(new StringReader(file.toString())), before=waitFor(repository.load());
        RestorePlan plan=new RestorePlan(backup,before); assertTrue(plan.canRestore());
        assertEquals(plan.missing.size(),(int)waitFor(repository.restore(backup,plan,count -> {})));
        LibraryBackup actual=waitFor(repository.load()); assertEquals(509,actual.copies());
        for(String source:LibraryBackup.COLLECTIONS) for(Map.Entry<String,Map<String,Object>> entry:backup.collections.get(source).entrySet()) assertTrue(source+"/"+entry.getKey(),BackupJson.same(entry.getValue(),actual.get(source,entry.getKey())));
        assertEquals(507L,actual.get("cubePositions","1").get("lastPosition")); assertEquals(13L,actual.get("cubePositions","5").get("lastPosition"));
        RestorePlan repeated=new RestorePlan(backup,actual); assertTrue(repeated.missing.isEmpty());
        String fingerprint=BackupJson.fingerprint(actual); assertEquals(0,(int)waitFor(repository.restore(backup,repeated,count -> fail("Nothing should be written"))));
        assertEquals(fingerprint,BackupJson.fingerprint(waitFor(repository.load())));
    }
    @Test public void conflictingExistingRecordBlocksAllWrites() throws Exception {
        LibraryBackup backup=fixture(1); waitFor(db.collection("books").document("old0000").set(fields("title","Changed","location",1L,"position",1L,"notes","Newer note")));
        LibraryBackup before=waitFor(repository.load()); RestorePlan plan=new RestorePlan(backup,before); assertFalse(plan.canRestore());
        try { waitFor(repository.restore(backup,plan,count -> {})); fail("Conflict must stop restore"); } catch(java.util.concurrent.ExecutionException expected) { }
        assertEquals(BackupJson.fingerprint(before),BackupJson.fingerprint(waitFor(repository.load())));
    }
    @Test public void libraryChangedAfterPreviewRequiresAnotherPreview() throws Exception {
        LibraryBackup backup=fixture(1), before=waitFor(repository.load()); RestorePlan plan=new RestorePlan(backup,before);
        waitFor(db.collection("books").document("new").set(fields("title","New book","location",2L,"position",1L)));
        String fingerprint=BackupJson.fingerprint(waitFor(repository.load()));
        try { waitFor(repository.restore(backup,plan,count -> {})); fail("Stale preview must stop restore"); } catch(java.util.concurrent.ExecutionException expected) { }
        assertEquals(fingerprint,BackupJson.fingerprint(waitFor(repository.load())));
    }
    @Test public void interruptedRestoreResumesWithoutDuplicatingCompletedChunks() throws Exception {
        LibraryBackup backup=fixture(220); RestorePlan plan=new RestorePlan(backup,waitFor(repository.load()));
        try { waitFor(repository.restore(backup,plan,count -> { throw new IllegalStateException("Simulate interruption after committed chunk"); })); fail(); } catch(java.util.concurrent.ExecutionException expected) { }
        LibraryBackup partial=waitFor(repository.load()); assertTrue(partial.copies()>0 && partial.copies()<backup.copies());
        RestorePlan remaining=new RestorePlan(backup,partial); assertTrue(remaining.canRestore()); assertTrue(remaining.matching>0);
        waitFor(repository.restore(backup,remaining,count -> {})); LibraryBackup actual=waitFor(repository.load()); assertEquals(backup.copies(),actual.copies());
        assertTrue(new RestorePlan(backup,actual).missing.isEmpty());
    }
    @Test public void concurrentPositionCounterChangeStopsRemainingChunks() throws Exception {
        LibraryBackup backup=fixture(220); RestorePlan plan=new RestorePlan(backup,waitFor(repository.load()));
        Task<Integer> restore=repository.restore(backup,plan,count -> { if(count==100) { try { waitFor(db.collection("cubePositions").document("1").update("lastPosition",999L)); } catch(Exception e) { throw new RuntimeException(e); } } });
        try { waitFor(restore); fail("Concurrent move/add must stop remaining chunks"); } catch(java.util.concurrent.ExecutionException expected) { }
        LibraryBackup actual=waitFor(repository.load()); assertEquals(999L,actual.get("cubePositions","1").get("lastPosition"));
        assertTrue(actual.copies()<backup.copies());
        RestorePlan resume=new RestorePlan(backup,actual); assertTrue(resume.canRestore()); waitFor(repository.restore(backup,resume,count -> {}));
        assertEquals(999L,waitFor(repository.load()).get("cubePositions","1").get("lastPosition"));
    }

    @Test public void matchingCopiesWithMissingCountersDoNotGetDuplicated() throws Exception {
        LibraryBackup backup=fixture(2); RestorePlan plan=new RestorePlan(backup,waitFor(repository.load()));
        waitFor(repository.restore(backup,plan,count -> {}));
        // Only isolated emulator fixtures are removed; production is never connected here.
        waitFor(db.collection("cubePositions").document("5").delete());
        LibraryBackup current=waitFor(repository.load()); RestorePlan repair=new RestorePlan(backup,current);
        assertTrue(repair.missing.isEmpty()); assertEquals(1,repair.counterChanges);
        assertEquals(0,(int)waitFor(repository.restore(backup,repair,count -> {})));
        LibraryBackup actual=waitFor(repository.load()); assertEquals(4,actual.copies()); assertEquals(13L,actual.get("cubePositions","5").get("lastPosition"));
    }
}
