package com.example.bookcase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;

public class BookDetailActivity extends AppCompatActivity {

    private TextView titleTextView, authorTextView, locationTextView, descriptionTextView;
    private ImageView bookImageView;
    private CheckBox readCheckBox;
    private ProgressBar progressBar;
    private Button moveButton, editBookButton, deleteBookButton, setReminderButton;
    private String bookId;
    private FirebaseFirestore firestore;
    private int currentCube, progress;
    private boolean isRead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        firestore = FirebaseFirestore.getInstance();

        titleTextView = findViewById(R.id.titleTextView);
        authorTextView = findViewById(R.id.authorTextView);
        locationTextView = findViewById(R.id.locationTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        bookImageView = findViewById(R.id.bookImageView);
        readCheckBox = findViewById(R.id.readCheckBox);
        progressBar = findViewById(R.id.progressBar);
        moveButton = findViewById(R.id.moveButton);
        editBookButton = findViewById(R.id.editBookButton);
        deleteBookButton = findViewById(R.id.deleteBookButton);
        setReminderButton = findViewById(R.id.setReminderButton);

        bookId = getIntent().getStringExtra("id");
        if (bookId != null) {
            listenForBookUpdates(bookId);
        }

        moveButton.setOnClickListener(v -> showMoveBookDialog());
        editBookButton.setOnClickListener(v -> {
            Intent intent = new Intent(BookDetailActivity.this, EditBookActivity.class);
            intent.putExtra("id", bookId);
            startActivity(intent);
        });
        deleteBookButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        setReminderButton.setOnClickListener(v -> setReadingReminder());
    }

    private void listenForBookUpdates(String bookId) {
        DocumentReference docRef = firestore.collection("books").document(bookId);
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.e("Firestore", "Error fetching book details", e);
                    return;
                }
                if (snapshot != null && snapshot.exists()) {
                    Log.d("Firestore", "Book Data: " + snapshot.getData()); // 🔍 DEBUG

                    Book book = snapshot.toObject(Book.class);
                    if (book != null) {
                        titleTextView.setText(book.getTitle());
                        authorTextView.setText("by " + book.getAuthor());
                        locationTextView.setText("Current Cube: " + book.getLocation());
                        descriptionTextView.setText(book.getDescription());
                        readCheckBox.setChecked(book.isRead());

                        progress = book.getProgress();
                        currentCube = book.getLocation();
                        isRead = book.isRead();

                        progressBar.setProgress(progress);
                        progressBar.setVisibility(isRead ? View.GONE : View.VISIBLE);

                        // 🔍 Debug Image URL
                        Log.d("Firestore", "Image URL: " + book.getImageUrl());

                        // Fix for Image Not Loading
                        if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
                            Glide.with(BookDetailActivity.this)
                                    .load(book.getImageUrl())
                                    .placeholder(R.drawable.books1) // Default placeholder
                                    .into(bookImageView);
                        } else {
                            bookImageView.setImageResource(R.drawable.books1);
                        }
                    }
                } else {
                    Log.e("Firestore", "Book document does not exist!");
                }
            }
        });
    }


    private void showMoveBookDialog() {
        String[] cubeOptions = {"Cube 1", "Cube 2", "Cube 3", "Cube 4", "Cube 5", "Cube 6", "Cube 7", "Cube 8"};

        new AlertDialog.Builder(this)
                .setTitle("Move Book to:")
                .setItems(cubeOptions, (dialog, which) -> {
                    int newCube = which + 1;
                    if (newCube != currentCube) {
                        moveBookToCube(bookId, newCube);
                    } else {
                        Toast.makeText(BookDetailActivity.this, "Book is already in Cube " + newCube, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void moveBookToCube(String bookId, int newCube) {
        firestore.collection("books").document(bookId)
                .update("location", newCube)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(BookDetailActivity.this, "Book moved to Cube " + newCube, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(BookDetailActivity.this, "Failed to move book", Toast.LENGTH_SHORT).show());
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Book")
                .setMessage("Are you sure you want to delete this book?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBook(bookId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBook(String bookId) {
        firestore.collection("books").document(bookId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(BookDetailActivity.this, "Book deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(BookDetailActivity.this, "Failed to delete book", Toast.LENGTH_SHORT).show());
    }

    private void setReadingReminder() {
        Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show();
    }
}
