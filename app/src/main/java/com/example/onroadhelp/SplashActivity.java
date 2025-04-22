package com.example.onroadhelp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class SplashActivity extends AppCompatActivity {

    Button getStarted;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        }
    public void opernLoginActivity(View view) {

            // Check if user is signed in (non-null) and update UI accordingly.
            mAuth = FirebaseAuth.getInstance();
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
                                            Intent intent = new Intent(SplashActivity.this, HelperMainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            Intent intent = new Intent(SplashActivity.this, UserMainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    } else {
                                        Log.d("LoginActivity", "Document exists, but 'role' field is missing.");
                                        Toast.makeText(SplashActivity.this, "Error: Could not determine user role.", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    Log.d("LoginActivity", "No such document");
                                    // Handle the case where the user document doesn't exist
                                    Toast.makeText(SplashActivity.this, "Error: User data not found.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Log.d("LoginActivity", "get failed with ", task.getException());
                                Toast.makeText(SplashActivity.this, "Error: Could not fetch user data.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }


        startActivity(new Intent(SplashActivity.this, ChooseRoleActivity.class));
    }
}
