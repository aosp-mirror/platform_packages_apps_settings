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

import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.flags.Flags;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioSharingSwitchBarController extends BasePreferenceController
        implements DefaultLifecycleObserver, OnCheckedChangeListener {
    private static final String TAG = "AudioSharingSwitchBarCtl";
    private static final String PREF_KEY = "audio_sharing_main_switch";

    interface OnSwitchBarChangedListener {
        void onSwitchBarChanged(boolean newState);
    }

    private final SettingsMainSwitchBar mSwitchBar;
    private final LocalBluetoothManager mBtManager;
    private final LocalBluetoothLeBroadcast mBroadcast;
    private final LocalBluetoothLeBroadcastAssistant mAssistant;
    private final Executor mExecutor;
    private final OnSwitchBarChangedListener mListener;
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
                    updateSwitch();
                }

                @Override
                public void onBroadcastStartFailed(int reason) {
                    Log.d(TAG, "onBroadcastStartFailed(), reason = " + reason);
                    // TODO: handle broadcast start fail
                    updateSwitch();
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
                }

                @Override
                public void onBroadcastStopped(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onBroadcastStopped(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    updateSwitch();
                }

                @Override
                public void onBroadcastStopFailed(int reason) {
                    Log.d(TAG, "onBroadcastStopFailed(), reason = " + reason);
                    // TODO: handle broadcast stop fail
                    updateSwitch();
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
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onSourceRemoveFailed(
                        @NonNull BluetoothDevice sink, int sourceId, int reason) {}

                @Override
                public void onReceiveStateChanged(
                        BluetoothDevice sink,
                        int sourceId,
                        BluetoothLeBroadcastReceiveState state) {}
            };

    AudioSharingSwitchBarController(
            Context context, SettingsMainSwitchBar switchBar, OnSwitchBarChangedListener listener) {
        super(context, PREF_KEY);
        mSwitchBar = switchBar;
        mListener = listener;
        mBtManager = Utils.getLocalBtManager(context);
        mBroadcast = mBtManager.getProfileManager().getLeAudioBroadcastProfile();
        mAssistant = mBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        mExecutor = Executors.newSingleThreadExecutor();
        mSwitchBar.setChecked(isBroadcasting());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mSwitchBar.addOnSwitchChangeListener(this);
        if (mBroadcast != null) {
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
        }
        if (mAssistant != null) {
            mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mSwitchBar.removeOnSwitchChangeListener(this);
        if (mBroadcast != null) {
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
        }
        if (mAssistant != null) {
            mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Filter out unnecessary callbacks when switch is disabled.
        if (!buttonView.isEnabled()) return;
        if (isChecked) {
            startAudioSharing();
        } else {
            stopAudioSharing();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableLeAudioSharing() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to host the {@link AudioSharingSwitchBarController} dialog.
     */
    public void init(DashboardFragment fragment) {
        this.mFragment = fragment;
    }

    private void startAudioSharing() {
        mSwitchBar.setEnabled(false);
        if (mBroadcast == null || isBroadcasting()) {
            Log.d(TAG, "Already in broadcasting or broadcast not support, ignore!");
            mSwitchBar.setEnabled(true);
            return;
        }
        if (mFragment == null) {
            Log.w(TAG, "Dialog fail to show due to null fragment.");
            mSwitchBar.setEnabled(true);
            return;
        }
        Map<Integer, List<CachedBluetoothDevice>> groupedDevices = fetchConnectedDevicesByGroupId();
        ArrayList<AudioSharingDeviceItem> deviceItems = new ArrayList<>();
        Optional<Integer> activeGroupId = Optional.empty();
        for (List<CachedBluetoothDevice> devices : groupedDevices.values()) {
            // Use random device in the group to represent the group.
            CachedBluetoothDevice device = devices.get(0);
            // TODO: add BluetoothUtils.isActiveLeAudioDevice to avoid directly using isActiveDevice
            if (device.isActiveDevice(BluetoothProfile.LE_AUDIO)) {
                activeGroupId = Optional.of(device.getGroupId());
            } else {
                AudioSharingDeviceItem item =
                        new AudioSharingDeviceItem(device.getName(), device.getGroupId());
                deviceItems.add(item);
            }
        }
        mTargetSinks = new ArrayList<>();
        activeGroupId.ifPresent(
                gId -> {
                    if (groupedDevices.containsKey(gId)) {
                        for (CachedBluetoothDevice device : groupedDevices.get(gId)) {
                            mTargetSinks.add(device.getDevice());
                        }
                    }
                });
        AudioSharingDialogFragment.show(
                mFragment,
                deviceItems,
                new AudioSharingDialogFragment.DialogEventListener() {
                    @Override
                    public void onItemClick(AudioSharingDeviceItem item) {
                        if (groupedDevices.containsKey(item.getGroupId())) {
                            for (CachedBluetoothDevice device :
                                    groupedDevices.get(item.getGroupId())) {
                                mTargetSinks.add(device.getDevice());
                            }
                        }
                        // TODO: handle app source name for broadcasting.
                        mBroadcast.startBroadcast("test", /* language= */ null);
                    }

                    @Override
                    public void onCancelClick() {
                        // TODO: handle app source name for broadcasting.
                        mBroadcast.startBroadcast("test", /* language= */ null);
                    }
                });
    }

    private void stopAudioSharing() {
        mSwitchBar.setEnabled(false);
        if (mBroadcast == null || !isBroadcasting()) {
            Log.d(TAG, "Already not broadcasting or broadcast not support, ignore!");
            mSwitchBar.setEnabled(true);
            return;
        }
        mBroadcast.stopBroadcast(mBroadcast.getLatestBroadcastId());
    }

    private void updateSwitch() {
        ThreadUtils.postOnMainThread(
                () -> {
                    boolean isBroadcasting = isBroadcasting();
                    if (mSwitchBar.isChecked() != isBroadcasting) {
                        mSwitchBar.setChecked(isBroadcasting);
                    }
                    mSwitchBar.setEnabled(true);
                    mListener.onSwitchBarChanged(isBroadcasting);
                });
    }

    private boolean isBroadcasting() {
        return mBroadcast != null && mBroadcast.isEnabled(null);
    }

    private Map<Integer, List<CachedBluetoothDevice>> fetchConnectedDevicesByGroupId() {
        // TODO: filter out devices with le audio disabled.
        List<BluetoothDevice> connectedDevices =
                mAssistant == null ? ImmutableList.of() : mAssistant.getConnectedDevices();
        Map<Integer, List<CachedBluetoothDevice>> groupedDevices = new HashMap<>();
        CachedBluetoothDeviceManager cacheManager = mBtManager.getCachedDeviceManager();
        for (BluetoothDevice device : connectedDevices) {
            CachedBluetoothDevice cachedDevice = cacheManager.findDevice(device);
            if (cachedDevice == null) {
                Log.d(TAG, "Skip device due to not being cached: " + device.getAnonymizedAddress());
                continue;
            }
            int groupId = cachedDevice.getGroupId();
            if (groupId == BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
                Log.d(TAG, "Skip device due to no valid group id");
                continue;
            }
            if (!groupedDevices.containsKey(groupId)) {
                groupedDevices.put(groupId, new ArrayList<>());
            }
            groupedDevices.get(groupId).add(cachedDevice);
        }
        return groupedDevices;
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
}
