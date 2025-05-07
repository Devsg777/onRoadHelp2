package com.example.onroadhelp;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

public class TrackHelper extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private String helperId;
    private String helperName;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration helperLocationListener;
    private TextView helperNameTextView;
    private TextView helperVehicleTextView;
    private TextView distanceTextView;
    private TextView etaTextView;
    private LatLng userLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_helper);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        helperNameTextView = findViewById(R.id.helperNameTextView);
        helperVehicleTextView = findViewById(R.id.helperVehicleTextView);
        distanceTextView = findViewById(R.id.distanceTextView);
        etaTextView = findViewById(R.id.etaTextView);

        helperId = getIntent().getStringExtra("helperId");
        double userLat = getIntent().getDoubleExtra("userLat", 0.0);
        double userLng = getIntent().getDoubleExtra("userLng", 0.0);
        helperName = getIntent().getStringExtra("helperName");
        String helperVehicle = "Bike"; // You might fetch this from Firestore as well

        userLatLng = new LatLng(userLat, userLng);
        helperNameTextView.setText(helperName);
        helperVehicleTextView.setText(helperVehicle);

        Log.d("TrackHelper", "onCreate: Retrieved helperId from Intent: " + helperId);

        if (helperId == null || userLat == 0.0 || userLng == 0.0) {
            Toast.makeText(this, "Error: Missing helper or user location data.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));

        startHelperLocationUpdates();
    }

    private void startHelperLocationUpdates() {
        if (helperId != null && !helperId.isEmpty()) {
            helperLocationListener = db.collection("helpers").document(helperId)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.w("TrackHelper", "Listen failed.", e);
                            Toast.makeText(TrackHelper.this, "Error tracking helper location.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            GeoPoint location = documentSnapshot.getGeoPoint("location");
                            String name = documentSnapshot.getString("name");
                            String vehicle = documentSnapshot.getString("vehicle");

                            if (location != null) {
                                LatLng helperLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                updateMap(helperLatLng);
                                updateDistanceAndETA(helperLatLng);
                                if (name != null && !name.isEmpty() && !name.equals(helperName)) {
                                    helperNameTextView.setText(name);
                                    helperName = name;
                                }
                                if (vehicle != null) helperVehicleTextView.setText("Vehicle: " + vehicle);
                            } else {
                                Log.d("TrackHelper", "Helper location is null in Firestore.");
                                Toast.makeText(TrackHelper.this, "Helper location not yet available.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d("TrackHelper", "Helper document not found in Firestore with ID: " + helperId);
                            Toast.makeText(TrackHelper.this, "Error: Helper information not found.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Log.e("TrackHelper", "Helper ID is null or empty, cannot start location updates.");
            Toast.makeText(TrackHelper.this, "Error: Invalid Helper ID.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateMap(LatLng helperLatLng) {
        if (mMap == null) return;
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));
        mMap.addMarker(new MarkerOptions().position(helperLatLng).title(helperName));

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(userLatLng, helperLatLng)
                .color(getResources().getColor(R.color.lavender)) // Ensure you have this color defined
                .width(10);
        mMap.addPolyline(polylineOptions);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(userLatLng);
        builder.include(helperLatLng);
        LatLngBounds bounds = builder.build();

        int padding = 100;
        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        } catch (Exception e) {
            // Handle the case where bounds might be invalid (e.g., same location)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(helperLatLng, 15));
        }
    }

    private void updateDistanceAndETA(LatLng helperLatLng) {
        float[] results = new float[1];
        Location.distanceBetween(userLatLng.latitude, userLatLng.longitude,
                helperLatLng.latitude, helperLatLng.longitude, results);
        float distanceInMeters = results[0];
        float distanceInKm = distanceInMeters / 1000;
        distanceTextView.setText(String.format("%.2f km", distanceInKm));

        if (distanceInKm > 0) {
            double timeInHours = distanceInKm / 30.0; // Assuming 30 km/h average speed
            int etaInMinutes = (int) (timeInHours * 60);
            etaTextView.setText(String.format("%d mins", etaInMinutes));
        } else {
            etaTextView.setText("Arriving");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (helperLocationListener != null) {
            helperLocationListener.remove();
            helperLocationListener = null;
        }
    }
}