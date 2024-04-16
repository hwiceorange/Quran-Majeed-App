package com.quran.quranaudio.online.prayertimes.timings.aladhan;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class AladhanTodayTimingsResponse {

    private String code;
    private String status;
    private AladhanData data;

    public String getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public AladhanData getData() {
        return data;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setData(AladhanData data) {
        this.data = data;
    }
}