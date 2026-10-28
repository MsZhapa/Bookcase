package com.example.bookcase;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class ShelfOrderTest {
    private Book book(String id, int cube, long position) {
        Book book = new Book(id, id, "author", "", "", false, 0, cube);
        book.setPosition(position);
        return book;
    }
    @Test public void sortsWithinCubeByPositionRatherThanTitleOrId() {
        Book second = book("a", 1, 2), first = book("z", 1, 1);
        List<Book> books = new ArrayList<>(Arrays.asList(second, book("b", 2, 1), first));
        books.sort(BookRepository.SHELF_ORDER);
        assertSame(first, books.get(0)); assertSame(second, books.get(1));
        assertEquals(2, books.get(2).getLocation());
    }
    @Test public void legacyBooksKeepStableOrderBeforeAppendedCopies() {
        List<Book> books = new ArrayList<>(Arrays.asList(book("new", 1, 1),
                book("z", 1, 0), book("a", 1, 0)));
        books.sort(BookRepository.SHELF_ORDER);
        assertEquals("a", books.get(0).getId());
        assertEquals("z", books.get(1).getId());
        assertEquals("new", books.get(2).getId());
    }
    @Test public void loanAndReturnRetainCopyLocationAndPosition() {
        BookCopy copy = new BookCopy("copy", "edition", 3, 17);
        assertEquals("AVAILABLE", copy.getLendingStatus());
        copy.setLendingStatus("LENT"); copy.setBorrowerName("Reader"); copy.setActiveLoanId("loan");
        assertEquals(3, copy.getCubeNumber()); assertEquals(17, copy.getPosition());
        copy.setLendingStatus("AVAILABLE"); copy.setBorrowerName(""); copy.setActiveLoanId(null);
        assertEquals(3, copy.getCubeNumber()); assertEquals(17, copy.getPosition());
    }
}
