package com.example.onroadhelp.ui.requests; // Replace with your actual package name

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.fragment.app.FragmentTransaction;

import com.example.onroadhelp.NavigationFragment;
import com.example.onroadhelp.adapter.ActiveRequestAdapter;
import com.example.onroadhelp.databinding.FragmentActiveRequestsBinding; // Import your binding class
import com.example.onroadhelp.model.SOSRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FragmentActiveRequests extends Fragment {

    private FragmentActiveRequestsBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ActiveRequestAdapter adapter;
    private List<SOSRequest> activeRequestsList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentActiveRequestsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        activeRequestsList = new ArrayList<>();

        binding.recyclerViewActiveRequests.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ActiveRequestAdapter(activeRequestsList); // We will create this adapter next
        binding.recyclerViewActiveRequests.setAdapter(adapter);

        loadActiveRequests();
    }

    private void loadActiveRequests() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("sos_requests")
                .whereEqualTo("customerId", userId)
                .whereIn("status", List.of("pending_acceptance", "accepted"))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        // Handle error
                        return;
                    }
                    if (querySnapshot != null) {
                        activeRequestsList.clear();
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            SOSRequest request = document.toObject(SOSRequest.class);
                            request.setRequestId(document.getId()); // Assuming you want to store the document ID
                            activeRequestsList.add(request);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}