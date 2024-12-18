/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.media.BluetoothMediaDevice;
import com.android.settingslib.media.LocalMediaManager;
import com.android.settingslib.media.MediaDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class MediaControlHelper {
    private static final String TAG = "MediaControlHelper";
    private final Context mContext;
    private final MediaSessionManager mMediaSessionManager;
    @Nullable private final LocalBluetoothManager mLocalBluetoothManager;
    private final List<Pair<LocalMediaManager, LocalMediaManager.DeviceCallback>>
            mLocalMediaManagers = new ArrayList<>();

    MediaControlHelper(Context context, @Nullable LocalBluetoothManager localBluetoothManager) {
        mContext = context;
        mMediaSessionManager = context.getSystemService(MediaSessionManager.class);
        mLocalBluetoothManager = localBluetoothManager;
    }

    void start() {
        if (mLocalBluetoothManager == null) {
            return;
        }
        var currentLeDevice =
                AudioStreamsHelper.getCachedBluetoothDeviceInSharingOrLeConnected(
                        mLocalBluetoothManager);
        if (currentLeDevice.isEmpty()) {
            Log.d(TAG, "start() : current LE device is empty!");
            return;
        }

        for (MediaController controller : mMediaSessionManager.getActiveSessions(null)) {
            String packageName = controller.getPackageName();

            // We won't stop media created from settings.
            if (Objects.equals(packageName, mContext.getPackageName())) {
                Log.d(TAG, "start() : skip package: " + packageName);
                continue;
            }

            // Start scanning and listen to device list update, stop this media if device matched.
            var localMediaManager = new LocalMediaManager(mContext, packageName);
            var deviceCallback =
                    new LocalMediaManager.DeviceCallback() {
                        public void onDeviceListUpdate(List<MediaDevice> devices) {
                            if (shouldStopMedia(
                                    controller,
                                    currentLeDevice.get(),
                                    localMediaManager.getCurrentConnectedDevice())) {
                                Log.d(
                                        TAG,
                                        "start() : Stopping media player for package: "
                                                + controller.getPackageName());
                                var controls = controller.getTransportControls();
                                if (controls != null) {
                                    controls.stop();
                                }
                            }
                        }
                    };
            localMediaManager.registerCallback(deviceCallback);
            localMediaManager.startScan();
            mLocalMediaManagers.add(new Pair<>(localMediaManager, deviceCallback));
        }
    }

    void stop() {
        mLocalMediaManagers.forEach(
                m -> {
                    m.first.stopScan();
                    m.first.unregisterCallback(m.second);
                });
        mLocalMediaManagers.clear();
    }

    private static boolean shouldStopMedia(
            MediaController controller,
            CachedBluetoothDevice currentLeDevice,
            MediaDevice currentMediaDevice) {
        // We won't stop media if it's already stopped.
        if (controller.getPlaybackState() != null
                && controller.getPlaybackState().getState() == PlaybackState.STATE_STOPPED) {
            Log.d(TAG, "shouldStopMedia() : skip already stopped: " + controller.getPackageName());
            return false;
        }

        var deviceForMedia =
                currentMediaDevice instanceof BluetoothMediaDevice
                        ? (BluetoothMediaDevice) currentMediaDevice
                        : null;
        return deviceForMedia != null
                && hasOverlap(deviceForMedia.getCachedDevice(), currentLeDevice);
    }

    private static boolean hasOverlap(
            CachedBluetoothDevice device1, CachedBluetoothDevice device2) {
        return device1.equals(device2)
                || device1.getMemberDevice().contains(device2)
                || device2.getMemberDevice().contains(device1);
    }
}
