package com.example.bookcase;

import android.app.Application;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.*;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.lifecycle.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class LibraryRestoreActivity extends BookcaseActivity {
    private RestoreModel model;
    private State displayed;
    private LibraryBackup lastPreview;
    private final ActivityResultLauncher<String[]> openFile=registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> { if(uri!=null) model.preview(uri); });
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved); setContentView(R.layout.activity_library_restore);
        ((TextView)findViewById(R.id.screenTitle)).setText(R.string.restore_library);
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        model=new ViewModelProvider(this).get(RestoreModel.class);
        findViewById(R.id.chooseBackupButton).setOnClickListener(v -> openFile.launch(new String[]{"application/json","text/plain","application/octet-stream"}));
        findViewById(R.id.restoreBackupButton).setOnClickListener(v -> {
            if(displayed==null || displayed.plan==null || displayed.busy) return;
            boolean settings=((CheckBox)findViewById(R.id.restoreCubeSettings)).isChecked();
            new AlertDialog.Builder(new ContextThemeWrapper(this,androidx.appcompat.R.style.Theme_AppCompat_Light_Dialog_Alert))
                    .setTitle(R.string.restore_confirm_title)
                    .setMessage(getString(R.string.restore_confirm,displayed.plan.missing.size())+(settings ? getString(R.string.restore_settings_confirm) : ""))
                    .setNegativeButton(android.R.string.cancel,null)
                    .setPositiveButton(R.string.restore_confirm_title,(dialog,which) -> model.restore(settings)).show();
        });
        ((CheckBox)findViewById(R.id.restoreCubeSettings)).setOnCheckedChangeListener((button,checked) -> updateButton());
        model.state.observe(this,result -> {
            displayed=result;
            ((TextView)findViewById(R.id.restoreStatus)).setText(result.message);
            findViewById(R.id.chooseBackupButton).setEnabled(!result.busy);
            CheckBox settings=findViewById(R.id.restoreCubeSettings);
            settings.setVisibility(result.plan==null ? android.view.View.GONE : android.view.View.VISIBLE);
            settings.setEnabled(!result.busy && result.plan!=null && result.plan.canRestore());
            if(result.backup!=null && result.backup!=lastPreview) { settings.setChecked(false); lastPreview=result.backup; }
            if(result.backup!=null) {
                StringBuilder names=new StringBuilder("\nBackup cube layout: "+result.backup.columns+" columns\n");
                for(int i=0;i<8;i++) names.append("Cube ").append(i+1).append(": ").append(result.backup.cubeNames[i]).append('\n');
                ((TextView)findViewById(R.id.backupSettingsPreview)).setText(names);
            } else ((TextView)findViewById(R.id.backupSettingsPreview)).setText("");
            updateButton();
        });
    }
    private void updateButton() {
        findViewById(R.id.restoreBackupButton).setEnabled(displayed!=null && !displayed.busy && displayed.plan!=null && displayed.plan.canRestore()
                && (!displayed.plan.missing.isEmpty() || displayed.plan.counterChanges>0 || ((CheckBox)findViewById(R.id.restoreCubeSettings)).isChecked()));
    }
    static final class State {
        final boolean busy; final String message; final LibraryBackup backup; final RestorePlan plan;
        State(boolean busy,String message,LibraryBackup backup,RestorePlan plan) { this.busy=busy; this.message=message; this.backup=backup; this.plan=plan; }
    }
    public static class RestoreModel extends AndroidViewModel {
        final MutableLiveData<State> state=new MutableLiveData<>(new State(false,"Choose a Bookcase full backup to preview. No data changes until you confirm restore.",null,null));
        private final ExecutorService worker=Executors.newSingleThreadExecutor();
        private volatile boolean busy,cleared;
        public RestoreModel(Application application) { super(application); }
        void preview(Uri uri) {
            if(busy) return; busy=true; state.setValue(new State(true,"Reading and checking backup…",null,null));
            worker.execute(() -> {
                try(InputStream stream=getApplication().getContentResolver().openInputStream(uri)) {
                    if(stream==null) throw new IOException("Cannot open backup");
                    LibraryBackup backup=BackupJson.read(new InputStreamReader(stream,StandardCharsets.UTF_8));
                    new BackupRepository().load().addOnSuccessListener(current -> { if(cleared) return; worker.execute(() -> {
                        try { RestorePlan plan=new RestorePlan(backup,current); busy=false; state.postValue(new State(false,plan.describe(backup),backup,plan)); }
                        catch(Exception error) { fail(error,false); }
                    }); }).addOnFailureListener(error -> fail(error,false));
                } catch(Exception error) { fail(error,false); }
            });
        }
        void restore(boolean settings) {
            State current=state.getValue(); if(busy || current==null || current.plan==null || !current.plan.canRestore()) return;
            busy=true; state.setValue(new State(true,"Restoring missing records… Keep Bookcase open. Interrupted restores can be resumed by selecting the same backup again.",current.backup,current.plan));
            new BackupRepository().restore(current.backup,current.plan,count -> state.postValue(new State(true,"Added "+count+" missing records…",current.backup,current.plan)))
                    .addOnSuccessListener(count -> {
                        if(settings) CubeSettings.save(getApplication(),current.backup.cubeNames,current.backup.columns);
                        busy=false; state.setValue(new State(false,"Restore complete. Added "+count+" records; matching records were unchanged. "+(settings ? "Cube names and layout restored." : "Your local cube names and layout were kept."),null,null));
                    }).addOnFailureListener(error -> fail(error,true));
        }
        private void fail(Exception error,boolean restoring) {
            android.util.Log.e("BookcaseRestore","Backup operation failed",error);
            busy=false;
            String detail=error instanceof IllegalArgumentException || error instanceof IllegalStateException ? error.getMessage() : "Check the file and internet connection, then try again.";
            state.postValue(new State(false,(restoring ? "Restore stopped. Some missing records may already have been added. Existing records were not overwritten or deleted. Select the same backup again to review and resume.\n\n" : "Could not preview backup. No library data was changed.\n\n")+detail,null,null));
        }
        @Override protected void onCleared() { cleared=true; worker.shutdown(); }
    }
}
