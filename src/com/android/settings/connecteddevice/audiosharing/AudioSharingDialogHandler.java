/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class AudioSharingDialogHandler {
    private static final String TAG = "AudioSharingDialogHandler";
    private final Context mContext;
    private final Fragment mHostFragment;
    @Nullable private final LocalBluetoothManager mLocalBtManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mAssistant;
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
                    AudioSharingUtils.toastMessage(
                            mContext, "Fail to start broadcast, reason " + reason);
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
                    AudioSharingUtils.toastMessage(
                            mContext, "Fail to stop broadcast, reason " + reason);
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
                    if (!mTargetSinks.isEmpty()) {
                        AudioSharingUtils.addSourceToTargetSinks(mTargetSinks, mLocalBtManager);
                        new SubSettingLauncher(mContext)
                                .setDestination(AudioSharingDashboardFragment.class.getName())
                                .setSourceMetricsCategory(
                                        (mHostFragment != null
                                                        && mHostFragment
                                                                instanceof DashboardFragment)
                                                ? ((DashboardFragment) mHostFragment)
                                                        .getMetricsCategory()
                                                : SettingsEnums.PAGE_UNKNOWN)
                                .launch();
                        mTargetSinks = new ArrayList<>();
                    }
                }

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

    public AudioSharingDialogHandler(@NonNull Context context, @NonNull Fragment fragment) {
        mContext = context;
        mHostFragment = fragment;
        mLocalBtManager = Utils.getLocalBluetoothManager(context);
        mBroadcast =
                mLocalBtManager != null
                        ? mLocalBtManager.getProfileManager().getLeAudioBroadcastProfile()
                        : null;
        mAssistant =
                mLocalBtManager != null
                        ? mLocalBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile()
                        : null;
    }

    /** Register callbacks for dialog handler */
    public void registerCallbacks(Executor executor) {
        if (mBroadcast != null) {
            mBroadcast.registerServiceCallBack(executor, mBroadcastCallback);
        }
    }

    /** Unregister callbacks for dialog handler */
    public void unregisterCallbacks() {
        if (mBroadcast != null) {
            mBroadcast.unregisterServiceCallBack(mBroadcastCallback);
        }
    }

    /** Handle dialog pop-up logic when device is connected. */
    public void handleDeviceConnected(
            @NonNull CachedBluetoothDevice cachedDevice, boolean userTriggered) {
        String anonymizedAddress = cachedDevice.getDevice().getAnonymizedAddress();
        boolean isBroadcasting = isBroadcasting();
        boolean isLeAudioSupported = AudioSharingUtils.isLeAudioSupported(cachedDevice);
        if (!isLeAudioSupported) {
            Log.d(TAG, "Handle non LE audio device connected, device = " + anonymizedAddress);
            // Handle connected ineligible (non LE audio) remote device
            handleNonLeAudioDeviceConnected(cachedDevice, isBroadcasting, userTriggered);
        } else {
            Log.d(TAG, "Handle LE audio device connected, device = " + anonymizedAddress);
            // Handle connected eligible (LE audio) remote device
            handleLeAudioDeviceConnected(cachedDevice, isBroadcasting, userTriggered);
        }
    }

    private void handleNonLeAudioDeviceConnected(
            @NonNull CachedBluetoothDevice cachedDevice,
            boolean isBroadcasting,
            boolean userTriggered) {
        if (isBroadcasting) {
            // Show stop audio sharing dialog when an ineligible (non LE audio) remote device
            // connected during a sharing session.
            Map<Integer, List<CachedBluetoothDevice>> groupedDevices =
                    AudioSharingUtils.fetchConnectedDevicesByGroupId(mLocalBtManager);
            List<AudioSharingDeviceItem> deviceItemsInSharingSession =
                    AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                            mLocalBtManager, groupedDevices, /* filterByInSharing= */ true);
            postOnMainThread(
                    () -> {
                        closeOpeningDialogsOtherThan(AudioSharingStopDialogFragment.tag());
                        AudioSharingStopDialogFragment.show(
                                mHostFragment,
                                deviceItemsInSharingSession,
                                cachedDevice,
                                () -> {
                                    cachedDevice.setActive();
                                    AudioSharingUtils.stopBroadcasting(mLocalBtManager);
                                });
                    });
        } else {
            if (userTriggered) {
                cachedDevice.setActive();
            }
            // Do nothing for ineligible (non LE audio) remote device when no sharing session.
            Log.d(
                    TAG,
                    "Ignore onProfileConnectionStateChanged for non LE audio without"
                            + " sharing session");
        }
    }

    private void handleLeAudioDeviceConnected(
            @NonNull CachedBluetoothDevice cachedDevice,
            boolean isBroadcasting,
            boolean userTriggered) {
        Map<Integer, List<CachedBluetoothDevice>> groupedDevices =
                AudioSharingUtils.fetchConnectedDevicesByGroupId(mLocalBtManager);
        if (isBroadcasting) {
            // If another device within the same is already in the sharing session, add source to
            // the device automatically.
            int groupId = AudioSharingUtils.getGroupId(cachedDevice);
            if (groupedDevices.containsKey(groupId)
                    && groupedDevices.get(groupId).stream()
                            .anyMatch(
                                    device ->
                                            BluetoothUtils.hasConnectedBroadcastSource(
                                                    device, mLocalBtManager))) {
                Log.d(
                        TAG,
                        "Automatically add another device within the same group to the sharing: "
                                + cachedDevice.getDevice().getAnonymizedAddress());
                if (mAssistant != null && mBroadcast != null) {
                    mAssistant.addSource(
                            cachedDevice.getDevice(),
                            mBroadcast.getLatestBluetoothLeBroadcastMetadata(),
                            /* isGroupOp= */ false);
                }
                return;
            }

            // Show audio sharing switch or join dialog according to device count in the sharing
            // session.
            List<AudioSharingDeviceItem> deviceItemsInSharingSession =
                    AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                            mLocalBtManager, groupedDevices, /* filterByInSharing= */ true);
            // Show audio sharing switch dialog when the third eligible (LE audio) remote device
            // connected during a sharing session.
            if (deviceItemsInSharingSession.size() >= 2) {
                postOnMainThread(
                        () -> {
                            closeOpeningDialogsOtherThan(
                                    AudioSharingDisconnectDialogFragment.tag());
                            AudioSharingDisconnectDialogFragment.show(
                                    mHostFragment,
                                    deviceItemsInSharingSession,
                                    cachedDevice,
                                    (AudioSharingDeviceItem item) -> {
                                        // Remove all sources from the device user clicked
                                        removeSourceForGroup(item.getGroupId(), groupedDevices);
                                        // Add current broadcast to the latest connected device
                                        addSourceForGroup(groupId, groupedDevices);
                                    });
                        });
            } else {
                // Show audio sharing join dialog when the first or second eligible (LE audio)
                // remote device connected during a sharing session.
                postOnMainThread(
                        () -> {
                            closeOpeningDialogsOtherThan(AudioSharingJoinDialogFragment.tag());
                            AudioSharingJoinDialogFragment.show(
                                    mHostFragment,
                                    deviceItemsInSharingSession,
                                    cachedDevice,
                                    new AudioSharingJoinDialogFragment.DialogEventListener() {
                                        @Override
                                        public void onShareClick() {
                                            addSourceForGroup(groupId, groupedDevices);
                                        }

                                        @Override
                                        public void onCancelClick() {}
                                    });
                        });
            }
        } else {
            List<AudioSharingDeviceItem> deviceItems = new ArrayList<>();
            for (List<CachedBluetoothDevice> devices : groupedDevices.values()) {
                // Use random device in the group within the sharing session to represent the group.
                CachedBluetoothDevice device = devices.get(0);
                if (AudioSharingUtils.getGroupId(device)
                        == AudioSharingUtils.getGroupId(cachedDevice)) {
                    continue;
                }
                deviceItems.add(AudioSharingUtils.buildAudioSharingDeviceItem(device));
            }
            // Show audio sharing join dialog when the second eligible (LE audio) remote
            // device connect and no sharing session.
            if (deviceItems.size() == 1) {
                postOnMainThread(
                        () -> {
                            closeOpeningDialogsOtherThan(AudioSharingJoinDialogFragment.tag());
                            AudioSharingJoinDialogFragment.show(
                                    mHostFragment,
                                    deviceItems,
                                    cachedDevice,
                                    new AudioSharingJoinDialogFragment.DialogEventListener() {
                                        @Override
                                        public void onShareClick() {
                                            mTargetSinks = new ArrayList<>();
                                            for (List<CachedBluetoothDevice> devices :
                                                    groupedDevices.values()) {
                                                for (CachedBluetoothDevice device : devices) {
                                                    mTargetSinks.add(device.getDevice());
                                                }
                                            }
                                            Log.d(
                                                    TAG,
                                                    "Start broadcast with sinks: "
                                                            + mTargetSinks.size());
                                            if (mBroadcast != null) {
                                                mBroadcast.startPrivateBroadcast();
                                            }
                                        }

                                        @Override
                                        public void onCancelClick() {
                                            if (userTriggered) {
                                                cachedDevice.setActive();
                                            }
                                        }
                                    });
                        });
            } else if (userTriggered) {
                cachedDevice.setActive();
            }
        }
    }

    private void closeOpeningDialogsOtherThan(String tag) {
        if (mHostFragment == null) return;
        List<Fragment> fragments = mHostFragment.getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment && !fragment.getTag().equals(tag)) {
                Log.d(TAG, "Remove staled opening dialog " + fragment.getTag());
                ((DialogFragment) fragment).dismiss();
            }
        }
    }

    /** Close opening dialogs for le audio device */
    public void closeOpeningDialogsForLeaDevice(@NonNull CachedBluetoothDevice cachedDevice) {
        if (mHostFragment == null) return;
        int groupId = AudioSharingUtils.getGroupId(cachedDevice);
        List<Fragment> fragments = mHostFragment.getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            CachedBluetoothDevice device = getCachedBluetoothDeviceFromDialog(fragment);
            if (device != null
                    && groupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID
                    && AudioSharingUtils.getGroupId(device) == groupId) {
                Log.d(TAG, "Remove staled opening dialog for group " + groupId);
                ((DialogFragment) fragment).dismiss();
            }
        }
    }

    /** Close opening dialogs for non le audio device */
    public void closeOpeningDialogsForNonLeaDevice(@NonNull CachedBluetoothDevice cachedDevice) {
        if (mHostFragment == null) return;
        String address = cachedDevice.getAddress();
        List<Fragment> fragments = mHostFragment.getChildFragmentManager().getFragments();
        for (Fragment fragment : fragments) {
            CachedBluetoothDevice device = getCachedBluetoothDeviceFromDialog(fragment);
            if (device != null && address != null && address.equals(device.getAddress())) {
                Log.d(
                        TAG,
                        "Remove staled opening dialog for device "
                                + cachedDevice.getDevice().getAnonymizedAddress());
                ((DialogFragment) fragment).dismiss();
            }
        }
    }

    @Nullable
    private CachedBluetoothDevice getCachedBluetoothDeviceFromDialog(Fragment fragment) {
        CachedBluetoothDevice device = null;
        if (fragment instanceof AudioSharingJoinDialogFragment) {
            device = ((AudioSharingJoinDialogFragment) fragment).getDevice();
        } else if (fragment instanceof AudioSharingStopDialogFragment) {
            device = ((AudioSharingStopDialogFragment) fragment).getDevice();
        } else if (fragment instanceof AudioSharingDisconnectDialogFragment) {
            device = ((AudioSharingDisconnectDialogFragment) fragment).getDevice();
        }
        return device;
    }

    private void removeSourceForGroup(
            int groupId, Map<Integer, List<CachedBluetoothDevice>> groupedDevices) {
        if (mAssistant == null) {
            Log.d(TAG, "Fail to add source due to null profiles, group = " + groupId);
            return;
        }
        if (!groupedDevices.containsKey(groupId)) {
            Log.d(TAG, "Fail to remove source for group " + groupId);
            return;
        }
        groupedDevices.get(groupId).stream()
                .map(CachedBluetoothDevice::getDevice)
                .filter(device -> device != null)
                .forEach(
                        device -> {
                            for (BluetoothLeBroadcastReceiveState source :
                                    mAssistant.getAllSources(device)) {
                                mAssistant.removeSource(device, source.getSourceId());
                            }
                        });
    }

    private void addSourceForGroup(
            int groupId, Map<Integer, List<CachedBluetoothDevice>> groupedDevices) {
        if (mBroadcast == null || mAssistant == null) {
            Log.d(TAG, "Fail to add source due to null profiles, group = " + groupId);
            return;
        }
        if (!groupedDevices.containsKey(groupId)) {
            Log.d(TAG, "Fail to add source due to invalid group id, group = " + groupId);
            return;
        }
        groupedDevices.get(groupId).stream()
                .map(CachedBluetoothDevice::getDevice)
                .filter(device -> device != null)
                .forEach(
                        device ->
                                mAssistant.addSource(
                                        device,
                                        mBroadcast.getLatestBluetoothLeBroadcastMetadata(),
                                        /* isGroupOp= */ false));
    }

    private void postOnMainThread(@NonNull Runnable runnable) {
        mContext.getMainExecutor().execute(runnable);
    }

    private boolean isBroadcasting() {
        return mBroadcast != null && mBroadcast.isEnabled(null);
    }
}
