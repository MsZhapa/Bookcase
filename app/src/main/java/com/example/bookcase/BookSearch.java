package com.example.bookcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Read-only filtering: retains physical copies, identity, and repository shelf order. */
public final class BookSearch {
    private BookSearch() {}

    public static List<Book> filter(List<Book> books, String query) {
        String text = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String isbnQuery = compact(text);
        List<Book> matches = new ArrayList<>();
        for (Book book : books) {
            if (text.isEmpty() || contains(book.getTitle(), text)
                    || contains(book.getAuthor(), text)
                    || (!isbnQuery.isEmpty() && compact(book.getIsbn()).contains(isbnQuery))
                    || Isbn.equivalent(text, book.getIsbn())) {
                matches.add(book);
            }
        }
        return matches;
    }

    private static boolean contains(String value, String text) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(text);
    }

    private static String compact(String value) {
        return value == null ? "" : value.replaceAll("[\\s\\p{Z}\\p{Pd}]", "").toLowerCase(Locale.ROOT);
    }
}
