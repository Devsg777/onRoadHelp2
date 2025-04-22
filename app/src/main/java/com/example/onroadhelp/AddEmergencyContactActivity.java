package com.example.onroadhelp;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddEmergencyContactActivity extends AppCompatActivity {

    private TextInputEditText editContactName, editContactPhone;
    private Button buttonSaveContact;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_emergency_contact);

        editContactName = findViewById(R.id.edit_contact_name);
        editContactPhone = findViewById(R.id.edit_contact_phone);
        buttonSaveContact = findViewById(R.id.button_save_contact);

        firestore = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        buttonSaveContact.setOnClickListener(v -> saveNewContact());
    }

    private void saveNewContact() {
        String name = editContactName.getText().toString().trim();
        String phone = editContactPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editContactName.setError("Contact name is required");
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            editContactPhone.setError("Phone number is required");
            return;
        }

        if (currentUser != null) {
            CollectionReference emergencyContactsRef = firestore
                    .collection("drivers")
                    .document(currentUser.getUid())
                    .collection("emergencyContacts");

            Map<String, Object> newContact = new HashMap<>();
            newContact.put("name", name);
            newContact.put("phoneNumber", phone);

            emergencyContactsRef.add(newContact)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Contact saved successfully", Toast.LENGTH_SHORT).show();
                        finish(); // Go back to the profile screen
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error saving contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Log the error for debugging
                    });
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
        }
    }
}