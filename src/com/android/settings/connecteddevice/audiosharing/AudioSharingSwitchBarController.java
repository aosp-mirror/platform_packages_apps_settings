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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class AudioSharingSwitchBarController extends BasePreferenceController
        implements DefaultLifecycleObserver, OnCheckedChangeListener {
    private static final String TAG = "AudioSharingSwitchBarCtl";
    private static final String PREF_KEY = "audio_sharing_main_switch";

    interface OnSwitchBarChangedListener {
        void onSwitchBarChanged();
    }

    private final SettingsMainSwitchBar mSwitchBar;
    private final BluetoothAdapter mBluetoothAdapter;
    private final LocalBluetoothManager mBtManager;
    private final LocalBluetoothLeBroadcast mBroadcast;
    private final LocalBluetoothLeBroadcastAssistant mAssistant;
    private final Executor mExecutor;
    private final OnSwitchBarChangedListener mListener;
    private DashboardFragment mFragment;
    private Map<Integer, List<CachedBluetoothDevice>> mGroupedConnectedDevices = new HashMap<>();
    private List<BluetoothDevice> mTargetActiveSinks = new ArrayList<>();
    private ArrayList<AudioSharingDeviceItem> mDeviceItemsForSharing = new ArrayList<>();
    @VisibleForTesting IntentFilter mIntentFilter;

    @VisibleForTesting
    BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
                    int adapterState =
                            intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothDevice.ERROR);
                    mSwitchBar.setChecked(isBroadcasting());
                    mSwitchBar.setEnabled(adapterState == BluetoothAdapter.STATE_ON);
                    mListener.onSwitchBarChanged();
                }
            };

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
                                    + metadata.getBroadcastName());
                    addSourceToTargetSinks(mTargetActiveSinks);
                    if (mFragment == null) {
                        Log.w(TAG, "Dialog fail to show due to null fragment.");
                        return;
                    }
                    ThreadUtils.postOnMainThread(
                            () -> {
                                AudioSharingDialogFragment.show(
                                        mFragment,
                                        mDeviceItemsForSharing,
                                        item -> {
                                            addSourceToTargetSinks(
                                                    mGroupedConnectedDevices
                                                            .getOrDefault(
                                                                    item.getGroupId(),
                                                                    ImmutableList.of())
                                                            .stream()
                                                            .map(CachedBluetoothDevice::getDevice)
                                                            .collect(Collectors.toList()));
                                        });
                            });
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
                    AudioSharingUtils.updateActiveDeviceIfNeeded(mBtManager);
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
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBtManager = Utils.getLocalBtManager(context);
        mBroadcast = mBtManager.getProfileManager().getLeAudioBroadcastProfile();
        mAssistant = mBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mSwitchBar.addOnSwitchChangeListener(this);
        mContext.registerReceiver(mReceiver, mIntentFilter, Context.RECEIVER_EXPORTED_UNAUDITED);
        if (mBroadcast != null) {
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
        }
        if (mAssistant != null) {
            mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
        }
        if (isAvailable()) {
            mSwitchBar.setChecked(isBroadcasting());
            mSwitchBar.setEnabled(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled());
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mSwitchBar.removeOnSwitchChangeListener(this);
        mContext.unregisterReceiver(mReceiver);
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
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
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
        mGroupedConnectedDevices = AudioSharingUtils.fetchConnectedDevicesByGroupId(mBtManager);
        ArrayList<AudioSharingDeviceItem> deviceItems =
                AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                        mBtManager, mGroupedConnectedDevices, /* filterByInSharing= */ false);
        // deviceItems is ordered. The active device is the first place if exits.
        mDeviceItemsForSharing = new ArrayList<>(deviceItems);
        if (!deviceItems.isEmpty() && deviceItems.get(0).isActive()) {
            for (CachedBluetoothDevice device :
                    mGroupedConnectedDevices.getOrDefault(
                            deviceItems.get(0).getGroupId(), ImmutableList.of())) {
                // If active device exists for audio sharing, share to it
                // automatically once the broadcast is started.
                mTargetActiveSinks.add(device.getDevice());
            }
            mDeviceItemsForSharing.remove(0);
        }
        // TODO: start broadcast with new API
        mBroadcast.startBroadcast("test", null);
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
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            boolean isBroadcasting = isBroadcasting();
                            ThreadUtils.postOnMainThread(
                                    () -> {
                                        if (mSwitchBar.isChecked() != isBroadcasting) {
                                            mSwitchBar.setChecked(isBroadcasting);
                                        }
                                        mSwitchBar.setEnabled(true);
                                        mListener.onSwitchBarChanged();
                                    });
                        });
    }

    private boolean isBroadcasting() {
        return mBroadcast != null && mBroadcast.isEnabled(null);
    }

    private void addSourceToTargetSinks(List<BluetoothDevice> sinks) {
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
