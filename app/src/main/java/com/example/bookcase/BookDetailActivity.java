package com.example.bookcase;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class BookDetailActivity extends AppCompatActivity {

    private TextView titleTextView, authorTextView, descriptionTextView, locationTextView, lendingInfoTextView;
    private ImageView bookImageView;

    private LinearLayout lendingHistoryContainer;

    private TextView progressPercentage;
    private ProgressBar progressBar;
    private LinearLayout progressLayout;
    private CheckBox readCheckBox;
    private Button moveButton, deleteBookButton, lendBookButton;
    private boolean isLent = false;
    private com.google.firebase.Timestamp selectedDueDate = null;
    private FirebaseFirestore firestore;
    private String bookId;
    private boolean normalizedCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        // 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText("Book");

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // Initialize views
        titleTextView = findViewById(R.id.titleTextView);
        authorTextView = findViewById(R.id.authorTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        locationTextView = findViewById(R.id.locationTextView);
        lendingInfoTextView = findViewById(R.id.lendingInfoTextView);
        bookImageView = findViewById(R.id.bookImageView);
        readCheckBox = findViewById(R.id.readCheckBox);
        progressBar = findViewById(R.id.progressBar);
        progressPercentage = findViewById(R.id.progressPercentage); // ✅ Initialize TextView
        progressLayout = findViewById(R.id.progressLayout); // ✅ Initialize Layout Container
        moveButton = findViewById(R.id.moveButton);
        Button editBookButton = findViewById(R.id.editBookButton);
        deleteBookButton = findViewById(R.id.deleteBookButton);
        lendBookButton = findViewById(R.id.lendBookButton);
        lendingHistoryContainer = findViewById(R.id.lendingHistoryContainer);

        firestore = FirebaseFirestore.getInstance();

        // Get book ID from intent
        normalizedCopy = getIntent().getBooleanExtra("normalizedCopy", false);
        bookId = getIntent().getStringExtra("bookId");
        if (bookId == null) {
            Toast.makeText(this, "Error: Book ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch book details
        loadBookDetails();
        loadLendingHistory();

        // Edit Book Button
        editBookButton.setOnClickListener(v -> {
            Intent intent = new Intent(BookDetailActivity.this, EditBookActivity.class);
            intent.putExtra("bookId", bookId);
            intent.putExtra("normalizedCopy", normalizedCopy);
            startActivity(intent);
        });

        // Handle Delete Book Button Click
        if (deleteBookButton != null) {
            deleteBookButton.setOnClickListener(v -> showDeleteConfirmationDialog());
        }

        // Move Book Button
        moveButton.setOnClickListener(v -> showMoveBookDialog());
        lendBookButton.setOnClickListener(v -> {
            if (isLent) {
                showReturnConfirmation();
            } else {
                showLendBookDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bookId != null) loadBookDetails();
    }

    private void loadBookDetails() {
        new BookRepository(firestore).load(bookId, normalizedCopy)
                .addOnSuccessListener(book -> {
                        if (book != null) {
                            isLent = "LENT".equals(book.getLendingStatus());
                            lendBookButton.setText(isLent ? "Mark as Returned" : "Lend Book");
                            if (isLent) {
                                String borrower = book.getBorrowerName();

                                String lendingInfo = "Lent out";

                                if (borrower != null && !borrower.isEmpty()) {
                                    lendingInfo += " to " + borrower;
                                }

                                lendingInfoTextView.setText(lendingInfo);
                                lendingInfoTextView.setVisibility(View.VISIBLE);

                            } else {
                                lendingInfoTextView.setVisibility(View.GONE);
                            }
                            titleTextView.setText(book.getTitle());
                            authorTextView.setText("by " + book.getAuthor());
                            locationTextView.setText("Current Cube: " + book.getLocation()
                                    + (book.getPosition() > 0 ? " · Position: " + book.getPosition() : ""));
                            descriptionTextView.setText(book.getDescription());

                            boolean isRead = book.isRead();
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
                })
                .addOnFailureListener(e -> Toast.makeText(BookDetailActivity.this, "Error loading book", Toast.LENGTH_SHORT).show());
    }
    private void loadLendingHistory() {

        firestore.collection("lendingRecords")
                .whereEqualTo("bookId", bookId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    lendingHistoryContainer.removeAllViews();

                    java.util.List<LendingRecord> records =
                            querySnapshot.toObjects(LendingRecord.class);

                    // Newest loans first
                    records.sort((a, b) -> {
                        if (a.getLentDate() == null && b.getLentDate() == null) return 0;
                        if (a.getLentDate() == null) return 1;
                        if (b.getLentDate() == null) return -1;
                        return b.getLentDate().compareTo(a.getLentDate());
                    });

                    if (records.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No lending history yet.");
                        emptyText.setTextSize(14);
                        lendingHistoryContainer.addView(emptyText);
                        return;
                    }

                    for (LendingRecord record : records) {

                        TextView recordView = new TextView(this);

                        String status = "ACTIVE".equals(record.getStatus())
                                ? "Lent out"
                                : "Returned";

                        String details =
                                "Borrower: " + record.getBorrowerName()
                                        + "\nLent: " + formatLendingDate(record.getLentDate())
                                        + "\nStatus: " + status;

                        if (record.getDueDate() != null) {
                            details += "\nDue: "
                                    + formatLendingDate(record.getDueDate());

                            if ("ACTIVE".equals(record.getStatus())
                                    && record.getDueDate().toDate()
                                    .before(new java.util.Date())) {
                                details += " (Overdue)";
                            }
                        }

                        if (record.getReturnedDate() != null) {
                            details += "\nReturned: "
                                    + formatLendingDate(record.getReturnedDate());
                        }

                        recordView.setText(details);
                        recordView.setTextSize(14);
                        recordView.setPadding(16, 16, 16, 16);

                        LinearLayout.LayoutParams params =
                                new LinearLayout.LayoutParams(
                                        LinearLayout.LayoutParams.MATCH_PARENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT
                                );

                        params.bottomMargin = 12;
                        recordView.setLayoutParams(params);

                        lendingHistoryContainer.addView(recordView);
                    }
                })
                .addOnFailureListener(e -> {
                    lendingHistoryContainer.removeAllViews();

                    TextView errorText = new TextView(this);
                    errorText.setText("Unable to load lending history.");
                    lendingHistoryContainer.addView(errorText);
                });
    }

    private String formatLendingDate(com.google.firebase.Timestamp timestamp) {

        if (timestamp == null) {
            return "Not specified";
        }

        java.text.SimpleDateFormat formatter =
                new java.text.SimpleDateFormat(
                        "dd MMM yyyy",
                        java.util.Locale.getDefault()
                );

        return formatter.format(timestamp.toDate());
    }

    private void showLendBookDialog() {

        android.widget.EditText borrowerInput =
                new android.widget.EditText(this);

        borrowerInput.setHint("Borrower's name");
        borrowerInput.setSingleLine(true);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, 8, padding, 0);
        container.addView(borrowerInput);

        // Reset due date for each new loan
        selectedDueDate = null;

        Button dueDateButton = new Button(this);
        dueDateButton.setText("Set due date (optional)");
        container.addView(dueDateButton);

        // Due date picker
        dueDateButton.setOnClickListener(v -> {

            java.util.Calendar calendar = java.util.Calendar.getInstance();

            android.app.DatePickerDialog datePicker =
                    new android.app.DatePickerDialog(
                            this,
                            (view, year, month, day) -> {

                                java.util.Calendar selected =
                                        java.util.Calendar.getInstance();

                                selected.set(year, month, day, 23, 59, 59);
                                selected.set(java.util.Calendar.MILLISECOND, 0);

                                selectedDueDate =
                                        new com.google.firebase.Timestamp(
                                                selected.getTime());

                                dueDateButton.setText(
                                        String.format(
                                                java.util.Locale.getDefault(),
                                                "Due: %02d/%02d/%04d",
                                                day, month + 1, year
                                        )
                                );
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    );

            datePicker.getDatePicker().setMinDate(
                    System.currentTimeMillis()
            );

            datePicker.show();
        });

        // Lending dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Lend Book")
                .setMessage("Who are you lending this book to?")
                .setView(container)
                .setPositiveButton("Lend Book", null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {

                            String borrower = borrowerInput.getText()
                                    .toString().trim();

                            if (borrower.isEmpty()) {
                                borrowerInput.setError("Enter a name");
                                return;
                            }

                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setEnabled(false);

                            // Current loan information
                            com.google.firebase.Timestamp lentDate =
                                    com.google.firebase.Timestamp.now();

                            java.util.Map<String, Object> updates =
                                    new java.util.HashMap<>();

                            updates.put("lendingStatus", "LENT");
                            updates.put("borrowerName", borrower);
                            updates.put("lentDate", lentDate);

                            if (selectedDueDate != null) {
                                updates.put("dueDate", selectedDueDate);
                            } else {
                                updates.put("dueDate",
                                        com.google.firebase.firestore.FieldValue.delete());
                            }

                            // Reference to the physical book
                            com.google.firebase.firestore.DocumentReference bookRef =
                                    new BookRepository(firestore).copyRef(bookId, normalizedCopy);

                            // Create a unique lending history record
                            com.google.firebase.firestore.DocumentReference loanRef =
                                    firestore.collection("lendingRecords").document();

                            LendingRecord loan = new LendingRecord(
                                    loanRef.getId(),
                                    bookId,
                                    borrower,
                                    lentDate,
                                    selectedDueDate
                            );

                            // Link the book to its active loan
                            updates.put("activeLoanId", loanRef.getId());

                            // Save both documents atomically
                            firestore.runTransaction(transaction -> {
                                com.google.firebase.firestore.DocumentSnapshot current = transaction.get(bookRef);
                                if (!current.exists()) throw new IllegalStateException("Book not found");
                                if ("LENT".equals(current.getString("lendingStatus")))
                                    throw new IllegalStateException("Book is already lent");
                                transaction.update(bookRef, updates);
                                transaction.set(loanRef, loan);
                                return null;
                            })
                                    .addOnSuccessListener(unused -> {

                                        isLent = true;
                                        lendBookButton.setText("Mark as Returned");

                                        // Refresh details and history
                                        loadBookDetails();
                                        loadLendingHistory();

                                        Toast.makeText(
                                                BookDetailActivity.this,
                                                "Book lent to " + borrower,
                                                Toast.LENGTH_SHORT
                                        ).show();

                                        dialog.dismiss();
                                    })
                                    .addOnFailureListener(e -> {

                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                .setEnabled(true);

                                        Toast.makeText(
                                                BookDetailActivity.this,
                                                "Failed to lend book: " + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show();
                                    });
                        }));

        dialog.show();
    }


    private void showReturnConfirmation() {

        new AlertDialog.Builder(this)
                .setTitle("Return Book")
                .setMessage("Mark this book as returned?")
                .setPositiveButton("Mark as Returned", (dialog, which) -> {

                    lendBookButton.setEnabled(false);

                    com.google.firebase.firestore.DocumentReference bookRef =
                            new BookRepository(firestore).copyRef(bookId, normalizedCopy);

                    firestore.runTransaction(transaction -> {

                        com.google.firebase.firestore.DocumentSnapshot bookSnapshot =
                                transaction.get(bookRef);

                        if (!bookSnapshot.exists()) {
                            throw new IllegalStateException("Book not found");
                        }

                        String activeLoanId =
                                bookSnapshot.getString("activeLoanId");

                        // Clear current lending information
                        java.util.Map<String, Object> updates =
                                new java.util.HashMap<>();

                        updates.put("lendingStatus", "AVAILABLE");
                        updates.put("borrowerName", "");
                        updates.put("lentDate",
                                com.google.firebase.firestore.FieldValue.delete());
                        updates.put("dueDate",
                                com.google.firebase.firestore.FieldValue.delete());
                        updates.put("activeLoanId",
                                com.google.firebase.firestore.FieldValue.delete());

                        // Update the lending history if this loan has a record
                        if (activeLoanId != null && !activeLoanId.isEmpty()) {

                            com.google.firebase.firestore.DocumentReference loanRef =
                                    firestore.collection("lendingRecords")
                                            .document(activeLoanId);

                            com.google.firebase.firestore.DocumentSnapshot loanSnapshot =
                                    transaction.get(loanRef);

                            if (!loanSnapshot.exists()) {
                                throw new IllegalStateException(
                                        "Active lending record not found");
                            }

                            String loanBookId = loanSnapshot.getString("bookId");

                            if (!bookId.equals(loanBookId)) {
                                throw new IllegalStateException(
                                        "Lending record belongs to another book");
                            }

                            transaction.update(
                                    loanRef,
                                    "status", "RETURNED",
                                    "returnedDate",
                                    com.google.firebase.firestore.FieldValue.serverTimestamp()
                            );
                        }

                        transaction.update(bookRef, updates);

                        return null;

                    }).addOnSuccessListener(unused -> {

                        isLent = false;
                        lendBookButton.setText("Lend Book");

                        loadBookDetails();
                        loadLendingHistory();

                        Toast.makeText(
                                BookDetailActivity.this,
                                "Book returned successfully!",
                                Toast.LENGTH_SHORT
                        ).show();

                    }).addOnFailureListener(e -> {

                        Toast.makeText(
                                BookDetailActivity.this,
                                "Failed to return book: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();

                    }).addOnCompleteListener(task -> {
                        lendBookButton.setEnabled(true);
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
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

    // 📌 Delete Book from Firestore
    private void deleteBook() {
        if (bookId != null) {
            new BookRepository(firestore).copyRef(bookId, normalizedCopy)
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
                    new BookRepository(firestore).move(bookId, normalizedCopy, newCube)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(BookDetailActivity.this, "Book moved to Cube " + newCube, Toast.LENGTH_SHORT).show();
                                loadBookDetails();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(BookDetailActivity.this, "Failed to move book", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
