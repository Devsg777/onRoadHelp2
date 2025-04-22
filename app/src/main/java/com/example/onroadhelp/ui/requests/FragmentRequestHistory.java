package com.example.onroadhelp.ui.requests; // Replace with your actual package name

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onroadhelp.adapter.RequestHistoryAdapter;
import com.example.onroadhelp.databinding.FragmentRequestHistoryBinding; // Import your binding class
import com.example.onroadhelp.model.SOSRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FragmentRequestHistory extends Fragment {

    private FragmentRequestHistoryBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RequestHistoryAdapter adapter;
    private List<SOSRequest> requestHistoryList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRequestHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        requestHistoryList = new ArrayList<>();

        binding.recyclerViewRequestHistory.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new RequestHistoryAdapter(requestHistoryList); // We will create this adapter next
        binding.recyclerViewRequestHistory.setAdapter(adapter);

        loadRequestHistory();
    }

    private void loadRequestHistory() {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId != null) {
            db.collection("sos_requests")
                    .whereEqualTo("customerId", userId)
                    .whereIn("status", List.of("resolved", "canceled"))
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener((querySnapshot, error) -> {
                        if (error != null) {
                            // Handle error
                            return;
                        }
                        if (querySnapshot != null) {
                            requestHistoryList.clear();
                            for (QueryDocumentSnapshot document : querySnapshot) {
                                SOSRequest request = document.toObject(SOSRequest.class);
                                request.setRequestId(document.getId());
                                requestHistoryList.add(request);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}