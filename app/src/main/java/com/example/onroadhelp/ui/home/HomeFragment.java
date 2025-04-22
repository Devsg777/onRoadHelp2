// HomeFragment.java
package com.example.onroadhelp.ui.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private NearbyServiceProviderAdapter adapter;
    private final List<NearbyServiceProvider> allNearbyProviders = new ArrayList<>();
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
        adapter = new NearbyServiceProviderAdapter(allNearbyProviders);
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
                Toast.makeText(getContext(), "View Profile clicked for " + activeHelperName, Toast.LENGTH_SHORT).show();
                // Implement view profile logic if needed
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
                            String helperPhoneNo = document.getString("phone_no");
                            updateActiveRequest(helperName, helperPhoneNo, helperId);
                        }
                    }
                });
    }

    private void updateActiveRequest(String helperName, String phone, String helperId) {
        this.activeHelperName = helperName;
        this.activeHelperPhoneNo = phone;
        this.activeHelperId = helperId;
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
                        List<NearbyServiceProvider> tempProviders = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d("FirestoreDebug", "Processing registered helper: " + document.getId());
                            String name = document.getString("name");
                            String type = document.getString("type");
                            String phoneNumber = document.getString("contactNumber");
                            Double latitude = document.getDouble("lat");
                            Double longitude = document.getDouble("lan");

                            Log.d("FirestoreData", "Registered - Name: " + name + ", Type: " + type + ", Phone: " + phoneNumber + ", Lat: " + latitude + ", Lon: " + longitude);

                            if (latitude != null && longitude != null) {
                                double distanceKm = calculateDistance(
                                        currentUserLocation.getLatitude(),
                                        currentUserLocation.getLongitude(),
                                        latitude,
                                        longitude
                                );

                                if (distanceKm <= MAX_DISTANCE_KM) {
                                    NearbyServiceProvider provider = new NearbyServiceProvider(
                                            name, distanceKm, type, phoneNumber, ProviderType.REGISTERED
                                    );
                                    tempProviders.add(provider);
                                    Log.d("ListDebug", "Added registered provider: " + name + ", Distance: " + distanceKm);
                                } else {
                                    Log.d("DistanceDebug", "Registered provider " + name + " is too far: " + distanceKm + " km");
                                }
                            } else {
                                Log.w("FirestoreData", "Registered helper " + document.getId() + " has null lat/lon.");
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

                            List<NearbyServiceProvider> tempProviders = new ArrayList<>();
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

    private void fetchPlaceDetails(String placeId, String name, double distance, String type, List<NearbyServiceProvider> tempProviders) {
        String placeDetailsUrl = "https://maps.googleapis.com/maps/api/place/details/json"
                + "?place_id=" + placeId
                + "&fields=formatted_phone_number"
                + "&key=" + getString(R.string.google_maps_api_key);

        Request placeDetailsRequest = new Request.Builder().url(placeDetailsUrl).build();

        httpClient.newCall(placeDetailsRequest).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API", "Place Details API request failed for place ID: " + placeId, e);
                NearbyServiceProvider provider = new NearbyServiceProvider(name, distance, type, "", ProviderType.API);
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

                        NearbyServiceProvider provider = new NearbyServiceProvider(name, distance, type, phoneNumber, ProviderType.API);
                        tempProviders.add(provider);
                        Log.d("ListDebug", "Added API provider: " + name + ", Distance: " + distance + ", Phone: " + phoneNumber);

                    } catch (JSONException e) {
                        Log.e("API", "JSON parsing error (Place Details)", e);
                        NearbyServiceProvider provider = new NearbyServiceProvider(name, distance, type, "", ProviderType.API);
                        tempProviders.add(provider);
                        Log.d("ListDebug", "Added API provider (parse error): " + name + ", Distance: " + distance);
                    }
                } else {
                    Log.w("API", "Place Details API request failed with code: " + response.code());
                    NearbyServiceProvider provider = new NearbyServiceProvider(name, distance, type, "", ProviderType.API);
                    tempProviders.add(provider);
                    Log.d("ListDebug", "Added API provider (API error): " + name + ", Distance: " + distance);
                }
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
        Collections.sort(allNearbyProviders, (p1, p2) -> Double.compare(p1.distance, p2.distance));
        adapter.notifyDataSetChanged();
    }

    private static class NearbyServiceProvider {
        String name;
        double distance;
        String type;
        String phoneNumber;
        ProviderType providerType;

        public NearbyServiceProvider(String name, double distance, String type, String phoneNumber, ProviderType providerType) {
            this.name = name;
            this.distance = distance;
            this.type = type;
            this.phoneNumber = phoneNumber;
            this.providerType = providerType;
        }
    }

    private enum ProviderType {
        REGISTERED,
        API
    }

    private static class NearbyServiceProviderAdapter extends RecyclerView.Adapter<NearbyServiceProviderAdapter.ViewHolder> {
        private final List<NearbyServiceProvider> providerList;

        public NearbyServiceProviderAdapter(List<NearbyServiceProvider> providerList) {
            this.providerList = providerList;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_nearby_service, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NearbyServiceProvider provider = providerList.get(position);
            Log.d("AdapterDebug", "Binding at position " + position + ": Name=" + provider.name +
                    ", Distance=" + provider.distance + ", Type=" + provider.type + ", Phone=" + provider.phoneNumber);
            holder.nameTextView.setText(provider.name);
            holder.distanceTextView.setText(String.format("%.2f km", provider.distance));
            holder.typeTextView.setText(provider.type);
            holder.phoneTextView.setText(provider.phoneNumber);

            if (provider.providerType == ProviderType.API && !TextUtils.isEmpty(provider.phoneNumber)) {
                holder.callButton.setVisibility(View.VISIBLE);
                holder.callButton.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + provider.phoneNumber));
                    v.getContext().startActivity(callIntent);
                });
            } else {
                holder.callButton.setVisibility(View.GONE);
            }

            if (provider.providerType == ProviderType.REGISTERED) {
                holder.itemView.setBackgroundColor(Color.parseColor("#E0F7FA"));
                holder.providerIcon.setImageResource(R.drawable.breakdown);
                holder.providerIcon.setVisibility(View.VISIBLE);
            } else {
                holder.itemView.setBackgroundColor(Color.parseColor("#F0F4C3"));
                holder.providerIcon.setImageResource(R.drawable.icons8_google);
                holder.providerIcon.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return providerList.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameTextView, distanceTextView, typeTextView, phoneTextView;
            ImageButton callButton;
            ImageView providerIcon;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.text_provider_name);
                distanceTextView = itemView.findViewById(R.id.text_provider_distance);
                typeTextView = itemView.findViewById(R.id.text_provider_type);
                phoneTextView = itemView.findViewById(R.id.text_provider_phone);
                callButton = itemView.findViewById(R.id.button_call);
                providerIcon = itemView.findViewById(R.id.image_provider_icon);
            }
        }
    }
}