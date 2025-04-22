package com.example.onroadhelp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onroadhelp.R; // Import your project's R class
import com.example.onroadhelp.model.ServiceStation; // Import the ServiceStation model
import java.util.List;

public class RegisteredStationAdapter extends RecyclerView.Adapter<RegisteredStationAdapter.StationViewHolder> {

    private List<ServiceStation> stationList;

    public RegisteredStationAdapter(List<ServiceStation> stationList) {
        this.stationList = stationList;
    }

    @NonNull
    @Override
    public StationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_registered_station, parent, false); // Create this layout
        return new StationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull StationViewHolder holder, int position) {
        ServiceStation station = stationList.get(position);
        holder.nameTextView.setText(station.getName());
        holder.addressTextView.setText(station.getAddress());
        holder.contactNumberTextView.setText(station.getContactNumber());
        holder.typeTextView.setText(station.getType());
        // Bind other data to views in the ViewHolder
    }

    @Override
    public int getItemCount() {
        return stationList.size();
    }

    public static class StationViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public TextView addressTextView;
        public TextView contactNumberTextView;
        public TextView typeTextView;

        public StationViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.text_station_name);        // Find views in item_registered_station.xml
            addressTextView = itemView.findViewById(R.id.text_station_address);
            contactNumberTextView = itemView.findViewById(R.id.text_station_contact);
            typeTextView = itemView.findViewById(R.id.text_station_type);
            // Find other views
        }
    }
}