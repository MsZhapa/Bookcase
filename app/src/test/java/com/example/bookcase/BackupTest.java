package com.example.bookcase;

import org.junit.Test;
import java.io.*;
import java.util.*;
import com.google.firebase.Timestamp;
import static org.junit.Assert.*;

public class BackupTest {
    static Map<String,Object> fields(Object... pairs) { Map<String,Object> out=new TreeMap<>(); for(int i=0;i<pairs.length;i+=2) out.put((String)pairs[i],pairs[i+1]); return out; }
    static LibraryBackup fixture() {
        LibraryBackup b=new LibraryBackup(); b.cubeNames[4]="Travel"; b.columns=3; b.exportedAt=42;
        b.collections.get("works").put("w",fields("id","w","title","A book"));
        b.collections.get("editions").put("e",fields("id","e","workId","w","title","A book","isbn","9780306406157","pageCount",400L));
        b.collections.get("bookCopies").put("c",fields("id","c","editionId","e","cubeNumber",5L,"position",12L,"read",false,"progress",25L,"currentPage",100L,"readingPageCount",400L,"notes","Ø notes 📚\n\"quoted\"","lendingStatus","LENT","borrowerName","Friend","activeLoanId","l","lentDate",new Timestamp(1700000000,123456000)));
        b.collections.get("books").put("old",fields("id","old","title","Legacy","location",1L,"position",0L,"read",false,"progress",0L,"description",null));
        b.collections.get("lendingRecords").put("l",fields("bookId","c","status","ACTIVE","lentDate",new Timestamp(1700000000,123456000)));
        WishItem wish=WishItem.create("Wanted","Author","");
        b.collections.get("wishlist").put(wish.getId(),fields("id",wish.getId(),"title",wish.getTitle(),"author",wish.getAuthor(),"isbn","","createdAt",1L));
        b.collections.get("cubePositions").put("5",fields("lastPosition",12L)); return b;
    }
    static String json(LibraryBackup b) throws Exception { StringWriter out=new StringWriter(); BackupJson.write(out,b); return out.toString(); }
    static LibraryBackup read(String s) throws Exception { return BackupJson.read(new StringReader(s)); }
    @Test public void completeRoundTripPreservesLinksNotesTimestampsAndSettings() throws Exception {
        LibraryBackup b=fixture(), actual=read(json(b));
        assertEquals(BackupJson.fingerprint(b),BackupJson.fingerprint(actual)); assertEquals(2,actual.copies());
        assertEquals("Travel",actual.cubeNames[4]); assertEquals(3,actual.columns); assertEquals(42,actual.exportedAt);
        assertEquals(new Timestamp(1700000000,123456000),actual.get("bookCopies","c").get("lentDate"));
        assertEquals(0L,actual.get("books","old").get("position")); assertTrue(actual.get("books","old").containsKey("description"));
    }
    @Test public void unchangedBackupHasNoWritesAndDoesNotConflateCopies() throws Exception {
        LibraryBackup b=fixture(), current=read(json(b)); RestorePlan plan=new RestorePlan(b,current);
        assertTrue(plan.canRestore()); assertEquals(0,plan.missing.size()); assertEquals(6,plan.matching);
    }
    @Test public void emptyRestorePreservesOriginalIdsAndPositions() {
        RestorePlan plan=new RestorePlan(fixture(),new LibraryBackup()); assertTrue(plan.canRestore()); assertEquals(6,plan.missing.size()); assertEquals(12,plan.counters[4]);
    }
    @Test public void changedCopyBlocksWholeRestore() throws Exception {
        LibraryBackup b=fixture(), current=read(json(b)); current.get("bookCopies","c").put("notes","New note");
        RestorePlan plan=new RestorePlan(b,current); assertFalse(plan.canRestore()); assertTrue(plan.conflicts.get(0).contains("existing record differs"));
    }
    @Test public void occupiedPositionBlocksNewCopy() {
        LibraryBackup current=new LibraryBackup(); current.collections.get("books").put("other",fields("location",5L,"position",12L));
        RestorePlan plan=new RestorePlan(fixture(),current); assertFalse(plan.canRestore()); assertTrue(plan.conflicts.get(0).contains("occupied"));
    }
    @Test public void sharedIsbnRetainsAdditionalPhysicalCopyAndHigherCounters() throws Exception {
        LibraryBackup b=fixture(), current=read(json(b)); current.collections.get("bookCopies").remove("c");
        current.collections.get("bookCopies").put("different",fields("editionId","e","cubeNumber",5L,"position",13L));
        current.collections.get("cubePositions").put("5",fields("lastPosition",99L));
        RestorePlan plan=new RestorePlan(b,current); assertTrue(plan.canRestore()); assertEquals(Collections.singletonList("bookCopies/c"),plan.missing); assertEquals(1,plan.additionalIsbnCopies); assertEquals(99,plan.counters[4]);
    }
    @Test public void partialRestoreCanResume() {
        LibraryBackup b=fixture(), current=new LibraryBackup(); current.collections.get("works").putAll(b.collections.get("works"));
        RestorePlan plan=new RestorePlan(b,current); assertTrue(plan.canRestore()); assertEquals(5,plan.missing.size()); assertEquals(1,plan.matching);
    }
    @Test public void unknownPositionsRemainUnknownAndDoNotConflict() {
        LibraryBackup current=new LibraryBackup(); current.collections.get("books").put("unknown",fields("location",1L,"position",0L));
        assertTrue(new RestorePlan(fixture(),current).canRestore());
    }
    @Test public void catalogueVersionOneIsExplainedRatherThanGuessed() throws Exception {
        try { read("{\"formatVersion\":1,\"copies\":[]}"); fail(); } catch(IllegalArgumentException e) { assertTrue(e.getMessage().contains("older catalogue")); }
    }
    @Test public void invalidReferencesAreRejected() {
        LibraryBackup b=fixture(); b.collections.get("works").clear(); assertThrows(IllegalArgumentException.class,b::validate);
        b=fixture(); b.collections.get("lendingRecords").clear(); assertThrows(IllegalArgumentException.class,b::validate);
    }
    @Test public void invalidPositionsAndCopyIdentitiesAreRejected() {
        LibraryBackup b=fixture(); b.get("bookCopies","c").put("cubeNumber",9L); assertThrows(IllegalArgumentException.class,b::validate);
        b=fixture(); b.collections.get("books").put("c",fields("location",1L)); assertThrows(IllegalArgumentException.class,b::validate);
    }
    @Test public void malformedFilesCannotReachPreview() throws Exception {
        assertThrows(IllegalArgumentException.class,()->read("{\"formatVersion\":2,\"formatVersion\":2}"));
        assertThrows(Exception.class,()->read(json(fixture())+"false"));
        assertThrows(Exception.class,()->read(json(fixture()).substring(0,100)));
        assertThrows(Exception.class,()->read(json(fixture()).replace("\"columns\":3","\"columns\":2.5")));
        assertThrows(Exception.class,()->read(json(fixture()).replace("\"type\":\"integer\",\"value\":\"12\"","\"type\":\"integer\",\"value\":\"1.2\"")));
    }
    @Test public void arbitraryFieldTypesAreNotSilentlyLost() {
        LibraryBackup b=fixture(); b.get("works","w").put("unsupported",new Object()); assertThrows(IllegalArgumentException.class,()->json(b));
    }
    @Test public void nestedFieldsAndLongPrecisionRoundTrip() throws Exception {
        LibraryBackup b=fixture(); b.get("works","w").put("extra",fields("large",9007199254740993L,"list",Arrays.asList(null,true,1.25,"value")));
        assertEquals(BackupJson.fingerprint(b),BackupJson.fingerprint(read(json(b))));
    }
    @Test public void missingCollectionsAndDuplicateRecordsAreRejected() throws Exception {
        String s=json(fixture()); assertThrows(Exception.class,()->read(s.replace("\"wishlist\"","\"unknown\"")));
        LibraryBackup b=fixture(); b.collections.get("books").put("bad/path",fields("location",1L)); assertThrows(IllegalArgumentException.class,b::validate);
    }
    @Test public void failedOutputIsNotReportedAsACompletedBackup() {
        assertThrows(Exception.class,()->BackupJson.write(new Writer() { public void write(char[] c,int o,int n) throws IOException { throw new IOException("Full"); } public void flush() {} public void close() {} },fixture()));
    }

    @Test public void missingCountersCanBeRepairedWithoutDuplicatingMatchingCopies() throws Exception {
        LibraryBackup b=fixture(), current=read(json(b)); current.collections.get("cubePositions").clear();
        RestorePlan plan=new RestorePlan(b,current); assertTrue(plan.missing.isEmpty()); assertTrue(plan.canRestore()); assertEquals(1,plan.counterChanges); assertEquals(12,plan.counters[4]);
    }
    @Test public void unrepresentableTimestampPrecisionIsRejectedBeforeRestore() throws Exception {
        String s=json(fixture()).replace("\"nanoseconds\":123456000","\"nanoseconds\":123456789");
        assertThrows(IllegalArgumentException.class,()->read(s));
    }
}
