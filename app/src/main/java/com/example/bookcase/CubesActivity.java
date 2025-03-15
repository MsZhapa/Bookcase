package com.example.bookcase;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class CubesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cubes); // Ensure layout is set before findViewById()

        // Find Buttons
        Button cube1Button = findViewById(R.id.cube1Button);
        Button cube2Button = findViewById(R.id.cube2Button);
        Button cube3Button = findViewById(R.id.cube3Button);
        Button cube4Button = findViewById(R.id.cube4Button);
        Button cube5Button = findViewById(R.id.cube5Button);
        Button cube6Button = findViewById(R.id.cube6Button);
        Button cube7Button = findViewById(R.id.cube7Button);
        Button cube8Button = findViewById(R.id.cube8Button);

        // Ensure buttons are not null
        if (cube1Button == null || cube2Button == null || cube3Button == null ||
                cube4Button == null || cube5Button == null || cube6Button == null ||
                cube7Button == null || cube8Button == null) {
            throw new RuntimeException("One or more buttons are null! Check activity_cubes.xml.");
        }

        // Set OnClickListeners
        setButtonClick(cube1Button, 1);
        setButtonClick(cube2Button, 2);
        setButtonClick(cube3Button, 3);
        setButtonClick(cube4Button, 4);
        setButtonClick(cube5Button, 5);
        setButtonClick(cube6Button, 6);
        setButtonClick(cube7Button, 7);
        setButtonClick(cube8Button, 8);
    }

    private void setButtonClick(Button button, int cubeNumber) {
        button.setOnClickListener(v -> {
            Intent intent = new Intent(CubesActivity.this, MainActivity.class);
            intent.putExtra("cubeNumber", cubeNumber);
            startActivity(intent);
        });
    }
}
