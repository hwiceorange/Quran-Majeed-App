package com.quran.quranaudio.online.prayertimes.notifier;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.ui.MainActivity;

import javax.inject.Inject;
import javax.inject.Singleton;

import static android.content.Context.MODE_PRIVATE;


@Singleton
@SuppressWarnings("deprecation")
class PrayerNotification extends BaseNotification {

    private final AdhanPlayer adhanPlayer;

    @Inject
    public PrayerNotification(AdhanPlayer adhanPlayer, PreferencesHelper preferencesHelper, Context context) {
        super(preferencesHelper, context);
        this.adhanPlayer = adhanPlayer;
    }

    public void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = NotifierConstants.ADTHAN_NOTIFICATION_CHANNEL_NAME;
            String description = NotifierConstants.ADTHAN_NOTIFICATION_CHANNEL_DESCRIPTION;
            String id = NotifierConstants.ADTHAN_NOTIFICATION_CHANNEL_ID;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel channel = new NotificationChannel(id, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public void createNotification(Intent intent) {
        int notificationId = intent.getIntExtra("notificationId", 0);
        String prayerTiming = intent.getStringExtra("prayerTiming");
        String prayerKey = intent.getStringExtra("prayerKey");
        String prayerCity = intent.getStringExtra("prayerCity");

        PrayerEnum prayerEnum = null;
        String notificationType = null;
        try {
            if (prayerKey != null) {
                prayerEnum = PrayerEnum.valueOf(prayerKey);
                notificationType = preferencesHelper.getNotificationTypeForPrayer(prayerEnum);
            }
        } catch (IllegalArgumentException e) {
            android.util.Log.e("PrayerNotification", "❌ Invalid prayer key for notification: " + prayerKey, e);
        }

        if (notificationType == null) {
            notificationType = PreferencesHelper.TYPE_AZAN; // fallback to Azan behaviour
        }

        if (PreferencesHelper.TYPE_NONE.equals(notificationType)) {
            android.util.Log.d("PrayerNotification", "⏭️ Notification type 'none' for " + prayerKey + ", skipping notification.");
            NotificationManagerCompat.from(context).cancel(notificationId);
            return;
        }

        String prayerName = context.getResources().getString(
                context.getResources().getIdentifier(prayerKey,
                        "string", context.getPackageName()));

        PendingIntent pendingIntent = getNotificationIntent();

        String closeActionTitle = context.getResources().getString(R.string.adthan_notification_close_action_title);

        String content = prayerName + " : " + prayerTiming;

        if (prayerCity != null) {
            content += " (" + prayerCity + ")";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotifierConstants.ADTHAN_NOTIFICATION_CHANNEL_ID)
                .setColor(getNotificationColor())
                .setContentTitle(context.getString(R.string.adthan_notification_title))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setDeleteIntent(createOnDismissedIntent(notificationId))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            builder
                    .addAction(android.R.drawable.ic_popup_reminder, closeActionTitle, getCloseNotificationActionIntent(notificationId))
                    .setSmallIcon(android.R.drawable.ic_popup_reminder);
        } else {
            builder
                    .addAction(getActionIcon(), closeActionTitle, getCloseNotificationActionIntent(notificationId))
                    .setSmallIcon(getNotificationIcon());
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(notificationId, builder.build());

        // 邦克送达埋点 —— 整套留存埋点里最重要的一条。
        //
        // 礼拜提醒是这个品类每天 5 次的系统级触点，是核心留存杠杆，
        // 但我们从来不知道用户到底有没有真的收到过。
        //
        // 关键细节：NotificationManagerCompat.notify() 在 POST_NOTIFICATIONS 被拒时
        // 是「静默失败」——不抛异常、不返回错误。所以必须同时带上权限状态，
        // 才能区分「调用了但用户没权限（等于没收到）」和「真的送达」。
        // 只有 notifPermitted 为真时，RetentionFunnel 才会把 rf_adhan_ok 置为 true，
        // 保证这个留存分层维度不被污染。
        com.quran.quranaudio.online.analytics.RetentionFunnel.adhanShown(
                context,
                prayerKey,
                notificationType == PreferencesHelper.TYPE_AZAN,
                notificationManager.areNotificationsEnabled());

        boolean shouldPlayAdhan = false;

        if (prayerEnum != null) {
            android.util.Log.d("PrayerNotification", "📳 " + prayerKey + " notification type: " + notificationType);
            switch (notificationType) {
                case PreferencesHelper.TYPE_AZAN:
                    shouldPlayAdhan = true;
                    if (preferencesHelper.isVibrationEnabledForPrayer(prayerEnum)) {
                        createVibration();
                    }
                    break;
                case PreferencesHelper.TYPE_VIBRATE:
                    createVibration();
                    break;
                case PreferencesHelper.TYPE_SILENT:
                    android.util.Log.d("PrayerNotification", "🔕 Silent notification for " + prayerKey);
                    break;
                case PreferencesHelper.TYPE_TEXT_TONE:
                    playTone(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), AudioManager.STREAM_NOTIFICATION, prayerEnum);
                    break;
                case PreferencesHelper.TYPE_CLOCK:
                    Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    if (alarmUri == null) {
                        alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    }
                    playTone(alarmUri, AudioManager.STREAM_ALARM, prayerEnum);
                    break;
                default:
                    android.util.Log.w("PrayerNotification", "⚠️ Unknown notification type " + notificationType + " for " + prayerKey + ", falling back to Adhan.");
                    shouldPlayAdhan = true;
                    if (preferencesHelper.isVibrationActivated()) {
                        createVibration();
                    }
                    break;
            }
        } else {
            android.util.Log.w("PrayerNotification", "⚠️ Unable to resolve PrayerEnum for " + prayerKey + ", falling back to Adhan playback.");
            shouldPlayAdhan = true;
            if (preferencesHelper.isVibrationActivated()) {
                createVibration();
            }
        }

        if (shouldPlayAdhan) {
            setupAdhanCall(prayerEnum, prayerKey);
        } else {
            android.util.Log.d("PrayerNotification", "🔕 Adhan playback skipped for " + prayerKey + " (type=" + notificationType + ")");
        }
    }

