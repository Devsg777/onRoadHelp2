// ViewProfileActivity.java
package com.example.onroadhelp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewProfileActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView imageProfilePicture;
    private TextView textProfileName;
    private TextView textProfileDesc;
    private TextView textProfileEmail;
    private TextView textProfilePhone;
    private TextView textProfileShopType;
    private LinearLayout layoutServices;
    private MapView mapViewProfile;
    private GoogleMap googleMap;
    private double providerLat;
    private double providerLng;
    private String providerName;
    private ImageView iconPhone; // Reference to the phone icon

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        imageProfilePicture = findViewById(R.id.image_profile_picture);
        textProfileName = findViewById(R.id.text_profile_name);
        textProfileDesc = findViewById(R.id.text_profile_desc);
        textProfileEmail = findViewById(R.id.text_profile_email);
        textProfilePhone = findViewById(R.id.text_profile_phone);
        textProfileShopType = findViewById(R.id.text_profile_shop_type);
        layoutServices = findViewById(R.id.layout_services);
        mapViewProfile = findViewById(R.id.map_view_profile);
        iconPhone = findViewById(R.id.icon_phone); // Initialize the phone icon

        mapViewProfile.onCreate(savedInstanceState);
        mapViewProfile.getMapAsync(this);

        // Retrieve the service provider ID (name) passed from the adapter
        String providerNameExtra = getIntent().getStringExtra("providerId");
        if (providerNameExtra != null && !providerNameExtra.isEmpty()) {
            this.providerName = providerNameExtra;
            fetchServiceProviderDetails(providerNameExtra);
        } else {
            finish();
        }

        // Set OnClickListener for the phone number TextView
        textProfilePhone.setOnClickListener(v -> initiateCall(textProfilePhone.getText().toString()));

        // Set OnClickListener for the phone icon ImageView
        iconPhone.setOnClickListener(v -> initiateCall(textProfilePhone.getText().toString()));
    }

    private void initiateCall(String phoneNumberWithPrefix) {
        // Remove the "Phone Number: " prefix if it exists
        String phoneNumber = phoneNumberWithPrefix.replace("Phone Number: ", "").trim();

        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "No phone application found...", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchServiceProviderDetails(String providerName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("helpers")
                .whereEqualTo("email", providerName)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (com.google.firebase.firestore.QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                            String name = documentSnapshot.getString("name");
                            String desc = documentSnapshot.getString("desc");
                            String email = documentSnapshot.getString("email");
                            String phoneNo = documentSnapshot.getString("phone_no");
                            String profilePicUrl = documentSnapshot.getString("profile_pic");
                            String servicesString = documentSnapshot.getString("services");
                            String shopType = documentSnapshot.getString("shop_type");
                            Double lat = documentSnapshot.getDouble("lat");
                            Double lng = documentSnapshot.getDouble("lng");

                            textProfileName.setText(name);
                            textProfileDesc.setText(desc);
                            textProfileEmail.setText(email);
                            textProfilePhone.setText("Phone Number: " + phoneNo); // Keep the prefix for display
                            textProfileShopType.setText(shopType);

                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.mechanic) // Make sure this exists
                                        .error(R.drawable.mechanic)
                                        .circleCrop()// Make sure this exists
                                        .into(imageProfilePicture);
                                imageProfilePicture.setVisibility(View.VISIBLE);
                            } else {
                                imageProfilePicture.setVisibility(View.GONE); // Or set a default image
                            }

                            if (servicesString != null && !servicesString.isEmpty()) {
                                String[] serviceList = servicesString.split(",");
                                for (String service : serviceList) {
                                    TextView serviceTextView = new TextView(this);
                                    serviceTextView.setText("• " + service.trim());
                                    serviceTextView.setTextSize(16);
                                    serviceTextView.setTextColor(getResources().getColor(R.color.lavender));
                                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.WRAP_CONTENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                    );
                                    params.setMargins(0, 0, 0, 8);
                                    serviceTextView.setLayoutParams(params);
                                    layoutServices.addView(serviceTextView);
                                }
                            } else {
                                TextView noServicesTextView = new TextView(this);
                                noServicesTextView.setText("No services listed.");
                                noServicesTextView.setTextSize(16);
                                noServicesTextView.setTextColor(getResources().getColor(R.color.lavender));
                                layoutServices.addView(noServicesTextView);
                            }

                            if (lat != null && lng != null) {
                                providerLat = lat;
                                providerLng = lng;
                                if (googleMap != null) {
                                    LatLng providerLocation = new LatLng(providerLat, providerLng);
                                    googleMap.addMarker(new MarkerOptions().position(providerLocation).title(providerName));
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(providerLocation, 15));
                                }
                            }

                            return;
                        }
                    } else {
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    finish();
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        LatLng providerLocation = new LatLng(providerLat, providerLng);
        googleMap.addMarker(new MarkerOptions().position(providerLocation).title(providerName));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(providerLocation, 15));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapViewProfile.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapViewProfile.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapViewProfile.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapViewProfile.onLowMemory();
    }
}