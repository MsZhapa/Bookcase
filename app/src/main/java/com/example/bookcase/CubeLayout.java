package com.example.bookcase;
import java.util.List;
public final class CubeLayout {
    public static final int COUNT=8;
    private CubeLayout() {}
    public static String label(String name,int cube) {
        if (cube<1 || cube>COUNT) throw new IllegalArgumentException("Invalid cube");
        String text=name==null ? "" : name.replaceAll("[\\s\\p{Z}]+"," ").trim();
        if (text.length()>40) throw new IllegalArgumentException("Cube names must be 40 characters or fewer");
        return text.isEmpty() ? "Cube "+cube : text;
    }
    public static void validate(String[] names,int columns) {
        if (names.length!=COUNT || columns<1 || columns>4) throw new IllegalArgumentException("Choose 1 to 4 columns");
        for (int i=0;i<COUNT;i++) label(names[i],i+1);
    }
    public static int[] counts(List<Book> books) {
        int[] counts=new int[COUNT];
        for (Book book:books) if (book.getLocation()>=1 && book.getLocation()<=COUNT) counts[book.getLocation()-1]++;
        return counts;
    }
}
