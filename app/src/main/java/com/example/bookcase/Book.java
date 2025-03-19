package com.example.bookcase;

public class Book {
    private String id; // Firestore document ID
    private String title;
    private String author;
    private String description;
    private String imageUrl;
    private boolean isRead;
    private int progress;
    private int location; // Cube location (1-8)

    // Empty constructor required for Firestore
    public Book() {}

    // Constructor
    public Book(String title, String author, String description, String imageUrl, boolean isRead, int progress, int location) {
        this.title = title;
        this.author = author;
        this.description = description;
        this.imageUrl = imageUrl;
        this.isRead = isRead;
        this.progress = progress;
        this.location = location;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }

    public int getLocation() { return location; }
    public void setLocation(int location) { this.location = location; }
}
