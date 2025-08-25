package com.quran.quranaudio.online.ads.data;

public class Constant {

    public static final String AD_STATUS = "1"; // 广告开启状态

    public static String AD_NETWORK = "admob";
    public static final String BACKUP_AD_NETWORK = "fan";

    public static final String ADMOB_BANNER_ID = "ca-app-pub-3966802724737141/1386840185"; // "ca-app-pub-3940256099942544/6300978111";
    public static final String ADMOB_INTERSTITIAL_ID = "ca-app-pub-3966802724737141/2182661506";// "ca-app-pub-3940256099942544/1033173712";
    public static final String ADMOB_NATIVE_ID ="ca-app-pub-3966802724737141/1300824672";// "ca-app-pub-3940256099942544/2247696110";
    public static final String ADMOB_APP_OPEN_AD_ID = "ca-app-pub-3966802724737141/3298687654"; //"ca-app-pub-3940256099942544/3419835294";


    public static final String GOOGLE_AD_MANAGER_BANNER_ID = "/69/exle/banner";
    public static final String GOOGLE_AD_MANAGER_INTERSTITIAL_ID = "/699/exa/inttitial";
    public static final String GOOGLE_AD_MANAGER_NATIVE_ID = "/64/example/native";
    public static final String GOOGLE_AD_MANAGER_APP_OPEN_AD_ID = "/69/exple/appopen";

    public static final String FAN_BANNER_ID = "154641400992658_154641934325938";
    public static final String FAN_INTERSTITIAL_ID = "154641400992658_154641894325942";
    public static final String FAN_NATIVE_ID = "154641400992658_154642014325930";


    public static final String STARTAPP_APP_ID = "0";

    public static final String UNITY_GAME_ID = "4089993";
    public static final String UNITY_BANNER_ID = "banner";
    public static final String UNITY_INTERSTITIAL_ID = "video";

    public static final String APPLOVIN_BANNER_ID = "da17eff31ae69f15";
    public static final String APPLOVIN_INTERSTITIAL_ID = "98f6a586ed642919";
    public static final String APPLOVIN_NATIVE_MANUAL_ID = "87343269587e8998";
    public static final String APPLOVIN_APP_OPEN_AP_ID = "de9f381d132b859a";

    public static final String APPLOVIN_BANNER_ZONE_ID = "afb7122672e86340";
    public static final String APPLOVIN_BANNER_MREC_ZONE_ID = "81287b697d935c32";
    public static final String APPLOVIN_INTERSTITIAL_ZONE_ID = "b6eba8b976279ea5";


    public static String NATIVE_STYLE = "default";
    public static final String STYLE_NEWS = "news";
    public static final String STYLE_RADIO = "radio";
    public static final String STYLE_VIDEO_SMALL = "video_small";
    public static final String STYLE_VIDEO_LARGE = "video_large";
    public static final String STYLE_STREAM = "stream";
    public static final String STYLE_DEFAULT = "default";

    public static boolean isAppOpen = false;

    public static final boolean FORCE_TO_SHOW_APP_OPEN_AD_ON_START = true; // 保留启动时开屏广告
    public static final boolean OPEN_ADS_ON_START = true; // 保留开屏广告
    public static final boolean OPEN_ADS_ON_RESUME = true; // 保留恢复时开屏广告

    public static final int INTERSTITIAL_AD_INTERVAL = 999999; // 禁用插页式广告通过设置极大间隔
    public static final int NATIVE_AD_INDEX = 2;
    public static final int NATIVE_AD_INTERVAL = 8;

}