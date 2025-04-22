package com.example.onroadhelp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;



public class LoginActivity extends AppCompatActivity {

    TextInputEditText editTextEmail, editTextPassword;
    Button loginButton;
    TextView registerNow;
    FirebaseAuth mAuth;
    TextView registerNow2;
    ProgressBar progressBar;


    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(userId).get()
                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() { // Changed to DocumentSnapshot
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult(); // Get a single DocumentSnapshot
                                if (document != null && document.exists()) {
                                    String role = document.getString("role");
                                    if (role != null) {
                                        if (role.equals("Helper")) {
                                            Intent intent = new Intent(LoginActivity.this, HelperMainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Intent intent = new Intent(LoginActivity.this, UserMainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    } else {
                                        Log.d("LoginActivity", "Document exists, but 'role' field is missing.");
                                        Toast.makeText(LoginActivity.this, "Error: Could not determine user role.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.d("LoginActivity", "No such document");
                                    // Handle the case where the user document doesn't exist
                                    Toast.makeText(LoginActivity.this, "Error: User data not found.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.d("LoginActivity", "get failed with ", task.getException());
                                Toast.makeText(LoginActivity.this, "Error: Could not fetch user data.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_btn);

        mAuth = FirebaseAuth.getInstance();

        registerNow2 = findViewById(R.id.register_now2);
        progressBar = findViewById(R.id.progressBar);

        registerNow2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, ChooseRoleActivity.class);
                startActivity(intent);
                finish();
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String email,password;
                email = String.valueOf(editTextEmail.getText());
                password = String.valueOf(editTextPassword.getText());


                if(TextUtils.isEmpty(email)){
                    Toast.makeText(LoginActivity.this,"Enter email",Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

               if(TextUtils.isEmpty(password)){
                   Toast.makeText(LoginActivity.this,"Enter password",Toast.LENGTH_SHORT).show();
                   progressBar.setVisibility(View.GONE);
                   return;
               }

                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener( new OnCompleteListener< AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information

                                    FirebaseUser currentUser = mAuth.getCurrentUser();

                                    if (currentUser != null) {
                                        String userId = currentUser.getUid();
                                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                                        db.collection("users").document(userId).get()
                                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() { // Changed to DocumentSnapshot
                                                    @Override
                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                        if (task.isSuccessful()) {
                                                            DocumentSnapshot document = task.getResult(); // Get a single DocumentSnapshot
                                                            if (document != null && document.exists()) {
                                                                String role = document.getString("role");
                                                                if (role != null) {
                                                                    if (role.equals("Helper")) {
                                                                        Intent intent = new Intent(LoginActivity.this, HelperMainActivity.class);
                                                                        startActivity(intent);
                                                                        finish();
                                                                    } else {
                                                                        Intent intent = new Intent(LoginActivity.this, UserMainActivity.class);
                                                                        startActivity(intent);
                                                                        finish();
                                                                    }
                                                                } else {
                                                                    Log.d("LoginActivity", "Document exists, but 'role' field is missing.");
                                                                    Toast.makeText(LoginActivity.this, "Error: Could not determine user role.", Toast.LENGTH_SHORT).show();
                                                                }
                                                            } else {
                                                                Log.d("LoginActivity", "No such document");
                                                                // Handle the case where the user document doesn't exist
                                                                Toast.makeText(LoginActivity.this, "Error: User data not found.", Toast.LENGTH_SHORT).show();
                                                            }
                                                        } else {
                                                            Log.d("LoginActivity", "get failed with ", task.getException());
                                                            Toast.makeText(LoginActivity.this, "Error: Could not fetch user data.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                    }

                                    Intent intent = new Intent(LoginActivity.this, UserMainActivity.class);
                                    startActivity(intent);
                                    finish();

                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(LoginActivity.this, "Incorrect email or password",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });

    }
}