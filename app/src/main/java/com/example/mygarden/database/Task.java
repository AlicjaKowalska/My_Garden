package com.example.mygarden.database;

import android.graphics.Bitmap;

public class Task {
    private String activity;
    private String name;
    private String localization;
    private Bitmap image;

    public Task(){};

    public Task(String activity, String name, String localization, Bitmap image)
    {
        this.activity = activity;
        this.name = name;
        this.localization = localization;
        this.image = image;
    }

    public String getActivity() { return activity; }
    public void setActivity(String activity) { this.activity = activity; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocalization() { return localization; }
    public void setLocalization(String localization) { this.localization = localization; }

    public Bitmap getImage() { return image; }
    public void setImage(Bitmap image) { this.image = image; }
}