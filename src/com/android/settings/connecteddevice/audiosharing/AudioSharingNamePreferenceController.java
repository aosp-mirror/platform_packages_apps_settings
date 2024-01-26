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

import static com.android.settings.connecteddevice.audiosharing.AudioSharingUtils.isBroadcasting;

import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingNamePreferenceController extends BasePreferenceController
        implements ValidatedEditTextPreference.Validator,
                Preference.OnPreferenceChangeListener,
                DefaultLifecycleObserver {

    private static final String TAG = "AudioSharingNamePreferenceController";
    private static final boolean DEBUG = BluetoothUtils.D;
    private static final String PREF_KEY = "audio_sharing_stream_name";

    private final BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, BluetoothLeBroadcastMetadata metadata) {
                    if (DEBUG) {
                        Log.d(
                                TAG,
                                "onBroadcastMetadataChanged() broadcastId : "
                                        + broadcastId
                                        + " metadata: "
                                        + metadata);
                    }
                    updateQrCodeIcon(true);
                }

                @Override
                public void onBroadcastStartFailed(int reason) {}

                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {}

                @Override
                public void onBroadcastStopFailed(int reason) {}

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    if (DEBUG) {
                        Log.d(
                                TAG,
                                "onBroadcastStopped() reason : "
                                        + reason
                                        + " broadcastId: "
                                        + broadcastId);
                    }
                    updateQrCodeIcon(false);
                }

                @Override
                public void onBroadcastUpdateFailed(int reason, int broadcastId) {
                    Log.w(TAG, "onBroadcastUpdateFailed() reason : " + reason);
                    // Do nothing if update failed.
                }

                @Override
                public void onBroadcastUpdated(int reason, int broadcastId) {
                    if (DEBUG) {
                        Log.d(TAG, "onBroadcastUpdated() reason : " + reason);
                    }
                    updateBroadcastName();
                }

                @Override
                public void onPlaybackStarted(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

    @Nullable private final LocalBluetoothManager mLocalBtManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    private final Executor mExecutor;
    private final AudioSharingNameTextValidator mAudioSharingNameTextValidator;
    @Nullable private AudioSharingNamePreference mPreference;

    public AudioSharingNamePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLocalBtManager = Utils.getLocalBluetoothManager(context);
        mBroadcast =
                (mLocalBtManager != null)
                        ? mLocalBtManager.getProfileManager().getLeAudioBroadcastProfile()
                        : null;
        mAudioSharingNameTextValidator = new AudioSharingNameTextValidator();
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
        if (mPreference != null) {
            mPreference.setValidator(this);
            updateBroadcastName();
            updateQrCodeIcon(isBroadcasting(mLocalBtManager));
        }
    }

    @Override
    public String getPreferenceKey() {
        return PREF_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mPreference != null
                && mPreference.getSummary() != null
                && ((String) newValue).contentEquals(mPreference.getSummary())) {
            return false;
        }

        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            if (mBroadcast != null) {
                                mBroadcast.setProgramInfo((String) newValue);
                                if (isBroadcasting(mLocalBtManager)) {
                                    // Update broadcast, UI update will be handled after callback
                                    mBroadcast.updateBroadcast();
                                } else {
                                    // Directly update UI if no ongoing broadcast
                                    updateBroadcastName();
                                }
                            }
                        });
        return true;
    }

    private void updateBroadcastName() {
        if (mPreference != null) {
            var unused =
                    ThreadUtils.postOnBackgroundThread(
                            () -> {
                                if (mBroadcast != null) {
                                    String name = mBroadcast.getProgramInfo();
                                    ThreadUtils.postOnMainThread(
                                            () -> {
                                                if (mPreference != null) {
                                                    mPreference.setText(name);
                                                    mPreference.setSummary(name);
                                                }
                                            });
                                }
                            });
        }
    }

    private void updateQrCodeIcon(boolean show) {
        if (mPreference != null) {
            ThreadUtils.postOnMainThread(
                    () -> {
                        if (mPreference != null) {
                            mPreference.setShowQrCodeIcon(show);
                        }
                    });
        }
    }

    @Override
    public boolean isTextValid(String value) {
        return mAudioSharingNameTextValidator.isTextValid(value);
    }
}
