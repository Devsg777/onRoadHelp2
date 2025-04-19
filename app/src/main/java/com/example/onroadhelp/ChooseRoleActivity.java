package com.example.onroadhelp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ChooseRoleActivity extends AppCompatActivity {

    ImageView img_service, img_driver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_choose_role);

        img_service = findViewById(R.id.Img_service);
        img_driver = findViewById(R.id.img_driver);

        img_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChooseRoleActivity.this, RegisterHelperActivity.class);
                startActivity(intent);
                finish();
            }
        });

        img_driver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChooseRoleActivity.this, RegisterUserActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}