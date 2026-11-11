package com.example.bookcase;

import org.junit.Test;
import java.io.*;
import java.util.*;
import static org.junit.Assert.*;

public class MigrationPlanTest {
    static Map<String,Object> fields(Object... pairs) { Map<String,Object> out=new TreeMap<>(); for(int i=0;i<pairs.length;i+=2) out.put((String)pairs[i],pairs[i+1]); return out; }
    static LibraryBackup fixture() {
        LibraryBackup b=new LibraryBackup(); b.cubeNames[4]="Travel";
        b.collections.get("books").put("one",fields("id","stale-embedded-id","title","Interaction Design 2nd Edition","author","Author","location",5L,"position",0L,"read",false,"progress",25L,"currentPage",100L,"readingPageCount",400L,"notes","My note","customField","Keep this","activeLoanId","loan","lendingStatus","LENT","borrowerName","Friend"));
        b.collections.get("lendingRecords").put("loan",fields("bookId","one","bookCopyId","one","status","ACTIVE")); return b;
    }
    static MigrationPlan make(LibraryBackup b) { return new MigrationPlan(b); }
    @Test public void convertsLegacyWithoutChangingSourceOrPersonalCopyFields() {
        LibraryBackup source=fixture(); String before=BackupJson.fingerprint(source); MigrationPlan plan=make(source); assertTrue(plan.ready());
        assertTrue(plan.target.collections.get("books").isEmpty()); assertEquals(1,plan.target.copies());
        Map<String,Object> copy=plan.target.get("bookCopies","one"); assertEquals("one",copy.get("id")); assertEquals(5L,copy.get("cubeNumber")); assertEquals(0L,copy.get("position"));
        for(String field:Arrays.asList("notes","progress","currentPage","readingPageCount","activeLoanId","lendingStatus","borrowerName")) assertEquals(source.get("books","one").get(field),copy.get(field));
        Map<String,Object> archive=BackupJson.readArchive((String)copy.get("migrationArchive")); assertTrue(BackupJson.same(source.get("books","one"),(Map<String,Object>)archive.get("fields")));
        assertEquals(before,BackupJson.fingerprint(source)); assertEquals("Travel",plan.target.cubeNames[4]);
    }
    @Test public void identicalMetadataSharesEditionButNeverPhysicalCopy() {
        LibraryBackup b=fixture(); b.collections.get("books").put("two",fields("title","Interaction Design 2nd Edition","author","Author","location",2L,"position",0L));
        MigrationPlan p=make(b); assertTrue(p.ready()); assertEquals(2,p.target.copies()); assertEquals(1,p.target.collections.get("editions").size()); assertEquals(1,p.target.collections.get("works").size());
    }
    @Test public void explicitEditionVariantsShareWorkButKeepEditionTitles() {
        LibraryBackup b=fixture(); b.collections.get("books").put("two",fields("title","Interaction Design 5th Edition","author","Author","location",2L));
        MigrationPlan p=make(b); assertTrue(p.ready()); assertEquals(1,p.target.collections.get("works").size()); assertEquals(2,p.target.collections.get("editions").size());
        Set<Object> titles=new HashSet<>(); for(Map<String,Object> e:p.target.collections.get("editions").values()) titles.add(e.get("title"));
        assertTrue(titles.contains("Interaction Design 2nd Edition")); assertTrue(titles.contains("Interaction Design 5th Edition"));
    }
    @Test public void differentAuthorsAndUnknownAuthorsStaySeparate() {
        LibraryBackup b=fixture(); b.collections.get("books").put("two",fields("title","Interaction Design 2nd Edition","author","Other","location",2L));
        b.collections.get("books").put("unknown1",fields("title","Unknown","location",1L)); b.collections.get("books").put("unknown2",fields("title","Unknown","location",1L));
        MigrationPlan p=make(b); assertTrue(p.ready()); assertEquals(4,p.target.collections.get("works").size()); assertEquals(2,p.uncertainWorks);
    }
    @Test public void equivalentIsbnFormatsShareAnEdition() {
        LibraryBackup b=fixture(); b.get("books","one").put("isbn","0306406152");
        b.collections.get("books").put("two",fields("title","Interaction Design 2nd Edition","author","Author","isbn","978-0-306-40615-7","location",2L));
        MigrationPlan p=make(b); assertTrue(p.ready()); assertEquals(1,p.target.collections.get("editions").size()); assertNotNull(p.target.get("editions","isbn-9780306406157"));
    }
    @Test public void conflictingIsbnMetadataBlocksMigration() {
        LibraryBackup b=fixture(); b.get("books","one").put("isbn","0306406152");
        b.collections.get("books").put("two",fields("title","Different title","author","Other","isbn","9780306406157","location",2L));
        assertFalse(make(b).ready());
    }
    @Test public void invalidIsbnBlocksRatherThanDiscardingIt() { LibraryBackup b=fixture(); b.get("books","one").put("isbn","123"); assertFalse(make(b).ready()); }
    @Test public void orphanedCatalogueRecordsNeedReview() { LibraryBackup b=fixture(); b.collections.get("works").put("orphan",fields("title","Orphan")); assertFalse(make(b).ready()); }
    @Test public void knownPositionsAndHighCountersArePreserved() {
        LibraryBackup b=fixture(); b.get("books","one").put("position",12L); b.collections.get("cubePositions").put("5",fields("lastPosition",99L));
        MigrationPlan p=make(b); assertTrue(p.ready()); assertEquals(12L,p.target.get("bookCopies","one").get("position")); assertEquals(99L,p.target.get("cubePositions","5").get("lastPosition"));
    }
    @Test public void duplicateKnownPositionsBlockBeforeWriting() {
        LibraryBackup b=fixture(); b.get("books","one").put("position",12L); b.collections.get("books").put("two",fields("title","Different","location",5L,"position",12L)); assertFalse(make(b).ready());
    }
    @Test public void oldReadingAliasAndUnknownMetadataAreArchived() {
        LibraryBackup b=fixture(); b.get("books","one").remove("read"); b.get("books","one").put("isRead",true);
        MigrationPlan p=make(b); assertEquals(true,p.target.get("bookCopies","one").get("read")); assertEquals(0L,p.target.get("bookCopies","one").get("position"));
    }
    @Test public void migrationIsDeterministicAndCanBeBackedUp() throws Exception {
        MigrationPlan a=make(fixture()),b=make(fixture()); assertEquals(BackupJson.fingerprint(a.target),BackupJson.fingerprint(b.target));
        StringWriter out=new StringWriter(); BackupJson.write(out,a.target); LibraryBackup roundTrip=BackupJson.read(new StringReader(out.toString())); a.verify(roundTrip);
        MigrationPlan repeated=make(roundTrip); assertTrue(repeated.ready()); assertEquals(BackupJson.fingerprint(a.target),BackupJson.fingerprint(repeated.target));
    }
    @Test public void verificationRejectsMissingOrAlteredCopies() {
        MigrationPlan p=make(fixture()); LibraryBackup wrong=new LibraryBackup(); assertThrows(IllegalArgumentException.class,()->p.verify(wrong));
        MigrationPlan changed=make(fixture()); changed.target.get("bookCopies","one").put("notes","Different"); assertThrows(IllegalArgumentException.class,()->p.verify(changed.target));
    }
    @Test public void normalizedSourceRetainsOriginalCatalogueInArchive() {
        LibraryBackup source=make(fixture()).target; source.get("bookCopies","one").remove("migrationArchive"); MigrationPlan p=make(source); assertTrue(p.ready());
        Map<String,Object> archive=BackupJson.readArchive((String)p.target.get("bookCopies","one").get("migrationArchive")); assertNotNull(archive.get("edition")); assertNotNull(archive.get("work"));
    }
    @Test public void unknownCopyIdentifiersAreStableAcrossReconversion() {
        LibraryBackup source=new LibraryBackup(); source.collections.get("books").put("unknown",fields("location",1L));
        MigrationPlan a=make(source),b=make(a.target); assertEquals(BackupJson.fingerprint(a.target),BackupJson.fingerprint(b.target));
    }
    /** Offline review CLI; input/output paths are supplied by the operator, never bundled data. */
    public static void main(String[] args) throws Exception {
        LibraryBackup source; try(Reader reader=new FileReader(args[0])) { source=BackupJson.read(reader); }
        MigrationPlan plan=new MigrationPlan(source); System.out.println("SOURCE_FP="+plan.sourceFingerprint); System.out.println("TARGET_FP="+BackupJson.fingerprint(plan.target)); System.out.println(plan.describe());
        if(plan.ready() && args.length>1) try(Writer writer=new FileWriter(args[1])) { BackupJson.write(writer,plan.target); }
        if(!plan.ready()) throw new IllegalStateException("Migration has blockers");
    }
}
