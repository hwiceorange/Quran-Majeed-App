package com.raiadnan.quranreader.compass.helper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class CompassUtils {
    public static String getDirectionString(float f) {
        String str = "NW";
        if (f >= 350.0f || f <= 10.0f) {
            str = "N";
        }
        if (f < 350.0f && f > 280.0f) {
            str = "NW";
        }
        if (f <= 280.0f && f > 260.0f) {
            str = "W";
        }
        if (f <= 260.0f && f > 190.0f) {
            str = "SW";
        }
        if (f <= 190.0f && f > 170.0f) {
            str = "S";
        }
        if (f <= 170.0f && f > 100.0f) {
            str = "SE";
        }
        if (f <= 100.0f && f > 80.0f) {
            str = "E";
        }
        return (f > 80.0f || f <= 10.0f) ? str : "NE";
    }

    public static BitmapDescriptor getBitmapFromVectorDrawable(Context context, int i) {
        Drawable drawable = ContextCompat.getDrawable(context, i);
        if (Build.VERSION.SDK_INT < 21) {
            drawable = DrawableCompat.wrap(drawable).mutate();
        }
        Bitmap createBitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(createBitmap);
    }
}
