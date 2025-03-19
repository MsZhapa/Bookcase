package com.example.bookcase;

import android.content.Intent;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private TextView titleTextView, authorTextView, descriptionTextView, locationTextView, readStatusTextView;
    private ImageView bookImageView;

    private TextView progressPercentage;
    private ProgressBar progressBar;
    private LinearLayout progressLayout;
    private CheckBox readCheckBox;
    private Button moveButton, editBookButton, deleteBookButton;
    private FirebaseFirestore firestore;
    private String bookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        // 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText("Book");

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Initialize views
        titleTextView = findViewById(R.id.titleTextView);
        authorTextView = findViewById(R.id.authorTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        locationTextView = findViewById(R.id.locationTextView);
        readStatusTextView = findViewById(R.id.readStatusTextView);
        bookImageView = findViewById(R.id.bookImageView);
        readCheckBox = findViewById(R.id.readCheckBox);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage); // ✅ Initialize TextView
        progressLayout = findViewById(R.id.progressLayout); // ✅ Initialize Layout Container
        moveButton = findViewById(R.id.moveButton);
        editBookButton = findViewById(R.id.editBookButton);
        deleteBookButton = findViewById(R.id.deleteBookButton);

        firestore = FirebaseFirestore.getInstance();

        // Get book ID from intent
        bookId = getIntent().getStringExtra("bookId");
        if (bookId == null) {
            Toast.makeText(this, "Error: Book ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch book details
        loadBookDetails();

        // Edit Book Button
        editBookButton.setOnClickListener(v -> {
            Intent intent = new Intent(BookDetailActivity.this, EditBookActivity.class);
            intent.putExtra("bookId", bookId);
            startActivity(intent);
        });

        // Handle Delete Book Button Click
        if (deleteBookButton != null) {
            deleteBookButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        }

        // Move Book Button
        moveButton.setOnClickListener(v -> showMoveBookDialog());
    }

    private void loadBookDetails() {
        firestore.collection("books").document(bookId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Book book = documentSnapshot.toObject(Book.class);
                        if (book != null) {
                            titleTextView.setText(book.getTitle());
                            authorTextView.setText("by " + book.getAuthor());
                            locationTextView.setText("Current Cube: " + book.getLocation());
                            descriptionTextView.setText(book.getDescription());

                            boolean isRead = Boolean.TRUE.equals(book.isRead());
                            readCheckBox.setChecked(isRead);

                            int progress = book.getProgress();
                            progressBar.setProgress(progress);
                            progressPercentage.setText(progress + "%"); // ✅ Update progress text

                            // ✅ Show or Hide ProgressBar and Indicator Based on Read Status
                            if (isRead) {
                                progressLayout.setVisibility(View.GONE);
                            } else {
                                progressLayout.setVisibility(View.VISIBLE);
                            }

                            // ✅ Load book image if available
                            if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
                                Glide.with(this).load(book.getImageUrl()).into(bookImageView);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(BookDetailActivity.this, "Error loading book", Toast.LENGTH_SHORT).show());
    }

    // 📌 Show Delete Confirmation Dialog
    private void showDeleteConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Book")
                .setMessage("Are you sure you want to delete this book?")
                .setPositiveButton("Delete", (dialog, which) -> deleteBook())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }


    // 📌 Delete book from Firestore
    // 📌 Delete Book from Firestore
    private void deleteBook() {
        if (bookId != null) {
            firestore.collection("books").document(bookId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(BookDetailActivity.this, "Book deleted", Toast.LENGTH_SHORT).show();
                        finish(); // Close activity after deletion
                    })
                    .addOnFailureListener(e -> Toast.makeText(BookDetailActivity.this, "Failed to delete book", Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Error: Book ID not found!", Toast.LENGTH_SHORT).show();
        }
    }

    // 📌 Show Move Book Dialog
    private void showMoveBookDialog() {
        String[] cubeOptions = {"Cube 1", "Cube 2", "Cube 3", "Cube 4", "Cube 5", "Cube 6", "Cube 7", "Cube 8"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Move Book to:")
                .setItems(cubeOptions, (dialog, which) -> {
                    int newCube = which + 1;
                    firestore.collection("books").document(bookId)
                            .update("location", newCube)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(BookDetailActivity.this, "Book moved to Cube " + newCube, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(BookDetailActivity.this, "Failed to move book", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
