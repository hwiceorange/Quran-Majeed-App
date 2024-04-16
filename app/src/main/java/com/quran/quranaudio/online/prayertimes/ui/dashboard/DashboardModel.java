package com.quran.quranaudio.online.prayertimes.ui.dashboard;

import android.content.Intent;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class DashboardModel {
    private String head, sub;
    private int image;
    private Intent intent;

    public DashboardModel() {
    }

    public DashboardModel(String head, String sub, int image) {
        this.head = head;
        this.sub = sub;
        this.image = image;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }
}
