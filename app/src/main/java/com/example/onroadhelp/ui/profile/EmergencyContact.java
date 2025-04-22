package com.example.onroadhelp.ui.profile; // Adjust the package name

public class EmergencyContact {
    private String name;
    private String phoneNumber;
    private String documentId; // To store the Firestore document ID

    public EmergencyContact() {
        // Default constructor required for Firestore
    }

    public EmergencyContact(String name, String phoneNumber) {
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

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}