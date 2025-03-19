package com.example.bookcase;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

public class EditBookActivity extends AppCompatActivity {

    private EditText editTitle, editAuthor, editDescription, editImageUrl;
    private CheckBox editReadCheckBox;
    private SeekBar editProgressBar;
    private Spinner cubeSpinner;
    private Button saveButton, uploadImageButton;
    private ImageView bookCoverImage;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String bookId;
    private Uri imageUri; // Stores selected image
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_book);

        // Initialize Firestore & Firebase Storage
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize Views
        editTitle = findViewById(R.id.editTitle);
        editAuthor = findViewById(R.id.editAuthor);
        editDescription = findViewById(R.id.editDescription);
        editImageUrl = findViewById(R.id.editImageUrl);
        editReadCheckBox = findViewById(R.id.editReadCheckBox);
        editProgressBar = findViewById(R.id.editProgressBar);
        cubeSpinner = findViewById(R.id.cubeSpinner);
        saveButton = findViewById(R.id.saveButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        bookCoverImage = findViewById(R.id.bookCoverImage);

        // Load Cube Options in Spinner
        String[] cubeOptions = {"Cube 1", "Cube 2", "Cube 3", "Cube 4", "Cube 5", "Cube 6", "Cube 7", "Cube 8"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cubeOptions);
        cubeSpinner.setAdapter(adapter);

        // Get book details from Intent
        bookId = getIntent().getStringExtra("id");
        editTitle.setText(getIntent().getStringExtra("title"));
        editAuthor.setText(getIntent().getStringExtra("author"));
        editDescription.setText(getIntent().getStringExtra("description"));
        editImageUrl.setText(getIntent().getStringExtra("imageUrl"));
        editReadCheckBox.setChecked(getIntent().getBooleanExtra("isRead", false));
        editProgressBar.setProgress(getIntent().getIntExtra("progress", 0));
        cubeSpinner.setSelection(getIntent().getIntExtra("location", 1) - 1);

        // Load image if available
        String imageUrl = getIntent().getStringExtra("imageUrl");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(bookCoverImage);
        }

        // Upload Image Button Click
        uploadImageButton.setOnClickListener(v -> openFileChooser());

        // Save Changes Button
        saveButton.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageToFirebase();
            } else {
                saveBookDetails(null);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
                            .addOnSuccessListener(uri -> saveBookDetails(uri.toString()))
                            .addOnFailureListener(e -> Toast.makeText(EditBookActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show()))
                    .addOnFailureListener(e -> Toast.makeText(EditBookActivity.this, "Upload failed", Toast.LENGTH_SHORT).show());
        }
    }

    // Save book details to Firestore
    private void saveBookDetails(String imageUrl) {
        String newTitle = editTitle.getText().toString();
        String newAuthor = editAuthor.getText().toString();
        String newDescription = editDescription.getText().toString();
        String finalImageUrl = (imageUrl != null) ? imageUrl : editImageUrl.getText().toString();
        boolean newIsRead = editReadCheckBox.isChecked();
        int newProgress = editProgressBar.getProgress();
        int newCubeLocation = cubeSpinner.getSelectedItemPosition() + 1; // Convert to cube number (1-8)

        // Update Firestore document
        firestore.collection("books").document(bookId)
                .update(
                        "title", newTitle,
                        "author", newAuthor,
                        "description", newDescription,
                        "imageUrl", finalImageUrl,
                        "isRead", newIsRead,
                        "progress", newProgress,
                        "location", newCubeLocation
                )
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditBookActivity.this, "Book Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(EditBookActivity.this, "Failed to update book", Toast.LENGTH_SHORT).show());
    }
}
