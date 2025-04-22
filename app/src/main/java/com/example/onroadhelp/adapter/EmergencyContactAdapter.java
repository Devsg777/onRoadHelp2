package com.example.onroadhelp.adapter; // Adjust the package name

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onroadhelp.R; // Import your R class
import com.example.onroadhelp.ui.profile.EmergencyContact; // Import the model class

import java.util.List;

public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    private List<EmergencyContact> contactList;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EmergencyContact contact);
        void onDeleteClick(EmergencyContact contact);
    }

    public EmergencyContactAdapter(List<EmergencyContact> contactList, OnItemClickListener listener) {
        this.contactList = contactList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency_contact, parent, false); // Create this layout file
        return new ContactViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact currentContact = contactList.get(position);
        holder.textName.setText(currentContact.getName());
        holder.textPhone.setText(currentContact.getPhoneNumber());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentContact);
            }
        });

        holder.buttonDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(currentContact);
            }
        });
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        public TextView textName;
        public TextView textPhone;
        public ImageView buttonDelete; // Assuming you have a delete button/icon in your item layout

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_contact_name); // Replace with your actual ID
            textPhone = itemView.findViewById(R.id.text_contact_phone); // Replace with your actual ID
            buttonDelete = itemView.findViewById(R.id.button_delete_contact); // Replace with your actual ID
        }
    }
}