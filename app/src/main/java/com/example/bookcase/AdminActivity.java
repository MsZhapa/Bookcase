package com.example.bookcase;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.UUID;
import com.bumptech.glide.Glide;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdminActivity extends AppCompatActivity {

    private EditText titleEditText, authorEditText, descriptionEditText, isbnEditText;
    private CheckBox readCheckBox;
    private SeekBar progressSeekBar;
    private Spinner cubeSpinner;
    private ImageView bookCoverImage;
    private String retrievedCoverUrl = "";

    private boolean saving;
    private int retrievedPageCount = 0;
    private String retrievedIsbn = "";
    private StorageReference storageReference;
    private Uri imageUri; // Stores the selected image

    private TextView adminProgressPercentage;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        // Initialize Firebase Storage
        storageReference = FirebaseStorage.getInstance().getReference();

        // 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText("Add a New Book");

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Initialize Views
        titleEditText = findViewById(R.id.titleEditText);
        isbnEditText = findViewById(R.id.isbnEditText);
        Button findBookButton = findViewById(R.id.findBookButton);
        authorEditText = findViewById(R.id.authorEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        readCheckBox = findViewById(R.id.readCheckBox);
        progressSeekBar = findViewById(R.id.adminProgressBar);
        adminProgressPercentage = findViewById(R.id.adminProgressPercentage); // ✅ Initialize TextView
        cubeSpinner = findViewById(R.id.cubeSpinner);
        Button addButton = findViewById(R.id.addButton);
        Button uploadImageButton = findViewById(R.id.uploadImageButton);
        findBookButton.setOnClickListener(v -> {
            String isbn;
            try {
                isbn = Isbn.normalize(isbnEditText.getText().toString());
                if (isbn.isEmpty()) throw new IllegalArgumentException();
            } catch (IllegalArgumentException invalid) {
                isbnEditText.setError("Enter a valid ISBN-10 or ISBN-13 (including checksum)");
                return;
            }

            lookupBookByIsbn(isbn);
        });
        bookCoverImage = findViewById(R.id.bookCoverImage);

        // ✅ Update progress percentage when SeekBar changes
        progressSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                adminProgressPercentage.setText(progress + "%"); // ✅ Update TextView with progress percentage
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Load Cube Options in Spinner
        String[] cubeOptions = {"Cube 1", "Cube 2", "Cube 3", "Cube 4", "Cube 5", "Cube 6", "Cube 7", "Cube 8"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cubeOptions);
        cubeSpinner.setAdapter(adapter);
        // Only set the initial selection; Android restores user edits after recreation.
        if (savedInstanceState == null) {
            int selectedCube = getIntent().getIntExtra("selectedCube", -1);
            if (selectedCube >= 1 && selectedCube <= cubeOptions.length) {
                cubeSpinner.setSelection(selectedCube - 1);
            }
        }

        // Upload Image Button Click
        uploadImageButton.setOnClickListener(v -> openFileChooser());

        // Add Book Button Click
        addButton.setOnClickListener(v -> {
            if (saving) return;
            if (titleEditText.getText().toString().trim().isEmpty()
                    || authorEditText.getText().toString().trim().isEmpty()
                    || descriptionEditText.getText().toString().trim().isEmpty()) {
                Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Isbn.normalize(isbnEditText.getText().toString());
            } catch (IllegalArgumentException invalid) {
                isbnEditText.setError("Enter a valid ISBN-10 or ISBN-13 (including checksum)");
                return;
            }
            saving = true;
            if (imageUri != null) {
                uploadImageToFirebase();
            } else {
                saveBookToFirestore(retrievedCoverUrl);
            }
        });
    }

    // Opens file chooser to pick an image

    private void lookupBookByIsbn(String isbn) {
        retrievedCoverUrl = "";
        retrievedPageCount = 0;
        retrievedIsbn = "";

        imageUri = null;
        bookCoverImage.setImageResource(R.drawable.books1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            HttpURLConnection connection = null;

            try {
                URL url = new URL(
                        "https://openlibrary.org/isbn/" + isbn + ".json"
                );

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty(
                        "User-Agent", "Bookcase/2.0 (personal book catalog)"
                );

                int responseCode = connection.getResponseCode();

                if (responseCode == 404) {
                    mainHandler.post(() ->
                            Toast.makeText(this,
                                    "Book not found",
                                    Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                if (responseCode != 200) {
                    throw new Exception("HTTP " + responseCode);
                }

                StringBuilder response = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                connection.getInputStream(),
                                StandardCharsets.UTF_8
                        ))) {
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                }

                JSONObject book = new JSONObject(response.toString());

                int pageCount = book.optInt("number_of_pages", 0);

                String title = book.optString("title", "");
                String author = book.optString("by_statement", "");
                String description = "";

                Object descriptionValue = book.opt("description");

                if (descriptionValue instanceof String) {
                    description = (String) descriptionValue;
                } else if (descriptionValue instanceof JSONObject) {
                    description = ((JSONObject) descriptionValue)
                            .optString("value", "");
                }

// If the edition has no description, check the Work record
                if (description.isEmpty()) {
                    JSONArray works = book.optJSONArray("works");

                    if (works != null && works.length() > 0) {
                        String workKey = works.getJSONObject(0)
                                .optString("key", "");

                        if (workKey.matches("/works/OL\\d+W")) {
                            try {
                                JSONObject work = fetchJson(
                                        "https://openlibrary.org" + workKey + ".json"
                                );

                                Object workDescription = work.opt("description");

                                if (workDescription instanceof String) {
                                    description = (String) workDescription;
                                } else if (workDescription instanceof JSONObject) {
                                    description = ((JSONObject) workDescription)
                                            .optString("value", "");
                                }

                            } catch (Exception e) {
                                android.util.Log.w(
                                        "Bookcase",
                                        "Could not retrieve Work description",
                                        e
                                );
                            }
                        }
                    }
                }

                final String finalDescription = description;
                String coverUrl = "";

                JSONArray covers = book.optJSONArray("covers");

                if (covers != null && covers.length() > 0) {
                    int coverId = covers.optInt(0);

                    if (coverId > 0) {
                        coverUrl = "https://covers.openlibrary.org/b/id/"
                                + coverId + "-L.jpg";
                    }
                }

                final String finalCoverUrl = coverUrl;
                mainHandler.post(() -> {
                    retrievedCoverUrl = finalCoverUrl;

                    if (!finalCoverUrl.isEmpty()) {
                        Glide.with(AdminActivity.this)
                                .load(finalCoverUrl)
                                .placeholder(R.drawable.books1)
                                .error(R.drawable.books1)
                                .into(bookCoverImage);
                    } else {
                        bookCoverImage.setImageResource(R.drawable.books1);
                    }
                });


                mainHandler.post(() -> {

                    // Store the ISBN and page count for saving to Firestore
                    retrievedPageCount = pageCount;
                    retrievedIsbn = isbn;

                    // Fill in the book information
                    if (!title.isEmpty()) {
                        titleEditText.setText(title);
                    }

                    if (!author.isEmpty()) {
                        authorEditText.setText(author);
                    }

                    if (!finalDescription.isEmpty()) {
                        descriptionEditText.setText(finalDescription);
                    }

                    Toast.makeText(
                            AdminActivity.this,
                            "Book found! Pages: " + pageCount,
                            Toast.LENGTH_SHORT
                    ).show();
                });

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this,
                                "Lookup failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show()
                );

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }

                executor.shutdown();
            }
        });
    }
    private JSONObject fetchJson(String urlString) throws Exception {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) new URL(urlString).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty(
                    "User-Agent", "Bookcase/2.0 (personal book catalog)"
            );

            if (connection.getResponseCode() != 200) {
                throw new Exception("HTTP " + connection.getResponseCode());
            }

            StringBuilder response = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(),
                            StandardCharsets.UTF_8
                    ))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return new JSONObject(response.toString());

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

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
                            .addOnFailureListener(e -> { saving = false; Toast.makeText(AdminActivity.this, "Failed to get image URL", Toast.LENGTH_SHORT).show(); }))
                    .addOnFailureListener(e -> { saving = false; Toast.makeText(AdminActivity.this, "Upload failed", Toast.LENGTH_SHORT).show(); });
        }
    }
    private void saveNewBook(FirebaseFirestore db, String bookId, Book book) {

        new BookRepository(db).add(book)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminActivity.this,
                            "Book Added Successfully!",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    saving = false;
                    Toast.makeText(AdminActivity.this,
                            "Failed to add book",
                            Toast.LENGTH_SHORT).show();
                });
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
            saving = false;
            Toast.makeText(this, "All fields must be filled!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Ensure bookId is **generated** before adding to Firestore
        String bookId = FirebaseFirestore.getInstance().collection("bookCopies").document().getId();

        // ✅ Create a **Book object** with the generated ID
        Book book = new Book(bookId, title, author, description, finalImageUrl, isRead, progress, cubeLocation);
        try {
            book.setIsbn(Isbn.normalize(isbnEditText.getText().toString()));
        } catch (IllegalArgumentException invalid) {
            saving = false;
            isbnEditText.setError("Enter a valid ISBN-10 or ISBN-13 (including checksum)");
            return;
        }

        book.setPageCount(retrievedPageCount);

        // ✅ Store the book in Firestore using its unique ID
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String isbn = book.getIsbn();

        if (isbn == null || isbn.isEmpty()) {
            // No ISBN: save normally
            saveNewBook(db, bookId, book);
            return;
        }

        new BookRepository(db).getBooks(null)
                .addOnSuccessListener(querySnapshot -> {

                    Book existingCopy = querySnapshot.stream()
                            .filter(existing -> Isbn.equivalent(isbn, existing.getIsbn()))
                            .findFirst().orElse(null);
                    if (existingCopy == null) {
                        // No duplicate found
                        saveNewBook(db, bookId, book);

                    } else {
                        // Existing ISBN found
                        new androidx.appcompat.app.AlertDialog.Builder(AdminActivity.this)
                                .setTitle("Book already exists")
                                .setMessage("You already own a copy of this edition. Would you like to add another physical copy?")
                                .setPositiveButton("Add Another Copy", (dialog, which) -> {
                                    saveNewBook(db, bookId, book);
                                })
                                .setNeutralButton("View Existing", (dialog, which) -> {
                                    saving = false;
                                    Intent intent = new Intent(AdminActivity.this, BookDetailActivity.class);
                                    intent.putExtra("bookId", existingCopy.getId());
                                    intent.putExtra("normalizedCopy", existingCopy.isNormalizedCopy());
                                    startActivity(intent);
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> saving = false)
                                .setOnCancelListener(dialog -> saving = false)
                                .show();
                    }
                })
                .addOnFailureListener(e -> {
                    saving = false;
                    Toast.makeText(AdminActivity.this,
                            "Could not check for duplicates. Please try again.",
                            Toast.LENGTH_LONG).show();
                });
    }

}
