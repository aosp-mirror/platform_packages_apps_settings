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

package com.android.settings.media;

import static android.media.AudioManager.STREAM_DEVICES_CHANGED_ACTION;

import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.bluetooth.Utils;
import com.android.settings.slices.SliceBackgroundWorker;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

/**
 * Listener for background change from {@code BluetoothCallback} to update media output indicator.
 */
public class MediaOutputIndicatorWorker extends SliceBackgroundWorker implements BluetoothCallback {

    private static final String TAG = "MediaOutputIndicatorWorker";

    private final DevicesChangedBroadcastReceiver mReceiver;
    private final Context mContext;

    private LocalBluetoothManager mLocalBluetoothManager;

    public MediaOutputIndicatorWorker(Context context, Uri uri) {
        super(context, uri);
        mReceiver = new DevicesChangedBroadcastReceiver();
        mContext = context;
    }

    @Override
    protected void onSlicePinned() {
        mLocalBluetoothManager = Utils.getLocalBtManager(getContext());
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        final IntentFilter intentFilter = new IntentFilter(STREAM_DEVICES_CHANGED_ACTION);
        mContext.registerReceiver(mReceiver, intentFilter);
        mLocalBluetoothManager.getEventManager().registerCallback(this);
    }

    @Override
    protected void onSliceUnpinned() {
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return;
        }
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void close() {
        mLocalBluetoothManager = null;
    }

    @Override
    public void onBluetoothStateChanged(int bluetoothState) {
        // To handle the case that Bluetooth on and no connected devices
        notifySliceChange();
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        if (bluetoothProfile == BluetoothProfile.A2DP ||
                bluetoothProfile == BluetoothProfile.HEARING_AID) {
            notifySliceChange();
        }
    }

    @Override
    public void onAudioModeChanged() {
        notifySliceChange();
    }

    @Nullable
    MediaController getActiveLocalMediaController() {
        final MediaSessionManager mMediaSessionManager = mContext.getSystemService(
                MediaSessionManager.class);

        for (MediaController controller : mMediaSessionManager.getActiveSessions(null)) {
            final MediaController.PlaybackInfo pi = controller.getPlaybackInfo();
            if (pi == null) {
                return null;
            }
            final PlaybackState playbackState = controller.getPlaybackState();
            if (playbackState == null) {
                return null;
            }
            if (pi.getPlaybackType() == MediaController.PlaybackInfo.PLAYBACK_TYPE_LOCAL
                    && playbackState.getState() == PlaybackState.STATE_PLAYING) {
                return controller;
            }
        }
        return null;
    }
    private class DevicesChangedBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TextUtils.equals(AudioManager.STREAM_DEVICES_CHANGED_ACTION, action)) {
                notifySliceChange();
            }
        }
    }
}
