package com.example.onroadhelp.ui2.profile;

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
import com.bumptech.glide.Glide;
import com.example.onroadhelp.LoginActivity;
import com.example.onroadhelp.R;
import com.example.onroadhelp.databinding.FragmentHelperProfileSettingsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class HelperProfileSettingsFragment extends Fragment {

    private FragmentHelperProfileSettingsBinding binding;
    private Uri profileImageUri;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageReference = storage.getReference();
    private String helperId;

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
        binding = FragmentHelperProfileSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        helperId = mAuth.getCurrentUser().getUid();
        loadHelperProfile();

        binding.buttonUploadPhoto.setOnClickListener(v -> openImageChooser());
        binding.buttonSaveProfile.setOnClickListener(v -> saveHelperProfile());
        binding.buttonLogout.setOnClickListener(v -> logoutHelper());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void loadHelperProfile() {
        if (helperId != null) {
            DocumentReference helperRef = db.collection("helpers").document(helperId);
            helperRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");
                            String phoneNo = documentSnapshot.getString("phone_no");
                            String desc = documentSnapshot.getString("desc");
                            String services = documentSnapshot.getString("services");
                            Boolean isAvailable = documentSnapshot.getBoolean("isAvailable");
                            String profilePicUrl = documentSnapshot.getString("profile_pic");

                            binding.editName.setText(name);
                            binding.editEmail.setText(email);
                            binding.editPhoneNo.setText(phoneNo);
                            binding.editDesc.setText(desc);
                            binding.editServices.setText(services);
                            if (isAvailable != null) {
                                binding.switchAvailability.setChecked(isAvailable);
                            }
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.picture)
                                        .circleCrop()
                                        .into(binding.imageProfile);
                            }
                        } else {
                            Log.d("ProfileSettings", "Helper profile not found.");
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
            StorageReference profileImageRef = storageReference.child("images/profiles/" + helperId + ".jpg");
            profileImageRef.putFile(profileImageUri)
                    .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        db.collection("helpers").document(helperId)
                                .update("profile_pic", downloadUrl)
                                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile photo updated.", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update photo URL.", Toast.LENGTH_SHORT).show());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error uploading photo.", Toast.LENGTH_SHORT).show());
        }
    }

    private void saveHelperProfile() {
        if (helperId != null) {
            String name = binding.editName.getText().toString().trim();
            String desc = binding.editDesc.getText().toString().trim();
            String services = binding.editServices.getText().toString().trim();
            boolean isAvailable = binding.switchAvailability.isChecked();

            Map<String, Object> profileUpdates = new HashMap<>();
            profileUpdates.put("name", name);
            profileUpdates.put("desc", desc);
            profileUpdates.put("services", services);
            profileUpdates.put("isAvailable", isAvailable);

            db.collection("helpers").document(helperId)
                    .update(profileUpdates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                        Log.d("ProfileSettings", "Helper profile updated.");
                        if (profileImageUri != null) {
                            uploadProfilePhoto();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProfileSettings", "Error updating profile: " + e.getMessage());
                        Toast.makeText(getContext(), "Error updating profile.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(getContext(), "Helper ID not found.", Toast.LENGTH_SHORT).show();
        }
    }

    private void logoutHelper() {
        mAuth.signOut();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }
}