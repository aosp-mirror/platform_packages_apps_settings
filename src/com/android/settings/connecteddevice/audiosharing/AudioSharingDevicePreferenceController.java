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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingDevicePreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver, DevicePreferenceCallback, BluetoothCallback {

    private static final String TAG = "AudioSharingDevicePrefController";
    private static final String KEY = "audio_sharing_device_list";
    private static final String KEY_AUDIO_SHARING_SETTINGS =
            "connected_device_audio_sharing_settings";

    private final LocalBluetoothManager mLocalBtManager;
    private final LocalBluetoothLeBroadcast mBroadcast;
    private final LocalBluetoothLeBroadcastAssistant mAssistant;
    private final Executor mExecutor;
    private PreferenceGroup mPreferenceGroup;
    private Preference mAudioSharingSettingsPreference;
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    private DashboardFragment mFragment;

    private final BluetoothLeBroadcast.Callback mBroadcastCallback =
            new BluetoothLeBroadcast.Callback() {
                @Override
                public void onBroadcastStarted(int reason, int broadcastId) {}

                @Override
                public void onBroadcastStartFailed(int reason) {}

                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata) {}

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {}

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

    public AudioSharingDevicePreferenceController(Context context) {
        super(context, KEY);
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        mBroadcast = mLocalBtManager.getProfileManager().getLeAudioBroadcastProfile();
        mAssistant = mLocalBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mLocalBtManager == null) {
            Log.d(TAG, "onStart() Bluetooth is not supported on this device");
            return;
        }
        if (mBroadcast == null || mAssistant == null) {
            Log.d(TAG, "onStart() Broadcast or assistant is not supported on this device");
            return;
        }
        if (mBluetoothDeviceUpdater == null) {
            Log.d(TAG, "onStart() Bluetooth device updater is not initialized");
            return;
        }
        mLocalBtManager.getEventManager().registerCallback(this);
        mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
        mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        mBluetoothDeviceUpdater.registerCallback();
        mBluetoothDeviceUpdater.refreshPreference();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mLocalBtManager == null) {
            Log.d(TAG, "onStop() Bluetooth is not supported on this device");
            return;
        }
        if (mBroadcast == null || mAssistant == null) {
            Log.d(TAG, "onStop() Broadcast or assistant is not supported on this device");
            return;
        }
        if (mBluetoothDeviceUpdater == null) {
            Log.d(TAG, "onStop() Bluetooth device updater is not initialized");
            return;
        }
        mLocalBtManager.getEventManager().unregisterCallback(this);
        // TODO: verify the reason for failing to unregister
        try {
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
            mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        } catch (IllegalArgumentException e) {
            Log.e(
                    TAG,
                    "Fail to unregister broadcast or assistant callback due to " + e.getMessage());
        }
        mBluetoothDeviceUpdater.unregisterCallback();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceGroup = screen.findPreference(KEY);
        mAudioSharingSettingsPreference =
                mPreferenceGroup.findPreference(KEY_AUDIO_SHARING_SETTINGS);
        mPreferenceGroup.setVisible(false);
        mAudioSharingSettingsPreference.setVisible(false);

        if (isAvailable()) {
            mBluetoothDeviceUpdater.setPrefContext(screen.getContext());
            mBluetoothDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                        && Flags.enableLeAudioSharing()
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceGroup.getPreferenceCount() == 1) {
            mPreferenceGroup.setVisible(true);
            mAudioSharingSettingsPreference.setVisible(true);
        }
        mPreferenceGroup.addPreference(preference);
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        mPreferenceGroup.removePreference(preference);
        if (mPreferenceGroup.getPreferenceCount() == 1) {
            mPreferenceGroup.setVisible(false);
            mAudioSharingSettingsPreference.setVisible(false);
        }
    }

    @Override
    public void onProfileConnectionStateChanged(
            @NonNull CachedBluetoothDevice cachedDevice,
            @ConnectionState int state,
            int bluetoothProfile) {
        if (state != BluetoothAdapter.STATE_CONNECTED || !cachedDevice.getDevice().isConnected()) {
            Log.d(TAG, "Ignore onProfileConnectionStateChanged, not connected state");
            return;
        }
        if (mFragment == null) {
            Log.d(TAG, "Ignore onProfileConnectionStateChanged, no host fragment");
            return;
        }
        if (mAssistant == null && mBroadcast == null) {
            Log.d(
                    TAG,
                    "Ignore onProfileConnectionStateChanged, no broadcast or assistant supported");
            return;
        }
        boolean isLeAudioSupported = isLeAudioSupported(cachedDevice);
        // For eligible (LE audio) remote device, we only check its connected LE audio profile.
        if (isLeAudioSupported && bluetoothProfile != BluetoothProfile.LE_AUDIO) {
            Log.d(
                    TAG,
                    "Ignore onProfileConnectionStateChanged, not the le profile for le audio"
                        + " device");
            return;
        }
        boolean isFirstConnectedProfile = isFirstConnectedProfile(cachedDevice, bluetoothProfile);
        // For ineligible (non LE audio) remote device, we only check its first connected profile.
        if (!isLeAudioSupported && !isFirstConnectedProfile) {
            Log.d(
                    TAG,
                    "Ignore onProfileConnectionStateChanged, not the first connected profile for"
                        + " non le audio device");
            return;
        }
        if (!isLeAudioSupported) {
            // Handle connected ineligible (non LE audio) remote device
            if (isBroadcasting()) {
                // Show stop audio sharing dialog when an ineligible (non LE audio) remote device
                // connected during a sharing session.
                AudioSharingStopDialogFragment.show(
                        mFragment,
                        cachedDevice.getName(),
                        () -> {
                            mBroadcast.stopBroadcast(mBroadcast.getLatestBroadcastId());
                        });
            }
            // Do nothing for ineligible (non LE audio) remote device when no sharing session.
        } else {
            // Handle connected eligible (LE audio) remote device
            if (isBroadcasting()) {
                // Show audio sharing switch or join dialog according to device count in the sharing
                // session.
                Map<Integer, List<CachedBluetoothDevice>> groupedDevices =
                        fetchConnectedDevicesByGroupId();
                ArrayList<AudioSharingDeviceItem> deviceItems =
                        buildDeviceItemsInSharingSession(groupedDevices);
                // Show switch audio sharing dialog when the third eligible (LE audio) remote device
                // connected during a sharing session.
                if (deviceItems.size() >= 2) {
                    AudioSharingDisconnectDialogFragment.show(
                            mFragment,
                            deviceItems,
                            cachedDevice.getName(),
                            (AudioSharingDeviceItem item) -> {
                                // Remove all sources from the device user clicked
                                for (CachedBluetoothDevice device :
                                        groupedDevices.get(item.getGroupId())) {
                                    for (BluetoothLeBroadcastReceiveState source :
                                            mAssistant.getAllSources(device.getDevice())) {
                                        mAssistant.removeSource(
                                                device.getDevice(), source.getSourceId());
                                    }
                                }
                                // Add current broadcast to the latest connected device
                                mAssistant.addSource(
                                        cachedDevice.getDevice(),
                                        mBroadcast.getLatestBluetoothLeBroadcastMetadata(),
                                        /* isGroupOp= */ true);
                            });
                } else {
                    // TODO: show dialog to add device to sharing session.
                }
            } else {
                // Show audio sharing join dialog when no sharing session.
                // TODO: show dialog to add device to sharing session.
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
        mFragment = fragment;
        mBluetoothDeviceUpdater =
                new AudioSharingBluetoothDeviceUpdater(
                        fragment.getContext(),
                        AudioSharingDevicePreferenceController.this,
                        fragment.getMetricsCategory());
    }

    private boolean isLeAudioSupported(CachedBluetoothDevice cachedDevice) {
        return cachedDevice.getProfiles().stream()
                .anyMatch(
                        profile ->
                                profile instanceof LeAudioProfile
                                        && profile.isEnabled(cachedDevice.getDevice()));
    }

    private boolean isFirstConnectedProfile(
            CachedBluetoothDevice cachedDevice, int bluetoothProfile) {
        return cachedDevice.getProfiles().stream()
                .noneMatch(
                        profile ->
                                profile.getProfileId() != bluetoothProfile
                                        && profile.getConnectionStatus(cachedDevice.getDevice())
                                                == BluetoothProfile.STATE_CONNECTED);
    }

    private boolean isBroadcasting() {
        return mBroadcast != null && mBroadcast.isEnabled(null);
    }

    private Map<Integer, List<CachedBluetoothDevice>> fetchConnectedDevicesByGroupId() {
        // TODO: filter out devices with le audio disabled.
        List<BluetoothDevice> connectedDevices =
                mAssistant == null ? ImmutableList.of() : mAssistant.getConnectedDevices();
        Map<Integer, List<CachedBluetoothDevice>> groupedDevices = new HashMap<>();
        CachedBluetoothDeviceManager cacheManager = mLocalBtManager.getCachedDeviceManager();
        for (BluetoothDevice device : connectedDevices) {
            CachedBluetoothDevice cachedDevice = cacheManager.findDevice(device);
            if (cachedDevice == null) {
                Log.d(TAG, "Skip device due to not being cached: " + device.getAnonymizedAddress());
                continue;
            }
            int groupId = cachedDevice.getGroupId();
            if (groupId == BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
                Log.d(
                        TAG,
                        "Skip device due to no valid group id: " + device.getAnonymizedAddress());
                continue;
            }
            if (!groupedDevices.containsKey(groupId)) {
                groupedDevices.put(groupId, new ArrayList<>());
            }
            groupedDevices.get(groupId).add(cachedDevice);
        }
        return groupedDevices;
    }

    private ArrayList<AudioSharingDeviceItem> buildDeviceItemsInSharingSession(
            Map<Integer, List<CachedBluetoothDevice>> groupedDevices) {
        ArrayList<AudioSharingDeviceItem> deviceItems = new ArrayList<>();
        for (List<CachedBluetoothDevice> devices : groupedDevices.values()) {
            for (CachedBluetoothDevice device : devices) {
                List<BluetoothLeBroadcastReceiveState> sourceList =
                        mAssistant.getAllSources(device.getDevice());
                if (!sourceList.isEmpty()) {
                    // Use random device in the group within the sharing session to
                    // represent the group.
                    deviceItems.add(
                            new AudioSharingDeviceItem(device.getName(), device.getGroupId()));
                    break;
                }
            }
        }
        return deviceItems;
    }
}
