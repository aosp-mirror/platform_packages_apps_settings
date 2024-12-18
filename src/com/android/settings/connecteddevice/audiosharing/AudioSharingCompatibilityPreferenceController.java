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

package com.android.settings.connecteddevice.audiosharing;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioSharingCompatibilityPreferenceController extends TogglePreferenceController
        implements DefaultLifecycleObserver, LocalBluetoothProfileManager.ServiceListener {

    private static final String TAG = "AudioSharingCompatibilityPrefController";

    private static final String PREF_KEY = "audio_sharing_stream_compatibility";

    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final LocalBluetoothProfileManager mProfileManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private TwoStatePreference mPreference;
    private final Executor mExecutor;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final AtomicBoolean mCallbacksRegistered = new AtomicBoolean(false);

    @VisibleForTesting
    final BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onBroadcastStarted(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    updateEnabled();
                }

                @Override
                public void onBroadcastStartFailed(int reason) {}

                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata) {}

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onBroadcastStopped(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    updateEnabled();
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

    public AudioSharingCompatibilityPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBtManager = Utils.getLocalBtManager(context);
        mProfileManager = mBtManager == null ? null : mBtManager.getProfileManager();
        mBroadcast = mProfileManager == null ? null : mProfileManager.getLeAudioBroadcastProfile();
        mExecutor = Executors.newSingleThreadExecutor();
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip register callbacks, feature not support");
            return;
        }
        if (!AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            Log.d(TAG, "Skip register callbacks, profile not ready");
            if (mProfileManager != null) {
                mProfileManager.addServiceListener(this);
            }
            return;
        }
        registerCallbacks();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip unregister callbacks, feature not support");
            return;
        }
        if (mProfileManager != null) {
            mProfileManager.removeServiceListener(this);
        }
        if (mBroadcast == null || !AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            Log.d(TAG, "Skip unregister callbacks, profile not ready");
            return;
        }
        if (mCallbacksRegistered.get()) {
            Log.d(TAG, "Unregister callbacks");
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
            mCallbacksRegistered.set(false);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return BluetoothUtils.isAudioSharingEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateEnabled();
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean isChecked() {
        return mBroadcast != null && mBroadcast.getImproveCompatibility();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (mBroadcast == null || mBroadcast.getImproveCompatibility() == isChecked) {
            if (mBroadcast != null) {
                Log.d(TAG, "Skip setting improveCompatibility, unchanged");
            }
            return false;
        }
        mBroadcast.setImproveCompatibility(isChecked);
        // TODO: call updateBroadcast once framework change ready.
        mMetricsFeatureProvider.action(
                mContext, SettingsEnums.ACTION_AUDIO_SHARING_IMPROVE_COMPATIBILITY, isChecked);
        return true;
    }

    @Override
    public void onServiceConnected() {
        if (AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            registerCallbacks();
            AudioSharingUtils.postOnMainThread(
                    mContext,
                    () -> {
                        if (mPreference != null) {
                            updateState(mPreference);
                        }
                        updateEnabled();
                    });
            if (mProfileManager != null) {
                mProfileManager.removeServiceListener(this);
            }
        }
    }

    @Override
    public void onServiceDisconnected() {
        // Do nothing
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    /** Test only: set callbacks registration state for test setup. */
    @VisibleForTesting
    void setCallbacksRegistered(boolean registered) {
        mCallbacksRegistered.set(registered);
    }

    private void registerCallbacks() {
        if (mBroadcast == null) {
            Log.d(TAG, "Skip register callbacks, profile not ready");
            return;
        }
        if (!mCallbacksRegistered.get()) {
            Log.d(TAG, "Register callbacks");
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
            mCallbacksRegistered.set(true);
        }
    }

    private void updateEnabled() {
        int disabledDescriptionRes =
                R.string.audio_sharing_stream_compatibility_disabled_description;
        int descriptionRes = R.string.audio_sharing_stream_compatibility_description;
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            boolean isBroadcasting = BluetoothUtils.isBroadcasting(mBtManager);
                            AudioSharingUtils.postOnMainThread(
                                    mContext,
                                    () -> {
                                        if (mPreference != null) {
                                            mPreference.setEnabled(!isBroadcasting);
                                            mPreference.setSummary(
                                                    isBroadcasting
                                                            ? mContext.getString(
                                                                    disabledDescriptionRes)
                                                            : mContext.getString(descriptionRes));
                                        }
                                    });
                        });
    }
}
