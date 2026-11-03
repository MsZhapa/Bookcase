package com.example.bookcase;
import java.util.List;

/** Counts copies, with the same mutually exclusive reading states as the list filters. */
public final class ReadingSummary {
    public final int total, unread, inProgress, finished, lent;
    public ReadingSummary(List<Book> books) {
        total = books.size();
        unread = BookFilters.filter(books, BookFilters.Status.UNREAD).size();
        inProgress = BookFilters.filter(books, BookFilters.Status.IN_PROGRESS).size();
        finished = BookFilters.filter(books, BookFilters.Status.FINISHED).size();
        lent = BookFilters.filter(books, BookFilters.Status.LENT).size();
    }
}
