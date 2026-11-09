package com.example.bookcase;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ReadingProgressActivity extends AppCompatActivity {
    private PageProgressForm form;
    private ProgressModel model;
    private boolean bound;
    private Bundle draft;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_reading_progress);
        ((TextView)findViewById(R.id.screenTitle)).setText(R.string.update_reading_progress);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        CheckBox finished=findViewById(R.id.progressFinished);
        form=new PageProgressForm(findViewById(android.R.id.content), finished); draft=state;
        model=new ViewModelProvider(this).get(ProgressModel.class);
        String id=getIntent().getStringExtra("bookId");
        if (id==null || id.isEmpty()) { finish(); return; }
        boolean normalized=getIntent().getBooleanExtra("normalizedCopy",false);
        findViewById(R.id.saveReadingProgress).setOnClickListener(v -> {
            if (form.apply(model.book)) model.save(form.value());
        });
        findViewById(R.id.retryProgressLoad).setOnClickListener(v -> model.load(id,normalized,true));
        model.phase.observe(this, phase -> {
            if (phase==ProgressModel.DONE) { finish(); return; }
            if (model.book!=null && !bound) {
                form.bind(model.book); form.restore(draft); draft=null; bound=true;
                ((TextView)findViewById(R.id.progressCopyLabel)).setText(CubeSettings.copyLabel(this,
                        model.book.getTitle(),model.book.getLocation(),model.book.getPosition()));
            }
            boolean enabled=model.book!=null && !model.busy;
            findViewById(R.id.saveReadingProgress).setEnabled(enabled);
            finished.setEnabled(enabled);
            findViewById(R.id.currentPageInput).setEnabled(enabled && !finished.isChecked());
            findViewById(R.id.totalPagesInput).setEnabled(enabled);
            findViewById(R.id.retryProgressLoad).setVisibility(phase==ProgressModel.LOAD_FAILED ? View.VISIBLE : View.GONE);
            int message=phase==ProgressModel.LOADING ? R.string.loading_books
                    : phase==ProgressModel.SAVING ? R.string.saving_progress
                    : phase==ProgressModel.LOAD_FAILED ? R.string.overview_failed
                    : phase==ProgressModel.SAVE_FAILED ? R.string.progress_save_failed : 0;
            ((TextView)findViewById(R.id.progressSaveStatus)).setText(message==0 ? "" : getString(message));
        });
        model.load(id,normalized,false);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        if (bound) form.save(state); super.onSaveInstanceState(state);
    }
    public static class ProgressModel extends ViewModel {
        static final int LOADING=0, READY=1, SAVING=2, DONE=3, LOAD_FAILED=4, SAVE_FAILED=5;
        final MutableLiveData<Integer> phase=new MutableLiveData<>();
        Book book; boolean busy,started; private String id; private boolean normalized;
        void load(String id, boolean normalized, boolean retry) {
            if (started && !retry) return;
            this.id=id; this.normalized=normalized; started=true; busy=true; phase.setValue(LOADING);
            new BookRepository().load(id,normalized).addOnSuccessListener(result -> {
                book=result; busy=false; phase.setValue(READY);
            }).addOnFailureListener(e -> { busy=false; phase.setValue(LOAD_FAILED); });
        }
        void save(ReadingProgress.State state) {
            if (busy || book==null) return;
            busy=true; phase.setValue(SAVING);
            new BookRepository().saveReadingProgress(id,normalized,state)
                    .addOnSuccessListener(ignored -> { busy=false; phase.setValue(DONE); })
                    .addOnFailureListener(e -> { busy=false; phase.setValue(SAVE_FAILED); });
        }
    }
}
