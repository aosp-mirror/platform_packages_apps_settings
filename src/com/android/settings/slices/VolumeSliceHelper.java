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

package com.android.settings.slices;

import static com.android.settings.slices.CustomSliceRegistry.VOLUME_SLICES_URI;

import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.SliceBroadcastRelay;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This helper is to handle the broadcasts of volume slices
 */
public class VolumeSliceHelper {

    private static final String TAG = "VolumeSliceHelper";

    @VisibleForTesting
    static Map<Uri, Integer> sRegisteredUri = new ConcurrentHashMap<>();
    @VisibleForTesting
    static IntentFilter sIntentFilter;

    static void registerIntentToUri(Context context, IntentFilter intentFilter, Uri sliceUri,
            int audioStream) {
        Log.d(TAG, "Registering uri for broadcast relay: " + sliceUri);
        synchronized (sRegisteredUri) {
            if (sRegisteredUri.isEmpty()) {
                SliceBroadcastRelay.registerReceiver(context, VOLUME_SLICES_URI,
                        VolumeSliceRelayReceiver.class, intentFilter);
                sIntentFilter = intentFilter;
            }
            sRegisteredUri.put(sliceUri, audioStream);
        }
    }

    static boolean unregisterUri(Context context, Uri sliceUri) {
        if (!sRegisteredUri.containsKey(sliceUri)) {
            return false;
        }

        Log.d(TAG, "Unregistering uri broadcast relay: " + sliceUri);
        synchronized (sRegisteredUri) {
            sRegisteredUri.remove(sliceUri);
            if (sRegisteredUri.isEmpty()) {
                sIntentFilter = null;
                SliceBroadcastRelay.unregisterReceivers(context, VOLUME_SLICES_URI);
            }
        }
        return true;
    }

    static void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (sIntentFilter == null || action == null || !sIntentFilter.hasAction(action)) {
            return;
        }

        final String uriString = intent.getStringExtra(SliceBroadcastRelay.EXTRA_URI);
        if (uriString == null) {
            return;
        }

        final Uri uri = Uri.parse(uriString);
        if (!VOLUME_SLICES_URI.equals(ContentProvider.getUriWithoutUserId(uri))) {
            Log.w(TAG, "Invalid uri: " + uriString);
            return;
        }

        if (AudioManager.VOLUME_CHANGED_ACTION.equals(action)) {
            handleVolumeChanged(context, intent);
        } else if (AudioManager.STREAM_MUTE_CHANGED_ACTION.equals(action)) {
            handleMuteChanged(context, intent);
        } else if (AudioManager.STREAM_DEVICES_CHANGED_ACTION.equals(action)) {
            handleStreamChanged(context, intent);
        } else {
            notifyAllStreamsChanged(context);
        }
    }

    private static void handleVolumeChanged(Context context, Intent intent) {
        final int vol = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_VALUE, -1);
        final int prevVol = intent.getIntExtra(AudioManager.EXTRA_PREV_VOLUME_STREAM_VALUE, -1);
        if (vol != prevVol) {
            handleStreamChanged(context, intent);
        }
    }

    /**
     *  When mute is changed, notifyChange on relevant Volume Slice ContentResolvers to mark them
     *  as needing update.
     *
     * In addition to the matching stream, we always notifyChange for the Notification stream
     * when Ring events are issued. This is to make sure that Notification always gets updated
     * for RingerMode changes, even if Notification's volume is zero and therefore it would not
     * get its own AudioManager.VOLUME_CHANGED_ACTION.
     */
    private static void handleMuteChanged(Context context, Intent intent) {
        final int inputType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
        handleStreamChanged(context, inputType);
        if (inputType == AudioManager.STREAM_RING) {
            handleStreamChanged(context, AudioManager.STREAM_NOTIFICATION);
        }
    }

    private static void handleStreamChanged(Context context, Intent intent) {
        final int inputType = intent.getIntExtra(AudioManager.EXTRA_VOLUME_STREAM_TYPE, -1);
        handleStreamChanged(context, inputType);
    }

    private static void handleStreamChanged(Context context, int inputType) {
        for (Map.Entry<Uri, Integer> entry : sRegisteredUri.entrySet()) {
            if (entry.getValue() == inputType) {
                context.getContentResolver().notifyChange(entry.getKey(), null /* observer */);
                if (inputType != AudioManager.STREAM_RING) { // Two URIs are mapped to ring
                    break;
                }
            }
        }
    }

    private static void notifyAllStreamsChanged(Context context) {
        sRegisteredUri.keySet().forEach(uri -> {
            context.getContentResolver().notifyChange(uri, null /* observer */);
        });
    }
}
