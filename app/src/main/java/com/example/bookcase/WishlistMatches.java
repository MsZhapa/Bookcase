package com.example.bookcase;
import java.util.List;
public final class WishlistMatches {
    public enum Match { NONE, ISBN, SIMILAR_TITLE }
    private WishlistMatches() {}
    public static Match owned(WishItem wish,List<Book> books) {
        boolean similar=false;
        for (Book book:books) {
            if (Isbn.equivalent(wish.getIsbn(),book.getIsbn())) return Match.ISBN;
            if (!CatalogueKeys.fold(wish.getAuthor()).isEmpty()
                    && CatalogueKeys.fold(wish.getTitle()).equals(CatalogueKeys.fold(book.getTitle()))
                    && CatalogueKeys.fold(wish.getAuthor()).equals(CatalogueKeys.fold(book.getAuthor()))) similar=true;
        }
        return similar ? Match.SIMILAR_TITLE : Match.NONE;
    }
    public static boolean matches(WishItem wish,String query) {
        String text=CatalogueKeys.fold(query);
        if (text.isEmpty()) return true;
        String isbn=wish.getIsbn()==null ? "" : wish.getIsbn();
        return CatalogueKeys.fold(wish.getTitle()).contains(text) || CatalogueKeys.fold(wish.getAuthor()).contains(text)
                || (!text.replaceAll("[\\s\\p{Z}\\p{Pd}]","").isEmpty() && isbn.contains(text.replaceAll("[\\s\\p{Z}\\p{Pd}]","")))
                || Isbn.equivalent(query,isbn);
    }
}
