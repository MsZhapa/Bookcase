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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;

public class AdminActivity extends AppCompatActivity {

    private EditText titleEditText, authorEditText, descriptionEditText;
    private CheckBox readCheckBox;
    private SeekBar progressSeekBar;
    private Spinner cubeSpinner;
    private Button addButton, uploadImageButton;
    private ImageView bookCoverImage;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private Uri imageUri; // Stores the selected image

    private TextView adminProgressPercentage;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firestore & Firebase Storage
        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialize Progress Views
        SeekBar adminProgressBar = findViewById(R.id.adminProgressBar);
        adminProgressPercentage = findViewById(R.id.adminProgressPercentage); // ✅ Initialize TextView

        // ✅ Update progress percentage when SeekBar changes
        adminProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                adminProgressPercentage.setText(progress + "%"); // ✅ Update TextView with progress percentage
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText("Add a New Book");

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> onBackPressed());

        // Initialize Views
        titleEditText = findViewById(R.id.titleEditText);
        authorEditText = findViewById(R.id.authorEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        readCheckBox = findViewById(R.id.readCheckBox);
        progressSeekBar = findViewById(R.id.adminProgressBar);
        cubeSpinner = findViewById(R.id.cubeSpinner);
        addButton = findViewById(R.id.addButton);
        uploadImageButton = findViewById(R.id.uploadImageButton);
        bookCoverImage = findViewById(R.id.bookCoverImage);

        // Load Cube Options in Spinner
        String[] cubeOptions = {"Cube 1", "Cube 2", "Cube 3", "Cube 4", "Cube 5", "Cube 6", "Cube 7", "Cube 8"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cubeOptions);
        cubeSpinner.setAdapter(adapter);

        // Upload Image Button Click
        uploadImageButton.setOnClickListener(v -> openFileChooser());

        // Add Book Button Click
        addButton.setOnClickListener(v -> {
            if (imageUri != null) {
                uploadImageToFirebase();
            } else {
                saveBookToFirestore(null);
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
                            .addOnSuccessListener(uri -> saveBookToFirestore(uri.toString()))
                            .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show()))
                    .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Upload failed", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveBookToFirestore(String imageUrl) {
        String title = titleEditText.getText().toString().trim();
        String author = authorEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String finalImageUrl = (imageUrl != null) ? imageUrl : ""; // Ensure it's empty if no image is uploaded
        boolean isRead = readCheckBox.isChecked();
        int progress = progressSeekBar.getProgress();
        int cubeLocation = cubeSpinner.getSelectedItemPosition() + 1; // Convert to cube number (1-8)

        if (title.isEmpty() || author.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Ensure bookId is **generated** before adding to Firestore
        String bookId = FirebaseFirestore.getInstance().collection("books").document().getId();

        // ✅ Create a **Book object** with the generated ID
        Book book = new Book(bookId, title, author, description, finalImageUrl, isRead, progress, cubeLocation);

        // ✅ Store the book in Firestore using its unique ID
        FirebaseFirestore.getInstance().collection("books").document(bookId)
                .set(book)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminActivity.this, "Book Added Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(AdminActivity.this, "Failed to add book", Toast.LENGTH_SHORT).show());
    }

}
