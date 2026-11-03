package com.example.bookcase;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

public final class LoanRules {
    private LoanRules() {}
    public static boolean overdue(Book book, long now, TimeZone zone) {
        return "LENT".equals(book.getLendingStatus()) && book.getDueDate() != null
                && day(book.getDueDate().toDate().getTime(), zone) < day(now, zone);
    }
    public static boolean dueToday(Book book, long now, TimeZone zone) {
        return "LENT".equals(book.getLendingStatus()) && book.getDueDate() != null
                && day(book.getDueDate().toDate().getTime(), zone) == day(now, zone);
    }
    private static long day(long millis, TimeZone zone) {
        Calendar day = Calendar.getInstance(zone);
        day.setTimeInMillis(millis); day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0); day.set(Calendar.SECOND, 0); day.set(Calendar.MILLISECOND, 0);
        return day.getTimeInMillis();
    }
    public static List<Book> active(List<Book> books, long now, TimeZone zone, boolean overdueOnly) {
        List<Book> loans = new ArrayList<>();
        for (Book book : books) {
            if ("LENT".equals(book.getLendingStatus()) && (!overdueOnly || overdue(book, now, zone)))
                loans.add(book);
        }
        loans.sort(Comparator.comparingInt((Book book) -> overdue(book, now, zone) ? 0 : 1)
                .thenComparingLong(book -> book.getDueDate() == null ? Long.MAX_VALUE
                        : book.getDueDate().toDate().getTime())
                .thenComparing(BookRepository.SHELF_ORDER));
        return loans;
    }
}
