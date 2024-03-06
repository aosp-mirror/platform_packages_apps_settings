/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.annotation.Nullable;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

public class AudioStreamsActiveDeviceSummaryUpdater implements BluetoothCallback {
    private static final String TAG = "AudioStreamsActiveDeviceSummaryUpdater";
    private static final boolean DEBUG = BluetoothUtils.D;
    private final LocalBluetoothManager mBluetoothManager;
    private String mSummary;
    private OnSummaryChangeListener mListener;

    public AudioStreamsActiveDeviceSummaryUpdater(
            Context context, OnSummaryChangeListener listener) {
        mBluetoothManager = Utils.getLocalBluetoothManager(context);
        mListener = listener;
    }

    @Override
    public void onActiveDeviceChanged(
            @Nullable CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onActiveDeviceChanged() with activeDevice : "
                            + (activeDevice == null ? "null" : activeDevice.getAddress())
                            + " on profile : "
                            + bluetoothProfile);
        }
        if (bluetoothProfile == BluetoothProfile.LE_AUDIO) {
            notifyChangeIfNeeded();
        }
    }

    void register(boolean register) {
        if (register) {
            notifyChangeIfNeeded();
            mBluetoothManager.getEventManager().registerCallback(this);
        } else {
            mBluetoothManager.getEventManager().unregisterCallback(this);
        }
    }

    private void notifyChangeIfNeeded() {
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    String summary = getSummary();
                    if (!TextUtils.equals(mSummary, summary)) {
                        mSummary = summary;
                        ThreadUtils.postOnMainThread(() -> mListener.onSummaryChanged(summary));
                    }
                });
    }

    private String getSummary() {
        var activeSink = AudioSharingUtils.getActiveSinkOnAssistant(mBluetoothManager);
        if (activeSink.isEmpty()) {
            return "No active LE Audio device";
        }
        return activeSink.get().getName();
    }

    /** Interface definition for a callback to be invoked when the summary has been changed. */
    interface OnSummaryChangeListener {
        /**
         * Called when summary has changed.
         *
         * @param summary The new summary.
         */
        void onSummaryChanged(String summary);
    }
}
