package com.example.onroadhelp.ui.profile; // Adjust the package name

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.onroadhelp.AddEmergencyContactActivity;
import com.example.onroadhelp.LoginActivity;
import com.example.onroadhelp.R;
import com.example.onroadhelp.adapter.EmergencyContactAdapter;
import com.example.onroadhelp.databinding.FragmentProfileSettingsBinding; // Adjust binding class
import com.example.onroadhelp.ui.profile.EmergencyContact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileSettingsFragment extends Fragment implements EmergencyContactAdapter.OnItemClickListener {

    private FragmentProfileSettingsBinding binding;
    private Uri profileImageUri;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageReference = storage.getReference();
    private String userId; // Using userId for driver
    private List<EmergencyContact> emergencyContactsList;
    private EmergencyContactAdapter contactAdapter;
    private DocumentReference driverDocumentRef;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            profileImageUri = result.getData().getData();
                            Glide.with(this)
                                    .load(profileImageUri)
                                    .circleCrop()
                                    .into(binding.imageProfile);
                        }
                    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileSettingsBinding.inflate(inflater, container, false);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            driverDocumentRef = db.collection("drivers").document(userId);
            loadUserProfile();
            loadEmergencyContacts();
        }

        binding.recyclerEmergencyContacts.setLayoutManager(new LinearLayoutManager(getContext()));
        emergencyContactsList = new ArrayList<>();
        contactAdapter = new EmergencyContactAdapter(emergencyContactsList, this); // Pass the listener
        binding.recyclerEmergencyContacts.setAdapter(contactAdapter);

        binding.buttonLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        binding.buttonAddEmergencyContact.setOnClickListener(v -> {
            // Navigate to a screen to add a new emergency contact
            startActivity(new Intent(getActivity(), AddEmergencyContactActivity.class));
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonUploadPhoto.setOnClickListener(v -> openImageChooser());
        binding.buttonSaveProfile.setOnClickListener(v -> saveUserProfile());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadUserProfile() {
        if (userId != null) {
            DocumentReference userRef = db.collection("drivers").document(userId); // Assuming "drivers" collection
            userRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phoneNo = documentSnapshot.getString("phone_no");
                            String profilePicUrl = documentSnapshot.getString("profile_pic");

                            binding.editName.setText(name);
                            binding.editEmail.setText(email);
                            binding.editPhoneNo.setText(phoneNo);
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.picture)
                                        .circleCrop()
                                        .into(binding.imageProfile);
                            }
                        } else {
                            Log.d("ProfileSettings", "User profile not found.");
                            Toast.makeText(getContext(), "Profile not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileSettings", "Error loading profile: " + e.getMessage());
                        Toast.makeText(getContext(), "Error loading profile.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfilePhoto() {
        if (profileImageUri != null) {
            StorageReference profileImageRef = storageReference.child("images/users/" + userId + ".jpg"); // Different path for users
            profileImageRef.putFile(profileImageUri)
                    .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        db.collection("drivers").document(userId) // Assuming "drivers" collection
                                .update("profile_pic", downloadUrl)
                                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile photo updated.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update photo URL.", Toast.LENGTH_SHORT).show());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error uploading photo.", Toast.LENGTH_SHORT).show());
        }
    }

    private void loadEmergencyContacts() {
        if (driverDocumentRef != null) {
            driverDocumentRef.collection("emergencyContacts") // Assuming emergency contacts are a sub-collection
                    .addSnapshotListener((value, error) -> {
                        if (error != null) {
                            Log.w("ProfileSettings", "Listen failed.", error);
                            Toast.makeText(getContext(), "Failed to load emergency contacts.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        emergencyContactsList.clear();
                        if (value != null) {
                            for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                                EmergencyContact contact = doc.toObject(EmergencyContact.class);
                                contact.setDocumentId(doc.getId()); // Store the document ID for deletion
                                emergencyContactsList.add(contact);
                            }
                        }
                        contactAdapter.notifyDataSetChanged();
                    });
        }
    }

    @Override
    public void onItemClick(EmergencyContact contact) {
        // Handle item click if needed
    }

    @Override
    public void onDeleteClick(EmergencyContact contact) {
        deleteEmergencyContact(contact.getDocumentId());
    }

    private void deleteEmergencyContact(String contactId) {
        if (driverDocumentRef != null && contactId != null) {
            driverDocumentRef.collection("emergencyContacts").document(contactId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Contact deleted successfully.", Toast.LENGTH_SHORT).show();
                        // The snapshot listener will automatically update the RecyclerView
                    })
                    .addOnFailureListener(e -> {
                        Log.w("ProfileSettings", "Error deleting document", e);
                        Toast.makeText(getContext(), "Failed to delete contact.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void saveUserProfile() {
        if (userId != null) {
            String name = binding.editName.getText().toString().trim();

            Map<String, Object> profileUpdates = new HashMap<>();
            profileUpdates.put("name", name);

            db.collection("drivers").document(userId) // Assuming "drivers" collection
                    .update(profileUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                        Log.d("ProfileSettings", "User profile updated.");
                        if (profileImageUri != null) {
                            uploadProfilePhoto();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileSettings", "Error updating profile: " + e.getMessage());
                        Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "User ID not found.", Toast.LENGTH_SHORT).show();
        }
    }
}