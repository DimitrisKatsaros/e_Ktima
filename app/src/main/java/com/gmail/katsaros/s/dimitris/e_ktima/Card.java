package com.gmail.katsaros.s.dimitris.e_ktima;

public class Card {
    private String title;
    private boolean checkbox = false;

    public Card(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCheckbox() {
        return checkbox;
    }

    public void setCheckbox(boolean checkbox) {
        this.checkbox = checkbox;
    }
}
