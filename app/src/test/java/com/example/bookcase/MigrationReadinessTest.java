package com.example.bookcase;
import org.junit.Test;
import static org.junit.Assert.*;
public class MigrationReadinessTest {
    @Test public void unchangedReviewedSnapshotsAllowFirstCutover() { MigrationReadiness.verify(false,"source","target","source","target"); }
    @Test public void editsBeforeOnePlusTestingBlockFirstSwitch() { assertThrows(IllegalStateException.class,()->MigrationReadiness.verify(false,"source","target","new source","target")); assertThrows(IllegalStateException.class,()->MigrationReadiness.verify(false,"source","target","source","changed target")); }
    @Test public void activatedLibraryCanBeOpenedOnAnotherDeviceAfterEdits() { MigrationReadiness.verify(true,"original","initial target","old archive","updated private library"); }
}
