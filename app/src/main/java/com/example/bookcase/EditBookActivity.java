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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

public class EditBookActivity extends AppCompatActivity {

    private EditText editTitle, editAuthor, editDescription;
    private CheckBox editReadCheckBox;

    private TextView editProgressPercentage;

    private SeekBar editProgressBar;
    private Spinner cubeSpinner;
    private Button saveButton, uploadImageButton;
    private ImageView bookCoverImage;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private String bookId;
    private Uri imageUri;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_book);

        // Initialize Firestore & Storage
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize Views
        editTitle = findViewById(R.id.editTitle);
        editProgressPercentage = findViewById(R.id.editProgressPercentage); // ✅ Initialize Percentage TextView
        editAuthor = findViewById(R.id.editAuthor);
        editDescription = findViewById(R.id.editDescription);
        editReadCheckBox = findViewById(R.id.editReadCheckBox);
        editProgressBar = findViewById(R.id.editProgressBar);
        cubeSpinner = findViewById(R.id.cubeSpinner);
        saveButton = findViewById(R.id.saveButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        bookCoverImage = findViewById(R.id.bookCoverImage);

        // 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText("Edit Book");

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Load Cube Options in Spinner
        String[] cubeOptions = {"Cube 1", "Cube 2", "Cube 3", "Cube 4", "Cube 5", "Cube 6", "Cube 7", "Cube 8"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cubeOptions);
        cubeSpinner.setAdapter(adapter);

        // ✅ Get bookId from Intent
        bookId = getIntent().getStringExtra("bookId");
        if (bookId == null || bookId.isEmpty()) {
            Toast.makeText(this, "Error: Book ID not found!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // ✅ Load book details from Firestore
        loadBookDetails();

        // ✅ Update progress percentage when SeekBar changes
        editProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                editProgressPercentage.setText(progress + "%"); // ✅ Update TextView with progress percentage
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Upload Image Button
        uploadImageButton.setOnClickListener(v -> openFileChooser());

        // Save Button
        saveButton.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageToFirebase();
            } else {
                saveBookDetails(null);
            }
        });
    }

    // 📌 **Step 1: Load Book Details from Firestore**
    // ✅ Load Book Details
    private void loadBookDetails() {
        firestore.collection("books").document(bookId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Book book = documentSnapshot.toObject(Book.class);
                        if (book != null) {
                            editTitle.setText(book.getTitle());
                            editAuthor.setText(book.getAuthor());
                            editDescription.setText(book.getDescription());
                            editReadCheckBox.setChecked(Boolean.TRUE.equals(book.isRead()));
                            editProgressBar.setProgress(book.getProgress());
                            editProgressPercentage.setText(book.getProgress() + "%"); // ✅ Set initial progress value

                            cubeSpinner.setSelection(book.getLocation() - 1);

                            if (book.getImageUrl() != null && !book.getImageUrl().isEmpty()) {
                                Glide.with(this).load(book.getImageUrl()).into(bookCoverImage);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(EditBookActivity.this, "Error loading book", Toast.LENGTH_SHORT).show()
                );
    }

    // 📌 **Step 2: Allow Image Upload**
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Book Cover"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            bookCoverImage.setImageURI(imageUri);
        }
    }

    // 📌 **Step 3: Upload Image to Firebase**
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

    // 📌 **Step 4: Save Updated Book Details**
    private void saveBookDetails(String imageUrl) {
        String newTitle = editTitle.getText().toString();
        String newAuthor = editAuthor.getText().toString();
        String newDescription = editDescription.getText().toString();
        String finalImageUrl = (imageUrl != null) ? imageUrl : getIntent().getStringExtra("imageUrl");
        boolean newIsRead = editReadCheckBox.isChecked();
        int newProgress = editProgressBar.getProgress();
        int newCubeLocation = cubeSpinner.getSelectedItemPosition() + 1;

        firestore.collection("books").document(bookId)
                .update("title", newTitle, "author", newAuthor, "description", newDescription,
                        "imageUrl", finalImageUrl, "isRead", newIsRead, "progress", newProgress, "location", newCubeLocation)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditBookActivity.this, "Book Updated", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(EditBookActivity.this, "Failed to update book", Toast.LENGTH_SHORT).show());
    }

}
