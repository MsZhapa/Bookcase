package com.example.bookcase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class CopyNotesActivity extends AppCompatActivity {
    private NotesModel model;
    private EditText editor;
    private boolean rendering;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_copy_notes);
        ((TextView)findViewById(R.id.screenTitle)).setText(R.string.personal_notes);
        editor = findViewById(R.id.notesEditor);
        model = new ViewModelProvider(this).get(NotesModel.class);
        String id = getIntent().getStringExtra("bookId");
        if (id == null || id.isEmpty()) { finish(); return; }
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!rendering && model.loaded) model.draft(s.toString());
            }
            public void afterTextChanged(Editable value) {}
        });
        findViewById(R.id.backButton).setOnClickListener(v -> leave());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            public void handleOnBackPressed() { leave(); }
        });
        findViewById(R.id.saveNotesButton).setOnClickListener(v -> model.save());
        findViewById(R.id.reloadNotesButton).setOnClickListener(v -> {
            if (model.dirty()) new AlertDialog.Builder(this).setMessage(R.string.notes_reload_confirm)
                    .setPositiveButton(R.string.reload_notes, (d,w) -> model.load(id,
                            getIntent().getBooleanExtra("normalizedCopy", false), true))
                    .setNegativeButton(R.string.cancel_action, null).show();
            else model.load(id, getIntent().getBooleanExtra("normalizedCopy", false), true);
        });
        model.phase.observe(this, phase -> {
            if (phase == NotesModel.DONE) { finish(); return; }
            if (model.loaded) ((TextView)findViewById(R.id.notesCopyLabel)).setText(
                    CubeSettings.copyLabel(this,model.title,model.cube,model.position));
            rendering = true;
            if (model.loaded && !editor.getText().toString().equals(model.draft())) editor.setText(model.draft());
            rendering = false;
            editor.setEnabled(model.loaded && !model.busy);
            findViewById(R.id.saveNotesButton).setEnabled(model.loaded && !model.busy);
            findViewById(R.id.reloadNotesButton).setEnabled(!model.busy);
            int label = phase == NotesModel.LOADING ? R.string.notes_loading
                    : phase == NotesModel.SAVING ? R.string.notes_saving
                    : phase == NotesModel.LOAD_FAILED ? R.string.notes_load_failed
                    : phase == NotesModel.SAVE_FAILED ? R.string.notes_save_failed : 0;
            ((TextView)findViewById(R.id.notesStatus)).setText(label == 0 ? "" : getString(label));
        });
        model.load(id, getIntent().getBooleanExtra("normalizedCopy", false), false);
    }
    private void leave() {
        if (model.busy || !model.dirty()) { finish(); return; }
        new AlertDialog.Builder(this).setMessage(R.string.notes_unsaved_confirm)
                .setPositiveButton(R.string.leave_notes, (d,w) -> finish())
                .setNegativeButton(R.string.cancel_action, null).show();
    }
    public static class NotesModel extends ViewModel {
        static final int LOADING=0, READY=1, SAVING=2, DONE=3, LOAD_FAILED=4, SAVE_FAILED=5;
        final MutableLiveData<Integer> phase = new MutableLiveData<>();
        final SavedStateHandle state;
        boolean loaded, busy, started;
        private String id;
        String title; int cube; long position;
        private boolean normalized;
        public NotesModel(SavedStateHandle state) { this.state = state; }
        String draft() { String text = state.get("draft"); return text == null ? "" : text; }
        void draft(String text) { state.set("draft", text); }
        boolean dirty() { return loaded && !draft().equals(state.get("original")); }
        void load(String id, boolean normalized, boolean reload) {
            if (started && !reload) return;
            this.id=id; this.normalized=normalized; started=true; busy=true;
            phase.setValue(LOADING);
            new BookRepository().load(id, normalized).addOnSuccessListener(book -> {
                if (reload || !state.contains("draft")) {
                    state.set("original", book.getNotes()); draft(book.getNotes());
                }
                title=book.getTitle(); cube=book.getLocation(); position=book.getPosition();
                loaded=true; busy=false; phase.setValue(READY);
            }).addOnFailureListener(e -> { busy=false; loaded=false; phase.setValue(LOAD_FAILED); });
        }
        void save() {
            if (!loaded || busy) return;
            busy=true; phase.setValue(SAVING);
            new BookRepository().saveNotes(id, normalized, state.get("original"), draft())
                    .addOnSuccessListener(ignored -> { busy=false; phase.setValue(DONE); })
                    .addOnFailureListener(e -> { busy=false; phase.setValue(SAVE_FAILED); });
        }
    }
}
