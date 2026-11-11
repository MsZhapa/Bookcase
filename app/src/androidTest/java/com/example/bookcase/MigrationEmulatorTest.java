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
import static org.junit.Assert.*;

/** Both source and target are isolated demo apps connected to localhost, never production. */
@RunWith(AndroidJUnit4.class)
public class MigrationEmulatorTest {
    private final List<FirebaseApp> apps=new ArrayList<>();
    private FirebaseFirestore source,target;
    private MigrationRepository migration;
    private static <T> T waitFor(Task<T> task) throws Exception { return Tasks.await(task,90,TimeUnit.SECONDS); }
    private FirebaseFirestore database() {
        String project="demo-v2-"+UUID.randomUUID().toString().substring(0,8);
        FirebaseApp app=FirebaseApp.initializeApp(InstrumentationRegistry.getInstrumentation().getTargetContext(),new FirebaseOptions.Builder().setApplicationId("1:123456:android:migration-test").setProjectId(project).setApiKey("fake-emulator-key").build(),project);
        apps.add(app); FirebaseFirestore db=FirebaseFirestore.getInstance(app);
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()); db.useEmulator("10.0.2.2",8080); return db;
    }
    @Before public void setup() { source=database(); target=database(); migration=new MigrationRepository(source,target); }
    @After public void teardown() throws Exception { if(source!=null) waitFor(source.terminate()); if(target!=null) waitFor(target.terminate()); for(FirebaseApp app:apps) app.delete(); }
    static Map<String,Object> fields(Object... pairs) { Map<String,Object> out=new TreeMap<>(); for(int i=0;i<pairs.length;i+=2) out.put((String)pairs[i],pairs[i+1]); return out; }
    private void seed(int legacy) throws Exception {
        LibraryBackup b=new LibraryBackup();
        for(int i=0;i<legacy;i++) b.collections.get("books").put(String.format(java.util.Locale.ROOT,"old%04d",i),fields("title","Book "+(i%170),"author","Author","location",(long)(i%8)+1,"position",0L,"read",false,"progress",0L,"notes","Original note"));
        b.collections.get("works").put("w",fields("id","w","title","Known Work","author","Author"));
        b.collections.get("editions").put("e",fields("id","e","workId","w","title","Known Work","author","Author","isbn","9780306406157","pageCount",400L));
        for(int i=0;i<2;i++) b.collections.get("bookCopies").put("copy"+i,fields("id","copy"+i,"editionId","e","cubeNumber",5L,"position",12L+i,"notes","Copy "+i,"currentPage",100L,"readingPageCount",400L,"progress",25L,"read",false));
        b.get("bookCopies","copy0").putAll(fields("activeLoanId","loan","lendingStatus","LENT","borrowerName","Friend","lentDate",new Timestamp(1700000000,123456000)));
        b.collections.get("lendingRecords").put("loan",fields("bookId","copy0","bookCopyId","copy0","status","ACTIVE","lentDate",new Timestamp(1700000000,123456000)));
        b.collections.get("cubePositions").put("5",fields("lastPosition",99L));
        WishItem wish=WishItem.create("Wanted","Author",""); b.collections.get("wishlist").put(wish.getId(),fields("id",wish.getId(),"title",wish.getTitle(),"author",wish.getAuthor(),"isbn","","createdAt",1L));
        BackupRepository repository=new BackupRepository(source); waitFor(repository.restore(b,new RestorePlan(b,waitFor(repository.load())),count->{}));
    }
    @Test public void largeMigrationPreservesHydratedCopiesAndOriginalDatabase() throws Exception {
        seed(507); LibraryBackup before=waitFor(new BackupRepository(source).load()); MigrationPlan plan=waitFor(migration.preview()); assertTrue(plan.ready());
        waitFor(migration.stage(plan,count->{})); LibraryBackup actual=waitFor(new BackupRepository(target).load()); plan.verify(actual);
        assertEquals(509,actual.copies()); assertTrue(actual.collections.get("books").isEmpty());
        assertEquals(BackupJson.fingerprint(before),BackupJson.fingerprint(waitFor(new BackupRepository(source).load())));
        List<Book> old=waitFor(new BookRepository(source).getBooks(null)), converted=waitFor(new BookRepository(target).getBooks(null)); assertEquals(old.size(),converted.size());
        for(int i=0;i<old.size();i++) {
            Book a=old.get(i),b=converted.get(i); assertEquals(a.getId(),b.getId()); assertTrue(b.isNormalizedCopy());
            assertEquals(a.getTitle(),b.getTitle()); assertEquals(a.getAuthor(),b.getAuthor()); assertEquals(a.getLocation(),b.getLocation()); assertEquals(a.getPosition(),b.getPosition());
            assertEquals(a.getNotes(),b.getNotes()); assertEquals(a.isRead(),b.isRead()); assertEquals(a.getProgress(),b.getProgress()); assertEquals(a.getCurrentPage(),b.getCurrentPage()); assertEquals(a.getReadingPageCount(),b.getReadingPageCount()); assertEquals(a.getBorrowerName(),b.getBorrowerName()); assertEquals(a.getLentDate(),b.getLentDate());
        }
        String fingerprint=BackupJson.fingerprint(actual); waitFor(migration.stage(plan,count->{})); assertEquals(fingerprint,BackupJson.fingerprint(waitFor(new BackupRepository(target).load())));
    }
    @Test public void unrelatedTargetIsNeverOverwritten() throws Exception {
        seed(1); MigrationPlan plan=waitFor(migration.preview()); waitFor(target.collection("books").document("unrelated").set(fields("title","Keep me","location",1L)));
        String before=BackupJson.fingerprint(waitFor(new BackupRepository(target).load()));
        try { waitFor(migration.stage(plan,count->{})); fail(); } catch(java.util.concurrent.ExecutionException expected) { }
        assertEquals(before,BackupJson.fingerprint(waitFor(new BackupRepository(target).load())));
    }
    @Test public void stalePreviewDoesNotWriteTarget() throws Exception {
        seed(1); MigrationPlan plan=waitFor(migration.preview()); waitFor(source.collection("books").document("old0000").update("notes","New note"));
        try { waitFor(migration.stage(plan,count->{})); fail(); } catch(java.util.concurrent.ExecutionException expected) { }
        assertEquals(0,waitFor(new BackupRepository(target).load()).documents()); assertEquals("New note",waitFor(source.collection("books").document("old0000").get()).getString("notes"));
    }
    @Test public void changedTargetBlocksRepeatedMigration() throws Exception {
        seed(1); MigrationPlan plan=waitFor(migration.preview()); waitFor(migration.stage(plan,count->{}));
        waitFor(target.collection("bookCopies").document("copy0").update("notes","Target note")); String before=BackupJson.fingerprint(waitFor(new BackupRepository(target).load()));
        try { waitFor(migration.stage(plan,count->{})); fail(); } catch(java.util.concurrent.ExecutionException expected) { }
        assertEquals(before,BackupJson.fingerprint(waitFor(new BackupRepository(target).load())));
    }
    @Test public void sourceEditDuringMigrationPreventsReadyForSwitching() throws Exception {
        seed(220); MigrationPlan plan=waitFor(migration.preview());
        try { waitFor(migration.stage(plan,count->{if(count==100) try { waitFor(source.collection("books").document("old0000").update("notes","Changed during migration")); } catch(Exception e) {throw new RuntimeException(e);} })); fail(); } catch(java.util.concurrent.ExecutionException expected) { }
        assertEquals("Changed during migration",waitFor(source.collection("books").document("old0000").get()).getString("notes"));
        assertEquals("Original note",waitFor(target.collection("bookCopies").document("old0000").get()).getString("notes"));
    }
    @Test public void migratedCopiesSupportNotesProgressMovesAndAdditionalCopies() throws Exception {
        seed(1); MigrationPlan plan=waitFor(migration.preview()); waitFor(migration.stage(plan,count->{})); String sourceBefore=BackupJson.fingerprint(waitFor(new BackupRepository(source).load()));
        BookRepository books=new BookRepository(target); waitFor(books.saveNotes("copy0",true,"Copy 0","Updated copy note"));
        assertEquals("Copy 1",waitFor(books.load("copy1",true)).getNotes());
        waitFor(books.saveReadingProgress("copy1",true,ReadingProgress.calculate(waitFor(books.load("copy1",true)),200,400,false))); assertEquals(50,waitFor(books.load("copy1",true)).getProgress());
        waitFor(books.move("copy1",true,2)); Book moved=waitFor(books.load("copy1",true)); assertEquals(2,moved.getLocation()); assertTrue(moved.getPosition()>0);
        Book extra=new Book("extra","Known Work","Author",null,null,false,0,5); extra.setIsbn("0306406152"); waitFor(books.add(extra));
        assertEquals(4,waitFor(books.getBooks(null)).size()); assertEquals(100,waitFor(books.load("extra",true)).getPosition());
        int workCount=waitFor(target.collection("works").get()).size();
        Book variant=new Book("variant","Known Work 2nd Edition","Author",null,null,false,0,5); variant.setIsbn("9780140328721"); waitFor(books.add(variant));
        assertEquals(workCount,waitFor(target.collection("works").get()).size());
        Book edited=waitFor(books.load("variant",true)); edited.setAuthor("Another author"); waitFor(books.save("variant",true,edited,5));
        String linked=waitFor(target.collection("editions").document("variant").get()).getString("workId"); assertEquals(CatalogueIdentity.workId(edited.getTitle(),edited.getAuthor(),edited.getId()),linked);
        assertEquals("Author",waitFor(books.load("copy0",true)).getAuthor());
        assertEquals("Updated copy note",waitFor(books.load("copy0",true)).getNotes()); assertEquals(sourceBefore,BackupJson.fingerprint(waitFor(new BackupRepository(source).load())));
    }
    @Test public void sameDatabaseCannotBeSelectedAsMigrationTarget() { assertThrows(IllegalArgumentException.class,()->new MigrationRepository(source,source)); }
}
