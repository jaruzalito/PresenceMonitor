package com.example.pracaobecnosc;

import com.google.gson.annotations.SerializedName;

public class HolidayResponse {
    @SerializedName("date")
    private String date;

    @SerializedName("localName")
    private String localName;

    @SerializedName("name")
    private String name;

    @SerializedName("countryCode")
    private String countryCode;

    @SerializedName("fixed")
    private boolean fixed;

    // Gettery
    public String getDate() {
        return date;
    }

    public String getLocalName() {
        return localName;
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public boolean isFixed() {
        return fixed;
    }
}