package com.example.onroadhelp.ui2.profile;

public class HelperEmergencyContact {
    private String name;
    private String phoneNumber;

    // Required empty constructor for Firestore
    public HelperEmergencyContact() {

    }

    public HelperEmergencyContact(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}