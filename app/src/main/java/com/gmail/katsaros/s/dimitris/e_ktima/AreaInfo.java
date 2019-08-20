package com.gmail.katsaros.s.dimitris.e_ktima;

import java.util.ArrayList;

public class AreaInfo {

    private String title;
    private String id;
    private ArrayList<MarkerInfo> markersList;

    public AreaInfo(String title, String id, ArrayList<MarkerInfo> markersList) {
        this.title = title;
        this.id = id;
        this.markersList = markersList;
    }

    public AreaInfo(ArrayList<MarkerInfo> markersList) {
        this.markersList = markersList;
    }

    public AreaInfo() {
        this.markersList = markersList;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ArrayList<MarkerInfo> getMarkersList() {
        return markersList;
    }

    public void setMarkersList(ArrayList<MarkerInfo> markersList) {
        this.markersList = markersList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
