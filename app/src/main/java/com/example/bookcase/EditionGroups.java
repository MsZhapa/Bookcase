package com.example.bookcase;
import java.util.*;
public final class EditionGroups {
    private EditionGroups() {}
    public static String baseTitle(String title) {
        if (title==null) return "";
        String base=title.replaceFirst("(?i)\\s+(?:[-–:,]\\s*)?(?:\\(?\\d+(?:st|nd|rd|th)?\\s*(?:edition|ed\\.?)\\)?|\\(?edition\\s+\\d+\\)?)\\s*$","").trim();
        return base.isEmpty() ? title.trim() : base;
    }
    public static String key(Book book) {
        String title=CatalogueKeys.fold(baseTitle(book.getTitle())), author=CatalogueKeys.fold(book.getAuthor());
        if (title.isEmpty() || author.isEmpty()) return "unknown-"+CatalogueKeys.hash((book.isNormalizedCopy()?"copies/":"books/")+book.getId());
        return CatalogueKeys.hash(title+"\u0000"+author);
    }
    public static List<Group> group(List<Book> books) {
        Map<String,Group> groups=new LinkedHashMap<>();
        for (Book book:books) {
            String key=key(book);
            Group group=groups.get(key);
            if (group==null) { group=new Group(key,baseTitle(book.getTitle()),book.getAuthor()); groups.put(key,group); }
            group.copies.add(book);
            try {
                String isbn=Isbn.normalize(book.getIsbn());
                if (isbn.isEmpty()) group.withoutIsbn++; else group.isbns.add(isbn);
            } catch (IllegalArgumentException invalid) { group.withoutIsbn++; }
        }
        List<Group> result=new ArrayList<>(groups.values());
        for (Group group:result) group.copies.sort(BookRepository.SHELF_ORDER);
        result.sort(Comparator.comparing((Group group)->CatalogueKeys.fold(group.title)).thenComparing(group->CatalogueKeys.fold(group.author)).thenComparing(group->group.key));
        return result;
    }
    public static final class Group {
        public final String key,title,author;
        public final List<Book> copies=new ArrayList<>();
        public final Set<String> isbns=new HashSet<>();
        public int withoutIsbn;
        Group(String key,String title,String author) { this.key=key; this.title=title; this.author=author; }
    }
}
