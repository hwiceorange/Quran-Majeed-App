package com.quran.quranaudio.online.prayertimes.location.osm;

import com.google.gson.annotations.SerializedName;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class NominatimReverseGeocodeResponse {

    @SerializedName("place_id")
    private long placeId;

    @SerializedName("osm_type")
    private String osmType;

    @SerializedName("osm_id")
    private long osmId;

    private double lat;
    private double lon;
    private NominatimAddress address;

    public long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(long placeId) {
        this.placeId = placeId;
    }

    public String getOsmType() {
        return osmType;
    }

    public void setOsmType(String osmType) {
        this.osmType = osmType;
    }

    public long getOsmId() {
        return osmId;
    }

    public void setOsmId(long osmId) {
        this.osmId = osmId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public NominatimAddress getAddress() {
        return address;
    }

    public void setAddress(NominatimAddress address) {
        this.address = address;
    }
}
