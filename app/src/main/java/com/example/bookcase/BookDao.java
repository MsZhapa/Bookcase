package com.example.bookcase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface BookDao {

    @Insert
    void insert(Book book);

    @Query("SELECT * FROM books WHERE location = :cubeNumber")
    List<Book> getBooksByCube(int cubeNumber);

    @Query("SELECT * FROM books")
    List<Book> getAllBooks();

    @Query("UPDATE books SET location = :newLocation WHERE id = :bookId")
    void updateBookLocation(int bookId, int newLocation);

}
