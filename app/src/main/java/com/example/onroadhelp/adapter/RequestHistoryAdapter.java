package com.example.onroadhelp.adapter; // Replace with your actual package name

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.onroadhelp.databinding.ItemHistoryRequestBinding; // Import your binding class
import com.example.onroadhelp.model.SOSRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RequestHistoryAdapter extends RecyclerView.Adapter<RequestHistoryAdapter.RequestHistoryViewHolder> {

    private final List<SOSRequest> requestHistoryList;

    public RequestHistoryAdapter(List<SOSRequest> requestHistoryList) {
        this.requestHistoryList = requestHistoryList;
    }

    @NonNull
    @Override
    public RequestHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryRequestBinding binding = ItemHistoryRequestBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RequestHistoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestHistoryViewHolder holder, int position) {
        SOSRequest currentRequest = requestHistoryList.get(position);
        holder.bind(currentRequest);
    }

    @Override
    public int getItemCount() {
        return requestHistoryList.size();
    }

    public static class RequestHistoryViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryRequestBinding binding;

        public RequestHistoryViewHolder(ItemHistoryRequestBinding binding) {
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

            // You can add more UI updates here based on other fields in your SOSRequest model
            // For example, if you have the helper's name for resolved requests:
            // if (request.getStatus().equals("resolved") && request.getHelperName() != null) {
            //     binding.textViewHelperName.setText("Assisted by: " + request.getHelperName());
            //     binding.textViewHelperName.setVisibility(View.VISIBLE);
            // } else {
            //     binding.textViewHelperName.setVisibility(View.GONE);
            // }
        }
    }
}