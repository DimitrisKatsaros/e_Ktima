package com.gmail.katsaros.s.dimitris.e_ktima;

import com.google.android.gms.maps.model.LatLng;

public class MarkerInfo {

    private LatLng latLng;
    private String index;

    public MarkerInfo(LatLng latLng, String index){
        this.latLng = latLng;
        this.index = index;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
}
