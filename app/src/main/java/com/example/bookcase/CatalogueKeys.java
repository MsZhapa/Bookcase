package com.example.bookcase;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Locale;
public final class CatalogueKeys {
    private CatalogueKeys() {}
    public static String fold(String text) {
        return text==null ? "" : Normalizer.normalize(text,Normalizer.Form.NFKC)
                .replaceAll("[\\s\\p{Z}]+"," ").trim().toLowerCase(Locale.ROOT);
    }
    public static String hash(String text) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result=new StringBuilder();
            for (byte b:bytes) result.append(String.format(Locale.ROOT,"%02x",b & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
