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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

public class AudioStreamsActiveDeviceSummaryUpdater implements BluetoothCallback {
    private final LocalBluetoothManager mBluetoothManager;
    private Context mContext;
    @Nullable private String mSummary;
    private OnSummaryChangeListener mListener;

    public AudioStreamsActiveDeviceSummaryUpdater(
            Context context, OnSummaryChangeListener listener) {
        mContext = context;
        mBluetoothManager = Utils.getLocalBluetoothManager(context);
        mListener = listener;
    }

    @Override
    public void onBluetoothStateChanged(@AdapterState int bluetoothState) {
        if (bluetoothState == BluetoothAdapter.STATE_OFF) {
            notifyChangeIfNeeded();
        }
    }

    @Override
    public void onProfileConnectionStateChanged(
            @NonNull CachedBluetoothDevice cachedDevice,
            @ConnectionState int state,
            int bluetoothProfile) {
        if (bluetoothProfile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT
                && (state == BluetoothAdapter.STATE_CONNECTED
                        || state == BluetoothAdapter.STATE_DISCONNECTED)) {
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
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            String summary = getSummary();
                            if (!TextUtils.equals(mSummary, summary)) {
                                mSummary = summary;
                                ThreadUtils.postOnMainThread(
                                        () -> mListener.onSummaryChanged(summary));
                            }
                        });
    }

    private String getSummary() {
        var connectedSink =
                AudioStreamsHelper.getCachedBluetoothDeviceInSharingOrLeConnected(
                        mBluetoothManager);
        if (connectedSink.isEmpty()) {
            return mContext.getString(R.string.audio_streams_dialog_no_le_device_title);
        }
        return connectedSink.get().getName();
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
