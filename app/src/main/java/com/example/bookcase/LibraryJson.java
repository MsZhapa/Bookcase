package com.example.bookcase;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/** Portable, versioned catalogue snapshot; one record per physical copy. */
public final class LibraryJson {
    private LibraryJson() {}
    public static void write(Writer out, List<Book> books, long exportedAt) throws IOException {
        out.write("{\"formatVersion\":1,\"exportedAtEpochMillis\":" + exportedAt
                + ",\"copyCount\":" + books.size() + ",\"copies\":[");
        boolean first = true;
        for (Book b : books) {
            if (!first) out.write(",");
            first = false;
            out.write("{\"id\":" + quote(b.getId())
                    + ",\"sourceCollection\":" + quote(b.isNormalizedCopy() ? "bookCopies" : "books")
                    + ",\"title\":" + quote(b.getTitle()) + ",\"author\":" + quote(b.getAuthor())
                    + ",\"isbn\":" + quote(b.getIsbn()) + ",\"pageCount\":" + b.getPageCount()
                    + ",\"description\":" + quote(b.getDescription())
                    + ",\"imageUrl\":" + quote(b.getImageUrl())
                    + ",\"notes\":" + quote(b.getNotes())
                    + ",\"lentDateEpochMillis\":" + (b.getLentDate() == null ? "null" : b.getLentDate().toDate().getTime())
                    + ",\"dueDateEpochMillis\":" + (b.getDueDate() == null ? "null" : b.getDueDate().toDate().getTime())
                    + ",\"cubeNumber\":" + b.getLocation() + ",\"position\":" + b.getPosition()
                    + ",\"read\":" + b.isRead() + ",\"progress\":" + b.getProgress()
                    + ",\"lendingStatus\":" + quote(b.getLendingStatus())
                    + ",\"borrowerName\":" + quote(b.getBorrowerName()) + "}");
        }
        out.write("]}\n");
    }
    static String quote(String value) {
        if (value == null) return "null";
        StringBuilder out = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 32 || Character.isSurrogate(c)) {
                        out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int)c));
                    } else out.append(c);
            }
        }
        return out.append('"').toString();
    }
}
