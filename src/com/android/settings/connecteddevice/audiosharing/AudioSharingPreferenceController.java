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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingPreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver, BluetoothCallback {
    private static final String TAG = "AudioSharingPreferenceController";

    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final BluetoothEventManager mEventManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private Preference mPreference;
    private final Executor mExecutor;

    @VisibleForTesting
    final BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {
                    refreshSummary();
                }

                @Override
                public void onBroadcastStartFailed(int reason) {}

                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata) {}

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    refreshSummary();
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
        mBtManager = Utils.getLocalBtManager(context);
        mEventManager = mBtManager == null ? null : mBtManager.getEventManager();
        mBroadcast =
                mBtManager == null
                        ? null
                        : mBtManager.getProfileManager().getLeAudioBroadcastProfile();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip register callbacks, feature not support");
            return;
        }
        if (mEventManager == null || mBroadcast == null) {
            Log.d(TAG, "Skip register callbacks, profile not ready");
            return;
        }
        mEventManager.registerCallback(this);
        mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip unregister callbacks, feature not support");
            return;
        }
        if (mEventManager == null || mBroadcast == null) {
            Log.d(TAG, "Skip register callbacks, profile not ready");
            return;
        }
        mEventManager.unregisterCallback(this);
        mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return BluetoothUtils.isAudioSharingEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return BluetoothUtils.isBroadcasting(mBtManager)
                ? mContext.getString(R.string.audio_sharing_summary_on)
                : mContext.getString(R.string.audio_sharing_summary_off);
    }

    @Override
    public void onBluetoothStateChanged(@AdapterState int bluetoothState) {
        refreshSummary();
    }

    private void refreshSummary() {
        if (mPreference == null) {
            return;
        }
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            final CharSequence summary = getSummary();
                            AudioSharingUtils.postOnMainThread(
                                    mContext,
                                    () -> {
                                        // Check nullability to pass NullAway check
                                        if (mPreference != null) {
                                            mPreference.setSummary(summary);
                                        }
                                    });
                        });
    }
}
