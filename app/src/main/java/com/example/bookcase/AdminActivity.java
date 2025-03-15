package com.example.bookcase;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {

    private EditText editTextTitle, editTextAuthor;
    private Spinner spinnerCube;
    private BookDatabase bookDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        editTextTitle = findViewById(R.id.editTextTitle);
        editTextAuthor = findViewById(R.id.editTextAuthor);
        spinnerCube = findViewById(R.id.spinnerCube);
        Button btnSave = findViewById(R.id.btnSave);

        bookDatabase = BookDatabase.getDatabase(this);

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBook();
            }
        });
    }

    private void saveBook() {
        String title = editTextTitle.getText().toString().trim();
        String author = editTextAuthor.getText().toString().trim();
        int location = spinnerCube.getSelectedItemPosition() + 1; // 1-8

        if (title.isEmpty() || author.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Insert book into database
        Book book = new Book(title, author, location);
        bookDatabase.bookDao().insert(book);

        Toast.makeText(this, "Book added successfully!", Toast.LENGTH_SHORT).show();

        // Clear fields
        editTextTitle.setText("");
        editTextAuthor.setText("");
        spinnerCube.setSelection(0);
    }
}
