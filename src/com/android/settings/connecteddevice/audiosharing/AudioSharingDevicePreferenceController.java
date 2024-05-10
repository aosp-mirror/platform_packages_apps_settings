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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
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
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingDevicePreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver, DevicePreferenceCallback, BluetoothCallback {
    private static final boolean DEBUG = BluetoothUtils.D;

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
    private List<BluetoothDevice> mTargetSinks = new ArrayList<>();

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
                }

                @Override
                public void onBroadcastStartFailed(int reason) {
                    Log.d(TAG, "onBroadcastStartFailed(), reason = " + reason);
                    // TODO: handle broadcast start fail
                }

                @Override
                public void onBroadcastMetadataChanged(
                        int broadcastId, @NonNull BluetoothLeBroadcastMetadata metadata) {
                    Log.d(
                            TAG,
                            "onBroadcastMetadataChanged(), broadcastId = "
                                    + broadcastId
                                    + ", metadata = "
                                    + metadata);
                    addSourceToTargetDevices(mTargetSinks);
                    mTargetSinks = new ArrayList<>();
                }

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onBroadcastStopped(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                }

                @Override
                public void onBroadcastStopFailed(int reason) {
                    Log.d(TAG, "onBroadcastStopFailed(), reason = " + reason);
                    // TODO: handle broadcast stop fail
                }

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
                    AudioSharingUtils.updateActiveDeviceIfNeeded(mLocalBtManager);
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
                    AudioSharingUtils.toastMessage(
                            mContext,
                            String.format(
                                    Locale.US,
                                    "Fail to add source to %s reason %d",
                                    sink.getAddress(),
                                    reason));
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
                    AudioSharingUtils.updateActiveDeviceIfNeeded(mLocalBtManager);
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
                    AudioSharingUtils.toastMessage(
                            mContext,
                            String.format(
                                    Locale.US,
                                    "Fail to remove source from %s reason %d",
                                    sink.getAddress(),
                                    reason));
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
        if (DEBUG) {
            Log.d(TAG, "onStart() Register callbacks for broadcast and assistant.");
        }
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
        if (DEBUG) {
            Log.d(TAG, "onStop() Unregister callbacks for broadcast and assistant.");
        }
        mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
        mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
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
        return AudioSharingUtils.isFeatureEnabled() && mBluetoothDeviceUpdater != null
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
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> handleOnProfileStateChanged(cachedDevice, bluetoothProfile));
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

    private void handleOnProfileStateChanged(
            @NonNull CachedBluetoothDevice cachedDevice, int bluetoothProfile) {
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
        if (DEBUG) {
            Log.d(
                    TAG,
                    "Start handling onProfileConnectionStateChanged for "
                            + cachedDevice.getDevice().getAnonymizedAddress());
        }
        if (!isLeAudioSupported) {
            // Handle connected ineligible (non LE audio) remote device
            handleOnProfileStateChangedForNonLeAudioDevice(cachedDevice);
        } else {
            // Handle connected eligible (LE audio) remote device
            handleOnProfileStateChangedForLeAudioDevice(cachedDevice);
        }
    }

    private void handleOnProfileStateChangedForNonLeAudioDevice(
            @NonNull CachedBluetoothDevice cachedDevice) {
        if (isBroadcasting()) {
            // Show stop audio sharing dialog when an ineligible (non LE audio) remote device
            // connected during a sharing session.
            ThreadUtils.postOnMainThread(
                    () -> {
                        closeOpeningDialogs();
                        AudioSharingStopDialogFragment.show(
                                mFragment,
                                cachedDevice.getName(),
                                () -> mBroadcast.stopBroadcast(mBroadcast.getLatestBroadcastId()));
                    });
        } else {
            // Do nothing for ineligible (non LE audio) remote device when no sharing session.
            if (DEBUG) {
                Log.d(
                        TAG,
                        "Ignore onProfileConnectionStateChanged for non LE audio without"
                                + " sharing session");
            }
        }
    }

    private void handleOnProfileStateChangedForLeAudioDevice(
            @NonNull CachedBluetoothDevice cachedDevice) {
        Map<Integer, List<CachedBluetoothDevice>> groupedDevices =
                AudioSharingUtils.fetchConnectedDevicesByGroupId(mLocalBtManager);
        if (isBroadcasting()) {
            if (groupedDevices.containsKey(cachedDevice.getGroupId())
                    && groupedDevices.get(cachedDevice.getGroupId()).stream()
                            .anyMatch(
                                    device ->
                                            AudioSharingUtils.hasBroadcastSource(
                                                    device, mLocalBtManager))) {
                Log.d(
                        TAG,
                        "Automatically add another device within the same group to the sharing: "
                                + cachedDevice.getDevice().getAnonymizedAddress());
                addSourceToTargetDevices(ImmutableList.of(cachedDevice.getDevice()));
                return;
            }
            // Show audio sharing switch or join dialog according to device count in the sharing
            // session.
            ArrayList<AudioSharingDeviceItem> deviceItemsInSharingSession =
                    AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                            mLocalBtManager, groupedDevices, /* filterByInSharing= */ true);
            // Show audio sharing switch dialog when the third eligible (LE audio) remote device
            // connected during a sharing session.
            if (deviceItemsInSharingSession.size() >= 2) {
                ThreadUtils.postOnMainThread(
                        () -> {
                            closeOpeningDialogs();
                            AudioSharingDisconnectDialogFragment.show(
                                    mFragment,
                                    deviceItemsInSharingSession,
                                    cachedDevice.getName(),
                                    (AudioSharingDeviceItem item) -> {
                                        // Remove all sources from the device user clicked
                                        if (groupedDevices.containsKey(item.getGroupId())) {
                                            for (CachedBluetoothDevice device :
                                                    groupedDevices.get(item.getGroupId())) {
                                                for (BluetoothLeBroadcastReceiveState source :
                                                        mAssistant.getAllSources(
                                                                device.getDevice())) {
                                                    mAssistant.removeSource(
                                                            device.getDevice(),
                                                            source.getSourceId());
                                                }
                                            }
                                        }
                                        // Add current broadcast to the latest connected device
                                        mAssistant.addSource(
                                                cachedDevice.getDevice(),
                                                mBroadcast.getLatestBluetoothLeBroadcastMetadata(),
                                                /* isGroupOp= */ true);
                                    });
                        });
            } else {
                // Show audio sharing join dialog when the first or second eligible (LE audio)
                // remote device connected during a sharing session.
                ThreadUtils.postOnMainThread(
                        () -> {
                            closeOpeningDialogs();
                            AudioSharingJoinDialogFragment.show(
                                    mFragment,
                                    deviceItemsInSharingSession,
                                    cachedDevice.getName(),
                                    () -> {
                                        // Add current broadcast to the latest connected device
                                        mAssistant.addSource(
                                                cachedDevice.getDevice(),
                                                mBroadcast.getLatestBluetoothLeBroadcastMetadata(),
                                                /* isGroupOp= */ true);
                                    });
                        });
            }
        } else {
            ArrayList<AudioSharingDeviceItem> deviceItems = new ArrayList<>();
            for (List<CachedBluetoothDevice> devices : groupedDevices.values()) {
                // Use random device in the group within the sharing session to represent the group.
                CachedBluetoothDevice device = devices.get(0);
                if (device.getGroupId() == cachedDevice.getGroupId()) {
                    continue;
                }
                deviceItems.add(AudioSharingUtils.buildAudioSharingDeviceItem(device));
            }
            // Show audio sharing join dialog when the second eligible (LE audio) remote
            // device connect and no sharing session.
            if (deviceItems.size() == 1) {
                ThreadUtils.postOnMainThread(
                        () -> {
                            closeOpeningDialogs();
                            AudioSharingJoinDialogFragment.show(
                                    mFragment,
                                    deviceItems,
                                    cachedDevice.getName(),
                                    () -> {
                                        mTargetSinks = new ArrayList<>();
                                        for (List<CachedBluetoothDevice> devices :
                                                groupedDevices.values()) {
                                            for (CachedBluetoothDevice device : devices) {
                                                mTargetSinks.add(device.getDevice());
                                            }
                                        }
                                        mBroadcast.startBroadcast("test", null);
                                    });
                        });
            }
        }
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

    private void addSourceToTargetDevices(List<BluetoothDevice> sinks) {
        if (sinks.isEmpty() || mBroadcast == null || mAssistant == null) {
            Log.d(TAG, "Skip adding source to target.");
            return;
        }
        BluetoothLeBroadcastMetadata broadcastMetadata =
                mBroadcast.getLatestBluetoothLeBroadcastMetadata();
        if (broadcastMetadata == null) {
            Log.e(TAG, "Error: There is no broadcastMetadata.");
            return;
        }
        for (BluetoothDevice sink : sinks) {
            Log.d(
                    TAG,
                    "Add broadcast with broadcastId: "
                            + broadcastMetadata.getBroadcastId()
                            + "to the device: "
                            + sink.getAnonymizedAddress());
            mAssistant.addSource(sink, broadcastMetadata, /* isGroupOp= */ false);
        }
    }

    private void closeOpeningDialogs() {
        if (mFragment == null) return;
        List<Fragment> fragments = mFragment.getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment) {
                Log.d(TAG, "Remove staled opening dialog " + fragment.getTag());
                ((DialogFragment) fragment).dismiss();
            }
        }
    }
}
