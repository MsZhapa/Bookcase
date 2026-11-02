package com.example.bookcase;

import java.util.ArrayList;
import java.util.List;

/** Filters physical copies without changing their shelf order or stored state. */
public final class BookFilters {
    public enum Status { ALL, UNREAD, IN_PROGRESS, FINISHED, LENT }
    private BookFilters() {}
    public static boolean finished(Book book) { return book.isRead() || book.getProgress() >= 100; }
    public static List<Book> filter(List<Book> books, Status status) {
        List<Book> result = new ArrayList<>();
        for (Book b : books) {
            boolean matches;
            switch (status) {
                case UNREAD: matches = !finished(b) && b.getProgress() <= 0; break;
                case IN_PROGRESS: matches = !finished(b) && b.getProgress() > 0; break;
                case FINISHED: matches = finished(b); break;
                case LENT: matches = "LENT".equals(b.getLendingStatus()); break;
                default: matches = true;
            }
            if (matches) result.add(b);
        }
        return result;
    }
}
