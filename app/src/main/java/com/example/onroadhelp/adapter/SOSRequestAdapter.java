package com.example.onroadhelp.adapter;

import android.util.Log;
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
        holder.bind(currentRequest);
    }

    @Override
    public int getItemCount() {
        return sosRequestList.size();
    }

    public class SOSRequestViewHolder extends RecyclerView.ViewHolder {
        TextView textProblem;
        TextView textLocation;
        TextView textTimestamp;
        TextView textStatus;
        TextView textVehicleType; // Added Vehicle Type
        TextView textVehicleNo; // Added Vehicle Model
        Button buttonAccept;
        Button buttonComplete; // Added Complete button

        public SOSRequestViewHolder(@NonNull View itemView) {
            super(itemView);
            textProblem = itemView.findViewById(R.id.text_problem);
            textLocation = itemView.findViewById(R.id.text_location);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textStatus = itemView.findViewById(R.id.text_status);
            buttonAccept = itemView.findViewById(R.id.button_accept);
            buttonComplete = itemView.findViewById(R.id.button_complete);// Initialize Complete button
            textVehicleType = itemView.findViewById(R.id.vehicle_type); // Initialize Vehicle Type
            textVehicleNo = itemView.findViewById(R.id.vehicle_number); // Initialize Vehicle Model
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(sosRequestList.get(position));
                }
            });

            buttonAccept.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    SOSRequest request = sosRequestList.get(position);
                    acceptSOSRequest(request.getRequestId(), position);
                }
            });

            buttonComplete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    SOSRequest request = sosRequestList.get(position);
                    buttonComplete.setEnabled(false); // Disable the button on click
                    completeSOSRequest(request.getRequestId(), position);
                }
            });
        }

        public void bind(SOSRequest request) {
            textProblem.setText(request.getProblem());
            textStatus.setText("Status: " + request.getStatus());

            if (request.getLocation() != null) {
                double latitude = request.getLocation().getLatitude();
                double longitude = request.getLocation().getLongitude();
                Log.d("SOSAdapter", "Latitude: " + latitude + ", Longitude: " + longitude);
                String geoUri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(Customer Location)", latitude, longitude, latitude, longitude);
                textLocation.setText(geoUri);
            } else {
                textLocation.setText("Location not available");
            }
            textVehicleType.setText("Vehicle Type: " + request.getVehicleType()); // Set Vehicle Type
            textVehicleNo.setText("Vehicle No: " + request.getVehicleNo()); // Set Vehicle Model

            // Set the vehicle type and number
            if (request.getVehicleType() != null) {
                textVehicleType.setText("Vehicle Type: " + request.getVehicleType());
            } else {
                textVehicleType.setText("Vehicle Type: N/A");
            }
            if (request.getVehicleNo() != null) {
                textVehicleNo.setText("Vehicle No: " + request.getVehicleNo());
            } else {
                textVehicleNo.setText("Vehicle No: N/A");
            }

            // Format the timestamp
            if (request.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                textTimestamp.setText("Timestamp: " + sdf.format(request.getTimestamp()));
            } else {
                textTimestamp.setText("Timestamp: N/A");
            }

            if (request.getStatus().equals("pending_acceptance")) {
                buttonAccept.setVisibility(View.VISIBLE);
                buttonComplete.setVisibility(View.GONE);
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
            } else if (request.getStatus().equals("accepted") && request.getAcceptedHelperId() != null && request.getAcceptedHelperId().equals(helperId)) {
                buttonAccept.setVisibility(View.GONE);
                buttonComplete.setVisibility(View.VISIBLE);
                itemView.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(request);
                    }
                });
                itemView.setClickable(true);
            } else {
                buttonAccept.setVisibility(View.GONE);
                buttonComplete.setVisibility(View.GONE);
                itemView.setOnClickListener(null);
                itemView.setClickable(false);
            }
            buttonComplete.setEnabled(true); // Re-enable when the item is rebound (for scrolling)
        }

        private void acceptSOSRequest(String requestId, int position) {
            db.collection("sos_requests").document(requestId)
                    .update("status", "accepted", "acceptedHelperId", helperId)
                    .addOnSuccessListener(aVoid -> {
                        if (position >= 0 && position < sosRequestList.size()) {
                            SOSRequest updatedRequest = sosRequestList.get(position);
                            updatedRequest.setStatus("accepted");
                            updatedRequest.setAcceptedHelperId(helperId);
                            notifyItemChanged(position);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle the error
                    });
        }

        private void completeSOSRequest(String requestId, int position) {
            db.collection("sos_requests").document(requestId)
                    .update("status", "completed")
                    .addOnSuccessListener(aVoid -> {
                        // Defensive check
                        if (position >= 0 && position < sosRequestList.size()) {
                            sosRequestList.remove(position);
                            notifyItemRemoved(position);
                        } else {
                            Log.e("SOSAdapter", "Invalid position for removal: " + position + ", list size: " + sosRequestList.size());
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle the error
                        if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                            buttonComplete.setEnabled(true); // Re-enable on failure
                        }
                    });
        }
    }
}