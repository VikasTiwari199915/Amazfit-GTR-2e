package com.vikas.gtr2e.utils;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.vikas.gtr2e.beans.MusicBean;
import com.vikas.gtr2e.beans.MusicStateBean;
import com.vikas.gtr2e.enums.MusicControl;
import com.vikas.gtr2e.services.GTR2eNotificationListenerService;

import java.util.List;

import lombok.Getter;

@Getter
public class MediaUtil {
    public static final String TAG = "MediaUtil";
    public static MusicBean bufferMusicBean = null;
    public static MusicStateBean bufferMusicStateBean = null;

    public static int getPhoneVolume(final Context context) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        final int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        final int volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return Math.round(100 * (volumeLevel / (float) volumeMax));
    }

    public static void refresh(Context context) {
        Log.i(TAG,"Refreshing media state");
        MediaController controller = getMediaController(context);
        if(controller==null) {
            bufferMusicBean = null;
            bufferMusicStateBean = null;
            return;
        }
        try {
            bufferMusicBean = extractMusicBean(controller.getMetadata());
            bufferMusicStateBean = extractMusicStateBean(controller.getPlaybackState());
        } catch (final Exception e) {
            Log.e(TAG,"Failed to get media info", e);
        }
    }

    public static void setMediaState(Context context, MusicControl control) {
        try {
            MediaController controller = getMediaController(context);
            if (controller == null) return;

            MediaController.TransportControls tc = controller.getTransportControls();

            switch (control) {
                case PLAY -> tc.play();
                case PAUSE -> tc.pause();
                case NEXT -> tc.skipToNext();
                case PREVIOUS -> tc.skipToPrevious();
                case VOLUME_UP -> controller.adjustVolume(AudioManager.ADJUST_RAISE, 0);
                case VOLUME_DOWN -> controller.adjustVolume(AudioManager.ADJUST_LOWER, 0);
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to control media", e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set media state", e);
        }
    }

    public static MediaController getMediaController(Context context) {
        try {
            MediaSessionManager msm = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> controllers = msm.getActiveSessions(new ComponentName(context, GTR2eNotificationListenerService.class));

            if (!controllers.isEmpty()) return controllers.get(0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                MediaSession.Token token = msm.getMediaKeyEventSession();
                return token != null ? new MediaController(context, token) : null;
            }
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to get media sessions - did not grant notification access?", e);
        } catch (Exception e) {
            Log.e(TAG, "Error getting media controller", e);
        }
        return null;
    }

    public static boolean isNotificationListenerEnabled(Context context) {
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");

        if (flat == null) return false;

        for (String name : flat.split(":")) {
            ComponentName cn = ComponentName.unflattenFromString(name);
            if (cn != null && context.getPackageName().equals(cn.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static MusicBean extractMusicBean(MediaMetadata d) {
        if (d == null) return null;

        MusicBean m = new MusicBean();
        try {
            m.artist = d.getString(MediaMetadata.METADATA_KEY_ARTIST);
            m.album = d.getString(MediaMetadata.METADATA_KEY_ALBUM);
            m.track = d.getString(MediaMetadata.METADATA_KEY_TITLE);
            m.duration = (int) (d.getLong(MediaMetadata.METADATA_KEY_DURATION) / 1000);
            m.trackCount = (int) d.getLong(MediaMetadata.METADATA_KEY_NUM_TRACKS);
            m.trackNr = (int) d.getLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER);
        } catch (Exception e) {
            Log.e(TAG, "Metadata extraction failed", e);
        }
        return m;
    }

    @Nullable
    public static MusicStateBean extractMusicStateBean(PlaybackState s) {
        if (s == null) return null;

        MusicStateBean state = new MusicStateBean();
        try {
            state.position = (int) (s.getPosition() / 1000);
            state.playRate = Math.round(100 * s.getPlaybackSpeed());
            state.repeat = MusicStateBean.STATE_UNKNOWN;
            state.shuffle = MusicStateBean.STATE_UNKNOWN;

            state.state = switch (s.getState()) {
                case PlaybackState.STATE_PLAYING -> MusicStateBean.STATE_PLAYING;
                case PlaybackState.STATE_STOPPED -> MusicStateBean.STATE_STOPPED;
                case PlaybackState.STATE_PAUSED -> MusicStateBean.STATE_PAUSED;
                default -> MusicStateBean.STATE_UNKNOWN;
            };

        } catch (Exception e) {
            Log.e(TAG, "Playback state extraction failed", e);
        }
        return state;
    }
}
