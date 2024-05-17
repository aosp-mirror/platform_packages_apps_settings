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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class AudioSharingSwitchBarController extends BasePreferenceController
        implements DefaultLifecycleObserver,
                OnCheckedChangeListener,
                LocalBluetoothProfileManager.ServiceListener {
    private static final String TAG = "AudioSharingSwitchBarCtl";
    private static final String PREF_KEY = "audio_sharing_main_switch";

    interface OnAudioSharingStateChangedListener {
        /**
         * The callback which will be triggered when:
         *
         * <p>1. Bluetooth on/off state changes. 2. Broadcast and assistant profile
         * connect/disconnect state changes. 3. Audio sharing start/stop state changes.
         */
        void onAudioSharingStateChanged();

        /**
         * The callback which will be triggered when:
         *
         * <p>Broadcast and assistant profile connected.
         */
        void onAudioSharingProfilesConnected();
    }

    private final SettingsMainSwitchBar mSwitchBar;
    private final BluetoothAdapter mBluetoothAdapter;
    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final LocalBluetoothProfileManager mProfileManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mAssistant;
    @Nullable private DashboardFragment mFragment;
    private final Executor mExecutor;
    private final OnAudioSharingStateChangedListener mListener;
    private Map<Integer, List<CachedBluetoothDevice>> mGroupedConnectedDevices = new HashMap<>();
    private List<BluetoothDevice> mTargetActiveSinks = new ArrayList<>();
    private List<AudioSharingDeviceItem> mDeviceItemsForSharing = new ArrayList<>();
    @VisibleForTesting IntentFilter mIntentFilter;
    private AtomicBoolean mCallbacksRegistered = new AtomicBoolean(false);

    @VisibleForTesting
    BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updateSwitch();
                    mListener.onAudioSharingStateChanged();
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
                    mListener.onAudioSharingStateChanged();
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
                    mListener.onAudioSharingStateChanged();
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
                public void onPlaybackStarted(int reason, int broadcastId) {
                    Log.d(
                            TAG,
                            "onPlaybackStarted(), reason = "
                                    + reason
                                    + ", broadcastId = "
                                    + broadcastId);
                    handleOnBroadcastReady();
                }

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
            Context context,
            SettingsMainSwitchBar switchBar,
            OnAudioSharingStateChangedListener listener) {
        super(context, PREF_KEY);
        mSwitchBar = switchBar;
        mListener = listener;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBtManager = Utils.getLocalBtManager(context);
        mProfileManager = mBtManager == null ? null : mBtManager.getProfileManager();
        mBroadcast = mProfileManager == null ? null : mProfileManager.getLeAudioBroadcastProfile();
        mAssistant =
                mProfileManager == null
                        ? null
                        : mProfileManager.getLeAudioBroadcastAssistantProfile();
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip register callbacks. Feature is not available.");
            return;
        }
        mContext.registerReceiver(mReceiver, mIntentFilter, Context.RECEIVER_EXPORTED_UNAUDITED);
        updateSwitch();
        if (!AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            if (mProfileManager != null) {
                mProfileManager.addServiceListener(this);
            }
            Log.d(TAG, "Skip register callbacks. Profile is not ready.");
            return;
        }
        registerCallbacks();
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip unregister callbacks. Feature is not available.");
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        if (mProfileManager != null) {
            mProfileManager.removeServiceListener(this);
        }
        unregisterCallbacks();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Filter out unnecessary callbacks when switch is disabled.
        if (!buttonView.isEnabled()) return;
        if (isChecked) {
            mSwitchBar.setEnabled(false);
            boolean isBroadcasting = AudioSharingUtils.isBroadcasting(mBtManager);
            if (mAssistant == null || mBroadcast == null || isBroadcasting) {
                Log.d(TAG, "Skip startAudioSharing, already broadcasting or not support.");
                mSwitchBar.setEnabled(true);
                if (!isBroadcasting) {
                    mSwitchBar.setChecked(false);
                }
                return;
            }
            if (mAssistant
                    .getDevicesMatchingConnectionStates(
                            new int[] {BluetoothProfile.STATE_CONNECTED})
                    .isEmpty()) {
                // Pop up dialog to ask users to connect at least one lea buds before audio sharing.
                AudioSharingUtils.postOnMainThread(
                        mContext,
                        () -> {
                            mSwitchBar.setEnabled(true);
                            mSwitchBar.setChecked(false);
                            if (mFragment != null) {
                                AudioSharingConfirmDialogFragment.show(mFragment);
                            }
                        });
                return;
            }
            startAudioSharing();
        } else {
            stopAudioSharing();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onServiceConnected() {
        Log.d(TAG, "onServiceConnected()");
        if (AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            registerCallbacks();
            updateSwitch();
            mListener.onAudioSharingProfilesConnected();
            mListener.onAudioSharingStateChanged();
            if (mProfileManager != null) {
                mProfileManager.removeServiceListener(this);
            }
        }
    }

    @Override
    public void onServiceDisconnected() {
        Log.d(TAG, "onServiceDisconnected()");
        // Do nothing.
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to host the {@link AudioSharingSwitchBarController} dialog.
     */
    public void init(DashboardFragment fragment) {
        this.mFragment = fragment;
    }

    /** Test only: set callback registration status in tests. */
    @VisibleForTesting
    public void setCallbacksRegistered(boolean registered) {
        mCallbacksRegistered.set(registered);
    }

    private void registerCallbacks() {
        if (!isAvailable()) {
            Log.d(TAG, "Skip registerCallbacks(). Feature is not available.");
            return;
        }
        if (mBroadcast == null || mAssistant == null) {
            Log.d(TAG, "Skip registerCallbacks(). Profile not support on this device.");
            return;
        }
        if (!mCallbacksRegistered.get()) {
            Log.d(TAG, "registerCallbacks()");
            mSwitchBar.addOnSwitchChangeListener(this);
            mBroadcast.registerServiceCallBack(mExecutor, mBroadcastCallback);
            mAssistant.registerServiceCallBack(mExecutor, mBroadcastAssistantCallback);
            mCallbacksRegistered.set(true);
        }
    }

    private void unregisterCallbacks() {
        if (!isAvailable() || !AudioSharingUtils.isAudioSharingProfileReady(mProfileManager)) {
            Log.d(TAG, "Skip unregisterCallbacks(). Feature is not available.");
            return;
        }
        if (mBroadcast == null || mAssistant == null) {
            Log.d(TAG, "Skip unregisterCallbacks(). Profile not support on this device.");
            return;
        }
        if (mCallbacksRegistered.get()) {
            Log.d(TAG, "unregisterCallbacks()");
            mSwitchBar.removeOnSwitchChangeListener(this);
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
            mAssistant.unregisterServiceCallBack(mBroadcastAssistantCallback);
            mCallbacksRegistered.set(false);
        }
    }

    private void startAudioSharing() {
        // Compute the device connection state before start audio sharing since the devices will
        // be set to inactive after the broadcast started.
        mGroupedConnectedDevices = AudioSharingUtils.fetchConnectedDevicesByGroupId(mBtManager);
        List<AudioSharingDeviceItem> deviceItems =
                AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                        mBtManager, mGroupedConnectedDevices, /* filterByInSharing= */ false);
        // deviceItems is ordered. The active device is the first place if exits.
        mDeviceItemsForSharing = new ArrayList<>(deviceItems);
        mTargetActiveSinks = new ArrayList<>();
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
        if (mBroadcast != null) {
            mBroadcast.startPrivateBroadcast();
        }
    }

    private void stopAudioSharing() {
        mSwitchBar.setEnabled(false);
        if (!AudioSharingUtils.isBroadcasting(mBtManager)) {
            Log.d(TAG, "Skip stopAudioSharing, already not broadcasting or broadcast not support.");
            mSwitchBar.setEnabled(true);
            return;
        }
        if (mBroadcast != null) {
            mBroadcast.stopBroadcast(mBroadcast.getLatestBroadcastId());
        }
    }

    private void updateSwitch() {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            boolean isBroadcasting = AudioSharingUtils.isBroadcasting(mBtManager);
                            boolean isStateReady =
                                    isBluetoothOn()
                                            && AudioSharingUtils.isAudioSharingProfileReady(
                                                    mProfileManager);
                            AudioSharingUtils.postOnMainThread(
                                    mContext,
                                    () -> {
                                        if (mSwitchBar.isChecked() != isBroadcasting) {
                                            mSwitchBar.setChecked(isBroadcasting);
                                        }
                                        if (mSwitchBar.isEnabled() != isStateReady) {
                                            mSwitchBar.setEnabled(isStateReady);
                                        }
                                        Log.d(
                                                TAG,
                                                "updateSwitch, checked = "
                                                        + isBroadcasting
                                                        + ", enabled = "
                                                        + isStateReady);
                                    });
                        });
    }

    private boolean isBluetoothOn() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private void handleOnBroadcastReady() {
        AudioSharingUtils.addSourceToTargetSinks(mTargetActiveSinks, mBtManager);
        mTargetActiveSinks.clear();
        if (mFragment == null) {
            Log.w(TAG, "Dialog fail to show due to null fragment.");
            mGroupedConnectedDevices.clear();
            mDeviceItemsForSharing.clear();
            return;
        }
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> {
                    // Check nullability to pass NullAway check
                    if (mFragment != null) {
                        AudioSharingDialogFragment.show(
                                mFragment,
                                mDeviceItemsForSharing,
                                item -> {
                                    AudioSharingUtils.addSourceToTargetSinks(
                                            mGroupedConnectedDevices
                                                    .getOrDefault(
                                                            item.getGroupId(), ImmutableList.of())
                                                    .stream()
                                                    .map(CachedBluetoothDevice::getDevice)
                                                    .collect(Collectors.toList()),
                                            mBtManager);
                                    mGroupedConnectedDevices.clear();
                                    mDeviceItemsForSharing.clear();
                                });
                    }
                });
    }
}
