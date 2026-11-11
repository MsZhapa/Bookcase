package com.example.bookcase;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ReadingOverviewActivity extends BookcaseActivity {
    private final int[] counts = {R.id.overviewAll, R.id.overviewUnread, R.id.overviewProgress,
            R.id.overviewFinished, R.id.overviewLent};
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_reading_overview);
        ((TextView)findViewById(R.id.screenTitle)).setText(R.string.reading_overview);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        for (int i = 0; i < counts.length; i++) {
            final int filter = i;
            findViewById(counts[i]).setOnClickListener(v -> {
                Intent list = new Intent(this, MainActivity.class);
                list.putExtra("statusFilter", filter); startActivity(list);
            });
        }
        findViewById(R.id.overviewRetry).setOnClickListener(v -> load());
    }
    @Override protected void onResume() { super.onResume(); load(); }
    private void load() {
        for (int id : counts) findViewById(id).setEnabled(false);
        findViewById(R.id.overviewRetry).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.overviewStatus)).setText(R.string.loading_books);
        new BookRepository().getBooks(null).addOnSuccessListener(books -> {
            ReadingSummary s = new ReadingSummary(books);
            int[] values = {s.total, s.unread, s.inProgress, s.finished, s.lent};
            int[] labels = {R.string.overview_total, R.string.overview_unread, R.string.overview_progress,
                    R.string.overview_finished, R.string.overview_lent};
            for (int i = 0; i < counts.length; i++) {
                ((TextView)findViewById(counts[i])).setText(getString(labels[i], values[i]));
                findViewById(counts[i]).setEnabled(true);
            }
            ((TextView)findViewById(R.id.overviewStatus)).setText("");
        }).addOnFailureListener(e -> {
            ((TextView)findViewById(R.id.overviewStatus)).setText(R.string.overview_failed);
            findViewById(R.id.overviewRetry).setVisibility(View.VISIBLE);
        });
    }
}
