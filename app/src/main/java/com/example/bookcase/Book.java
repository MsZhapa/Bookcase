package com.example.bookcase;

import androidx.annotation.NonNull;

import java.util.UUID;


public class Book {

    @NonNull
    private String id = ""; // Add bookId field
    private String title;
    private String lendingStatus = "AVAILABLE";
    private String isbn;
    private int pageCount;
    private String author;
    private String description;
    private String imageUrl;
    private boolean isRead = false;
    private int progress;
    private int location;

    private long position;
    private String borrowerName;
    private boolean normalizedCopy;

    public long getPosition() { return position; }
    public void setPosition(long value) { position = value; }
    public String getBorrowerName() { return borrowerName; }
    public void setBorrowerName(String value) { borrowerName = value; }
    @com.google.firebase.firestore.Exclude
    public boolean isNormalizedCopy() { return normalizedCopy; }
    public void setNormalizedCopy(boolean value) { normalizedCopy = value; }

    // Required for Firestore deserialization
    public Book() {}

    public Book(String id, String title, String author,
                String description, String imageUrl,
                boolean isRead, int progress, int location) {

        this.id = id != null ? id : UUID.randomUUID().toString();
        this.title = title;
        this.author = author;
        this.description = description;
        this.imageUrl = imageUrl;
        this.isRead = isRead;
        this.progress = progress;
        this.location = location;
    }

    // ✅ **Getters and Setters**
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLendingStatus() { return lendingStatus; }
    public void setLendingStatus(String lendingStatus) { this.lendingStatus = lendingStatus; }


    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public int getLocation() { return location; }
    public void setLocation(int location) { this.location = location; }
}
