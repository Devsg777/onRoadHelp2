package com.example.onroadhelp.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

        public ProviderViewHolder(ItemNearbyServiceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ServiceProvider provider) {
            binding.textProviderName.setText(provider.getName());
            binding.textProviderType.setText(provider.getType());
            binding.textProviderDistance.setText(String.format(Locale.getDefault(), "%.2f km", provider.getDistance()));

            if (provider.isFromApi()) {
                binding.textProviderPhone.setVisibility(View.VISIBLE);
                binding.buttonCall.setVisibility(View.VISIBLE);
                binding.textProviderPhone.setText(provider.getPhone() != null ? provider.getPhone() : "N/A");

                binding.buttonCall.setOnClickListener(v -> {
                    String phone = provider.getPhone();
                    if (phone != null && !phone.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        v.getContext().startActivity(intent);
                    }
                });

                binding.imageProviderIcon.setVisibility(View.VISIBLE); // Optional: show an icon for API sources
            } else {
                binding.textProviderPhone.setVisibility(View.GONE);
                binding.buttonCall.setVisibility(View.GONE);
                binding.imageProviderIcon.setVisibility(View.GONE);
            }
        }
    }
}
