package com.example.bookcase;
import android.os.Bundle;
import android.widget.*;
import android.text.InputFilter;
import androidx.appcompat.app.AppCompatActivity;
public class CubeSettingsActivity extends AppCompatActivity {
    private final EditText[] fields=new EditText[8]; private Spinner columns;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_cube_settings);
        ((TextView)findViewById(R.id.screenTitle)).setText(R.string.customize_cubes);
        findViewById(R.id.backButton).setOnClickListener(v->finish());
        columns=findViewById(R.id.cubeColumns);
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.filter_item,getResources().getStringArray(R.array.cube_column_options));
        adapter.setDropDownViewResource(R.layout.filter_item);columns.setAdapter(adapter);
        columns.setSelection(state==null?CubeSettings.columns(this)-1:state.getInt("columns",1));
        LinearLayout container=findViewById(R.id.cubeNameFields);
        int[] ids={R.id.cubeName1,R.id.cubeName2,R.id.cubeName3,R.id.cubeName4,R.id.cubeName5,R.id.cubeName6,R.id.cubeName7,R.id.cubeName8};
        String[] restored=state==null?null:state.getStringArray("names");
        for (int i=0;i<8;i++) {
            TextView label=new TextView(this);label.setText(getString(R.string.default_cube_label,i+1));label.setTextColor(0xff212121);label.setLabelFor(ids[i]);container.addView(label);
            fields[i]=new EditText(this);fields[i].setId(ids[i]);fields[i].setSingleLine();fields[i].setTextColor(0xff212121);
            fields[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff616161));
            if (android.os.Build.VERSION.SDK_INT>=26) fields[i].setImportantForAutofill(android.view.View.IMPORTANT_FOR_AUTOFILL_NO);
            fields[i].setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
            fields[i].setText(restored==null?CubeSettings.name(this,i+1):restored[i]);
            container.addView(fields[i],new LinearLayout.LayoutParams(-1,(int)(56*getResources().getDisplayMetrics().density)));
        }
        findViewById(R.id.saveCubeSettings).setOnClickListener(v->{
            String[] names=new String[8];for (int i=0;i<8;i++) names[i]=fields[i].getText().toString();
            CubeSettings.save(this,names,columns.getSelectedItemPosition()+1);finish();
        });
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        String[] names=new String[8];for (int i=0;i<8;i++) names[i]=fields[i].getText().toString();
        state.putStringArray("names",names);state.putInt("columns",columns.getSelectedItemPosition());super.onSaveInstanceState(state);
    }
}
