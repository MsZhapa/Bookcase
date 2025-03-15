package com.example.bookcase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView bookListView;
    private BookDatabase bookDatabase;
    private int cubeNumber;
    private List<Book> books;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bookListView = findViewById(R.id.bookListView);
        bookDatabase = BookDatabase.getDatabase(this);

        // Get cube number from intent
        cubeNumber = getIntent().getIntExtra("cubeNumber", 1);

        // Fetch books by cube
        books = bookDatabase.bookDao().getBooksByCube(cubeNumber);

        // Convert book titles to String Array for ListView
        String[] bookTitles = new String[books.size()];
        for (int i = 0; i < books.size(); i++) {
            bookTitles[i] = books.get(i).title;
        }

        // Set adapter for ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, bookTitles);
        bookListView.setAdapter(adapter);

        bookListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Book selectedBook = books.get(position);

                // Open BookDetailActivity
                Intent intent = new Intent(MainActivity.this, BookDetailActivity.class);
                intent.putExtra("id", selectedBook.id);
                intent.putExtra("title", selectedBook.title);
                intent.putExtra("author", selectedBook.author);
                intent.putExtra("location", selectedBook.location);
                startActivity(intent);
            }
        });
        
    }
}
