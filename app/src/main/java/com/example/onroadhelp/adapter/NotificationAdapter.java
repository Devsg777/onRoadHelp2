package com.example.onroadhelp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.onroadhelp.R;
import com.example.onroadhelp.model.Notification;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notificationList;

    public NotificationAdapter(List<Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification currentNotification = notificationList.get(position);
        holder.textTitle.setText(currentNotification.getTitle());
        holder.textBody.setText(currentNotification.getBody());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        holder.textTimestamp.setText(sdf.format(currentNotification.getTimestamp()));

        // Optional: You can add logic here to change the appearance of read/unread notifications
        /*
        if (currentNotification.isRead()) {
            holder.itemView.setAlpha(0.7f); // Example: Slightly fade read notifications
        } else {
            holder.itemView.setAlpha(1.0f);
        }
        */
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textBody;
        TextView textTimestamp;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_notification_title);
            textBody = itemView.findViewById(R.id.text_notification_body);
            textTimestamp = itemView.findViewById(R.id.text_notification_timestamp);
        }
    }
}