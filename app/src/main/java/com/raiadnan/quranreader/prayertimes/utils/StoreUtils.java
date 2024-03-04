package com.raiadnan.quranreader.prayertimes.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class StoreUtils {
    private static final String MARKET_DETAILS_ID = "market://details?id=";
    private static final String PLAY_STORE_LINK = "https://play.google.com/store/apps/details?id=";

    public static void goToStore(Context context, String str) {
        try {
            context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(MARKET_DETAILS_ID + str)));
        } catch (ActivityNotFoundException unused) {
            context.startActivity(new Intent("android.intent.action.VIEW", Uri.parse(PLAY_STORE_LINK + str)));
        }
    }

    public static void ShareApp(Context context, String str) {
        Intent intent = new Intent();
        intent.setAction("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.TEXT", PLAY_STORE_LINK + str);
        intent.setType("text/plain");
        context.startActivity(intent);
    }

    public static void Feedback(Context context, String str) {
        Intent intent = new Intent("android.intent.action.SEND");
        intent.putExtra("android.intent.extra.EMAIL", new String[]{str});
        intent.setType("text/plain");
        intent.setType("message/rfc822");
        intent.putExtra("android.intent.extra.SUBJECT", "[Feedback] 2131755035");
        intent.putExtra("android.intent.extra.TEXT", "");
        context.startActivity(Intent.createChooser(intent, null));
    }
}
