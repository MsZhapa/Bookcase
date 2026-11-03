package com.example.bookcase;
import org.junit.Test;
import java.util.Map;
import static org.junit.Assert.*;
public class ReadingProgressTest {
    @Test public void calculatesPercentageFromPagesAndFinishesOnlyAtEnd() {
        Book original=new Book(); ReadingProgress.State partial=ReadingProgress.calculate(original,75,300,false);
        assertEquals(25,partial.percent);assertFalse(partial.finished);assertEquals(Integer.valueOf(75),partial.page);
        ReadingProgress.State end=ReadingProgress.calculate(original,300,300,false);
        assertEquals(100,end.percent);assertTrue(end.finished);
        assertEquals(99,ReadingProgress.calculate(original,299,300,false).percent);
    }
    @Test public void preservesLegacyPercentageWithoutGuessingPage() {
        Book original=new Book();original.setProgress(69);
        ReadingProgress.State state=ReadingProgress.calculate(original,null,300,false);
        assertEquals(69,state.percent);assertNull(state.page);assertFalse(state.finished);
    }
    @Test public void supportsUnreadAndFinishedWhenTotalUnknown() {
        ReadingProgress.State unread=ReadingProgress.calculate(new Book(),0,null,false);
        assertEquals(0,unread.percent);assertFalse(unread.finished);assertNull(unread.total);
        ReadingProgress.State read=ReadingProgress.calculate(new Book(),null,null,true);
        assertEquals(100,read.percent);assertTrue(read.finished);assertNull(read.page);
        assertNull(read.total);
    }
    @Test public void marksFinishedWithKnownTotalAndCanRestartFinishedLegacyBook() {
        ReadingProgress.State read=ReadingProgress.calculate(new Book(),20,300,true);
        assertEquals(Integer.valueOf(300),read.page);assertEquals(100,read.percent);
        Book old=new Book();old.setRead(true);old.setProgress(100);
        assertEquals(0,ReadingProgress.calculate(old,null,null,false).percent);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsPagesBeyondEnd() {
        ReadingProgress.calculate(new Book(),301,300,false);
    }
    @Test(expected=IllegalArgumentException.class) public void requiresTotalForPositivePage() {
        ReadingProgress.calculate(new Book(),15,null,false);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativePage() {
        ReadingProgress.calculate(new Book(),-1,300,false);
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsNegativeTotal() {
        ReadingProgress.calculate(new Book(),1,-3,false);
    }
    @Test(expected=IllegalArgumentException.class) public void clearingRecordedPageRequiresExplicitZero() {
        Book old=new Book();old.setCurrentPage(20);
        ReadingProgress.calculate(old,null,300,false);
    }
    @Test public void handlesLongBooksWithoutRoundingToUnreadOrFinishedPrematurely() {
        assertEquals(1,ReadingProgress.calculate(new Book(),1,10000,false).percent);
        assertEquals(99,ReadingProgress.calculate(new Book(),Integer.MAX_VALUE-1,Integer.MAX_VALUE,false).percent);
        assertEquals(100,ReadingProgress.calculate(new Book(),Integer.MAX_VALUE,Integer.MAX_VALUE,false).percent);
    }
    @Test public void bookmarkAndTotalBelongToCopyAndUpdateLeavesOtherFieldsUntouched() {
        Book first=new Book("a","Same","Author","description","cover",false,0,3);
        Book second=new Book("b","Same","Author","description","cover",false,0,3);
        first.setPageCount(500);second.setPageCount(500);first.setPosition(42);first.setNotes("keep me");
        ReadingProgress.State state=ReadingProgress.calculate(first,75,300,false);state.apply(first);
        assertEquals(300,ReadingProgress.total(first));assertEquals(500,ReadingProgress.total(second));
        assertEquals(500,first.getPageCount());assertNull(second.getCurrentPage());assertEquals(42,first.getPosition());
        assertEquals("keep me",first.getNotes());assertEquals(25,first.getProgress());
        Map<String,Object> update=state.fields(true);assertEquals(4,update.size());
        assertFalse(update.containsKey("position"));assertFalse(update.containsKey("notes"));
        assertFalse(update.containsKey("editionId"));assertFalse(update.containsKey("lendingStatus"));
        assertEquals(false,state.fields(false).get("isRead"));
    }
}
