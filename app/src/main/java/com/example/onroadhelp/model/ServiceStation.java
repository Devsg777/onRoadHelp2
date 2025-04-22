package com.example.onroadhelp.model;

public class ServiceStation {
    private String name;
    private String address;
    private String contactNumber;
    private double latitude;  // For map display
    private double longitude; // For map display
    private String type; // e.g., "Mechanic", "Fuel Station", "Towing"
    // Add other relevant fields as needed (e.g., openingHours, servicesOffered)

    public ServiceStation() {
        // Required empty constructor for Firestore
    }

    public ServiceStation(String name, String address, String contactNumber, double latitude, double longitude, String type) {
        this.name = name;
        this.address = address;
        this.contactNumber = contactNumber;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    // Add getters and setters for other fields if you have them
}