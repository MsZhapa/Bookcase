package com.example.bookcase;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SearchView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BookAdapter bookAdapter;
    private List<Book> bookList = new ArrayList<>();

    private final List<Book> allBooks = new ArrayList<>();
    private SearchView searchView;
    private TextView searchStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText(getIntent().getIntExtra("selectedCube", -1) == -1
                ? "All books" : "Cube " + getIntent().getIntExtra("selectedCube", -1));

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        bookList = new ArrayList<>();
        bookAdapter = new BookAdapter(this, bookList);
        recyclerView.setAdapter(bookAdapter);

        searchView = findViewById(R.id.bookSearch);
        searchStatus = findViewById(R.id.searchStatus);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String query) { searchView.clearFocus(); return true; }
            public boolean onQueryTextChange(String query) { applySearch(); return true; }
        });
        findViewById(R.id.searchBooksButton).setVisibility(View.VISIBLE);
        findViewById(R.id.searchBooksButton).setOnClickListener(v -> showSearch());
        if (savedInstanceState != null) {
            searchView.setQuery(savedInstanceState.getString("search", ""), false);
            if (savedInstanceState.getBoolean("searchVisible")) showSearch();
        } else if (getIntent().getBooleanExtra("openSearch", false)) {
            showSearch();
        }

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
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        int cube = getIntent().getIntExtra("selectedCube", -1);
        if (bookAdapter != null) fetchBooksFromCube(cube);
    }

    private void fetchBooksFromCube(int cubeNumber) {
        searchStatus.setText(R.string.loading_books);
        new BookRepository().getBooks(cubeNumber == -1 ? null : cubeNumber)
                .addOnSuccessListener(books -> {
                    allBooks.clear(); allBooks.addAll(books);
                    applySearch();
                })
                .addOnFailureListener(e -> {
                    searchStatus.setText(R.string.loading_books_failed);
                    Toast.makeText(this, "Error loading books: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
    private void showSearch() {
        searchView.setVisibility(View.VISIBLE);
        searchView.requestFocus();
        searchView.post(() -> {
            android.view.inputmethod.InputMethodManager keyboard =
                    (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(searchView.findFocus(), android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void applySearch() {
        bookList.clear();
        bookList.addAll(BookSearch.filter(allBooks, searchView.getQuery().toString()));
        bookAdapter.notifyDataSetChanged();
        searchStatus.setText(bookList.isEmpty() ? getString(R.string.no_matching_books)
                : getString(R.string.search_count, bookList.size(), allBooks.size()));
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("searchVisible", searchView.getVisibility() == View.VISIBLE);
        state.putString("search", searchView.getQuery().toString());
        super.onSaveInstanceState(state);
    }
}
