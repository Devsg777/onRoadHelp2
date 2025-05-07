package com.example.onroadhelp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.onroadhelp.services.LocationTrackingService;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterHelperActivity extends AppCompatActivity {

    private Button registerUserBtn;
    private TextInputEditText editTextName, editTextEmail, EditTextPhoneNo, editTextPassword, EditTextPsswordConf;
    TextView registerNow2;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    double lat, lng;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            lat = data.getDoubleExtra("latitude", 0.0);
            lng = data.getDoubleExtra("longitude", 0.0);
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


        AutoCompleteTextView shopTypeDropdown = findViewById(R.id.shop_type_dropdown);
        AutoCompleteTextView servicesDropdown = findViewById(R.id.services_dropdown);

// Data Arrays

        String[] servicesOptions = {
                "Tyre Services",
                "Battery Jumpstart Help",
                "Fuel Delivery",
                "On-Site Repair",
                "Tow Truck Booking",
                "Accident Assistance"
        };
        String[] shopTypes = {"Mechanic Shop", "Towing Service", "Other"};

        ArrayAdapter<String> shopTypeAdapter = new ArrayAdapter<>(
                this,
                com.google.android.material.R.layout.support_simple_spinner_dropdown_item,
                shopTypes
        );

        shopTypeDropdown.setAdapter(shopTypeAdapter);



// Multi-select Services
        boolean[] selectedServices = new boolean[servicesOptions.length];
        ArrayList<String> selectedItems = new ArrayList<>();

        servicesDropdown.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(RegisterHelperActivity.this)
                    .setTitle("Select Services")
                    .setMultiChoiceItems(servicesOptions, selectedServices, (dialog, which, isChecked) -> {
                        selectedServices[which] = isChecked;
                    })
                    .setPositiveButton("OK", (dialog, which) -> {
                        selectedItems.clear();
                        for (int i = 0; i < selectedServices.length; i++) {
                            if (selectedServices[i]) {
                                selectedItems.add(servicesOptions[i]);
                            }
                        }
                        servicesDropdown.setText(TextUtils.join(", ", selectedItems));
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        registerUserBtn.setOnClickListener(v -> {
            String name = editTextName.getText().toString();
            String email = editTextEmail.getText().toString();
            String phoneNo = EditTextPhoneNo.getText().toString();
            String password = editTextPassword.getText().toString();
            String passwordConf = EditTextPsswordConf.getText().toString();
            String shopType = shopTypeDropdown.getText().toString();
            String services = servicesDropdown.getText().toString();
            String location = "Lat: " + lat + ", Lng: " + lng;
            if(TextUtils.isEmpty(name)) {
            Toast.makeText(RegisterHelperActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
            }else if (TextUtils.isEmpty(email)) {
                Toast.makeText(RegisterHelperActivity.this, "Please enter your email", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(phoneNo)) {
                Toast.makeText(RegisterHelperActivity.this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(password)) {
                Toast.makeText(RegisterHelperActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(passwordConf)) {
                Toast.makeText(RegisterHelperActivity.this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(shopType)) {
                Toast.makeText(RegisterHelperActivity.this, "Please select a shop type", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(services)) {
                Toast.makeText(RegisterHelperActivity.this, "Please select services", Toast.LENGTH_SHORT).show();
            } else if (TextUtils.isEmpty(location)) {
                Toast.makeText(RegisterHelperActivity.this, "Please select a location", Toast.LENGTH_SHORT).show();
            } else if (!password.equals(passwordConf)) {
                Toast.makeText(RegisterHelperActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            } else if (password.length() < 6) {
                Toast.makeText(RegisterHelperActivity.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else if (lat == 0.0 && lng == 0.0) {
                Toast.makeText(RegisterHelperActivity.this, "Please select a valid location", Toast.LENGTH_SHORT).show();
            } else if (selectedItems.isEmpty()) {
                Toast.makeText(RegisterHelperActivity.this, "Please select at least one service", Toast.LENGTH_SHORT).show();
            } else {
                // Register the user
                registerUser(name, email, phoneNo, password, shopType, services, location);
            }


        });
    }

    private void registerUser(String name, String email, String phoneNo, String password, String shopType,  String services, String location) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Add data to the Cloud firestore if the user is registered successfully
                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            FirebaseUser u = mAuth.getCurrentUser();
                            String userId = u.getUid();

                            Map<String, Object> user = new HashMap<>();
                            user.put("email", email);
                            user.put("role", "Helper");
                            Map<String, Object> helper = new HashMap<>();
                            helper.put("name", name);
                            helper.put("email", email);
                            helper.put("phone_no", phoneNo);
                            helper.put("shop_type", shopType);
                            helper.put("services", services);
                            helper.put("lat", lat);
                            helper.put("lng", lng);

                            db.collection("users").document(userId)
                                    .set(user);
                            db.collection("helpers").document(userId)
                                    .set(helper)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterHelperActivity.this, "Shop Created Successfully.",
                                                Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(RegisterHelperActivity.this, HelperMainActivity.class);
                                        startActivity(intent);
                                        startLocationTrackingService();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(RegisterHelperActivity.this, "Failed to add Helper data.",
                                                Toast.LENGTH_SHORT).show();
                                    });

                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(RegisterHelperActivity.this, "Account creation failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void startLocationTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void stopLocationTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);
    }
}