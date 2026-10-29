package com.example.bookcase;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class BookSearchTest {
    private Book book(String id, String title, String author, String isbn) {
        Book b = new Book(id, title, author, "", "", false, 0, 3);
        b.setIsbn(isbn); b.setPosition(42); return b;
    }
    @Test public void matchesTitleAuthorAndFormattedPartialIsbn() {
        Book b = book("a", "The Hobbit", "J. R. R. Tolkien", "978-0-306-40615-7");
        for (String q : Arrays.asList(" HOBBIT ", "tolkien", "9780306", "0-306-40615-2"))
            assertEquals(Arrays.asList(b), BookSearch.filter(Arrays.asList(b), q));
        assertTrue(BookSearch.filter(Arrays.asList(b), "missing").isEmpty());
    }
    @Test public void preservesEveryCopyOrderIdentityAndPosition() {
        Book a = book("a", "Same", "Author", "9780306406157");
        Book b = book("b", "Same", "Author", "9780306406157");
        b.setNormalizedCopy(true); b.setLocation(6); b.setPosition(99);
        List<Book> source = Arrays.asList(b, a);
        assertEquals(source, BookSearch.filter(source, "same"));
        assertEquals(source, BookSearch.filter(source, ""));
        assertSame(b, BookSearch.filter(source, "author").get(0));
        assertEquals(99, b.getPosition()); assertEquals(6, b.getLocation());
        assertEquals(42, a.getPosition()); assertEquals(2, source.size());
    }
    @Test public void handlesMissingMetadataAndPunctuation() {
        Book b = book("a", null, null, null);
        assertTrue(BookSearch.filter(Arrays.asList(b), "x").isEmpty());
        assertTrue(BookSearch.filter(Arrays.asList(b), "---").isEmpty());
        assertEquals(1, BookSearch.filter(Arrays.asList(b), null).size());
    }
}
