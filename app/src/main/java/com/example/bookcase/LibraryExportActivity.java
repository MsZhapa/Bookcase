package com.example.bookcase;

import android.app.Application;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LibraryExportActivity extends AppCompatActivity {
    private ExportModel model;
    private final ActivityResultLauncher<String> createFile = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) model.export(uri);
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_library_export);
        ((TextView)findViewById(R.id.screenTitle)).setText(R.string.export_library);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        model = new ViewModelProvider(this).get(ExportModel.class);
        findViewById(R.id.saveExportButton).setOnClickListener(v -> createFile.launch(
                "Bookcase-library-" + new java.text.SimpleDateFormat("yyyy-MM-dd-HHmmss",
                        java.util.Locale.ROOT).format(new java.util.Date()) + ".json"));
        model.state.observe(this, result -> {
            findViewById(R.id.saveExportButton).setEnabled(!result.busy);
            ((TextView)findViewById(R.id.exportStatus)).setText(result.count >= 0
                    ? getString(R.string.export_success, result.count) : getString(result.message));
        });
    }

    /** Keeps the export running through rotation; never writes to Firestore. */
    public static class ExportModel extends AndroidViewModel {
        final MutableLiveData<ExportState> state = new MutableLiveData<>();
        private volatile boolean busy;
        public ExportModel(Application application) { super(application); }
        void export(Uri uri) {
            if (busy) return;
            busy = true;
            state.setValue(new ExportState(true, R.string.export_working, -1));
            new BackupRepository().load().addOnSuccessListener(backup -> {
                backup.exportedAt=System.currentTimeMillis();
                backup.columns=CubeSettings.columns(getApplication());
                for(int i=0;i<8;i++) backup.cubeNames[i]=CubeSettings.name(getApplication(),i+1);
                ExecutorService worker = Executors.newSingleThreadExecutor();
                worker.execute(() -> {
                    try (OutputStream stream = getApplication().getContentResolver().openOutputStream(uri, "wt")) {
                        if (stream == null) throw new java.io.IOException("Cannot open destination");
                        try (OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8)) {
                            BackupJson.write(writer, backup);
                        }
                        busy = false;
                        state.postValue(new ExportState(false, 0, backup.copies()));
                    } catch (Exception error) { fail(); }
                });
                worker.shutdown();
            }).addOnFailureListener(error -> fail());
        }
        private void fail() {
            busy = false;
            state.postValue(new ExportState(false, R.string.export_failed, -1));
        }
    }
    private static class ExportState {
        final boolean busy;
        final int message, count;
        ExportState(boolean busy, int message, int count) {
            this.busy = busy; this.message = message; this.count = count;
        }
    }
}
