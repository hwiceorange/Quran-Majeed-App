package com.quran.quranaudio.online.prayertimes.timings.aladhan;

import java.util.List;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class AladhanCalendarResponse {

    private String code;
    private String status;
    private List<AladhanData> data;

    public String getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<AladhanData> getData() {
        return data;
    }

    public void setData(List<AladhanData> data) {
        this.data = data;
    }
}