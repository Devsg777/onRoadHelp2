package com.example.onroadhelp.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.onroadhelp.UserMainActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

public class LocationTrackingService extends Service {

    private static final String CHANNEL_ID = "location_tracking_channel";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String userId; // Will store the current user's ID

    private LocationRequest locationRequest;
    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                Log.d("LocationService", "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());
                updateUserLocationInFirestore(location.getLatitude(), location.getLongitude());
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        createNotificationChannel();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user's ID

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // Adjust as needed
        locationRequest.setFastestInterval(2000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Check user role here (you might need to fetch it from Firestore)
        // For simplicity, let's assume you have a way to know the role
        boolean isServiceProvider = checkIfCurrentUserIsServiceProvider();

        if (isServiceProvider) {
            startLocationUpdates();
            Notification notification = buildNotification("Tracking your location...");
            startForeground(1, notification);
        } else {
            stopForeground(true);
            stopSelf();
        }
        return START_STICKY;
    }

    private boolean checkIfCurrentUserIsServiceProvider() {
        // Implement your logic to determine if the current user is a service provider
        // This might involve checking a user profile in Firestore or a custom claim in Firebase Auth
        // For now, let's just return true for demonstration purposes.
        // **Replace this with your actual role-checking mechanism.**
        return true;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Log.e("LocationService", "Location permission not granted.");
            // Handle the case where permission is not granted
        }
    }

    private void updateUserLocationInFirestore(double latitude, double longitude) {
        if (userId != null) {
            GeoPoint geoPoint = new GeoPoint(latitude, longitude);
            db.collection("helpers").document(userId) // Use the user's UID as the document ID
                    .update("location", geoPoint)
                    .addOnSuccessListener(aVoid -> Log.d("FirestoreUpdate", "Helper location updated"))
                    .addOnFailureListener(e -> Log.w("FirestoreUpdate", "Error updating location", e));
        } else {
            Log.e("LocationService", "User ID is null, cannot update location.");
        }
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking Service",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification buildNotification(String text) {
        Intent notificationIntent = new Intent(this, /* Your MainActivity or relevant activity */ UserMainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Use your app's icon
                .setContentTitle("Location Tracking")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setOngoing(true); // Make it a foreground service notification

        return builder.build();
    }
}