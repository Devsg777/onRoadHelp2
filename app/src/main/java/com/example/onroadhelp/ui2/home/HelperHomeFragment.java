package com.example.onroadhelp.ui2.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelperHomeFragment extends Fragment {

    private RecyclerView recyclerViewSosRequests;
    private SOSRequestAdapter adapter;
    private List<SOSRequest> sosRequestList = new ArrayList<>();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String currentHelperId;

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

    private void fetchSOSRequests() {
        db.collection("sos_requests")
                .whereIn("status", Arrays.asList("pending", "accepted"))
                .whereEqualTo("acceptedHelperId", currentHelperId) // Show only this helper's accepted requests
                .orderBy("timestamp")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("FetchSOS", "Listen failed.", e);
                            return;
                        }

                        sosRequestList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            SOSRequest request = doc.toObject(SOSRequest.class);
                            request.setRequestId(doc.getId());
                            sosRequestList.add(request);
                        }
                        adapter = new SOSRequestAdapter(sosRequestList, currentHelperId);
                        recyclerViewSosRequests.setAdapter(adapter);
                        adapter.setOnItemClickListener(new SOSRequestAdapter.OnItemClickListener() {
                            @Override
                            public void onItemClick(SOSRequest sosRequest) {
                                // Handle click on accepted request (e.g., view details)
                                Log.d("HelperHome", "Clicked accepted request: " + sosRequest.getRequestId());
                                // Implement navigation or action here
                            }
                        });
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}