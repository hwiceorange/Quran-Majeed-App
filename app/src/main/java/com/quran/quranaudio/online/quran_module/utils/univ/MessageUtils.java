package com.quran.quranaudio.online.quran_module.utils.univ;

import android.content.Context;
import android.widget.Toast;

import com.peacedesign.android.widget.dialog.base.PeaceDialog;
import com.quran.quranaudio.online.R;

import java.lang.ref.WeakReference;

public class MessageUtils {
    private static WeakReference<Toast> mToast;

    public static void showRemovableToast(Context context, int msgRes, int duration) {
        showRemovableToast(context, context.getString(msgRes), duration);
    }

    public static void showRemovableToast(Context context, CharSequence msg, int duration) {
        try {
            mToast.get().cancel();
        } catch (Exception ignored) {}

        mToast = new WeakReference<>(Toast.makeText(context, msg, duration));
        mToast.get().show();
    }

    public static void popNoInternetMessage(Context ctx, boolean cancelable, Runnable runOnDismiss) {
        // 🎯 Firebase Analytics: 记录网络错误弹窗（可能导致用户流失）
        try {
            com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(ctx)
                .logUIException(ctx.getString(R.string.strTitleNoInternet), 
                    ctx.getString(R.string.strMsgNoInternetLong), "no_internet_dialog");
        } catch (Exception e) {
            android.util.Log.e("MessageUtils", "Analytics logging failed: " + e.getMessage());
        }
        
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(ctx);
        builder.setTitle(R.string.strTitleNoInternet);
        builder.setMessage(R.string.strMsgNoInternetLong);
        builder.setNeutralButton(R.string.strLabelClose, null);
        if (runOnDismiss != null) {
            builder.setOnDismissListener(dialog -> runOnDismiss.run());
        }
        builder.setCancelable(cancelable);
        builder.setFocusOnNeutral(true);
        builder.show();
    }

    public static void popMessage(Context context, String title, String msg, String btn, Runnable action) {
        PeaceDialog.Builder builder = PeaceDialog.newBuilder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setNeutralButton(btn, (dialog, which) -> {
            if (action != null) {
                action.run();
            }
        });
        builder.setFocusOnNeutral(true);
        builder.show();
    }
}
