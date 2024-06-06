package com.quran.quranaudio.online.prayertimes.location.photon;

import com.google.gson.annotations.SerializedName;


public class FeatureProperties {

    private String country;

    @SerializedName("countrycode")
    private String countryCode;

    @SerializedName("osm_key")
    private String osmKey;

    @SerializedName("osm_value")
    private String osmValue;

    private String name;

    private String state;

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getOsmKey() {
        return osmKey;
    }

    public void setOsmKey(String osmKey) {
        this.osmKey = osmKey;
    }

    public String getOsmValue() {
        return osmValue;
    }

    public void setOsmValue(String osmValue) {
        this.osmValue = osmValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}
