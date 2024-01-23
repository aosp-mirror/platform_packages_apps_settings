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
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingDeviceVolumeGroupController extends AudioSharingBasePreferenceController
        implements DevicePreferenceCallback {
    private static final String TAG = "AudioSharingDeviceVolumeGroupController";
    private static final String KEY = "audio_sharing_device_volume_group";

    private final LocalBluetoothManager mLocalBtManager;
    private final LocalBluetoothLeBroadcastAssistant mAssistant;
    private final Executor mExecutor;
    private VolumeControlProfile mVolumeControl;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private FragmentManager mFragmentManager;
    private PreferenceGroup mPreferenceGroup;
    private List<AudioSharingDeviceVolumePreference> mVolumePreferences = new ArrayList<>();
    private Map<Integer, Integer> mValueMap = new HashMap<Integer, Integer>();

    private BluetoothVolumeControl.Callback mVolumeControlCallback =
            new BluetoothVolumeControl.Callback() {
                @Override
                public void onVolumeOffsetChanged(
                        @NonNull BluetoothDevice device, int volumeOffset) {}

                @Override
                public void onDeviceVolumeChanged(
                        @NonNull BluetoothDevice device,
                        @IntRange(from = -255, to = 255) int volume) {
                    CachedBluetoothDevice cachedDevice =
                            mLocalBtManager.getCachedDeviceManager().findDevice(device);
                    if (cachedDevice == null) return;
                    int groupId = AudioSharingUtils.getGroupId(cachedDevice);
                    mValueMap.put(groupId, volume);
                    for (AudioSharingDeviceVolumePreference preference : mVolumePreferences) {
                        if (preference.getCachedDevice() != null
                                && AudioSharingUtils.getGroupId(preference.getCachedDevice())
                                        == groupId) {
                            // If the callback return invalid volume, try to
                            // get the volume from AudioManager.STREAM_MUSIC
                            int finalVolume = getAudioVolumeIfNeeded(volume);
                            Log.d(
                                    TAG,
                                    "onDeviceVolumeChanged: set volume to "
                                            + finalVolume
                                            + " for "
                                            + device.getAnonymizedAddress());
                            mContext.getMainExecutor()
                                    .execute(() -> preference.setProgress(finalVolume));
                            break;
                        }
                    }
                }
            };

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
        if (mVolumeControl != null) {
            Log.d(TAG, "onStart() Registered volume control callback");
            mVolumeControl.registerCallback(mExecutor, mVolumeControlCallback);
        }
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
        if (mVolumeControl != null) {
            Log.d(TAG, "onStop() Unregistered volume control callback");
            mVolumeControl.unregisterCallback(mVolumeControlCallback);
            mValueMap.clear();
        }
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        mVolumePreferences.clear();
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
        if (preference instanceof AudioSharingDeviceVolumePreference) {
            var volumePref = (AudioSharingDeviceVolumePreference) preference;
            mVolumePreferences.add(volumePref);
            if (volumePref.getProgress() > 0) return;
            CachedBluetoothDevice device = volumePref.getCachedDevice();
            if (device == null) return;
            int volume = mValueMap.getOrDefault(AudioSharingUtils.getGroupId(device), -1);
            // If the volume is invalid, try to get the volume from AudioManager.STREAM_MUSIC
            int finalVolume = getAudioVolumeIfNeeded(volume);
            Log.d(
                    TAG,
                    "onDeviceAdded: set volume to "
                            + finalVolume
                            + " for "
                            + device.getDevice().getAnonymizedAddress());
            mContext.getMainExecutor().execute(() -> volumePref.setProgress(finalVolume));
        }
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceGroup.removePreference(preference);
        if (mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(false);
        }
        if (preference instanceof AudioSharingDeviceVolumePreference) {
            var volumePref = (AudioSharingDeviceVolumePreference) preference;
            if (mVolumePreferences.contains(volumePref)) {
                mVolumePreferences.remove(volumePref);
            }
            CachedBluetoothDevice device = volumePref.getCachedDevice();
            Log.d(
                    TAG,
                    "onDeviceRemoved: "
                            + (device == null
                                    ? "null"
                                    : device.getDevice().getAnonymizedAddress()));
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
