package com.abhi245y.driverwmb;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.GeoPoint;

public class BusList {

    private GeoPoint bus_location;

    private BusList(){
    }

    public BusList(GeoPoint bus_location) {
        this.bus_location = bus_location;
    }

    public GeoPoint getBus_location() {
        return bus_location;
    }

    public void setBus_location(GeoPoint bus_location) {
        this.bus_location = bus_location;
    }
}

