package com.example.bookcase;
public class WishItem {
    private String id,title,author,isbn;
    private long createdAt;
    public WishItem() {}
    public String getId() { return id; } public void setId(String value) { id=value; }
    public String getTitle() { return title; } public void setTitle(String value) { title=value; }
    public String getAuthor() { return author; } public void setAuthor(String value) { author=value; }
    public String getIsbn() { return isbn; } public void setIsbn(String value) { isbn=value; }
    public long getCreatedAt() { return createdAt; } public void setCreatedAt(long value) { createdAt=value; }
    public static WishItem create(String title,String author,String isbn) {
        if (CatalogueKeys.fold(title).isEmpty()) throw new IllegalArgumentException("Enter a book title");
        if (title.length()>300 || (author!=null && author.length()>300)) throw new IllegalArgumentException("Title and author must be 300 characters or fewer");
        WishItem wish=new WishItem(); wish.title=title.trim(); wish.author=author==null ? "" : author.trim();
        wish.isbn=Isbn.normalize(isbn);
        wish.id=wish.isbn.isEmpty() ? "title-"+CatalogueKeys.hash(CatalogueKeys.fold(wish.title)+"\u0000"+CatalogueKeys.fold(wish.author)) : "isbn-"+wish.isbn;
        wish.createdAt=System.currentTimeMillis(); return wish;
    }
}
