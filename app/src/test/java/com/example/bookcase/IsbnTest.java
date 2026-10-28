package com.example.bookcase;

import org.junit.Test;
import static org.junit.Assert.*;

public class IsbnTest {
    @Test public void equivalentFormatsAndChecksums() {
        assertEquals("9780306406157", Isbn.normalize("0-306-40615-2"));
        assertTrue(Isbn.equivalent(" 978 0 306 40615 7 ", "0–306–40615–2"));
        assertTrue(Isbn.equivalent("0-8044-2957-x", "9780804429573"));
        assertEquals("9791090636071", Isbn.normalize("9791090636071"));
    }
    @Test public void absentInvalidAndDifferentEditionsDoNotMatch() {
        assertEquals("", Isbn.normalize(null));
        assertFalse(Isbn.equivalent("", ""));
        assertFalse(Isbn.equivalent("9780306406157", "9780804429573"));
        assertFalse(Isbn.equivalent("0306406153", "0306406153"));
        assertFalse(Isbn.equivalent("9780306406158", "0306406152"));
    }
    @Test public void invalidInputsAreRejected() {
        for (String input : new String[]{"0306406153", "9780306406158", "123", "978030640615X", "1234567890128"}) {
            try { Isbn.normalize(input); fail(input); }
            catch (IllegalArgumentException expected) { }
        }
    }
}
