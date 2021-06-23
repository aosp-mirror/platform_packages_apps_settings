/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.settings.media;

import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.sound.MediaOutputPreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities that can be shared between {@link MediaOutputIndicatorWorker} and
 * {@link MediaOutputPreferenceController}.
 */
public class MediaOutputUtils {

    private static final String TAG = "MediaOutputUtils";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    /**
     *  Returns a {@link MediaController} that state is playing and type is local playback,
     *  and also have active sessions.
     */
    @Nullable
    public static MediaController getActiveLocalMediaController(
            MediaSessionManager mediaSessionManager) {

        MediaController localController = null;
        final List<String> remoteMediaSessionLists = new ArrayList<>();
        for (MediaController controller : mediaSessionManager.getActiveSessions(null)) {
            final MediaController.PlaybackInfo pi = controller.getPlaybackInfo();
            if (pi == null) {
                // do nothing
                continue;
            }
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState == null) {
                // do nothing
                continue;
            }
            if (DEBUG) {
                Log.d(TAG, "getActiveLocalMediaController() package name : "
                        + controller.getPackageName()
                        + ", play back type : " + pi.getPlaybackType() + ", play back state : "
                        + playbackState.getState());
            }
            if (playbackState.getState() != PlaybackState.STATE_PLAYING) {
                // do nothing
                continue;
            }
            if (pi.getPlaybackType() == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
                if (localController != null && TextUtils.equals(localController.getPackageName(),
                        controller.getPackageName())) {
                    localController = null;
                }
                if (!remoteMediaSessionLists.contains(controller.getPackageName())) {
                    remoteMediaSessionLists.add(controller.getPackageName());
                }
                continue;
            }
            if (pi.getPlaybackType() == MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL) {
                if (localController == null
                        && !remoteMediaSessionLists.contains(controller.getPackageName())) {
                    localController = controller;
                }
            }
        }
        return localController;
    }
}
