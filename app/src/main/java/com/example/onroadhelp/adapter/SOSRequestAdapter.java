package com.example.onroadhelp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.onroadhelp.R;
import com.example.onroadhelp.model.SOSRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class SOSRequestAdapter extends RecyclerView.Adapter<SOSRequestAdapter.SOSRequestViewHolder> {

    private List<SOSRequest> sosRequestList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String helperId; // To identify the current helper
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(SOSRequest sosRequest);
    }

    public SOSRequestAdapter(List<SOSRequest> sosRequestList, String helperId) {
        this.sosRequestList = sosRequestList;
        this.helperId = helperId;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SOSRequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sos_request, parent, false);
        return new SOSRequestViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull SOSRequestViewHolder holder, int position) {
        SOSRequest currentRequest = sosRequestList.get(position);
        holder.textProblem.setText(currentRequest.getProblem());
        holder.textLocation.setText(String.format(Locale.getDefault(), "Latitude: %.2f, Longitude: %.2f",
                currentRequest.getLocation().getLatitude(), currentRequest.getLocation().getLongitude()));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        holder.textTimestamp.setText("Timestamp: " + sdf.format(currentRequest.getTimestamp()));
        holder.textStatus.setText("Status: " + currentRequest.getStatus());

        if (currentRequest.getStatus().equals("pending")) {
            holder.buttonAccept.setVisibility(View.VISIBLE);
            holder.buttonComplete.setVisibility(View.GONE);
            holder.buttonAccept.setOnClickListener(v -> {
                acceptSOSRequest(currentRequest.getRequestId(), position);
            });
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        } else if (currentRequest.getStatus().equals("accepted") && currentRequest.getAcceptedHelperId() != null && currentRequest.getAcceptedHelperId().equals(helperId)) {
            holder.buttonAccept.setVisibility(View.GONE);
            holder.buttonComplete.setVisibility(View.VISIBLE);
            holder.buttonComplete.setOnClickListener(v -> {
                completeSOSRequest(currentRequest.getRequestId(), position);
            });
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(currentRequest);
                }
            });
            holder.itemView.setClickable(true);
        } else {
            holder.buttonAccept.setVisibility(View.GONE);
            holder.buttonComplete.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(null);
            holder.itemView.setClickable(false);
        }
    }

    private void acceptSOSRequest(String requestId, int position) {
        db.collection("sos_requests").document(requestId)
                .update("status", "accepted", "acceptedHelperId", helperId)
                .addOnSuccessListener(aVoid -> {
                    SOSRequest updatedRequest = sosRequestList.get(position);
                    updatedRequest.setStatus("accepted");
                    updatedRequest.setAcceptedHelperId(helperId);
                    notifyItemChanged(position);
                })
                .addOnFailureListener(e -> {
                    // Handle the error
                });
    }

    private void completeSOSRequest(String requestId, int position) {
        db.collection("sos_requests").document(requestId)
                .update("status", "completed")
                .addOnSuccessListener(aVoid -> {
                    // Update the local list and notify the adapter
                    sosRequestList.remove(position);
                    notifyItemRemoved(position);
                    // Optionally show a confirmation message
                })
                .addOnFailureListener(e -> {
                    // Handle the error
                });
    }

    @Override
    public int getItemCount() {
        return sosRequestList.size();
    }

    public static class SOSRequestViewHolder extends RecyclerView.ViewHolder {
        TextView textProblem;
        TextView textLocation;
        TextView textTimestamp;
        TextView textStatus;
        Button buttonAccept;
        Button buttonComplete; // Added Complete button

        public SOSRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            textProblem = itemView.findViewById(R.id.text_problem);
            textLocation = itemView.findViewById(R.id.text_location);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textStatus = itemView.findViewById(R.id.text_status);
            buttonAccept = itemView.findViewById(R.id.button_accept);
            buttonComplete = itemView.findViewById(R.id.button_complete); // Initialize Complete button
        }
    }
}