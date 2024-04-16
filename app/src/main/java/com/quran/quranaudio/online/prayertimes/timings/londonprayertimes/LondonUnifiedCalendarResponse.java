package com.quran.quranaudio.online.prayertimes.timings.londonprayertimes;

import java.util.Map;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class LondonUnifiedCalendarResponse {

    private Map<String, LondonUnifiedTimingsResponse> times;

    public void setTimes(Map<String, LondonUnifiedTimingsResponse> times) {
        this.times = times;
    }

    public Map<String, LondonUnifiedTimingsResponse> getTimes() {
        return times;
    }
}