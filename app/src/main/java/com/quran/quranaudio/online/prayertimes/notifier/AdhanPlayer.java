package com.quran.quranaudio.online.prayertimes.notifier;

import android.content.ContentResolver;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.media.VolumeProviderCompat;

import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class AdhanPlayer {

    private final MediaPlayer adhanMediaPlayer;
    private final MediaPlayer douaMediaPlayer;
    private final PreferencesHelper preferencesHelper;
    private final Context context;

    @Inject
    public AdhanPlayer(PreferencesHelper preferencesHelper, Context context) {
        this.preferencesHelper = preferencesHelper;
        this.context = context;
        adhanMediaPlayer = new MediaPlayer();
        douaMediaPlayer = new MediaPlayer();
    }

    public void playAdhan(PrayerEnum prayerEnum) {
        if (prayerEnum == null) {
            prayerEnum = PrayerEnum.FAJR; // Fallback
        }

        if (!adhanMediaPlayer.isPlaying() || !douaMediaPlayer.isPlaying()) {
            try {
                initializeAdhanMediaPlayer(prayerEnum);
                initializeDouaeMediaPlayer(prayerEnum);
            } catch (IOException e) {
                Log.e("AdhanPlayer", "Cannot play Adhan", e);
                return;
            }

            adhanMediaPlayer.start();

            if (preferencesHelper.isDouaeAfterAdhanEnabled() &&
                    !Uri.parse(preferencesHelper.getAdhanCaller()).equals(UiUtils.uriFromRaw(PreferencesConstants.SHORT_PRAYER_CALL, context)) &&
                    !Uri.parse(preferencesHelper.getAdhanCaller()).equals(UiUtils.uriFromRaw(PreferencesConstants.TAKBEER_ONLY_CALL, context))) {

                adhanMediaPlayer.setNextMediaPlayer(douaMediaPlayer);
            }
        }

        setOnCompletionListeners();
    }

    public void stopAdhan() {
        if (adhanMediaPlayer.isPlaying()) {
            adhanMediaPlayer.stop();
        }

        if (douaMediaPlayer.isPlaying()) {
            douaMediaPlayer.stop();
        }
    }

    public void setOnCompletionListeners() {
        MediaSessionCompat adhanMediaSession = createMediaSession("Adhan");
        MediaSessionCompat douaeMediaSession = createMediaSession("Douae");

        adhanMediaPlayer.setOnCompletionListener(mp -> adhanMediaSession.release());
        douaMediaPlayer.setOnCompletionListener(mp -> douaeMediaSession.release());
    }

    private void initializeAdhanMediaPlayer(PrayerEnum prayerEnum) throws IOException {
        adhanMediaPlayer.reset();
        adhanMediaPlayer.setDataSource(context, getAdhanUri(prayerEnum));
        setAudioAttribute(adhanMediaPlayer);
        adhanMediaPlayer.setLooping(false);
        adhanMediaPlayer.prepare();
        applyVolume(adhanMediaPlayer, prayerEnum);
    }

    private void initializeDouaeMediaPlayer(PrayerEnum prayerEnum) throws IOException {
        douaMediaPlayer.reset();
        douaMediaPlayer.setDataSource(context, getDouaeUri(context));
        setAudioAttribute(douaMediaPlayer);
        douaMediaPlayer.setLooping(false);
        douaMediaPlayer.prepare();
        applyVolume(douaMediaPlayer, prayerEnum);
    }

    private void setAudioAttribute(MediaPlayer mediaPlayer) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            builder.setUsage(AudioAttributes.USAGE_ALARM);
            builder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
            builder.setLegacyStreamType(AudioManager.STREAM_ALARM);

            mediaPlayer.setAudioAttributes(builder.build());
        } else {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        }
    }

    private Uri getAdhanUri(PrayerEnum prayerEnum) {
        if (prayerEnum == PrayerEnum.FAJR) {
            return Uri.parse(preferencesHelper.getFajrAdhanCaller());
        }
        return Uri.parse(preferencesHelper.getAdhanCaller());
    }

    private Uri getDouaeUri(Context context) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "/" + R.raw.douae_after_athan);
    }

    private MediaSessionCompat createMediaSession(String tag) {
        MediaSessionCompat mediaSession = new MediaSessionCompat(context, tag);
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0)
                .build());

        VolumeProviderCompat volumeProvider =
                new VolumeProviderCompat(VolumeProviderCompat.VOLUME_CONTROL_RELATIVE, 100, 50) {
                    @Override
                    public void onAdjustVolume(int direction) {
                        if (direction == -1) {
                            stopAdhan();
                        }
                        mediaSession.release();
                    }
                };
        mediaSession.setPlaybackToRemote(volumeProvider);
        mediaSession.setActive(true);

        return mediaSession;
    }

    private void applyVolume(MediaPlayer mediaPlayer, PrayerEnum prayerEnum) {
        int volumePercent = preferencesHelper.getVolumeForPrayer(prayerEnum);
        float volumeScalar = Math.max(0f, Math.min(1f, volumePercent / 100f));
        mediaPlayer.setVolume(volumeScalar, volumeScalar);
        Log.d("AdhanPlayer", "🔊 Applying volume " + volumePercent + "% for " + prayerEnum + " (scalar=" + volumeScalar + ")");
    }
}