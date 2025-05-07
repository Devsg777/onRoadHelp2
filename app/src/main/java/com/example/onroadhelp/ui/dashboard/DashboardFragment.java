package com.example.onroadhelp.ui.dashboard;

import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.onroadhelp.R;
import com.example.onroadhelp.TrackHelper;
import com.example.onroadhelp.databinding.FragmentDashboardBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private LatLng selectedLocation;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private static final int SMS_PERMISSION_REQUEST_CODE = 102;
    private ProgressBar progressBar;
    private Button sosButton;
    private String currentRequestId;
    private ListenerRegistration sosRequestUpdateListener;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                }
                Boolean coarse = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                }
                if (fine != null && fine || coarse != null && coarse) {
                    getCurrentLocationAndPin();
                } else {
                    Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(map -> {
                googleMap = map;

                googleMap.setOnMapClickListener(latLng -> {
                    selectedLocation = latLng;
                    googleMap.clear();
                    googleMap.addMarker(new MarkerOptions().position(latLng).title("Selected Location"));
                    loadHelpersOnMap();
                });
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    googleMap.setMyLocationEnabled(true);
                }
                checkLocationPermissions();
            });
        }

        AutoCompleteTextView problemDropdown = binding.shopTypeDropdown;
        String[] problems = {"Flat Tire", "Engine Issue", "Accident", "Battery Issue", "Fuel Issue", "Lockout", "Other"};
        problemDropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, problems));

        AutoCompleteTextView vehicleTypeDropdown = binding.vehicleTypeDropdown;
        String[] vehicleTypes = {"Car", "Bike", "Truck","Van","Bus","EV-Car","EV-Bike","Other"};
        vehicleTypeDropdown.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, vehicleTypes));

        EditText vehicleNumber = binding.vehicleNumber;

        sosButton = binding.sosButton;
        progressBar = binding.progressBar;
        checkAnyPendigRequests();
        sosButton.setOnClickListener(v -> {
            if (selectedLocation == null) {
                Toast.makeText(getContext(), "Please select a location first!", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedProblem = problemDropdown.getText().toString();
            if (selectedProblem.isEmpty()) {
                Toast.makeText(getContext(), "Please select a problem!", Toast.LENGTH_SHORT).show();
                return;
            }
            String vehicleNo = vehicleNumber.getText().toString();
            String vehicleType = vehicleTypeDropdown.getText().toString();
            if (currentRequestId != null) {
                Toast.makeText(getContext(), "You already have a pending SOS request!", Toast.LENGTH_SHORT).show();
                return;
            }

            sosButton.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
            sendSOSRequest(selectedLocation, selectedProblem, vehicleNo, vehicleType);
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (sosRequestUpdateListener != null) {
            sosRequestUpdateListener.remove();
        }
    }

    private void checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        } else {
            getCurrentLocationAndPin();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocationAndPin() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && googleMap != null) {
                selectedLocation = new LatLng(location.getLatitude(), location.getLongitude());
                googleMap.clear();

                Marker userMarker = googleMap.addMarker(new MarkerOptions()
                        .position(selectedLocation)
                        .title("Your Location"));

                if (userMarker != null) {
                    userMarker.showInfoWindow();
                }

                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
                loadHelpersOnMap();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
        );
    }

    private  void checkAnyPendigRequests(){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("sos_requests")
                .whereEqualTo("customerId", userId)
                .whereEqualTo("status", "pending_acceptance")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String requestId = document.getId();
                            String status = document.getString("status");
                            if ("pending_acceptance".equals(status)) {
                                // SOS request is pending acceptance
                                currentRequestId = requestId;
                                progressBar.setVisibility(View.VISIBLE);
                                sosButton.setEnabled(false);
                                break;
                            }
                        }
                    }
                });
    }
    private void loadHelpersOnMap() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("helpers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && googleMap != null) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Double lat = document.getDouble("lat");
                            Double lng = document.getDouble("lng");
                            String name = document.getString("name");
                            String shopType = document.getString("shop_type");
                            String helperId = document.getId();

                            if (lat != null && lng != null) {
                                LatLng helperLocation = new LatLng(lat, lng);

                                Marker helperMarker = googleMap.addMarker(new MarkerOptions()
                                        .position(helperLocation)
                                        .title(name != null ? name : "Helper")
                                        .snippet(shopType != null ? shopType : "Shop")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                                if (helperMarker != null) {
                                    helperMarker.setTag(helperId);
                                }
                            }
                        }
                    } else {
                        Toast.makeText(getContext(), "Failed to load helpers", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendSOSRequest(LatLng location, String problem, String vehicleNo, String vehicleType) {
        Toast.makeText(getContext(), "SOS Requesting...", Toast.LENGTH_SHORT).show();

        String userId = mAuth.getCurrentUser().getUid();
        GeoPoint geoPoint = new GeoPoint(location.latitude, location.longitude);

        // 1. Create SOS Request in Firestore
        Map<String, Object> sosRequest = new HashMap<>();
        sosRequest.put("customerId", userId);
        sosRequest.put("location", geoPoint);
        sosRequest.put("problem", problem);
        sosRequest.put("status", "pending_acceptance"); // Initial status
        sosRequest.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        sosRequest.put("vehicleNo", vehicleNo); // Placeholder for vehicle number
        sosRequest.put("vehicleType",vehicleType); // Placeholder for vehicle type

        db.collection("sos_requests")
                .add(sosRequest)
                .addOnSuccessListener(documentReference -> {
                    currentRequestId = documentReference.getId();
                    Log.d("SOS", "SOS Request created with ID: " + currentRequestId);
                    broadcastSOS(currentRequestId, location, problem);
                    startListeningForHelperAcceptance(currentRequestId);
                    sendEmergencyNotifications(location, problem);
                    Toast.makeText(getContext(), "SOS request sent. Waiting for a helper...", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error creating SOS request.", Toast.LENGTH_SHORT).show();
                    Log.e("SOS", "Error creating SOS request: " + e.getMessage());
                    // Re-enable button and hide progress bar on failure
                    if (sosButton != null && progressBar != null) {
                        sosButton.setEnabled(true);
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void startListeningForHelperAcceptance(String requestId) {
        sosRequestUpdateListener = db.collection("sos_requests").document(requestId)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Log.w("SOS", "Listen for SOS update failed.", e);
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        String acceptedHelperId = documentSnapshot.getString("acceptedHelperId");

                        if ("accepted".equals(status) && acceptedHelperId != null && !acceptedHelperId.isEmpty()) {
                            // Helper has accepted the request
                            Log.d("SOS", "Helper accepted. Request ID: " + requestId + ", Helper ID: " + acceptedHelperId);
                            stopListeningForHelperAcceptance(); // Stop listening for further updates
                            navigateToTrackHelperActivity(requestId, acceptedHelperId);
                        } else if ("completed".equals(status) || "cancelled".equals(status)) {
                            // Request completed or cancelled, stop listening and update UI accordingly
                            Log.d("SOS", "SOS request completed or cancelled.");
                            stopListeningForHelperAcceptance();
                            // Optionally show a message to the user
                            if (progressBar != null) {
                                progressBar.setVisibility(View.GONE);
                            }
                            if (sosButton != null) {
                                sosButton.setEnabled(true);
                            }
                        }
                    }
                });
    }

    private void stopListeningForHelperAcceptance() {
        if (sosRequestUpdateListener != null) {
            sosRequestUpdateListener.remove();
            sosRequestUpdateListener = null;
        }
    }

    private void navigateToTrackHelperActivity(String requestId, String helperId) {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), TrackHelper.class);
            intent.putExtra("requestId", requestId);
            intent.putExtra("helperId", helperId);
            startActivity(intent);
            // Optionally finish the current activity/fragment if you don't want to go back
            // getActivity().finish();
        }
    }

    private void broadcastSOS(String requestId, LatLng location, String problem) {
        // Query all helpers - you might want to filter by their service type or availability
        db.collection("helpers")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.DocumentSnapshot helperDoc : queryDocumentSnapshots) {
                        String helperToken = helperDoc.getString("fcmToken");
                        if (helperToken != null) {
                            sendNotificationToHelper(helperToken, "New SOS Request!", "A customer needs help with: " + problem + " nearby. Request ID: " + requestId);
                        }
                    }
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "No service providers found nearby.", Toast.LENGTH_SHORT).show();
                        // Optionally update SOS request status to 'failed_no_providers'
                        db.collection("sos_requests").document(requestId).update("status", "failed_no_providers");
                        stopListeningForHelperAcceptance();
                        if (progressBar != null) {
                            progressBar.setVisibility(View.GONE);
                        }
                        if (sosButton != null) {
                            sosButton.setEnabled(true);
                        }
                        // Optionally show a message to the user
                    }
                })
                .addOnFailureListener(e -> Log.e("SOS", "Error querying helpers for broadcast: " + e.getMessage()));
    }

    private void sendNotificationToHelper(String fcmToken, String title, String body) {
        // Implement your FCM notification sending logic here (likely server-side)
        Log.d("FCM", "Sending notification to token: " + fcmToken + " - Title: " + title + ", Body: " + body);
        // Placeholder
    }

    private void sendEmergencyNotifications(LatLng location, String problem) {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("drivers").document(userId).collection("emergencyContacts")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> phoneNumbers = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot contactDoc : queryDocumentSnapshots) {
                        String phoneNumber = contactDoc.getString("phoneNumber");
                        if (phoneNumber != null) {
                            phoneNumbers.add(phoneNumber);
                        }
                    }
                    sendEmergencySMS(phoneNumbers, problem, location);
                    sendEmergencyPushNotification(phoneNumbers, problem, location);
                })
                .addOnFailureListener(e -> Log.e("SOS", "Error fetching emergency contacts: " + e.getMessage()));
    }

    private void sendEmergencySMS(List<String> phoneNumbers, String problem, LatLng location) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestSmsPermission();
            return;
        }

        SmsManager smsManager = SmsManager.getDefault();
        String message = "EMERGENCY! User needs help with: " + problem + " at location: " +
                "[http://maps.google.com/?q=](http://maps.google.com/?q=)" + location.latitude + "," + location.longitude;

        for (String phoneNumber : phoneNumbers) {
            try {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Log.d("SMS", "SMS sent to: " + phoneNumber);
            } catch (Exception e) {
                Log.e("SMS", "Error sending SMS to " + phoneNumber + ": " + e.getMessage());
            }
        }
    }

    private void requestSmsPermission() {
        ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST_CODE);
    }

    private void sendEmergencyPushNotification(List<String> phoneNumbers, String problem, LatLng location) {
        // Implement push notification logic
        Log.d("Notification", "Sending push notification to emergency contacts for: " + problem + " at " + location);
    }

    private void startNavigation(LatLng customerLocation, String helperId) {
        // This function will now be called from the WaitingForHelperFragment
        // after a helper accepts.
        Log.d("Navigation", "Navigation intent created for customer to: " + customerLocation + " by helper: " + helperId);
        Uri customerNavigationIntentUri = Uri.parse("google.navigation:q=" + customerLocation.latitude + "," + customerLocation.longitude);
        Intent customerNavigationIntent = new Intent(Intent.ACTION_VIEW, customerNavigationIntentUri);
        customerNavigationIntent.setPackage("com.google.android.apps.maps");
        startActivity(customerNavigationIntent);

        // You'll likely fetch helper profile and navigate to a combined Navigation/Profile screen here.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SMS permission granted, you can now try sending the SMS again
                if (selectedLocation != null && binding != null && binding.shopTypeDropdown != null) {
                    // You might need to store the problem temporarily
                    // sendEmergencySMS(emergencyContacts, binding.shopTypeDropdown.getText().toString(), selectedLocation);
                }
            } else {
                Toast.makeText(getContext(), "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}