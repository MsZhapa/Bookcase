package com.example.bookcase;
import java.util.Objects;
/** Initial cutover must still match its reviewed snapshots. Active V2 is independent. */
public final class MigrationReadiness {
    private MigrationReadiness() {}
    public static void verify(boolean activated,String expectedSource,String expectedTarget,String actualSource,String actualTarget) {
        if(activated) return;
        if(!Objects.equals(expectedSource,actualSource) || !Objects.equals(expectedTarget,actualTarget))
            throw new IllegalStateException("The library changed since migration. Refresh and verify the V2 copy before the first switch. No library records were changed.");
    }
}
