package com.example.onroadhelp.model;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

public class ServiceProvider {
    private String name;
    private String type;
    private String phone;
    private double distance;
    private boolean fromApi;
    private LatLng latLng;

    // Getters, setters, constructors omitted for brevity

    public static ServiceProvider fromPlacesApi(JSONObject obj, Location userLocation) {
        ServiceProvider p = new ServiceProvider();
        JSONObject loc = obj.optJSONObject("geometry").optJSONObject("location");

        p.name = obj.optString("name");
        p.type = obj.optJSONArray("types").optString(0);
        p.phone = "N/A";
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

    public boolean isFromApi() {
        return fromApi;
    }
}
