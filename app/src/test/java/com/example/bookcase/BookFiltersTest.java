package com.example.bookcase;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class BookFiltersTest {
    private Book book(String id, int progress, boolean read, boolean lent) {
        Book b = new Book(id, "Shared title", "Author", "", "", read, progress, 2);
        b.setLendingStatus(lent ? "LENT" : "AVAILABLE"); b.setPosition(7); return b;
    }
    @Test public void readingStatesAreExclusiveAndLendingIsIndependent() {
        Book a=book("a",0,false,true), b=book("b",35,false,false),
             c=book("c",0,true,false), d=book("d",100,false,true);
        List<Book> input=Arrays.asList(a,b,c,d);
        assertEquals(Arrays.asList(a), BookFilters.filter(input, BookFilters.Status.UNREAD));
        assertEquals(Arrays.asList(b), BookFilters.filter(input, BookFilters.Status.IN_PROGRESS));
        assertEquals(Arrays.asList(c,d), BookFilters.filter(input, BookFilters.Status.FINISHED));
        assertEquals(Arrays.asList(a,d), BookFilters.filter(input, BookFilters.Status.LENT));
        assertEquals(input, BookFilters.filter(input, BookFilters.Status.ALL));
        assertEquals(7, a.getPosition()); assertEquals(2,a.getLocation());
    }
    @Test public void combinesWithSearchWithoutCollapsingCopies() {
        Book a=book("a",30,false,false), b=book("b",40,false,false), c=book("c",0,false,false);
        List<Book> matches=BookFilters.filter(BookSearch.filter(Arrays.asList(b,a,c),"SHARED"),BookFilters.Status.IN_PROGRESS);
        assertEquals(Arrays.asList(b,a),matches); assertSame(b,matches.get(0));
        assertTrue(BookFilters.filter(BookSearch.filter(Arrays.asList(a),"missing"),BookFilters.Status.ALL).isEmpty());
    }
}
