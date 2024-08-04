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

import static com.android.settingslib.bluetooth.BluetoothUtils.isBroadcasting;

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
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.ValidatedEditTextPreference;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioSharingNamePreferenceController extends BasePreferenceController
        implements ValidatedEditTextPreference.Validator,
                Preference.OnPreferenceChangeListener,
                DefaultLifecycleObserver,
                LocalBluetoothProfileManager.ServiceListener {

    private static final String TAG = "AudioSharingNamePreferenceController";
    private static final boolean DEBUG = BluetoothUtils.D;
    private static final String PREF_KEY = "audio_sharing_stream_name";

    @VisibleForTesting
    final BluetoothLeBroadcast.Callback mBroadcastCallback =
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
                }

                @Override
                public void onBroadcastUpdated(int reason, int broadcastId) {
                    if (DEBUG) {
                        Log.d(TAG, "onBroadcastUpdated() reason : " + reason);
                    }
                }

                @Override
                public void onPlaybackStarted(int reason, int broadcastId) {}

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final LocalBluetoothProfileManager mProfileManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private AudioSharingNamePreference mPreference;
    private final Executor mExecutor;
    private final AudioSharingNameTextValidator mAudioSharingNameTextValidator;

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private AtomicBoolean mCallbacksRegistered = new AtomicBoolean(false);

    public AudioSharingNamePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mBtManager = Utils.getLocalBluetoothManager(context);
        mProfileManager = mBtManager == null ? null : mBtManager.getProfileManager();
        mBroadcast =
                (mProfileManager != null) ? mProfileManager.getLeAudioBroadcastProfile() : null;
        mAudioSharingNameTextValidator = new AudioSharingNameTextValidator();
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
        if (mPreference != null) {
            mPreference.setValidator(this);
            updateBroadcastName();
            updateQrCodeIcon(isBroadcasting(mBtManager));
        }
    }

    @Override
    public void onServiceConnected() {
        if (AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            registerCallbacks();
            updateBroadcastName();
            updateQrCodeIcon(isBroadcasting(mBtManager));
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
                                boolean isBroadcasting = isBroadcasting(mBtManager);
                                mBroadcast.setBroadcastName((String) newValue);
                                // We currently don't have a UI field for program info so we keep it
                                // consistent with broadcast name.
                                mBroadcast.setProgramInfo((String) newValue);
                                if (isBroadcasting) {
                                    mBroadcast.updateBroadcast();
                                }
                                updateBroadcastName();
                                mMetricsFeatureProvider.action(
                                        mContext,
                                        SettingsEnums.ACTION_AUDIO_STREAM_NAME_UPDATED,
                                        isBroadcasting ? 1 : 0);
                            }
                        });
        return true;
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

    private void updateBroadcastName() {
        if (mPreference != null) {
            var unused =
                    ThreadUtils.postOnBackgroundThread(
                            () -> {
                                if (mBroadcast != null) {
                                    String name = mBroadcast.getBroadcastName();
                                    AudioSharingUtils.postOnMainThread(
                                            mContext,
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
            AudioSharingUtils.postOnMainThread(
                    mContext,
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
