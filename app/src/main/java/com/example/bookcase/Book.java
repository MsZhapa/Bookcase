package com.example.bookcase;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "books")
public class Book {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String author;
    public int location; // Cube number (1-8)

    public Book(String title, String author, int location) {
        this.title = title;
        this.author = author;
        this.location = location;
    }
}
