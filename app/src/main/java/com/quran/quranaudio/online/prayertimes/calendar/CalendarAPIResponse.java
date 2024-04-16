package com.quran.quranaudio.online.prayertimes.calendar;

import com.quran.quranaudio.online.prayertimes.timings.aladhan.AladhanDate;

import java.util.List;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class CalendarAPIResponse {

    private String code;
    private String status;
    private List<AladhanDate> data;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AladhanDate> getData() {
        return data;
    }

    public void setData(List<AladhanDate> data) {
        this.data = data;
    }
}