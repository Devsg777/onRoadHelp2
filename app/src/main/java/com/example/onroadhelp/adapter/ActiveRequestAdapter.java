package com.example.onroadhelp.adapter; // Replace with your actual package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onroadhelp.databinding.ItemActiveRequestBinding; // Import your binding class
import com.example.onroadhelp.model.SOSRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ActiveRequestAdapter extends RecyclerView.Adapter<ActiveRequestAdapter.ActiveRequestViewHolder> {

    private final List<SOSRequest> activeRequestsList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public ActiveRequestAdapter(List<SOSRequest> activeRequestsList) {
        this.activeRequestsList = activeRequestsList;
    }

    @NonNull
    @Override
    public ActiveRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemActiveRequestBinding binding = ItemActiveRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ActiveRequestViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveRequestViewHolder holder, int position) {
        SOSRequest currentRequest = activeRequestsList.get(position);
        holder.bind(currentRequest);
    }

    @Override
    public int getItemCount() {
        return activeRequestsList.size();
    }

    public static class ActiveRequestViewHolder extends RecyclerView.ViewHolder {
        private final ItemActiveRequestBinding binding;

        public ActiveRequestViewHolder(ItemActiveRequestBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(SOSRequest request) {
            binding.textViewProblem.setText(request.getProblem());

            // Format the timestamp
            if (request.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                binding.textViewTimestamp.setText(sdf.format(request.getTimestamp()));
            } else {
                binding.textViewTimestamp.setText("N/A");
            }

            binding.textViewStatus.setText(request.getStatus());

            // Control the visibility of the cancel button based on the status
            if (request.getStatus().equals("pending_acceptance")) {
                binding.buttonCancel.setVisibility(View.VISIBLE);
                binding.buttonCancel.setOnClickListener(v -> {
                    // Implement the logic to cancel the request here
                    // You'll likely want to update the 'status' field in Firestore
                    FirebaseFirestore.getInstance()
                            .collection("sos_requests")
                            .document(request.getRequestId())
                            .update("status", "cancelled")
                            .addOnSuccessListener(aVoid -> {
                                // Optionally show a success message
                            })
                            .addOnFailureListener(e -> {
                                // Handle the error
                            });
                    // After canceling, remove the item from the list and update the UI
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        ((ActiveRequestAdapter.ActiveRequestViewHolder) this).removeItem(position);
                    }
                });
            } else {
                binding.buttonCancel.setVisibility(View.GONE);
            }

            // You can add more UI updates here based on other fields in your SOSRequest model
        }

        public void removeItem(int position) {
            ActiveRequestAdapter adapter = (ActiveRequestAdapter) ((RecyclerView) binding.getRoot().getParent()).getAdapter();
            if (adapter != null) {
                adapter.activeRequestsList.remove(position);
                adapter.notifyItemRemoved(position);
            }
        }
    }
}