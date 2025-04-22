package com.example.onroadhelp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class ChooseRoleActivity extends AppCompatActivity {

    ImageView img_service, img_driver;
    FirebaseAuth  mAuth;
    FirebaseFirestore db;

//    @Override
//    public void onStart() {
//        super.onStart();
//        // Check if user is signed in (non-null) and update UI accordingly.
//        mAuth = FirebaseAuth.getInstance();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser != null) {
//            db = FirebaseFirestore.getInstance();
//            db.collection("users")
//                    .whereEqualTo("email", currentUser.getEmail())
//                    .get()
//                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                        @Override
//                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                            if (task.isSuccessful()) {
//                                for (QueryDocumentSnapshot document : task.getResult()) {
//                                    String role = document.getString("role");
//                                    assert role != null;
//                                    if (role.equals("helper")) {
//                                        Intent intent = new Intent(ChooseRoleActivity.this, HelperMainActivity.class);
//                                        startActivity(intent);
//                                        finish();
//                                    } else {
//                                        Intent intent = new Intent(ChooseRoleActivity.this, UserMainActivity.class);
//                                        startActivity(intent);
//                                        finish();
//                                    }
//                                }
//                            }
//                        }
//                    });
//        }
//
//    }

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