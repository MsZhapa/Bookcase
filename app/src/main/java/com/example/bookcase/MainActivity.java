package com.example.bookcase;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
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
        backButton.setOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        bookList = new ArrayList<>();
        bookAdapter = new BookAdapter(this, bookList);
        recyclerView.setAdapter(bookAdapter);

        // 🔥 Get selectedCube from Intent
        int selectedCube = getIntent().getIntExtra("selectedCube", -1);

        if (selectedCube != -1) {
            fetchBooksFromCube(selectedCube);
        } else {
            Toast.makeText(this, "No cube selected!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchBooksFromCube(int cubeNumber) {
        firestore.collection("books")
                .whereEqualTo("location", cubeNumber) // 🔥 Filter by correct cube
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Book book = document.toObject(Book.class);
                        bookList.add(book);
                    }
                    bookAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, "Error loading books", Toast.LENGTH_SHORT).show()
                );
    }


    private void fetchBooksFromFirestore() {
        firestore.collection("books").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        bookList = task.getResult().toObjects(Book.class);
                        bookAdapter = new BookAdapter(MainActivity.this, bookList);
                        recyclerView.setAdapter(bookAdapter);
                    } else {
                        Log.e("Firestore", "Error fetching books", task.getException());
                    }
                });
    }
}
