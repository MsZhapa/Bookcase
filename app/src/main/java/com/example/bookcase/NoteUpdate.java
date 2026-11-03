package com.example.bookcase;
import java.util.Collections;
import java.util.Map;

public final class NoteUpdate {
    public static final int MAX_LENGTH = 20000;
    private NoteUpdate() {}
    public static Map<String, Object> fields(String current, String expected, String text) {
        if (!value(current).equals(value(expected)))
            throw new IllegalStateException("Notes changed elsewhere. Reload before saving.");
        String note = value(text);
        if (note.length() > MAX_LENGTH) throw new IllegalArgumentException("Note is too long");
        return Collections.singletonMap("notes", note);
    }
    private static String value(String text) { return text == null ? "" : text; }
}
