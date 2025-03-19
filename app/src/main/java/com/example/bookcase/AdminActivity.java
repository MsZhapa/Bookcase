package com.example.bookcase;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminActivity extends AppCompatActivity {

    private EditText editTitle, editAuthor, editDescription;
    private Spinner cubeSpinner;
    private Button addBookButton, uploadImageButton;
    private ImageView bookCoverImage;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Uri imageUri; // Stores selected image
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);



        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Ensure user is signed in before proceeding
        if (auth.getCurrentUser() == null) {
            signInAnonymously();
        }

        // Enable Firestore Debugging
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)  // Force Firestore to always use network
                .build();
        firestore.setFirestoreSettings(settings);

        Log.d("FirestoreDebug", "Firestore initialized successfully");

        // Initialize Views
        editTitle = findViewById(R.id.editTitle);
        editAuthor = findViewById(R.id.editAuthor);
        editDescription = findViewById(R.id.editDescription);
        cubeSpinner = findViewById(R.id.cubeSpinner);
        addBookButton = findViewById(R.id.addBookButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        bookCoverImage = findViewById(R.id.bookCoverImage);

        // Load Cube Options in Spinner
        String[] cubeOptions = {"Cube 1", "Cube 2", "Cube 3", "Cube 4", "Cube 5", "Cube 6", "Cube 7", "Cube 8"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cubeOptions);
        cubeSpinner.setAdapter(adapter);

        // Upload Image Button Click
        uploadImageButton.setOnClickListener(v -> openFileChooser());

        // Add Book Button Click
        addBookButton.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageToFirebase();
            } else {
                addBookToFirestore(null);
            }
        });
    }

    // 📌 Sign in the user anonymously to prevent FirebaseAuth NullPointerException
    private void signInAnonymously() {
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        Log.d("FirebaseAuth", "Signed in anonymously as: " + (user != null ? user.getUid() : "null"));
                    } else {
                        Toast.makeText(this, "Firebase Authentication failed.", Toast.LENGTH_SHORT).show();
                        Log.e("FirebaseAuth", "Anonymous sign-in failed", task.getException());
                    }
                });
    }

    // Opens file chooser to pick an image
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Book Cover"), PICK_IMAGE_REQUEST);
    }

    // Handles selected image result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            bookCoverImage.setImageURI(imageUri); // Preview selected image
        }
    }

    // Upload image to Firebase Storage
    private void uploadImageToFirebase() {
        if (imageUri != null) {
            String fileName = "book_covers/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference fileReference = storageReference.child(fileName);

            fileReference.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> fileReference.getDownloadUrl()
                            .addOnSuccessListener(uri -> addBookToFirestore(uri.toString()))
                            .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show()))
                    .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Upload failed", Toast.LENGTH_SHORT).show());
        }
    }

    // 📌 Add Book to Firestore
    private void addBookToFirestore(String imageUrl) {
        String title = editTitle.getText().toString().trim();
        String author = editAuthor.getText().toString().trim();
        String description = editDescription.getText().toString().trim();
        int location = cubeSpinner.getSelectedItemPosition() + 1; // Convert to Cube number (1-8)

        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(this, "Title and Author are required", Toast.LENGTH_SHORT).show();
            Log.e("FirestoreDebug", "Book not added: Title or Author missing");
            return;
        }

        // Create book object
        Map<String, Object> book = new HashMap<>();
        book.put("title", title);
        book.put("author", author);
        book.put("description", description);
        book.put("imageUrl", imageUrl != null ? imageUrl : "");
        book.put("isRead", false);
        book.put("progress", 0);
        book.put("location", location);

        Log.d("FirestoreDebug", "Attempting to save book: " + book);

        // Save to Firestore
        firestore.collection("books")
                .add(book)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FirestoreDebug", "Book added successfully: " + documentReference.getId());
                    Toast.makeText(AdminActivity.this, "Book added!", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreDebug", "Error adding book", e);
                    Toast.makeText(AdminActivity.this, "Failed to add book: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // 📌 Clear input fields after adding a book
    private void clearFields() {
        editTitle.setText("");
        editAuthor.setText("");
        editDescription.setText("");
        bookCoverImage.setImageResource(R.drawable.books1); // Reset to default
        cubeSpinner.setSelection(0);
        imageUri = null; // Clear selected image
    }
}
