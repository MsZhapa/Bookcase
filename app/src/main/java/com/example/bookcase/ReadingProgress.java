package com.example.bookcase;
import java.util.LinkedHashMap;
import java.util.Map;

/** Page-based state; old percentage-only records are never converted into guessed bookmarks. */
public final class ReadingProgress {
    private ReadingProgress() {}
    public static int total(Book book) {
        return book.getReadingPageCount() != null && book.getReadingPageCount() > 0
                ? book.getReadingPageCount() : Math.max(0, book.getPageCount());
    }
    public static State calculate(Book original, Integer page, Integer total, boolean finished) {
        int count = total == null ? 0 : total;
        if (count < 0) throw new IllegalArgumentException("Total pages must be positive");
        if (page != null && page < 0) throw new IllegalArgumentException("Current page cannot be negative");
        if (finished) return new State(count > 0 ? Integer.valueOf(count) : original.getCurrentPage(),
                count > 0 ? count : null, 100, true);
        if (page == null) {
            if (original.getCurrentPage() != null)
                throw new IllegalArgumentException("Enter your current page, or 0 to mark unread");
            // Leaving a legacy bookmark blank preserves the saved percentage.
            return new State(null, count > 0 ? count : null,
                    BookFilters.finished(original) ? 0 : original.getProgress(), false);
        }
        if (page > 0 && count == 0) throw new IllegalArgumentException("Enter the total page count");
        if (count > 0 && page > count) throw new IllegalArgumentException("Current page exceeds total pages");
        int percent = count == 0 || page == 0 ? 0
                : Math.max(1, (int)((long)page * 100 / count));
        return new State(page, count > 0 ? count : null, percent, count > 0 && page == count);
    }
    public static final class State {
        public final Integer page, total;
        public final int percent;
        public final boolean finished;
        State(Integer page, Integer total, int percent, boolean finished) {
            this.page=page; this.total=total; this.percent=percent; this.finished=finished;
        }
        public void apply(Book book) {
            book.setCurrentPage(page); book.setReadingPageCount(total);
            book.setProgress(percent); book.setRead(finished);
        }
        public Map<String,Object> fields(boolean normalized) {
            Map<String,Object> fields = new LinkedHashMap<>();
            fields.put("currentPage", page); fields.put("readingPageCount", total);
            fields.put("progress", percent); fields.put("read", finished);
            if (!normalized) fields.put("isRead", finished);
            return fields;
        }
    }
}
