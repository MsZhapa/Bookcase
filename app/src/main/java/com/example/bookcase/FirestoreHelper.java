package com.example.bookcase;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;

public class FirestoreHelper {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // Add a book
    public void addBook(Book book) {
        if (book.getId().isEmpty()) book.setId(db.collection("bookCopies").document().getId());
        new BookRepository(db).add(book)
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding book", e));
    }

    // Fetch all books
    public void getAllBooks(FirestoreCallback callback) {
        new BookRepository(db).getBooks(null)
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(callback::onFailure);
    }

    // Update book
    public void updateBook(String bookId, Book updatedBook) {
        new BookRepository(db).save(bookId, updatedBook.isNormalizedCopy(), updatedBook, updatedBook.getLocation())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Book updated"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating book", e));
    }

    // Delete book
    public void deleteBook(String bookId) {
        deleteBook(bookId, false);
    }

    public void deleteBook(String bookId, boolean normalizedCopy) {
        new BookRepository(db).copyRef(bookId, normalizedCopy).delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Book deleted"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting book", e));
    }

    // Callback interface
    public interface FirestoreCallback {
        void onSuccess(List<Book> bookList);
        void onFailure(Exception e);
    }
}
