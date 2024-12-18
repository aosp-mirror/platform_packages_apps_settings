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
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothVolumeControl;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
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
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioSharingDeviceVolumeGroupController extends AudioSharingBasePreferenceController
        implements DevicePreferenceCallback {
    private static final String TAG = "AudioSharingVolCtlr";
    private static final String KEY = "audio_sharing_device_volume_group";

    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final LocalBluetoothProfileManager mProfileManager;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mAssistant;
    @Nullable private final VolumeControlProfile mVolumeControl;
    @Nullable private final ContentResolver mContentResolver;
    @Nullable private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private final Executor mExecutor;
    private final ContentObserver mSettingsObserver;
    @Nullable private PreferenceGroup mPreferenceGroup;
    private List<AudioSharingDeviceVolumePreference> mVolumePreferences = new ArrayList<>();
    private Map<Integer, Integer> mValueMap = new HashMap<Integer, Integer>();
    private AtomicBoolean mCallbacksRegistered = new AtomicBoolean(false);

    @VisibleForTesting
    BluetoothVolumeControl.Callback mVolumeControlCallback =
            new BluetoothVolumeControl.Callback() {
                @Override
                public void onDeviceVolumeChanged(
                        @NonNull BluetoothDevice device,
                        @IntRange(from = -255, to = 255) int volume) {
                    CachedBluetoothDevice cachedDevice =
                            mBtManager == null
                                    ? null
                                    : mBtManager.getCachedDeviceManager().findDevice(device);
                    if (cachedDevice == null) return;
                    int groupId = BluetoothUtils.getGroupId(cachedDevice);
                    mValueMap.put(groupId, volume);
                    for (AudioSharingDeviceVolumePreference preference : mVolumePreferences) {
                        if (preference.getCachedDevice() != null
                                && BluetoothUtils.getGroupId(preference.getCachedDevice())
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

    @VisibleForTesting
    BluetoothLeBroadcastAssistant.Callback mBroadcastAssistantCallback =
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
                public void onSourceAdded(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceAddFailed(
                        @NonNull BluetoothDevice sink,
                        @NonNull BluetoothLeBroadcastMetadata source,
                        int reason) {}

                @Override
                public void onSourceModified(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceModifyFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceRemoved(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {
                    Log.d(TAG, "onSourceRemoved: update volume list.");
                    if (mBluetoothDeviceUpdater != null) {
                        mBluetoothDeviceUpdater.forceUpdate();
                    }
                }

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onReceiveStateChanged(
                        @NonNull BluetoothDevice sink,
                        int sourceId,
                        @NonNull BluetoothLeBroadcastReceiveState state) {
                    if (BluetoothUtils.isConnected(state)) {
                        Log.d(TAG, "onReceiveStateChanged: synced, update volume list.");
                        if (mBluetoothDeviceUpdater != null) {
                            mBluetoothDeviceUpdater.forceUpdate();
                        }
                    }
                }
            };

    public AudioSharingDeviceVolumeGroupController(Context context) {
        super(context, KEY);
        mBtManager = Utils.getLocalBtManager(mContext);
        mProfileManager = mBtManager == null ? null : mBtManager.getProfileManager();
        mAssistant =
                mProfileManager == null
                        ? null
                        : mProfileManager.getLeAudioBroadcastAssistantProfile();
        mVolumeControl = mProfileManager == null ? null : mProfileManager.getVolumeControlProfile();
        mExecutor = Executors.newSingleThreadExecutor();
        mContentResolver = context.getContentResolver();
        mSettingsObserver = new SettingsObserver();
    }

    private class SettingsObserver extends ContentObserver {
        SettingsObserver() {
            super(new Handler(Looper.getMainLooper()));
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "onChange, fallback device group id has been changed");
            for (AudioSharingDeviceVolumePreference preference : mVolumePreferences) {
                preference.setOrder(getPreferenceOrderForDevice(preference.getCachedDevice()));
            }
        }
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        super.onStart(owner);
        registerCallbacks();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        super.onStop(owner);
        unregisterCallbacks();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        mVolumePreferences.clear();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceGroup = screen.findPreference(KEY);
        if (mPreferenceGroup != null) {
            mPreferenceGroup.setVisible(false);
        }

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
        if (mPreferenceGroup != null) {
            if (mPreferenceGroup.getPreferenceCount() == 0) {
                mPreferenceGroup.setVisible(true);
            }
            mPreferenceGroup.addPreference(preference);
        }
        if (preference instanceof AudioSharingDeviceVolumePreference) {
            var volumePref = (AudioSharingDeviceVolumePreference) preference;
            CachedBluetoothDevice cachedDevice = volumePref.getCachedDevice();
            volumePref.setOrder(getPreferenceOrderForDevice(cachedDevice));
            mVolumePreferences.add(volumePref);
            if (volumePref.getProgress() > 0) return;
            int volume = mValueMap.getOrDefault(BluetoothUtils.getGroupId(cachedDevice), -1);
            // If the volume is invalid, try to get the volume from AudioManager.STREAM_MUSIC
            int finalVolume = getAudioVolumeIfNeeded(volume);
            Log.d(
                    TAG,
                    "onDeviceAdded: set volume to "
                            + finalVolume
                            + " for "
                            + cachedDevice.getDevice().getAnonymizedAddress());
            AudioSharingUtils.postOnMainThread(mContext, () -> volumePref.setProgress(finalVolume));
        }
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        if (mPreferenceGroup != null) {
            mPreferenceGroup.removePreference(preference);
            if (mPreferenceGroup.getPreferenceCount() == 0) {
                mPreferenceGroup.setVisible(false);
            }
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
        if (mPreferenceGroup != null && mPreferenceGroup.getPreferenceCount() == 0) {
            mPreferenceGroup.setVisible(false);
            return;
        }
        super.updateVisibility();
    }

    @Override
    public void onAudioSharingProfilesConnected() {
        registerCallbacks();
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

    @VisibleForTesting
    void setDeviceUpdater(@Nullable AudioSharingDeviceVolumeControlUpdater updater) {
        mBluetoothDeviceUpdater = updater;
    }

    /** Test only: set callback registration status in tests. */
    @VisibleForTesting
    void setCallbacksRegistered(boolean registered) {
        mCallbacksRegistered.set(registered);
    }

    /** Test only: set volume map in tests. */
    @VisibleForTesting
    void setVolumeMap(@Nullable Map<Integer, Integer> map) {
        mValueMap.clear();
        mValueMap.putAll(map);
    }

    /** Test only: set value for private preferenceGroup in tests. */
    @VisibleForTesting
    void setPreferenceGroup(@Nullable PreferenceGroup group) {
        mPreferenceGroup = group;
        mPreference = group;
    }

    @VisibleForTesting
    ContentObserver getSettingsObserver() {
        return mSettingsObserver;
    }

    private void registerCallbacks() {
        if (!isAvailable()) {
            Log.d(TAG, "Skip registerCallbacks(). Feature is not available.");
            return;
        }
        if (mAssistant == null
                || mVolumeControl == null
                || mBluetoothDeviceUpdater == null
                || mContentResolver == null
                || !AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            Log.d(TAG, "Skip registerCallbacks(). Profile is not ready.");
            return;
        }
        if (!mCallbacksRegistered.get()) {
            Log.d(TAG, "registerCallbacks()");
            mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
            mVolumeControl.registerCallback(mExecutor, mVolumeControlCallback);
            mBluetoothDeviceUpdater.registerCallback();
            mContentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(BluetoothUtils.getPrimaryGroupIdUriForBroadcast()),
                    false,
                    mSettingsObserver);
            mCallbacksRegistered.set(true);
        }
    }

    private void unregisterCallbacks() {
        if (!isAvailable()) {
            Log.d(TAG, "Skip unregister callbacks. Feature is not available.");
            return;
        }
        if (mAssistant == null
                || mVolumeControl == null
                || mBluetoothDeviceUpdater == null
                || mContentResolver == null
                || !AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            Log.d(TAG, "Skip unregisterCallbacks(). Profile is not ready.");
            return;
        }
        if (mCallbacksRegistered.get()) {
            Log.d(TAG, "unregisterCallbacks()");
            mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
            mVolumeControl.unregisterCallback(mVolumeControlCallback);
            mBluetoothDeviceUpdater.unregisterCallback();
            mContentResolver.unregisterContentObserver(mSettingsObserver);
            mValueMap.clear();
            mCallbacksRegistered.set(false);
        }
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

    private int getPreferenceOrderForDevice(@NonNull CachedBluetoothDevice cachedDevice) {
        int groupId = BluetoothUtils.getGroupId(cachedDevice);
        // The fallback device rank first among the audio sharing device list.
        return (groupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID
                && groupId == BluetoothUtils.getPrimaryGroupIdForBroadcast(mContentResolver))
                ? 0
                : 1;
    }
}
