package com.example.onroadhelp.ui2.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onroadhelp.R;
import com.example.onroadhelp.adapter.SOSRequestAdapter;
import com.example.onroadhelp.model.SOSRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HelperHomeFragment extends Fragment {

    private RecyclerView recyclerViewSosRequests;
    private SOSRequestAdapter adapter;
    private List<SOSRequest> sosRequestList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentHelperId;
    private ListenerRegistration pendingRequestsListenerRegistration;
    private ListenerRegistration acceptedRequestsListenerRegistration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_helper_home, container, false);
        recyclerViewSosRequests = root.findViewById(R.id.sos_requests_recycler_view);
        recyclerViewSosRequests.setLayoutManager(new LinearLayoutManager(getContext()));

        currentHelperId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchSOSRequests();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Re-fetch data if the logged-in user has changed
        String newHelperId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (!newHelperId.equals(currentHelperId)) {
            currentHelperId = newHelperId;
            fetchSOSRequests();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Detach the listeners to prevent memory leaks and unnecessary updates
        if (pendingRequestsListenerRegistration != null) {
            pendingRequestsListenerRegistration.remove();
            pendingRequestsListenerRegistration = null;
        }
        if (acceptedRequestsListenerRegistration != null) {
            acceptedRequestsListenerRegistration.remove();
            acceptedRequestsListenerRegistration = null;
        }
    }

    private void fetchSOSRequests() {
        if (currentHelperId == null) {
            // Handle the case where the user ID is not yet available
            Log.w("HelperHome", "Current user ID is null. Cannot fetch SOS requests.");
            return;
        }

        List<SOSRequest> combinedList = new ArrayList<>();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Query for pending_acceptance requests NOT created by the current user
        if (pendingRequestsListenerRegistration != null) {
            pendingRequestsListenerRegistration.remove(); // Remove previous listener
        }
        pendingRequestsListenerRegistration = db.collection("sos_requests")
                .whereEqualTo("status", "pending_acceptance")
                .orderBy("timestamp")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("FetchSOS", "Listen failed for pending requests.", e);
                            return;
                        }
                        List<SOSRequest> pendingRequests = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            SOSRequest request = doc.toObject(SOSRequest.class);
                            request.setRequestId(doc.getId());
                            pendingRequests.add(request);
                        }
                        combinedList.clear();
                        combinedList.addAll(pendingRequests);
                        for (SOSRequest acceptedRequest : acceptedRequestsList) {
                            if (!combinedList.contains(acceptedRequest)) {
                                combinedList.add(acceptedRequest);
                            }
                        }
                        updateAdapter(combinedList);
                    }
                });

        // Query for accepted requests by the current helper
        if (acceptedRequestsListenerRegistration != null) {
            acceptedRequestsListenerRegistration.remove(); // Remove previous listener
        }
        acceptedRequestsListenerRegistration = db.collection("sos_requests")
                .whereEqualTo("status", "accepted")
                .whereEqualTo("acceptedHelperId", currentHelperId)
                .orderBy("timestamp")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("FetchSOS", "Listen failed for accepted requests.", e);
                            return;
                        }
                        acceptedRequestsList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            SOSRequest request = doc.toObject(SOSRequest.class);
                            request.setRequestId(doc.getId());
                            acceptedRequestsList.add(request);
                        }
                        combinedList.clear();
                        combinedList.addAll(acceptedRequestsList);
                        for (SOSRequest pendingRequest : pendingRequestsList) {
                            if (!combinedList.contains(pendingRequest)) {
                                combinedList.add(pendingRequest);
                            }
                        }
                        updateAdapter(combinedList);
                    }
                });
    }

    private List<SOSRequest> pendingRequestsList = new ArrayList<>();
    private List<SOSRequest> acceptedRequestsList = new ArrayList<>();

    private void updateAdapter(List<SOSRequest> combinedList) {
        if (isAdded()) { // Check if the fragment is still attached before updating UI
            adapter = new SOSRequestAdapter(combinedList, currentHelperId);
            recyclerViewSosRequests.setAdapter(adapter);
            adapter.setOnItemClickListener(new SOSRequestAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(SOSRequest sosRequest) {
                    Log.d("HelperHome", "Clicked request: " + sosRequest.getRequestId() + ", Status: " + sosRequest.getStatus());
                    if (sosRequest.getStatus().equals("pending_acceptance")) {
                        showAcceptRejectDialog(sosRequest);
                    } else if (sosRequest.getStatus().equals("accepted")) {
                        // Handle click on accepted request
                    }
                }
            });
            adapter.notifyDataSetChanged();
        }
    }

    private void showAcceptRejectDialog(final SOSRequest sosRequest) {
        if (isAdded()) {
            acceptSOSRequest(sosRequest);
        }
    }

    private void acceptSOSRequest(SOSRequest sosRequest) {
        if (isAdded()) {
            db.collection("sos_requests")
                    .document(sosRequest.getRequestId())
                    .update("status", "accepted",
                            "acceptedHelperId", currentHelperId)
                    .addOnSuccessListener(aVoid -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Request accepted!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("HelperHome", "Error accepting request: " + e.getMessage());
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Failed to accept request.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}