package com.example.onroadhelp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterHelperActivity extends AppCompatActivity {

    private Button registerUserBtn;
    private TextInputEditText editTextName, editTextEmail, EditTextPhoneNo, editTextPassword, EditTextPsswordConf;
    TextView registerNow2;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            double lat = data.getDoubleExtra("latitude", 0.0);
            double lng = data.getDoubleExtra("longitude", 0.0);
            TextView locationText = findViewById(R.id.selected_location_text);
            locationText.setText("Lat: " + lat + ", Lng: " + lng);
            // You can also store these lat/lng values in your database
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_helper);
        editTextPassword = findViewById(R.id.password);
        editTextName = findViewById(R.id.shop_name);
        editTextEmail = findViewById(R.id.email);
        EditTextPhoneNo = findViewById(R.id.phone_no);
        EditTextPsswordConf = findViewById(R.id.password_conf);
        registerUserBtn = findViewById(R.id.register_user_btn);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();
        registerNow2 = findViewById(R.id.register_now2);
        registerNow2.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterHelperActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btn_pick_location).setOnClickListener(v -> {
            Intent intent = new Intent(RegisterHelperActivity.this, LocationPickerActivity.class);
            startActivityForResult(intent, 1001);
        });

        registerUserBtn.setOnClickListener(v -> {
            String name = editTextName.getText().toString();
            String email = editTextEmail.getText().toString();
            String phoneNo = EditTextPhoneNo.getText().toString();
            String password = editTextPassword.getText().toString();
            String passwordConf = EditTextPsswordConf.getText().toString();

        });
    }
}