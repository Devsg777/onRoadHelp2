package com.example.onroadhelp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterUserActivity extends AppCompatActivity {

    private Button registerUserBtn;
    private TextInputEditText editTextName, editTextEmail, EditTextPhoneNo, editTextPassword, EditTextPsswordConf;
    TextView registerNow2;
    FirebaseAuth mAuth;
    ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_user);
        editTextPassword = findViewById(R.id.password);
        editTextName = findViewById(R.id.name);
        editTextEmail = findViewById(R.id.email);
        EditTextPhoneNo = findViewById(R.id.phone_no);
        EditTextPsswordConf = findViewById(R.id.password_conf);
        registerUserBtn = findViewById(R.id.register_user_btn);
        progressBar = findViewById(R.id.progressBar);


        mAuth = FirebaseAuth.getInstance();

        registerNow2 = findViewById(R.id.register_now2);
        registerNow2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterUserActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        registerUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);
                String name,email,phoneNo,password,passwordConf;
                name = String.valueOf(editTextName.getText());
                email = String.valueOf(editTextEmail.getText());
                phoneNo = String.valueOf(EditTextPhoneNo.getText());
                password = String.valueOf(editTextPassword.getText());
                passwordConf = String.valueOf(EditTextPsswordConf.getText());

                if(TextUtils.isEmpty(name)){
                    Toast.makeText(RegisterUserActivity.this,"Enter name",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(email)){
                    Toast.makeText(RegisterUserActivity.this,"Enter email",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(phoneNo)){
                    Toast.makeText(RegisterUserActivity.this,"Enter phone number",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    Toast.makeText(RegisterUserActivity.this,"Enter password",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(passwordConf)){
                    Toast.makeText(RegisterUserActivity.this,"Enter confirm password",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(!password.equals(passwordConf)){
                    Toast.makeText(RegisterUserActivity.this,"Password not matching",Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener( new OnCompleteListener< AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Toast.makeText(RegisterUserActivity.this, "Account created successfully.",
                                            Toast.LENGTH_SHORT).show();
                                    // Add data to the Cloud firestore if the user is registered successfully
                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    String userId = user.getUid();
                                    Map<String, Object> user1 = new HashMap<>();
                                    user1.put("name", name);
                                    user1.put("email", email);
                                    user1.put("phone_no", phoneNo);

                                    db.collection("users").document(userId)
                                            .set(user1)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(RegisterUserActivity.this, "User Created Successfully.",
                                                        Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(RegisterUserActivity.this, LoginActivity.class);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(RegisterUserActivity.this, "Failed to add user data.",
                                                        Toast.LENGTH_SHORT).show();
                                            });

                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(RegisterUserActivity.this, "Account creation failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });


            }
        });

    }
}