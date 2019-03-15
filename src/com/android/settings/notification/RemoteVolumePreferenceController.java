/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification;

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.volume.MediaSessions;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class RemoteVolumePreferenceController extends
    VolumeSeekBarPreferenceController {

    private static final String KEY_REMOTE_VOLUME = "remote_volume";
    @VisibleForTesting
    static final int REMOTE_VOLUME = 100;

    private MediaSessionManager mMediaSessionManager;
    private MediaSessions mMediaSessions;
    @VisibleForTesting
    MediaSession.Token mActiveToken;
    @VisibleForTesting
    MediaController mMediaController;

    @VisibleForTesting
    MediaSessions.Callbacks mCallbacks = new MediaSessions.Callbacks() {
        @Override
        public void onRemoteUpdate(MediaSession.Token token, String name,
                MediaController.PlaybackInfo pi) {
            if (mActiveToken == null) {
                updateToken(token);
            }
            if (Objects.equals(mActiveToken, token)) {
                updatePreference(mPreference, mActiveToken, pi);
            }
        }

        @Override
        public void onRemoteRemoved(MediaSession.Token t) {
            if (Objects.equals(mActiveToken, t)) {
                updateToken(null);
                if (mPreference != null) {
                    mPreference.setVisible(false);
                }
            }
        }

        @Override
        public void onRemoteVolumeChanged(MediaSession.Token token, int flags) {
            if (Objects.equals(mActiveToken, token)) {
                final MediaController.PlaybackInfo pi = mMediaController.getPlaybackInfo();
                if (pi != null) {
                    setSliderPosition(pi.getCurrentVolume());
                }
            }
        }
    };

    public RemoteVolumePreferenceController(Context context) {
        super(context, KEY_REMOTE_VOLUME);
        mMediaSessionManager = context.getSystemService(MediaSessionManager.class);
        mMediaSessions = new MediaSessions(context, Looper.getMainLooper(), mCallbacks);
    }

    @Override
    public int getAvailabilityStatus() {
        final List<MediaController> controllers = mMediaSessionManager.getActiveSessions(null);
        for (MediaController mediaController : controllers) {
            final MediaController.PlaybackInfo pi = mediaController.getPlaybackInfo();
            if (isRemote(pi)) {
                updateToken(mediaController.getSessionToken());
                return AVAILABLE;
            }
        }

        // No active remote media at this point
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mMediaController != null) {
            updatePreference(mPreference, mActiveToken, mMediaController.getPlaybackInfo());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        super.onResume();
        mMediaSessions.init();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        super.onPause();
        mMediaSessions.destroy();
    }

    @Override
    public int getSliderPosition() {
        if (mPreference != null) {
            return mPreference.getProgress();
        }
        if (mMediaController == null) {
            return 0;
        }
        final MediaController.PlaybackInfo playbackInfo = mMediaController.getPlaybackInfo();
        return playbackInfo != null ? playbackInfo.getCurrentVolume() : 0;
    }

    @Override
    public boolean setSliderPosition(int position) {
        if (mPreference != null) {
            mPreference.setProgress(position);
        }
        if (mMediaController == null) {
            return false;
        }
        mMediaController.setVolumeTo(position, 0);
        return true;
    }

    @Override
    public int getMaxSteps() {
        if (mPreference != null) {
            return mPreference.getMax();
        }
        if (mMediaController == null) {
            return 0;
        }
        final MediaController.PlaybackInfo playbackInfo = mMediaController.getPlaybackInfo();
        return playbackInfo != null ? playbackInfo.getMaxVolume() : 0;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), KEY_REMOTE_VOLUME);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_REMOTE_VOLUME;
    }

    @Override
    public int getAudioStream() {
        // This can be anything because remote volume controller doesn't rely on it.
        return REMOTE_VOLUME;
    }

    @Override
    public int getMuteIcon() {
        return R.drawable.ic_volume_remote_mute;
    }

    public static boolean isRemote(MediaController.PlaybackInfo pi) {
        return pi != null
                && pi.getPlaybackType() == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE;
    }

    @Override
    public Class<? extends SliceBackgroundWorker> getBackgroundWorkerClass() {
        return RemoteVolumeSliceWorker.class;
    }

    private void updatePreference(VolumeSeekBarPreference seekBarPreference,
            MediaSession.Token token, MediaController.PlaybackInfo playbackInfo) {
        if (seekBarPreference == null || token == null || playbackInfo == null) {
            return;
        }

        seekBarPreference.setMax(playbackInfo.getMaxVolume());
        seekBarPreference.setVisible(true);
        setSliderPosition(playbackInfo.getCurrentVolume());
    }

    private void updateToken(MediaSession.Token token) {
        mActiveToken = token;
        if (token != null) {
            mMediaController = new MediaController(mContext, mActiveToken);
        } else {
            mMediaController = null;
        }
    }

    /**
     * Listener for background change to remote volume, which listens callback
     * from {@code MediaSessions}
     */
    public static class RemoteVolumeSliceWorker extends SliceBackgroundWorker<Void> implements
            MediaSessions.Callbacks {

        private MediaSessions mMediaSessions;

        public RemoteVolumeSliceWorker(Context context, Uri uri) {
            super(context, uri);
            mMediaSessions = new MediaSessions(context, Looper.getMainLooper(), this);
        }

        @Override
        protected void onSlicePinned() {
            mMediaSessions.init();
        }

        @Override
        protected void onSliceUnpinned() {
            mMediaSessions.destroy();
        }

        @Override
        public void close() throws IOException {
            mMediaSessions = null;
        }

        @Override
        public void onRemoteUpdate(MediaSession.Token token, String name,
                MediaController.PlaybackInfo pi) {
            notifySliceChange();
        }

        @Override
        public void onRemoteRemoved(MediaSession.Token t) {
            notifySliceChange();
        }

        @Override
        public void onRemoteVolumeChanged(MediaSession.Token token, int flags) {
            notifySliceChange();
        }
    }
}