    private PendingIntent getNotificationIntent() {
        Intent notificationIntent = new Intent(context, MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntentCreator.getActivity(context, 0,
                notificationIntent, 0);
    }

    private PendingIntent createOnDismissedIntent(int notificationId) {
        Intent intent = new Intent(context, NotificationDismissedReceiver.class);
        intent.setClass(context, NotificationDismissedReceiver.class);
        intent.putExtra("notificationId", notificationId);

        return PendingIntentCreator.getBroadcast(context.getApplicationContext(),
                notificationId, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private void setupAdhanCall(PrayerEnum prayerEnum, String prayerKey) {
        String adhanCallKeyPart = PreferencesConstants.ADTHAN_CALL_ENABLED_KEY;
        String callPreferenceKey = prayerKey + adhanCallKeyPart;

        final SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, MODE_PRIVATE);
        boolean callEnabled = sharedPreferences.getBoolean(callPreferenceKey, false);

        if (callEnabled) {
            if (prayerEnum == null) {
                try {
                    prayerEnum = PrayerEnum.valueOf(prayerKey);
                } catch (IllegalArgumentException e) {
                    android.util.Log.w("PrayerNotification", "⚠️ Unable to parse prayer enum for " + prayerKey + ", defaulting to FAJR", e);
                    prayerEnum = PrayerEnum.FAJR;
                }
            }
            adhanPlayer.playAdhan(prayerEnum);
        }
    }

    private void createVibration() {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = new long[]{0, 1000, 500, 1000, 500, 500, 500};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1),
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build());
        } else {
            vibrator.vibrate(pattern, -1);
        }
    }

    private PendingIntent getCloseNotificationActionIntent(int notificationId) {
        Intent intentAction = new Intent(context, NotifierActionReceiver.class);

        intentAction.setAction(NotifierConstants.ADTHAN_NOTIFICATION_CANCEL_ADHAN_ACTION);
        intentAction.putExtra("notificationId", notificationId);
        intentAction.setClass(context, NotifierActionReceiver.class);

        return PendingIntentCreator.getBroadcast(context, 1, intentAction, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void playTone(Uri toneUri, int streamType, PrayerEnum prayerEnum) {
        if (toneUri == null) {
            android.util.Log.w("PrayerNotification", "⚠️ Tone URI is null, skipping playback");
            return;
        }

        try {
            Ringtone ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), toneUri);
            if (ringtone == null) {
                android.util.Log.w("PrayerNotification", "⚠️ Unable to obtain ringtone for URI: " + toneUri);
                return;
            }

            int volumePercent = prayerEnum != null ? preferencesHelper.getVolumeForPrayer(prayerEnum) : 100;
            float volumeScalar = Math.max(0f, Math.min(1f, volumePercent / 100f));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                int usage = streamType == AudioManager.STREAM_ALARM ? AudioAttributes.USAGE_ALARM : AudioAttributes.USAGE_NOTIFICATION_EVENT;
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                ringtone.setVolume(volumeScalar);
            } else {
                ringtone.setStreamType(streamType);
                android.util.Log.w("PrayerNotification", "⚠️ Cannot adjust ringtone volume on Android versions below 9 (P); using system volume.");
            }

            ringtone.play();
            android.util.Log.d("PrayerNotification", "🔊 Playing tone for " + prayerEnum + " at volume " + volumePercent + "%");
        } catch (Exception e) {
            android.util.Log.e("PrayerNotification", "❌ Failed to play tone for URI: " + toneUri, e);
        }
    }
}
