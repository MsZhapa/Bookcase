package com.example.bookcase;
import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;
public class ReadingSummaryTest {
    @Test public void copiesCountSeparatelyAndLendingOverlapsReadingStates() {
        Book a = new Book("a","Same","Author","","",false,0,1);
        Book b = new Book("b","Same","Author","","",false,45,2);
        Book c = new Book("c","Other","Author","","",true,0,1);
        Book d = new Book("d","Other","Author","","",false,100,1);
        a.setLendingStatus("LENT"); d.setLendingStatus("LENT");
        ReadingSummary s = new ReadingSummary(Arrays.asList(a,b,c,d));
        assertEquals(4,s.total); assertEquals(1,s.unread); assertEquals(1,s.inProgress);
        assertEquals(2,s.finished); assertEquals(2,s.lent);
        assertEquals(s.total,s.unread+s.inProgress+s.finished);
        assertEquals(2,b.getLocation()); assertEquals(45,b.getProgress());
    }
    @Test public void emptyLibraryHasZeroCounts() {
        ReadingSummary s = new ReadingSummary(Collections.emptyList());
        assertEquals(0,s.total+s.unread+s.inProgress+s.finished+s.lent);
    }
}
