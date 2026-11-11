package com.example.bookcase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CubesActivity extends BookcaseActivity {
    private final android.widget.Button[] buttons=new android.widget.Button[8];
    private final int[] ids={R.id.cube1Button,R.id.cube2Button,R.id.cube3Button,R.id.cube4Button,
            R.id.cube5Button,R.id.cube6Button,R.id.cube7Button,R.id.cube8Button};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cubes);

        // 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText(R.string.select_cube);

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        findViewById(R.id.searchBooksButton).setVisibility(android.view.View.VISIBLE);
        findViewById(R.id.searchBooksButton).setOnClickListener(v -> openMainActivity(-1));

        LibraryMenu.attach(this);

        for (int i=0;i<8;i++) {
            buttons[i]=findViewById(ids[i]); final int cube=i+1;
            buttons[i].setOnClickListener(v->openMainActivity(cube));
        }
        findViewById(R.id.cubeCountStatus).setOnClickListener(v->loadCounts());
    }
    @Override protected void onResume() { super.onResume(); renderLayout(); loadCounts(); }
    private void renderLayout() {
        android.widget.GridLayout grid=findViewById(R.id.cubeGrid); grid.removeAllViews();
        int columns=CubeSettings.columns(this); grid.setColumnCount(columns);grid.setRowCount((8+columns-1)/columns);
        for (int i=0;i<8;i++) {
            android.widget.GridLayout.LayoutParams params=new android.widget.GridLayout.LayoutParams(
                    android.widget.GridLayout.spec(i/columns,1f),android.widget.GridLayout.spec(i%columns,1f));
            params.width=0;params.height=0;params.setGravity(android.view.Gravity.FILL);
            buttons[i].setAllCaps(false);buttons[i].setTextSize(16);buttons[i].setPadding(4,4,4,4);
            buttons[i].setText(CubeSettings.numbered(this,i+1));grid.addView(buttons[i],params);
        }
    }
    private void loadCounts() {
        TextView status=findViewById(R.id.cubeCountStatus);status.setVisibility(android.view.View.VISIBLE);status.setText(R.string.loading_cube_counts);
        new BookRepository().getBooks(null).addOnSuccessListener(books->{
            int[] counts=CubeLayout.counts(books);int assigned=0;
            for (int i=0;i<8;i++) { assigned+=counts[i];buttons[i].setText(CubeSettings.numbered(this,i+1)+"\n"+getString(R.string.cube_copy_count,counts[i])); }
            if (assigned==books.size()) status.setVisibility(android.view.View.GONE);
            else status.setText(getString(R.string.unassigned_cube_count,books.size()-assigned));
        }).addOnFailureListener(e->status.setText(R.string.cube_counts_failed));
    }

    private void openMainActivity(int cubeNumber) {
        Intent intent = new Intent(CubesActivity.this, MainActivity.class);
        intent.putExtra("selectedCube", cubeNumber);
        intent.putExtra("openSearch", cubeNumber == -1);
        startActivity(intent);
    }
}
