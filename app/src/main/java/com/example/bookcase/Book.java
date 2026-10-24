package com.example.bookcase;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import java.util.UUID;

@Entity(tableName = "books")
public class Book {
    @PrimaryKey
    @NonNull
    private String id = ""; // Add bookId field
    private String title;
    private String author;
    private String description;
    private String imageUrl;
    private boolean isRead = false;
    private int progress;
    private int location;

    // Default constructor (needed for Firestore & Room)
    public Book() {}

    // Constructor with title, author, location (used for preloading in BookDatabase)
    @Ignore
    public Book(String title, String author, int location) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.author = author;
        this.location = location;
    }

    // Constructor without ID (Used for Firestore auto-generated IDs)
    @Ignore
    public Book(String title, String author, String description, String imageUrl, boolean isRead, int progress, int location) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.author = author;
        this.description = description;
        this.imageUrl = imageUrl;
        this.isRead = isRead;
        this.progress = progress;
        this.location = location;
    }

    // ✅ **Constructor with ID**
    @Ignore
    public Book(String id, String title, String author, String description, String imageUrl, boolean isRead, int progress, int location) {
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
