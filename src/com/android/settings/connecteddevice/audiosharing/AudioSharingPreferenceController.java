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

package com.android.settings.connecteddevice.audiosharing;

import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver, BluetoothCallback {
    private static final String TAG = "AudioSharingPreferenceController";

    private final LocalBluetoothManager mLocalBtManager;
    private final Executor mExecutor;
    @Nullable private LocalBluetoothLeBroadcast mBroadcast = null;
    @Nullable private Preference mPreference;

    private final BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {
                    if (mPreference != null) {
                        refreshSummary(mPreference);
                    }
                }

                @Override
                public void onBroadcastStartFailed(int reason) {}

                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata) {}

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    if (mPreference != null) {
                        refreshSummary(mPreference);
                    }
                }

                @Override
                public void onBroadcastStopFailed(int reason) {}

                @Override
                public void onBroadcastUpdated(int reason, int broadcastId) {}

                @Override
                public void onBroadcastUpdateFailed(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStarted(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

    public AudioSharingPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLocalBtManager = Utils.getLocalBtManager(context);
        if (mLocalBtManager != null) {
            mBroadcast = mLocalBtManager.getProfileManager().getLeAudioBroadcastProfile();
        }
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().registerCallback(this);
        }
        if (mBroadcast != null) {
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().unregisterCallback(this);
        }
        if (mBroadcast != null) {
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return AudioSharingUtils.isBroadcasting(mLocalBtManager) ? "On" : "Off";
    }

    @Override
    public void onBluetoothStateChanged(@AdapterState int bluetoothState) {
        if (mPreference != null) {
            refreshSummary(mPreference);
        }
    }
}
