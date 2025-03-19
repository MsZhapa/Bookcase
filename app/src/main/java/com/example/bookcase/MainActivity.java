package com.example.bookcase;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();

        // Fetch books from Firestore
        fetchBooksFromFirestore();
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
