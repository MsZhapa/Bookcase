package com.example.bookcase;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BookAdapter bookAdapter;
    private FirebaseFirestore firestore;
    private List<Book> bookList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText("Book List");

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        bookList = new ArrayList<>();
        bookAdapter = new BookAdapter(this, bookList);
        recyclerView.setAdapter(bookAdapter);

        // 🔥 Get selectedCube from Intent
        int selectedCube = getIntent().getIntExtra("selectedCube", -1);

        if (selectedCube >= 1 && selectedCube <= 8) {
            View addBookButton = findViewById(R.id.addBookToCubeButton);
            addBookButton.setVisibility(View.VISIBLE);
            addBookButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AdminActivity.class);
                intent.putExtra("selectedCube", selectedCube);
                startActivity(intent);
            });
        } else {
            Toast.makeText(this, "No cube selected!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int cube = getIntent().getIntExtra("selectedCube", -1);
        if (cube != -1 && bookAdapter != null) fetchBooksFromCube(cube);
    }

    private void fetchBooksFromCube(int cubeNumber) {
        new BookRepository().getBooks(cubeNumber)
                .addOnSuccessListener(books -> {
                    bookList.clear(); bookList.addAll(books);
                    bookAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error loading books: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
