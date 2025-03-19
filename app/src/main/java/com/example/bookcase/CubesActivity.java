package com.example.bookcase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

public class CubesActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cubes);
// 🏷️ Set Screen Title
        TextView screenTitle = findViewById(R.id.screenTitle);
        screenTitle.setText("Select a Cube and find a book");

        // 🔙 Handle Back Button Click
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CubesActivity.this, WelcomeActivity.class);
                startActivity(intent);
            }
        });
        // Example: If Cube 1 button is clicked
        findViewById(R.id.cube1Button).setOnClickListener(v -> openMainActivity(1));
        findViewById(R.id.cube2Button).setOnClickListener(v -> openMainActivity(2));
        findViewById(R.id.cube3Button).setOnClickListener(v -> openMainActivity(3));
        findViewById(R.id.cube4Button).setOnClickListener(v -> openMainActivity(4));
        findViewById(R.id.cube5Button).setOnClickListener(v -> openMainActivity(5));
        findViewById(R.id.cube6Button).setOnClickListener(v -> openMainActivity(6));
        findViewById(R.id.cube7Button).setOnClickListener(v -> openMainActivity(7));
        findViewById(R.id.cube8Button).setOnClickListener(v -> openMainActivity(8));
    }

    private void openMainActivity(int cubeNumber) {
        Intent intent = new Intent(CubesActivity.this, MainActivity.class);
        intent.putExtra("selectedCube", cubeNumber);
        startActivity(intent);
    }
}
