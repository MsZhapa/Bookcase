package com.example.bookcase;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public class LoansActivity extends AppCompatActivity {
    private final List<Book> books = new ArrayList<>(), visible = new ArrayList<>();
    private BookAdapter adapter;
    private CheckBox overdueOnly;
    private boolean loaded;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_loans);
        ((TextView)findViewById(R.id.screenTitle)).setText(R.string.loans_overview);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        RecyclerView list = findViewById(R.id.loanList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookAdapter(this, visible, true); list.setAdapter(adapter);
        overdueOnly = findViewById(R.id.overdueOnly);
        if (state != null) overdueOnly.setChecked(state.getBoolean("overdueOnly"));
        overdueOnly.setOnCheckedChangeListener((button, checked) -> { if (loaded) render(); });
        findViewById(R.id.loansRetry).setOnClickListener(v -> load());
    }
    @Override protected void onResume() { super.onResume(); load(); }
    private void load() {
        loaded = false; overdueOnly.setEnabled(false);
        visible.clear(); adapter.notifyDataSetChanged();
        ((TextView)findViewById(R.id.loansStatus)).setText(R.string.loading_books);
        ((TextView)findViewById(R.id.loansEmpty)).setText("");
        findViewById(R.id.loansRetry).setVisibility(View.GONE);
        new BookRepository().getBooks(null).addOnSuccessListener(result -> {
            books.clear(); books.addAll(result); loaded = true;
            overdueOnly.setEnabled(true); render();
        }).addOnFailureListener(e -> {
            ((TextView)findViewById(R.id.loansStatus)).setText(R.string.overview_failed);
            findViewById(R.id.loansRetry).setVisibility(View.VISIBLE);
        });
    }
    private void render() {
        long now = System.currentTimeMillis(); TimeZone zone = TimeZone.getDefault();
        int all = LoanRules.active(books, now, zone, false).size();
        int overdue = LoanRules.active(books, now, zone, true).size();
        visible.clear(); visible.addAll(LoanRules.active(books, now, zone, overdueOnly.isChecked()));
        adapter.notifyDataSetChanged();
        ((TextView)findViewById(R.id.loansStatus)).setText(getString(R.string.loans_summary, all, overdue));
        ((TextView)findViewById(R.id.loansEmpty)).setText(visible.isEmpty()
                ? getString(overdueOnly.isChecked() ? R.string.no_overdue_loans : R.string.no_active_loans) : "");
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("overdueOnly", overdueOnly.isChecked()); super.onSaveInstanceState(state);
    }
}
