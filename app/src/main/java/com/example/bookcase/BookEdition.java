package com.example.bookcase;

public class BookEdition {

    private String id;
    private String workId;
    private String isbn;
    private String title;
    private String author;
    private String description;
    private String imageUrl;
    private int pageCount;

    public BookEdition() {
        // Required by Firestore
    }

    public BookEdition(String id, String workId, String isbn,
                       String title, String author, String description,
                       String imageUrl, int pageCount) {
        this.id = id;
        this.workId = workId;
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.description = description;
        this.imageUrl = imageUrl;
        this.pageCount = pageCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getWorkId() { return workId; }
    public void setWorkId(String workId) { this.workId = workId; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}