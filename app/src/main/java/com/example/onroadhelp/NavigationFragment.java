package com.example.onroadhelp; // Replace with your actual package name

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.onroadhelp.databinding.FragmentNavigationBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Locale;

public class NavigationFragment extends Fragment implements OnMapReadyCallback {

    private FragmentNavigationBinding binding;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String requestId; // You'll need to pass the request ID to this fragment
    private String helperId; // You'll get this when the request is accepted
    private ListenerRegistration helperLocationListener;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable locationUpdateRunnable;
    private static final long LOCATION_UPDATE_INTERVAL = 5000; // Update every 5 seconds

    // Helper information (replace with actual data fetching)
    private String helperName = "John Doe";
    private String helperVehicle = "Bike (KA 01 XX 1234)";
    private LatLng helperCurrentLocation;
    private LatLng customerCurrentLocation; // You'll need to get this

    public NavigationFragment() {
        // Required empty public constructor
    }

    public static NavigationFragment newInstance(String requestId) {
        NavigationFragment fragment = new NavigationFragment();
        Bundle args = new Bundle();
        args.putString("requestId", requestId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            requestId = getArguments().getString("requestId");
            // You might need to fetch the helperId associated with this requestId here
            // For now, we'll assume you have it or will get it via a listener
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        // Get current customer location (you need to implement this)
        getCurrentCustomerLocation();
    }

    private void getCurrentCustomerLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Handle permission request here if not granted
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        customerCurrentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        if (mMap != null) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customerCurrentLocation, 15f));
                            mMap.addMarker(new MarkerOptions().position(customerCurrentLocation).title("Your Location"));
                        }
                    }
                });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNavigationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Set static helper info initially (replace with actual data)
        binding.helperNameTextView.setText(helperName);
        binding.helperVehicleTextView.setText(helperVehicle);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Enable my location if permission is granted
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        // Move camera to current customer location if available
        if (customerCurrentLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(customerCurrentLocation, 15f));
            mMap.addMarker(new MarkerOptions().position(customerCurrentLocation).title("Your Location"));
        }

        // Start listening for helper's location updates
        startHelperLocationUpdates();
    }

    private void startHelperLocationUpdates() {
        // Assuming you have the helperId associated with the accepted request
        if (helperId != null) {
            helperLocationListener = db.collection("helpers") // Replace "helpers" with your helper collection name
                    .document(helperId)
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Log.w("NavigationFragment", "Listen failed.", e);
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Double latitude = documentSnapshot.getDouble("latitude"); // Replace "latitude" with your field name
                            Double longitude = documentSnapshot.getDouble("longitude"); // Replace "longitude" with your field name
                            if (latitude != null && longitude != null) {
                                helperCurrentLocation = new LatLng(latitude, longitude);
                                updateMapWithHelperLocation();
                                calculateAndUpdateDistanceAndTime();
                            } else {
                                Log.d("NavigationFragment", "Helper location data is null.");
                                Toast.makeText(getContext(), "Waiting for helper's location...", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.d("NavigationFragment", "Helper document not found.");
                            Toast.makeText(getContext(), "Helper information not available.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // If helperId is not yet available, you might need to fetch it
            // based on the requestId or listen for updates on the request document
            Log.d("NavigationFragment", "Helper ID not yet available.");
            Toast.makeText(getContext(), "Waiting for helper assignment...", Toast.LENGTH_SHORT).show();
            // You might set up a listener on the 'requests' collection here to get the helperId
        }
    }

    private void updateMapWithHelperLocation() {
        if (mMap != null && helperCurrentLocation != null) {
            mMap.clear(); // Clear previous markers
            mMap.addMarker(new MarkerOptions().position(customerCurrentLocation).title("Your Location"));
            mMap.addMarker(new MarkerOptions().position(helperCurrentLocation).title(helperName));
            // Optionally, you can draw a polyline to show the route
            // For simplicity, we're just showing markers here
        }
    }

    private void calculateAndUpdateDistanceAndTime() {
        if (helperCurrentLocation != null && customerCurrentLocation != null) {
            // In a real app, you would use the Directions API or Distance Matrix API
            // to get accurate distance and time based on roads.
            // For a simple approximation, you can use Location.distanceBetween:
            float[] results = new float[1];
            Location.distanceBetween(customerCurrentLocation.latitude, customerCurrentLocation.longitude,
                    helperCurrentLocation.latitude, helperCurrentLocation.longitude, results);
            float distanceInMeters = results[0];
            float distanceInKilometers = distanceInMeters / 1000;
            binding.distanceTextView.setText(String.format(Locale.getDefault(), "%.2f km", distanceInKilometers));

            // Simple ETA calculation (assuming a constant speed - replace with actual routing)
            float averageSpeedKmPerHour = 30; // Example speed
            float timeInHours = distanceInKilometers / averageSpeedKmPerHour;
            int timeInMinutes = (int) (timeInHours * 60);
            binding.etaTextView.setText(String.format(Locale.getDefault(), "%d mins", timeInMinutes));
        } else {
            binding.distanceTextView.setText("N/A");
            binding.etaTextView.setText("N/A");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (helperLocationListener != null) {
            helperLocationListener.remove(); // Stop listening for location updates
        }
        if (handler != null && locationUpdateRunnable != null) {
            handler.removeCallbacks(locationUpdateRunnable); // Stop any scheduled updates
        }
    }
}