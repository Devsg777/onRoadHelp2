package com.example.onroadhelp.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onroadhelp.R;
import com.example.onroadhelp.ViewProfileActivity; // Import the new activity
import com.example.onroadhelp.databinding.ItemNearbyServiceBinding;
import com.example.onroadhelp.model.ServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NearbyServiceAdapter extends RecyclerView.Adapter<NearbyServiceAdapter.ProviderViewHolder> {

    private List<ServiceProvider> providerList = new ArrayList<>();

    public void setProviders(List<ServiceProvider> providers) {
        this.providerList = providers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNearbyServiceBinding binding = ItemNearbyServiceBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProviderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        ServiceProvider provider = providerList.get(position);
        holder.bind(provider);
    }

    @Override
    public int getItemCount() {
        return providerList.size();
    }

    static class ProviderViewHolder extends RecyclerView.ViewHolder {

        private final ItemNearbyServiceBinding binding;
        private final ImageView providerIconImageView;

        public ProviderViewHolder(ItemNearbyServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.providerIconImageView = binding.imageProviderIcon;

            // Set OnClickListener for the item view to open the profile
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ServiceProvider provider = ((NearbyServiceAdapter) getBindingAdapter()).providerList.get(position);
                    if (!provider.isFromApi()) { // Only for registered providers
                        Intent intent = new Intent(v.getContext(), ViewProfileActivity.class);
                        // Assuming your ServiceProvider model for registered providers has an 'id' field
                        // You'll need to fetch this ID in your HomeFragment when retrieving data
                        // and store it in the ServiceProvider object.
                        // For now, let's assume you add a 'providerId' field.
                        // intent.putExtra("providerId", provider.getProviderId());
                        // Replace the above line with how you access the registered provider's ID

                        // Temporary way to pass a unique identifier (e.g., name)
                        intent.putExtra("providerId", provider.getEmail());
                        v.getContext().startActivity(intent);
                    }
                }
            });
        }

        public void bind(ServiceProvider provider) {
            binding.textProviderName.setText(provider.getName());
            binding.textProviderType.setText(provider.getType());
            binding.textProviderDistance.setText(String.format(Locale.getDefault(), "%.2f km", provider.getDistance()));

            if (provider.getPhone() != null && !provider.getPhone().isEmpty()) {
                binding.textProviderPhone.setVisibility(View.VISIBLE);
                binding.buttonCall.setVisibility(View.VISIBLE);
                binding.textProviderPhone.setText(provider.getPhone());

                binding.buttonCall.setOnClickListener(v -> {
                    String phone = provider.getPhone();
                    if (phone != null && !phone.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        v.getContext().startActivity(intent);
                    }
                });

                providerIconImageView.setVisibility(View.VISIBLE);
                if (provider.isFromApi()) {
                    providerIconImageView.setImageResource(R.drawable.icons8_google);
                } else {
                    providerIconImageView.setImageResource(R.drawable.breakdown);
                }

            } else {
                binding.textProviderPhone.setVisibility(View.GONE);
                binding.buttonCall.setVisibility(View.GONE);
                providerIconImageView.setVisibility(View.GONE);
            }
        }
    }
}