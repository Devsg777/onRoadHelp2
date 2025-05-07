// HomeFragment.java
package com.example.onroadhelp.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onroadhelp.R;
import com.example.onroadhelp.TrackHelper;
import com.example.onroadhelp.ViewProfileActivity;
import com.example.onroadhelp.adapter.NearbyServiceAdapter;
import com.example.onroadhelp.model.ServiceProvider;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class HomeFragment extends Fragment {

    private LinearLayout layoutActiveRequest;
    private TextView textHelperName;
    private TextView textHelperPhoneNo;
    private Button buttonTrack;
    private Button buttonViewProfile;
    private Button buttonCancel;
    private ImageButton buttonCall;

    private RecyclerView nearbyProvidersRecyclerView;
    private NearbyServiceAdapter adapter;
    private final List<ServiceProvider> allNearbyProviders = new ArrayList<>();
    private final OkHttpClient httpClient = new OkHttpClient();

    private FusedLocationProviderClient fusedLocationClient;
    private Location currentUserLocation;
    private String userId;
    private FirebaseFirestore firestore;
    private PlacesClient placesClient;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int MAX_DISTANCE_KM = 100;

    private boolean isActiveRequest = false;
    private String activeHelperName;
    private String activeHelperPhoneNo;
    private String activeHelperId;
    private String activeHelperEmail;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        firestore = FirebaseFirestore.getInstance();
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_api_key));
        }
        placesClient = Places.createClient(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        layoutActiveRequest = root.findViewById(R.id.layout_active_request);
        textHelperName = root.findViewById(R.id.text_helper_name);
        textHelperPhoneNo = root.findViewById(R.id.text_helper_phone_no);
        buttonTrack = root.findViewById(R.id.button_track);
        buttonViewProfile = root.findViewById(R.id.button_view_profile);
        buttonCancel = root.findViewById(R.id.button_cancel);
        buttonCall = root.findViewById(R.id.button_call);

        nearbyProvidersRecyclerView = root.findViewById(R.id.recycler_nearby_providers);
        nearbyProvidersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NearbyServiceAdapter();
        nearbyProvidersRecyclerView.setAdapter(adapter);
        nearbyProvidersRecyclerView.setNestedScrollingEnabled(false);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        getActiveSosRequest();
        getCurrentLocation();
        return root;
    }

    private void setupActiveRequest() {
        if (isActiveRequest && activeHelperId != null) {
            layoutActiveRequest.setVisibility(View.VISIBLE);
            textHelperName.setText("Helper: " + activeHelperName);
            textHelperPhoneNo.setText("Phone No: " + activeHelperPhoneNo);

            buttonCall.setOnClickListener(v -> {
                String phone = activeHelperPhoneNo;
                if (phone != null && !phone.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + phone));
                    v.getContext().startActivity(intent);
                }
            });

            buttonTrack.setOnClickListener(v -> {
                Log.d("TrackHelper", "onCreate: Retrieved helperId from Intent: " + activeHelperId + "");
                Intent trackIntent = new Intent(getActivity(), TrackHelper.class);
                trackIntent.putExtra("helperId", activeHelperId);
                if (currentUserLocation != null) {
                    trackIntent.putExtra("userLat", currentUserLocation.getLatitude());
                    trackIntent.putExtra("userLng", currentUserLocation.getLongitude());
                }
                trackIntent.putExtra("helperName", activeHelperName);
                startActivity(trackIntent);
            });

            buttonViewProfile.setOnClickListener(v -> {
//                Toast.makeText(getContext(), "View Profile clicked for " + activeHelperName, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(v.getContext(), ViewProfileActivity.class);
                intent.putExtra("providerId",activeHelperEmail );
                v.getContext().startActivity(intent);
            });

            buttonCancel.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Cancel clicked for " + activeHelperName, Toast.LENGTH_SHORT).show();
                // Implement cancel request logic if needed
                isActiveRequest = false;
                layoutActiveRequest.setVisibility(View.GONE);
            });
        } else {
            layoutActiveRequest.setVisibility(View.GONE);
        }
    }

    private void getActiveSosRequest() {
        firestore.collection("sos_requests")
                .whereEqualTo("status", "accepted").whereEqualTo("customerId", userId).limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String helperId = document.getString("acceptedHelperId");

                            if (helperId != null && !helperId.isEmpty()) {
                                getHelperforId(helperId);
                                return; // Assuming only one active request at a time
                            }
                        }
                        // If no active request found
                        isActiveRequest = false;
                        setupActiveRequest();
                    } else {
                        Log.w("FirestoreData", "Error getting active SOS requests.", task.getException());
                        isActiveRequest = false;
                        setupActiveRequest();
                    }
                });
    }

    private void getHelperforId(String helperId) {
        firestore.collection("helpers").document(helperId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String helperName = document.getString("name");
                            String helperPhoneNo = document.getString("contactNumber");
                            String helper_email = document.getString("email");// Assuming "contactNumber"
                            updateActiveRequest(helperName, helperPhoneNo, helperId,helper_email);
                        }
                    }
                });
    }

    private void updateActiveRequest(String helperName, String phone, String helperId,String helper_email) {
        this.activeHelperName = helperName;
        this.activeHelperPhoneNo = phone;
        this.activeHelperId = helperId;
        this.activeHelperEmail = helper_email;
        isActiveRequest = true;
        setupActiveRequest();
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        currentUserLocation = location;
                        Log.d("LocationDebug", "Current Location Success: " + currentUserLocation);
                        fetchNearbyServices();
                    } else {
                        Log.w("Location", "Last known location was null.");
                    }
                })
                .addOnFailureListener(e -> Log.e("Location", "Error getting last location: " + e.getMessage()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(requireContext(), "Location permission is required.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchNearbyServices() {
        if (currentUserLocation != null) {
            allNearbyProviders.clear();
            fetchRegisteredProviders();
            fetchApiProviders();
        }
    }
    private void fetchRegisteredProviders() {
        Log.d("FetchDebug", "Fetching registered providers...");
        firestore.collection("helpers")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && currentUserLocation != null) {
                        Log.d("FetchDebug", "Successfully fetched registered providers.");
                        List<ServiceProvider> tempProviders = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("FirestoreDebug", "Processing registered helper: " + document.getId());
                            String name = document.getString("name");
                            String email = document.getString("email");
                            String type = document.getString("shop_type");
                            String phoneNumber = document.getString("phone_no");
                            Double latitude = document.getDouble("lat");
                            Double longitude = document.getDouble("lng");

                            Log.d("FirestoreData", "Registered - Name: " + name + ", Type: " + type + ", Phone: " + phoneNumber + ", Lat: " + latitude + ", Lon: " + longitude);

                            if (latitude != null && longitude != null && phoneNumber != null && !phoneNumber.isEmpty()) {
                                double distanceKm = calculateDistance(
                                        currentUserLocation.getLatitude(),
                                        currentUserLocation.getLongitude(),
                                        latitude,
                                        longitude
                                );

                                if (distanceKm <= MAX_DISTANCE_KM) {
                                    ServiceProvider provider = new ServiceProvider(
                                            name, email, distanceKm, type, phoneNumber, false
                                            // isFromApi = false for registered
                                    );
                                    tempProviders.add(provider);
                                    Log.d("ListDebug", "Added registered provider: " + name + ", Distance: " + distanceKm);
                                } else {
                                    Log.d("DistanceDebug", "Registered provider " + name + " is too far: " + distanceKm + " km");
                                }
                            } else {
                                Log.w("FirestoreData", "Registered helper " + document.getId() + " has null lat/lon or phone.");
                            }
                        }
                        requireActivity().runOnUiThread(() -> {
                            allNearbyProviders.addAll(tempProviders);
                            sortAndUpdateList();
                        });
                    } else {
                        Log.w("HelperHomeFragment", "Error getting registered helpers.", task.getException());
                    }
                });
    }

    private void fetchApiProviders() {
        List<String> types = Arrays.asList("mechanic", "gas_station", "car_repair");
        Log.d("FetchDebug", "Fetching API providers...");

        for (String type : types) {
            String nearbySearchUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json"
                    + "?location=" + currentUserLocation.getLatitude() + "," + currentUserLocation.getLongitude()
                    + "&radius=" + (MAX_DISTANCE_KM * 1000)
                    + "&type=" + type
                    + "&key=" + getString(R.string.google_maps_api_key);

            Request nearbySearchRequest = new Request.Builder().url(nearbySearchUrl).build();

            httpClient.newCall(nearbySearchRequest).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e("API", "Nearby Search API request failed for type: " + type, e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String json = response.body().string();
                        try {
                            JSONObject root = new JSONObject(json);
                            JSONArray results = root.getJSONArray("results");
                            Log.d("API_Debug", "Nearby Search results for " + type + ": " + results.length());

                            List<ServiceProvider> tempProviders = new ArrayList<>();
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject place = results.getJSONObject(i);
                                String placeId = place.getString("place_id");
                                String name = place.optString("name");
                                JSONObject location = place.getJSONObject("geometry").getJSONObject("location");
                                double lat = location.getDouble("lat");
                                double lng = location.getDouble("lng");

                                double distanceKm = calculateDistance(
                                        currentUserLocation.getLatitude(),
                                        currentUserLocation.getLongitude(),
                                        lat, lng
                                );
                                Log.d("API_Data", "Nearby - Name: " + name + ", Lat: " + lat + ", Lon: " + lng + ", Distance: " + distanceKm);

                                fetchPlaceDetails(placeId, name, distanceKm, type, tempProviders);
                            }
                            requireActivity().runOnUiThread(() -> {
                                allNearbyProviders.addAll(tempProviders);
                                sortAndUpdateList();
                            });

                        } catch (JSONException e) {
                            Log.e("API", "JSON parsing error (Nearby Search)", e);
                        }
                    } else {
                        Log.w("API", "Nearby Search API request failed with code: " + response.code());
                    }
                }
            });
        }
    }

    private void fetchPlaceDetails(String placeId, String name, double distance, String type, List<ServiceProvider> tempProviders) {
        String placeDetailsUrl = "https://maps.googleapis.com/maps/api/place/details/json"
                + "?place_id=" + placeId
                + "&fields=formatted_phone_number"
                + "&key=" + getString(R.string.google_maps_api_key);

        Request placeDetailsRequest = new Request.Builder().url(placeDetailsUrl).build();

        httpClient.newCall(placeDetailsRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API", "Place Details API request failed for place ID: " + placeId, e);
                ServiceProvider provider = new ServiceProvider(name,"", distance, type, "", true); // isFromApi = true for API
                tempProviders.add(provider);
                Log.d("ListDebug", "Added API provider (no details): " + name + ", Distance: " + distance);
                // Sorting will be handled after the main API call finishes
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    try {
                        JSONObject root = new JSONObject(json);
                        JSONObject result = root.optJSONObject("result");
                        String phoneNumber = "";
                        if (result != null) {
                            phoneNumber = result.optString("formatted_phone_number", "");
                        }
                        Log.d("API_Details", "Details for " + name + " - Phone: " + phoneNumber);

                        ServiceProvider provider = new ServiceProvider(name,"", distance, type, phoneNumber, true); // isFromApi = true for API
                        tempProviders.add(provider);
                        Log.d("ListDebug", "Added API provider: " + name + ", Distance: " + distance + ", Phone: " + phoneNumber);

                    } catch (JSONException e) {
                        Log.e("API", "JSON parsing error (Place Details)", e);
                        ServiceProvider provider = new ServiceProvider(name, "",distance, type, "", true); // isFromApi = true for API
                        tempProviders.add(provider);
                        Log.d("ListDebug", "Added API provider (parse error): " + name + ", Distance: " + distance);
                    }
                } else {
                    Log.w("API", "Place Details API request failed with code: " + response.code());
                    ServiceProvider provider = new ServiceProvider(name, "",distance, type, "", true); // isFromApi = true for API
                    tempProviders.add(provider);
                    Log.d("ListDebug", "Added API provider (API error): " + name + ", Distance: " + distance);
                }
                // Sorting will
                // Sorting will be handled after the main API call finishes
            }
        });
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000;
    }

    private void sortAndUpdateList() {
        Log.d("ListSortDebug", "Sorting and updating list. Size: " + allNearbyProviders.size());
        Collections.sort(allNearbyProviders, (p1, p2) -> Double.compare(p1.getDistance(), p2.getDistance()));
        adapter.setProviders(new ArrayList<>(allNearbyProviders)); // Pass a new list to avoid potential modification issues
    }
}