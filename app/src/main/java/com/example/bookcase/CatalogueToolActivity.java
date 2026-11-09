package com.example.bookcase;
import android.os.Bundle;
import android.text.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import java.util.function.Consumer;
public abstract class CatalogueToolActivity extends AppCompatActivity {
    protected LinearLayout controls;
    protected RecyclerView list;
    protected TextView status;
    protected Button retry;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_catalogue_tool);
        controls=findViewById(R.id.toolControls); list=findViewById(R.id.toolList);
        status=findViewById(R.id.toolStatus); retry=findViewById(R.id.toolRetry);
        list.setLayoutManager(new LinearLayoutManager(this));
        findViewById(R.id.backButton).setOnClickListener(v->finish());
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                |android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    protected void title(int text) { ((TextView)findViewById(R.id.screenTitle)).setText(text); }
    protected EditText search(String query,Consumer<String> onChange) {
        EditText input=new EditText(this); input.setSingleLine(true); input.setTextColor(0xff212121);
        input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff616161));
        if (android.os.Build.VERSION.SDK_INT>=26) input.setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
        input.setHintTextColor(0xff616161); input.setHint(R.string.search_books_hint); input.setText(query);
        input.setMinHeight((int)(48*getResources().getDisplayMetrics().density));
        input.setId(R.id.toolSearch); controls.addView(input);
        input.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { onChange.accept(s.toString()); }
            public void afterTextChanged(Editable value) {}
        }); return input;
    }
}
