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
import androidx.preference.PreferenceManager;

import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.common.TimingType;
import com.quran.quranaudio.online.prayertimes.ui.MainActivity;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
@SuppressWarnings("deprecation")
class ReminderNotification extends BaseNotification {

    private final ReminderPlayer reminderPlayer;

    @Inject
    public ReminderNotification(ReminderPlayer reminderPlayer, PreferencesHelper preferencesHelper, Context context) {
        super(preferencesHelper, context);
        this.reminderPlayer = reminderPlayer;
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
        String notificationTitle;

        String prayerType = intent.getStringExtra("prayerType");
        int notificationId = intent.getIntExtra("notificationId", 0);
        String prayerTiming = intent.getStringExtra("prayerTiming");
        String prayerKey = intent.getStringExtra("prayerKey");
        String prayerCity = intent.getStringExtra("prayerCity");

        String prayerName = context.getResources().getString(
                context.getResources().getIdentifier(prayerKey,
                        "string", context.getPackageName()));

        PendingIntent pendingIntent = getNotificationIntent();

        boolean isComplementaryTiming = prayerType.equals(TimingType.COMPLEMENTARY.toString());

        if (prayerType != null && isComplementaryTiming) {
            notificationTitle = context.getString(R.string.adthan_notification_title);
        } else {
            notificationTitle = context.getString(R.string.adthan_reminder_notification_title);
        }

        String content = prayerName + " : " + prayerTiming;

        if (prayerCity != null) {
            content += " (" + prayerCity + ")";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotifierConstants.ADTHAN_NOTIFICATION_CHANNEL_ID)
                .setColor(getNotificationColor())
                .setContentTitle(notificationTitle)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setDeleteIntent(createOnDismissedIntent(notificationId))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            builder.setSmallIcon(android.R.drawable.ic_popup_reminder);
        } else {
            builder.setSmallIcon(getNotificationIcon());
        }

        PrayerEnum prayerEnum = null;
        String notificationType = null;
        try {
            if (prayerKey != null) {
                prayerEnum = PrayerEnum.valueOf(prayerKey);
                notificationType = preferencesHelper.getNotificationTypeForPrayer(prayerEnum);
            }
        } catch (IllegalArgumentException e) {
            android.util.Log.e("ReminderNotification", "❌ Invalid prayer key: " + prayerKey, e);
        }

        if (notificationType == null) {
            notificationType = PreferencesHelper.TYPE_AZAN; // fallback to Azan behaviour
        }

        if (PreferencesHelper.TYPE_NONE.equals(notificationType)) {
            android.util.Log.d("ReminderNotification", "⏭️ Notification type 'none' for " + prayerKey + ", skipping reminder notification.");
            NotificationManagerCompat.from(context).cancel(notificationId);
            return;
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.notify(notificationId, builder.build());

        boolean shouldPlayAdhan = false;

        if (prayerEnum != null) {
            android.util.Log.d("ReminderNotification", "📳 " + prayerKey + " reminder, notification type: " + notificationType);
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
                    android.util.Log.d("ReminderNotification", "🔕 Silent reminder for " + prayerKey);
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
                    android.util.Log.w("ReminderNotification", "⚠️ Unknown notification type " + notificationType + " for " + prayerKey + ", falling back to Adhan.");
                    shouldPlayAdhan = true;
                    if (preferencesHelper.isVibrationActivated()) {
                        createVibration();
                    }
                    break;
            }
        } else {
            android.util.Log.w("ReminderNotification", "⚠️ Unable to resolve PrayerEnum for " + prayerKey + ", falling back to Adhan playback.");
            shouldPlayAdhan = true;
            if (preferencesHelper.isVibrationActivated()) {
                createVibration();
            }
        }

        if (shouldPlayAdhan) {
            setupCall(!isComplementaryTiming, prayerEnum, prayerKey);
        } else {
            android.util.Log.d("ReminderNotification", "🔕 Adhan playback skipped for reminder " + prayerKey + " (type=" + notificationType + ")");
        }
    }

    private PendingIntent getNotificationIntent() {
        Intent notificationIntent = new Intent(context, MainActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntentCreator.getActivity(context, 0,
                notificationIntent, 0);
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

    private PendingIntent createOnDismissedIntent(int notificationId) {
        Intent intent = new Intent(context, NotificationDismissedReceiver.class);
        intent.setClass(context, NotificationDismissedReceiver.class);
        intent.putExtra("notificationId", notificationId);

        return PendingIntentCreator.getBroadcast(context.getApplicationContext(),
                notificationId, intent, PendingIntent.FLAG_ONE_SHOT);
    }

    private void setupCall(boolean isReminder, PrayerEnum prayerEnum, String prayerKey) {
        boolean callEnabled;

        if (isReminder) {
            callEnabled = preferencesHelper.isReminderCallEnabled();
        } else {
            String adhanCallKeyPart = PreferencesConstants.TIMING_REMINDER_CALL_ENABLED;
            String callPreferenceKey = prayerKey + adhanCallKeyPart;

            final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            callEnabled = defaultSharedPreferences.getBoolean(callPreferenceKey, false);
        }

        if (callEnabled) {
            reminderPlayer.playAdhan(prayerEnum, isReminder);
        }
    }

    private void playTone(Uri toneUri, int streamType, PrayerEnum prayerEnum) {
        if (toneUri == null) {
            android.util.Log.w("ReminderNotification", "⚠️ Tone URI is null, skipping playback");
            return;
        }

        try {
            Ringtone ringtone = RingtoneManager.getRingtone(context.getApplicationContext(), toneUri);
            if (ringtone == null) {
                android.util.Log.w("ReminderNotification", "⚠️ Unable to obtain ringtone for URI: " + toneUri);
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
                android.util.Log.w("ReminderNotification", "⚠️ Cannot adjust ringtone volume on Android versions below 9 (P); using system volume.");
            }

            ringtone.play();
            android.util.Log.d("ReminderNotification", "🔊 Playing reminder tone for " + prayerEnum + " at volume " + volumePercent + "%");
        } catch (Exception e) {
            android.util.Log.e("ReminderNotification", "❌ Failed to play tone for URI: " + toneUri, e);
        }
    }
}
