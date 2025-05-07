package com.example.onroadhelp.model;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

public class ServiceProvider {
    private String name;
    private String email;
    private String type;
    private String phone;
    private double distance;
    private boolean fromApi;
    private LatLng latLng;

    public ServiceProvider() {
        // Default constructor
    }

    public ServiceProvider(String name,String email, double distance, String type, String phone, boolean fromApi) {
        this.name = name;
        this.type = type;
        this.phone = phone;
        this.distance = distance;
        this.fromApi = fromApi;
        this.email = email;
    }

    public static ServiceProvider fromPlacesApi(JSONObject obj, Location userLocation) {
        ServiceProvider p = new ServiceProvider();
        JSONObject loc = obj.optJSONObject("geometry").optJSONObject("location");

        p.name = obj.optString("name");
        if (obj.has("types") && obj.optJSONArray("types").length() > 0) {
            p.type = obj.optJSONArray("types").optString(0);
        } else {
            p.type = "Other"; // Default type if not available
        }
        p.phone = null; // Phone number will be fetched in Place Details
        p.latLng = new LatLng(loc.optDouble("lat"), loc.optDouble("lng"));
        p.distance = distanceBetween(userLocation, p.latLng);
        p.fromApi = true;
        return p;
    }

    private static double distanceBetween(Location user, LatLng point) {
        Location loc = new Location("");
        loc.setLatitude(point.latitude);
        loc.setLongitude(point.longitude);
        return user.distanceTo(loc) / 1000.0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public boolean isFromApi() {
        return fromApi;
    }

    public void setFromApi(boolean fromApi) {
        this.fromApi = fromApi;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }
}