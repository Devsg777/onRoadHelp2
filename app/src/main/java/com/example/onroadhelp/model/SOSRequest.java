package com.example.onroadhelp.model;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class SOSRequest {
    private String requestId;
    private String customerId;
    private String problem;
    private GeoPoint location;
    @ServerTimestamp
    private Date timestamp;
    private String status;
    private String acceptedHelperId; // Add this field

    // Default constructor (required by Firestore)
    public SOSRequest() {
    }

    public SOSRequest(String customerId, String problem, GeoPoint location, String status) {
        this.customerId = customerId;
        this.problem = problem;
        this.location = location;
        this.status = status;
        this.acceptedHelperId = null; // Initialize as null
    }

    // Getter for requestId
    public String getRequestId() {
        return requestId;
    }

    // Setter for requestId (if needed)
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    // Getter for customerId
    public String getCustomerId() {
        return customerId;
    }

    // Setter for customerId (if needed)
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    // Getter for problem
    public String getProblem() {
        return problem;
    }

    // Setter for problem (if needed)
    public void setProblem(String problem) {
        this.problem = problem;
    }

    // Getter for location
    public GeoPoint getLocation() {
        return location;
    }

    // Setter for location (if needed)
    public void setLocation(GeoPoint location) {
        this.location = location;
    }

    // Getter for timestamp
    public Date getTimestamp() {
        return timestamp;
    }

    // Setter for timestamp (if needed)
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    // Getter for status
    public String getStatus() {
        return status;
    }

    // Setter for status (if needed)
    public void setStatus(String status) {
        this.status = status;
    }

    // Getter for acceptedHelperId (NEW METHOD)
    public String getAcceptedHelperId() {
        return acceptedHelperId;
    }

    // Setter for acceptedHelperId (if needed)
    public void setAcceptedHelperId(String acceptedHelperId) {
        this.acceptedHelperId = acceptedHelperId;
    }
}