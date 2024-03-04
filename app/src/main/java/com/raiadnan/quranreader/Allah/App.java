package com.raiadnan.quranreader.Allah;

import android.app.Application;
import android.graphics.Typeface;


public class App extends Application {
    private static App app;
    public Typeface faceArabic;
    public Typeface faceRobotoB;
    public Typeface faceRobotoL;
    public Typeface faceRobotoR;

    public interface SimpleCallback {
        void callback(Object obj);
    }

    public static App get() {
        return app;
    }

    public void onCreate() {
        super.onCreate();
        app = this;

        this.faceRobotoL = Typeface.createFromAsset(getAssets(), "Roboto_Light.ttf");
        this.faceRobotoB = Typeface.createFromAsset(getAssets(), "Roboto_Bold.ttf");
        this.faceRobotoR = Typeface.createFromAsset(getAssets(), "Roboto_Regular.ttf");
        this.faceArabic = Typeface.createFromAsset(getAssets(), "XBZarIndoPak.ttf");

       // new FlurryAgent.Builder().withLogEnabled(true).build(this, "5YXS3G3CMRB4SJ5JW3YQ");
    }


}
