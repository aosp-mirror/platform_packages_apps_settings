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

import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast.EXTRA_START_LE_AUDIO_SHARING;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcast;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class AudioSharingDialogHandler {
    private static final String TAG = "AudioSharingDlgHandler";
    private final Context mContext;
    private final Fragment mHostFragment;
    @Nullable private final LocalBluetoothManager mLocalBtManager;
    @Nullable private final CachedBluetoothDeviceManager mDeviceManager;
    @Nullable private final LocalBluetoothLeBroadcast mBroadcast;
    @Nullable private final LocalBluetoothLeBroadcastAssistant mAssistant;
    @Nullable private final AudioManager mAudioManager;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private boolean mIsStoppingBroadcast = false;

    @VisibleForTesting
    final BluetoothLeBroadcast.Callback mBroadcastCallback =
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
                    AudioSharingUtils.toastMessage(
                            mContext,
                            mContext.getString(R.string.audio_sharing_sharing_stopped_label));
                    mIsStoppingBroadcast = false;
                }

                @Override
                public void onBroadcastStopFailed(int reason) {
                    Log.d(TAG, "onBroadcastStopFailed(), reason = " + reason);
                    if (mIsStoppingBroadcast) {
                        mMetricsFeatureProvider.action(
                                mContext,
                                SettingsEnums.ACTION_AUDIO_SHARING_STOP_FAILED,
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY);
                        AudioSharingUtils.toastMessage(
                                mContext, "Fail to stop broadcast, reason " + reason);
                        mIsStoppingBroadcast = false;
                    }
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
                }

                @Override
                public void onPlaybackStopped(int reason, int broadcastId) {}
            };

    public AudioSharingDialogHandler(@NonNull Context context, @NonNull Fragment fragment) {
        mContext = context;
        mHostFragment = fragment;
        mLocalBtManager = Utils.getLocalBluetoothManager(context);
        mDeviceManager = mLocalBtManager != null ? mLocalBtManager.getCachedDeviceManager() : null;
        mBroadcast =
                mLocalBtManager != null
                        ? mLocalBtManager.getProfileManager().getLeAudioBroadcastProfile()
                        : null;
        mAssistant =
                mLocalBtManager != null
                        ? mLocalBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile()
                        : null;
        mAudioManager = context.getSystemService(AudioManager.class);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
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
        if (mAudioManager != null) {
            int audioMode = mAudioManager.getMode();
            if (audioMode == AudioManager.MODE_RINGTONE
                    || audioMode == AudioManager.MODE_IN_CALL
                    || audioMode == AudioManager.MODE_IN_COMMUNICATION) {
                Log.d(TAG, "Skip handleDeviceConnected, audio mode = " + audioMode);
                // TODO: add metric for this case
                if (userTriggered) {
                    // If this method is called with user triggered, e.g. manual click on the
                    // "Connected devices" page, we need call setActive for the device, since user
                    // intend to switch active device for the call.
                    cachedDevice.setActive();
                }
                return;
            }
        }
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
            Map<Integer, List<BluetoothDevice>> groupedDevices =
                    AudioSharingUtils.fetchConnectedDevicesByGroupId(mLocalBtManager);
            List<AudioSharingDeviceItem> deviceItemsInSharingSession =
                    AudioSharingUtils.buildOrderedConnectedLeadAudioSharingDeviceItem(
                            mLocalBtManager, groupedDevices, /* filterByInSharing= */ true);
            AudioSharingStopDialogFragment.DialogEventListener listener =
                    () -> {
                        cachedDevice.setActive();
                        mIsStoppingBroadcast = true;
                        AudioSharingUtils.stopBroadcasting(mLocalBtManager);
                    };
            Pair<Integer, Object>[] eventData =
                    AudioSharingUtils.buildAudioSharingDialogEventData(
                            SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY,
                            SettingsEnums.DIALOG_STOP_AUDIO_SHARING,
                            userTriggered,
                            deviceItemsInSharingSession.size(),
                            /* candidateDeviceCount= */ 0);
            postOnMainThread(
                    () -> {
                        closeOpeningDialogsOtherThan(AudioSharingStopDialogFragment.tag());
                        AudioSharingStopDialogFragment.show(
                                mHostFragment,
                                deviceItemsInSharingSession,
                                cachedDevice,
                                listener,
                                eventData);
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
        Map<Integer, List<BluetoothDevice>> groupedDevices =
                AudioSharingUtils.fetchConnectedDevicesByGroupId(mLocalBtManager);
        BluetoothDevice btDevice = cachedDevice.getDevice();
        String deviceAddress = btDevice == null ? "" : btDevice.getAnonymizedAddress();
        int groupId = BluetoothUtils.getGroupId(cachedDevice);
        if (isBroadcasting) {
            // If another device within the same is already in the sharing session, add source to
            // the device automatically.
            if (groupedDevices.containsKey(groupId)
                    && groupedDevices.get(groupId).stream()
                            .anyMatch(
                                    device ->
                                            BluetoothUtils.hasConnectedBroadcastSourceForBtDevice(
                                                    device, mLocalBtManager))) {
                Log.d(
                        TAG,
                        "Automatically add another device within the same group to the sharing: "
                                + deviceAddress);
                if (mAssistant != null && mBroadcast != null) {
                    mAssistant.addSource(
                            btDevice,
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
                AudioSharingDisconnectDialogFragment.DialogEventListener listener =
                        (AudioSharingDeviceItem item) -> {
                            // Remove all sources from the device user clicked
                            removeSourceForGroup(item.getGroupId(), groupedDevices);
                            // Add current broadcast to the latest connected device
                            addSourceForGroup(groupId, groupedDevices);
                        };
                Pair<Integer, Object>[] eventData =
                        AudioSharingUtils.buildAudioSharingDialogEventData(
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY,
                                SettingsEnums.DIALOG_AUDIO_SHARING_SWITCH_DEVICE,
                                userTriggered,
                                deviceItemsInSharingSession.size(),
                                /* candidateDeviceCount= */ 1);
                postOnMainThread(
                        () -> {
                            closeOpeningDialogsOtherThan(
                                    AudioSharingDisconnectDialogFragment.tag());
                            AudioSharingDisconnectDialogFragment.show(
                                    mHostFragment,
                                    deviceItemsInSharingSession,
                                    cachedDevice,
                                    listener,
                                    eventData);
                            Log.d(TAG, "Show disconnect dialog, device = " + deviceAddress);
                        });
            } else {
                // Show audio sharing join dialog when the first or second eligible (LE audio)
                // remote device connected during a sharing session.
                AudioSharingJoinDialogFragment.DialogEventListener listener =
                        new AudioSharingJoinDialogFragment.DialogEventListener() {
                            @Override
                            public void onShareClick() {
                                addSourceForGroup(groupId, groupedDevices);
                            }

                            @Override
                            public void onCancelClick() {}
                        };
                Pair<Integer, Object>[] eventData =
                        AudioSharingUtils.buildAudioSharingDialogEventData(
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY,
                                SettingsEnums.DIALOG_AUDIO_SHARING_ADD_DEVICE,
                                userTriggered,
                                deviceItemsInSharingSession.size(),
                                /* candidateDeviceCount= */ 1);
                postOnMainThread(
                        () -> {
                            closeOpeningDialogsOtherThan(AudioSharingJoinDialogFragment.tag());
                            AudioSharingJoinDialogFragment.show(
                                    mHostFragment,
                                    deviceItemsInSharingSession,
                                    cachedDevice,
                                    listener,
                                    eventData);
                            Log.d(TAG, "Show join dialog, device = " + deviceAddress);
                        });
            }
        } else {
            // Build a list of AudioSharingDeviceItem for connected devices other than cachedDevice.
            List<AudioSharingDeviceItem> deviceItems = new ArrayList<>();
            for (Map.Entry<Integer, List<BluetoothDevice>> entry : groupedDevices.entrySet()) {
                if (entry.getKey() == groupId) continue;
                // Use random device in the group within the sharing session to represent the group.
                for (BluetoothDevice device : entry.getValue()) {
                    CachedBluetoothDevice cDevice =
                            mDeviceManager != null ? mDeviceManager.findDevice(device) : null;
                    if (cDevice != null) {
                        deviceItems.add(AudioSharingUtils.buildAudioSharingDeviceItem(cDevice));
                        break;
                    }
                }
            }
            // Show audio sharing join dialog when the second eligible (LE audio) remote
            // device connect and no sharing session.
            if (groupedDevices.size() == 2 && deviceItems.size() == 1) {
                AudioSharingJoinDialogFragment.DialogEventListener listener =
                        new AudioSharingJoinDialogFragment.DialogEventListener() {
                            @Override
                            public void onShareClick() {
                                Bundle args = new Bundle();
                                args.putBoolean(EXTRA_START_LE_AUDIO_SHARING, true);
                                new SubSettingLauncher(mContext)
                                        .setDestination(
                                                AudioSharingDashboardFragment.class.getName())
                                        .setSourceMetricsCategory(
                                                (mHostFragment instanceof DashboardFragment)
                                                        ? ((DashboardFragment) mHostFragment)
                                                                .getMetricsCategory()
                                                        : SettingsEnums.PAGE_UNKNOWN)
                                        .setArguments(args)
                                        .launch();
                            }

                            @Override
                            public void onCancelClick() {
                                if (userTriggered) {
                                    cachedDevice.setActive();
                                }
                            }
                        };

                Pair<Integer, Object>[] eventData =
                        AudioSharingUtils.buildAudioSharingDialogEventData(
                                SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY,
                                SettingsEnums.DIALOG_START_AUDIO_SHARING,
                                userTriggered,
                                /* deviceCountInSharing= */ 0,
                                /* candidateDeviceCount= */ 2);
                postOnMainThread(
                        () -> {
                            closeOpeningDialogsOtherThan(AudioSharingJoinDialogFragment.tag());
                            AudioSharingJoinDialogFragment.show(
                                    mHostFragment, deviceItems, cachedDevice, listener, eventData);
                            Log.d(TAG, "Show start dialog, device = " + deviceAddress);
                        });
            } else if (userTriggered) {
                cachedDevice.setActive();
                Log.d(TAG, "Set active device = " + deviceAddress);
            } else {
                Log.d(TAG, "Fail to handle LE audio device connected, device = " + deviceAddress);
            }
        }
    }

    private void closeOpeningDialogsOtherThan(String tag) {
        if (mHostFragment == null) return;
        List<Fragment> fragments;
        try {
            fragments = mHostFragment.getChildFragmentManager().getFragments();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to closeOpeningDialogsOtherThan " + tag + ": " + e.getMessage());
            return;
        }
        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment
                    && fragment.getTag() != null
                    && !fragment.getTag().equals(tag)) {
                Log.d(TAG, "Remove staled opening dialog " + fragment.getTag());
                ((DialogFragment) fragment).dismiss();
                logDialogDismissEvent(fragment);
            }
        }
    }

    /** Close opening dialogs for le audio device */
    public void closeOpeningDialogsForLeaDevice(@NonNull CachedBluetoothDevice cachedDevice) {
        if (mHostFragment == null) return;
        int groupId = BluetoothUtils.getGroupId(cachedDevice);
        List<Fragment> fragments;
        try {
            fragments = mHostFragment.getChildFragmentManager().getFragments();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to closeOpeningDialogsForLeaDevice: " + e.getMessage());
            return;
        }
        for (Fragment fragment : fragments) {
            CachedBluetoothDevice device = getCachedBluetoothDeviceFromDialog(fragment);
            if (device != null
                    && groupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID
                    && BluetoothUtils.getGroupId(device) == groupId) {
                Log.d(TAG, "Remove staled opening dialog for group " + groupId);
                ((DialogFragment) fragment).dismiss();
                logDialogDismissEvent(fragment);
            }
        }
    }

    /** Close opening dialogs for non le audio device */
    public void closeOpeningDialogsForNonLeaDevice(@NonNull CachedBluetoothDevice cachedDevice) {
        if (mHostFragment == null) return;
        String address = cachedDevice.getAddress();
        List<Fragment> fragments;
        try {
            fragments = mHostFragment.getChildFragmentManager().getFragments();
        } catch (IllegalStateException e) {
            Log.d(TAG, "Fail to closeOpeningDialogsForNonLeaDevice: " + e.getMessage());
            return;
        }
        for (Fragment fragment : fragments) {
            CachedBluetoothDevice device = getCachedBluetoothDeviceFromDialog(fragment);
            if (device != null && address != null && address.equals(device.getAddress())) {
                Log.d(
                        TAG,
                        "Remove staled opening dialog for device "
                                + cachedDevice.getDevice().getAnonymizedAddress());
                ((DialogFragment) fragment).dismiss();
                logDialogDismissEvent(fragment);
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
            int groupId, Map<Integer, List<BluetoothDevice>> groupedDevices) {
        if (mAssistant == null) {
            Log.d(TAG, "Fail to remove source due to null profiles, group = " + groupId);
            return;
        }
        if (!groupedDevices.containsKey(groupId)) {
            Log.d(TAG, "Fail to remove source for group " + groupId);
            return;
        }
        groupedDevices.getOrDefault(groupId, ImmutableList.of()).stream()
                .forEach(
                        device -> {
                            for (BluetoothLeBroadcastReceiveState source :
                                    mAssistant.getAllSources(device)) {
                                mAssistant.removeSource(device, source.getSourceId());
                            }
                        });
    }

    private void addSourceForGroup(
            int groupId, Map<Integer, List<BluetoothDevice>> groupedDevices) {
        if (mBroadcast == null || mAssistant == null) {
            Log.d(TAG, "Fail to add source due to null profiles, group = " + groupId);
            return;
        }
        if (!groupedDevices.containsKey(groupId)) {
            Log.d(TAG, "Fail to add source due to invalid group id, group = " + groupId);
            return;
        }
        groupedDevices.getOrDefault(groupId, ImmutableList.of()).stream()
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

    private void logDialogDismissEvent(Fragment fragment) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            int pageId = SettingsEnums.PAGE_UNKNOWN;
                            if (fragment instanceof AudioSharingJoinDialogFragment) {
                                pageId =
                                        ((AudioSharingJoinDialogFragment) fragment)
                                                .getMetricsCategory();
                            } else if (fragment instanceof AudioSharingStopDialogFragment) {
                                pageId =
                                        ((AudioSharingStopDialogFragment) fragment)
                                                .getMetricsCategory();
                            } else if (fragment instanceof AudioSharingDisconnectDialogFragment) {
                                pageId =
                                        ((AudioSharingDisconnectDialogFragment) fragment)
                                                .getMetricsCategory();
                            }
                            mMetricsFeatureProvider.action(
                                    mContext,
                                    SettingsEnums.ACTION_AUDIO_SHARING_DIALOG_AUTO_DISMISS,
                                    pageId);
                        });
    }
}
