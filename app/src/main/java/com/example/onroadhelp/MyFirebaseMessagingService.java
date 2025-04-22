package com.example.onroadhelp; // Adjust your package name

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFCMService";

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);

        // Send this token to your server (or store it in Firestore)
        // associated with the helper's user ID.
        sendRegistrationToServer(token);
    }

    private void sendRegistrationToServer(String token) {
        // TODO: Implement this method to send the device's FCM token to your server
        // or directly to Firestore.

        // Example of sending the token to Firestore:
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String userId = auth.getCurrentUser().getUid();
            db.collection("helpers").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM token updated in Firestore for user: " + userId))
                    .addOnFailureListener(e -> Log.w(TAG, "Error updating FCM token in Firestore: " + e.getMessage()));
        } else {
            Log.w(TAG, "User not logged in, cannot update FCM token.");
            // You might want to handle this case (e.g., store locally and update on login)
        }
    }

    // ... (onMessageReceived and sendNotification methods remain the same) ...
}