package com.example.bookcase;

import java.util.Locale;

/** Canonical ISBN-13 keys, without rewriting existing library records. */
public final class Isbn {
    private Isbn() {}

    public static String normalize(String input) {
        if (input == null) return "";
        String value = input.replaceAll("[\\s\\p{Z}\\p{Pd}]", "").toUpperCase(Locale.ROOT);
        if (value.isEmpty()) return "";
        if (value.matches("[0-9]{9}[0-9X]")) {
            int sum = 0;
            for (int i = 0; i < 10; i++) {
                int digit = value.charAt(i) == 'X' ? 10 : value.charAt(i) - '0';
                sum += (10 - i) * digit;
            }
            if (sum % 11 != 0) throw new IllegalArgumentException("Invalid ISBN-10 checksum");
            String prefix = "978" + value.substring(0, 9);
            return prefix + checkDigit(prefix);
        }
        if (value.matches("97[89][0-9]{10}")
                && value.charAt(12) - '0' == checkDigit(value.substring(0, 12))) return value;
        throw new IllegalArgumentException("Enter a valid ISBN-10 or ISBN-13");
    }

    private static int checkDigit(String prefix) {
        int sum = 0;
        for (int i = 0; i < 12; i++) sum += (prefix.charAt(i) - '0') * (i % 2 == 0 ? 1 : 3);
        return (10 - sum % 10) % 10;
    }

    public static boolean equivalent(String first, String second) {
        try {
            String key = normalize(first);
            return !key.isEmpty() && key.equals(normalize(second));
        } catch (IllegalArgumentException invalidLegacyIsbn) {
            return false;
        }
    }
}
