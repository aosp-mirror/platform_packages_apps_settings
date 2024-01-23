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

import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingCompatibilityPreferenceController extends TogglePreferenceController
        implements DefaultLifecycleObserver {

    private static final String TAG = "AudioSharingCompatibilityPrefController";

    private static final String PREF_KEY = "audio_sharing_stream_compatibility";
    private static final String SHARING_OFF_SUMMARY =
            "Helps some devices like hearing aids connect by reducing audio quality";
    private static final String SHARING_ON_SUMMARY =
            "Turns off the audio sharing to config the compatibility";

    private final LocalBluetoothManager mBtManager;
    private final Executor mExecutor;
    private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private TwoStatePreference mPreference;

    private final BluetoothLeBroadcast.Callback mBroadcastCallback =
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
        mBroadcast = mBtManager.getProfileManager().getLeAudioBroadcastProfile();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mBroadcast != null) {
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mBroadcast != null) {
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
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
            Log.d(
                    TAG,
                    "Skip setting improveCompatibility, unchanged = "
                            + (mBroadcast.getImproveCompatibility() == isChecked));
            return false;
        }
        mBroadcast.setImproveCompatibility(isChecked);
        // TODO: call updateBroadcast once framework change ready.
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return 0;
    }

    private void updateEnabled() {
        mContext.getMainExecutor()
                .execute(
                        () -> {
                            if (mPreference != null) {
                                boolean isBroadcasting =
                                        AudioSharingUtils.isBroadcasting(mBtManager);
                                mPreference.setEnabled(!isBroadcasting);
                                mPreference.setSummary(
                                        isBroadcasting ? SHARING_ON_SUMMARY : SHARING_OFF_SUMMARY);
                            }
                        });
    }
}
