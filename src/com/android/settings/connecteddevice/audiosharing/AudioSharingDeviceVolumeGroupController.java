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

import android.annotation.IntRange;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothVolumeControl;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.utils.ThreadUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingDeviceVolumeGroupController extends AudioSharingBasePreferenceController
        implements DevicePreferenceCallback {
    private static final boolean DEBUG = BluetoothUtils.D;

    private static final String TAG = "AudioSharingDeviceVolumeGroupController";
    private static final String KEY = "audio_sharing_device_volume_group";

    private final LocalBluetoothManager mLocalBtManager;
    private final LocalBluetoothLeBroadcastAssistant mAssistant;
    private final Executor mExecutor;
    private VolumeControlProfile mVolumeControl;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private FragmentManager mFragmentManager;
    private PreferenceGroup mPreferenceGroup;
    private Map<Preference, BluetoothVolumeControl.Callback> mCallbackMap =
            new HashMap<Preference, BluetoothVolumeControl.Callback>();

    private BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
            new BluetoothLeBroadcastAssistant.Callback() {
                @Override
                public void onSearchStarted(int reason) {}

                @Override
                public void onSearchStartFailed(int reason) {}

                @Override
                public void onSearchStopped(int reason) {}

                @Override
                public void onSearchStopFailed(int reason) {}

                @Override
                public void onSourceFound(@NonNull BluetoothLeBroadcastMetadata source) {}

                @Override
                public void onSourceAdded(@NonNull BluetoothDevice sink, int sourceId, int reason) {
                    Log.d(
                            TAG,
                            "onSourceAdded(), sink = "
                                    + sink
                                    + ", sourceId = "
                                    + sourceId
                                    + ", reason = "
                                    + reason);
                    mBluetoothDeviceUpdater.forceUpdate();
                }

                @Override
                public void onSourceAddFailed(
                        @NonNull BluetoothDevice sink,
                        @NonNull BluetoothLeBroadcastMetadata source,
                        int reason) {
                    Log.d(
                            TAG,
                            "onSourceAddFailed(), sink = "
                                    + sink
                                    + ", source = "
                                    + source
                                    + ", reason = "
                                    + reason);
                }

                @Override
                public void onSourceModified(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceModifyFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceRemoved(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                    Log.d(
                            TAG,
                            "onSourceRemoved(), sink = "
                                    + sink
                                    + ", sourceId = "
                                    + sourceId
                                    + ", reason = "
                                    + reason);
                    mBluetoothDeviceUpdater.forceUpdate();
                }

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                    Log.d(
                            TAG,
                            "onSourceRemoveFailed(), sink = "
                                    + sink
                                    + ", sourceId = "
                                    + sourceId
                                    + ", reason = "
                                    + reason);
                }

                @Override
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {}
            };

    public AudioSharingDeviceVolumeGroupController(Context context) {
        super(context, KEY);
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        mAssistant = mLocalBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        mExecutor = Executors.newSingleThreadExecutor();
        if (mLocalBtManager != null) {
            mVolumeControl = mLocalBtManager.getProfileManager().getVolumeControlProfile();
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        super.onStart(owner);
        if (mAssistant == null) {
            Log.d(TAG, "onStart() Broadcast or assistant is not supported on this device");
            return;
        }
        if (mBluetoothDeviceUpdater == null) {
            Log.d(TAG, "onStart() Bluetooth device updater is not initialized");
            return;
        }
        mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        mBluetoothDeviceUpdater.registerCallback();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        super.onStop(owner);
        if (mAssistant == null) {
            Log.d(TAG, "onStop() Broadcast or assistant is not supported on this device");
            return;
        }
        if (mBluetoothDeviceUpdater == null) {
            Log.d(TAG, "onStop() Bluetooth device updater is not initialized");
            return;
        }
        mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        mBluetoothDeviceUpdater.unregisterCallback();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        for (var entry : mCallbackMap.entrySet()) {
            if (DEBUG) {
                Log.d(TAG, "onDestroy: unregister callback for " + entry.getKey());
            }
            mVolumeControl.unregisterCallback(entry.getValue());
        }
        mCallbackMap.clear();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceGroup = screen.findPreference(KEY);
        mPreferenceGroup.setVisible(false);

        if (isAvailable() && mBluetoothDeviceUpdater != null) {
            mBluetoothDeviceUpdater.setPrefContext(screen.getContext());
            mBluetoothDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(true);
        }
        mPreferenceGroup.addPreference(preference);
        if (mVolumeControl != null && preference instanceof AudioSharingDeviceVolumePreference) {
            BluetoothVolumeControl.Callback callback =
                    buildVcCallback((AudioSharingDeviceVolumePreference) preference);
            mCallbackMap.put(preference, callback);
            if (DEBUG) {
                Log.d(TAG, "onDeviceAdded: register callback for " + preference);
            }
            mVolumeControl.registerCallback(mExecutor, callback);
        }
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceGroup.removePreference(preference);
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(false);
        }
        if (mVolumeControl != null && mCallbackMap.containsKey(preference)) {
            if (DEBUG) {
                Log.d(TAG, "onDeviceRemoved: unregister callback for " + preference);
            }
            mVolumeControl.unregisterCallback(mCallbackMap.get(preference));
            mCallbackMap.remove(preference);
        }
    }

    @Override
    public void updateVisibility() {
        if (mPreferenceGroup != null) {
            mPreferenceGroup.setVisible(false);
            if (mPreferenceGroup.getPreferenceCount() > 0) {
                super.updateVisibility();
            }
        }
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to provide the context and metrics category for {@link
     *     AudioSharingBluetoothDeviceUpdater} and provide the host for dialogs.
     */
    public void init(DashboardFragment fragment) {
        mBluetoothDeviceUpdater =
                new AudioSharingDeviceVolumeControlUpdater(
                        fragment.getContext(),
                        AudioSharingDeviceVolumeGroupController.this,
                        fragment.getMetricsCategory());
    }

    private BluetoothVolumeControl.Callback buildVcCallback(
            AudioSharingDeviceVolumePreference preference) {
        return new BluetoothVolumeControl.Callback() {
            @Override
            public void onVolumeOffsetChanged(BluetoothDevice device, int volumeOffset) {}

            @Override
            public void onDeviceVolumeChanged(
                    @android.annotation.NonNull BluetoothDevice device,
                    @IntRange(from = -255, to = 255) int volume) {
                CachedBluetoothDevice cachedDevice =
                        mLocalBtManager.getCachedDeviceManager().findDevice(device);
                if (cachedDevice == null) return;
                if (preference.getCachedDevice() != null
                        && preference.getCachedDevice().getGroupId() == cachedDevice.getGroupId()) {
                    // If the callback return invalid volume, try to get the volume from
                    // AudioManager.STREAM_MUSIC
                    int finalVolume = getAudioVolumeIfNeeded(volume);
                    Log.d(
                            TAG,
                            "onDeviceVolumeChanged: set volume to "
                                    + finalVolume
                                    + " for "
                                    + device.getAnonymizedAddress());
                    ThreadUtils.postOnMainThread(
                            () -> {
                                preference.setProgress(finalVolume);
                            });
                }
            }
        };
    }

    private int getAudioVolumeIfNeeded(int volume) {
        if (volume >= 0) return volume;
        try {
            AudioManager audioManager = mContext.getSystemService(AudioManager.class);
            int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int min = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC);
            return Math.round(
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 255f / (max - min));
        } catch (RuntimeException e) {
            Log.e(TAG, "Fail to fetch current music stream volume, error = " + e);
            return volume;
        }
    }
}
