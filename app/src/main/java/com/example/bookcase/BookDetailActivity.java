package com.example.bookcase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BookDetailActivity extends AppCompatActivity {

    private TextView titleTextView, authorTextView, locationTextView;
    private Spinner cubeSpinner;
    private Button moveButton;
    private BookDatabase bookDatabase;
    private int bookId, currentCube;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        // Initialize views
        titleTextView = findViewById(R.id.titleTextView);
        authorTextView = findViewById(R.id.authorTextView);
        locationTextView = findViewById(R.id.locationTextView);
        cubeSpinner = findViewById(R.id.cubeSpinner);
        moveButton = findViewById(R.id.moveButton);

        bookDatabase = BookDatabase.getDatabase(this);

        // Get book data from intent
        bookId = getIntent().getIntExtra("id", -1);
        String title = getIntent().getStringExtra("title");
        String author = getIntent().getStringExtra("author");
        currentCube = getIntent().getIntExtra("location", -1);

        // Set book details
        titleTextView.setText(title);
        authorTextView.setText("Author: " + author);
        locationTextView.setText("Current Cube: " + currentCube);

        // Populate cube selection spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.cube_numbers, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cubeSpinner.setAdapter(adapter);

        // Set default selection to current cube
        cubeSpinner.setSelection(currentCube - 1);

        // Move book when button is clicked
        moveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int newCube = cubeSpinner.getSelectedItemPosition() + 1;
                if (newCube != currentCube) {
                    bookDatabase.bookDao().updateBookLocation(bookId, newCube);
                    Toast.makeText(BookDetailActivity.this, "Book moved to Cube " + newCube, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(BookDetailActivity.this, "Book is already in Cube " + newCube, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
