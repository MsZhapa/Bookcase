package com.example.bookcase;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.List;

public class FirestoreHelper {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference booksRef = db.collection("books");

    // Add a book
    public void addBook(Book book) {
        booksRef.add(book)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Book added with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e("Firestore", "Error adding book", e));
    }

    // Fetch all books
    public void getAllBooks(FirestoreCallback callback) {
        booksRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Book> bookList = task.getResult().toObjects(Book.class);
                callback.onSuccess(bookList);
            } else {
                callback.onFailure(task.getException());
            }
        });
    }

    // Update book
    public void updateBook(String bookId, Book updatedBook) {
        booksRef.document(bookId).set(updatedBook)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Book updated"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error updating book", e));
    }

    // Delete book
    public void deleteBook(String bookId) {
        booksRef.document(bookId).delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Book deleted"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error deleting book", e));
    }

    // Callback interface
    public interface FirestoreCallback {
        void onSuccess(List<Book> bookList);
        void onFailure(Exception e);
    }
}
