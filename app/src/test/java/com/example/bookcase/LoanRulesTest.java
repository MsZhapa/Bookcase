package com.example.bookcase;
import com.google.firebase.Timestamp;
import org.junit.Test;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import static org.junit.Assert.*;
public class LoanRulesTest {
    private long instant(String text) { return java.time.Instant.parse(text).toEpochMilli(); }
    private Book loan(String id, String due) {
        Book book=new Book(id,"Same ISBN","Author","","",false,0,1);
        book.setLendingStatus("LENT");
        if (due != null) book.setDueDate(new Timestamp(new Date(instant(due))));
        return book;
    }
    @Test public void dueTodayIsNotOverdueAndMissingDateIsNeverOverdue() {
        long now=instant("2026-11-03T20:00:00Z"); TimeZone zone=TimeZone.getTimeZone("Europe/Copenhagen");
        Book today=loan("a","2026-11-03T01:00:00Z"), missing=loan("b",null);
        assertTrue(LoanRules.dueToday(today,now,zone)); assertFalse(LoanRules.overdue(today,now,zone));
        assertFalse(LoanRules.overdue(missing,now,zone)); assertFalse(LoanRules.dueToday(missing,now,zone));
        today.setLendingStatus("AVAILABLE"); assertFalse(LoanRules.dueToday(today,now,zone));
    }
    @Test public void overdueUsesLocalDatesIncludingDaylightSavingBoundary() {
        long now=instant("2026-11-02T23:30:00Z");
        Book book=loan("a","2026-11-02T20:00:00Z");
        assertTrue(LoanRules.overdue(book,now,TimeZone.getTimeZone("Europe/Copenhagen")));
        assertFalse(LoanRules.overdue(book,now,TimeZone.getTimeZone("America/New_York")));
        Book dst=loan("dst","2026-10-25T00:30:00Z");
        assertTrue(LoanRules.dueToday(dst,instant("2026-10-25T22:30:00Z"),TimeZone.getTimeZone("Europe/Copenhagen")));
        assertTrue(LoanRules.overdue(dst,instant("2026-10-25T23:30:00Z"),TimeZone.getTimeZone("Europe/Copenhagen")));
    }
    @Test public void sortsUrgentFirstAndKeepsDistinctCopiesWithoutChangingShelfOrder() {
        Book undated=loan("none",null), future=loan("future","2026-11-05T12:00:00Z"),
            a=loan("a","2026-11-01T12:00:00Z"), b=loan("b","2026-11-01T12:00:00Z"),
            returned=loan("returned","2026-10-01T12:00:00Z");
        returned.setLendingStatus("AVAILABLE"); a.setPosition(2); b.setPosition(1);
        List<Book> input=Arrays.asList(undated,future,a,b,returned);
        long now=instant("2026-11-03T12:00:00Z"); TimeZone zone=TimeZone.getTimeZone("Europe/Copenhagen");
        assertEquals(Arrays.asList(b,a,future,undated),LoanRules.active(input,now,zone,false));
        assertEquals(Arrays.asList(b,a),LoanRules.active(input,now,zone,true));
        assertSame(undated,input.get(0)); assertEquals(2,a.getPosition()); assertEquals(1,b.getPosition());
    }
}
