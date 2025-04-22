package com.example.onroadhelp.model;

import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Notification {
    private String notificationId;
    private String title;
    private String body;
    @ServerTimestamp
    private Date timestamp;
    private boolean isRead; // Optional: To track if the notification has been read

    // Required empty constructor for Firestore
    public Notification() {}

    public Notification(String title, String body, Date timestamp, boolean isRead) {
        this.title = title;
        this.body = body;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    // Add setters if needed
}