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

import static com.android.settings.connecteddevice.audiosharing.AudioSharingUtils.MetricKey.METRIC_KEY_CANDIDATE_DEVICE_COUNT;
import static com.android.settings.connecteddevice.audiosharing.AudioSharingUtils.MetricKey.METRIC_KEY_DEVICE_COUNT_IN_SHARING;
import static com.android.settings.connecteddevice.audiosharing.AudioSharingUtils.MetricKey.METRIC_KEY_PAGE_ID;
import static com.android.settings.connecteddevice.audiosharing.AudioSharingUtils.MetricKey.METRIC_KEY_SOURCE_PAGE_ID;
import static com.android.settings.connecteddevice.audiosharing.AudioSharingUtils.MetricKey.METRIC_KEY_USER_TRIGGERED;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothCsipSetCoordinator;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager;
import com.android.settingslib.bluetooth.LeAudioProfile;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcast;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfile;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.bluetooth.VolumeControlProfile;
import com.android.settingslib.flags.Flags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AudioSharingUtils {
    public static final String SETTINGS_KEY_FALLBACK_DEVICE_GROUP_ID =
            "bluetooth_le_broadcast_fallback_active_group_id";
    private static final String TAG = "AudioSharingUtils";
    private static final boolean DEBUG = BluetoothUtils.D;

    public enum MetricKey {
        METRIC_KEY_SOURCE_PAGE_ID,
        METRIC_KEY_PAGE_ID,
        METRIC_KEY_USER_TRIGGERED,
        METRIC_KEY_DEVICE_COUNT_IN_SHARING,
        METRIC_KEY_CANDIDATE_DEVICE_COUNT
    }

    /**
     * Fetch {@link CachedBluetoothDevice}s connected to the broadcast assistant. The devices are
     * grouped by CSIP group id.
     *
     * @param localBtManager The BT manager to provide BT functions.
     * @return A map of connected devices grouped by CSIP group id.
     */
    public static Map<Integer, List<CachedBluetoothDevice>> fetchConnectedDevicesByGroupId(
            @Nullable LocalBluetoothManager localBtManager) {
        Map<Integer, List<CachedBluetoothDevice>> groupedDevices = new HashMap<>();
        if (localBtManager == null) {
            Log.d(TAG, "Skip fetchConnectedDevicesByGroupId due to bt manager is null");
            return groupedDevices;
        }
        LocalBluetoothLeBroadcastAssistant assistant =
                localBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        if (assistant == null) {
            Log.d(TAG, "Skip fetchConnectedDevicesByGroupId due to assistant profile is null");
            return groupedDevices;
        }
        List<BluetoothDevice> connectedDevices =
                assistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED});
        CachedBluetoothDeviceManager cacheManager = localBtManager.getCachedDeviceManager();
        for (BluetoothDevice device : connectedDevices) {
            CachedBluetoothDevice cachedDevice = cacheManager.findDevice(device);
            if (cachedDevice == null) {
                Log.d(TAG, "Skip device due to not being cached: " + device.getAnonymizedAddress());
                continue;
            }
            int groupId = getGroupId(cachedDevice);
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
        if (DEBUG) {
            Log.d(TAG, "fetchConnectedDevicesByGroupId: " + groupedDevices);
        }
        return groupedDevices;
    }

    /**
     * Fetch a list of ordered connected lead {@link CachedBluetoothDevice}s eligible for audio
     * sharing. The active device is placed in the first place if it exists. The devices can be
     * filtered by whether it is already in the audio sharing session.
     *
     * @param localBtManager The BT manager to provide BT functions. *
     * @param groupedConnectedDevices devices connected to broadcast assistant grouped by CSIP group
     *     id.
     * @param filterByInSharing Whether to filter the device by if is already in the sharing
     *     session.
     * @return A list of ordered connected devices eligible for the audio sharing. The active device
     *     is placed in the first place if it exists.
     */
    public static List<CachedBluetoothDevice> buildOrderedConnectedLeadDevices(
            @Nullable LocalBluetoothManager localBtManager,
            Map<Integer, List<CachedBluetoothDevice>> groupedConnectedDevices,
            boolean filterByInSharing) {
        List<CachedBluetoothDevice> orderedDevices = new ArrayList<>();
        for (List<CachedBluetoothDevice> devices : groupedConnectedDevices.values()) {
            CachedBluetoothDevice leadDevice = getLeadDevice(devices);
            if (leadDevice == null) {
                Log.d(TAG, "Skip due to no lead device");
                continue;
            }
            if (filterByInSharing
                    && !BluetoothUtils.hasConnectedBroadcastSource(leadDevice, localBtManager)) {
                Log.d(
                        TAG,
                        "Filtered the device due to not in sharing session: "
                                + leadDevice.getDevice().getAnonymizedAddress());
                continue;
            }
            orderedDevices.add(leadDevice);
        }
        orderedDevices.sort(
                (CachedBluetoothDevice d1, CachedBluetoothDevice d2) -> {
                    // Active above not inactive
                    int comparison =
                            (isActiveLeAudioDevice(d2) ? 1 : 0)
                                    - (isActiveLeAudioDevice(d1) ? 1 : 0);
                    if (comparison != 0) return comparison;
                    // Bonded above not bonded
                    comparison =
                            (d2.getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0)
                                    - (d1.getBondState() == BluetoothDevice.BOND_BONDED ? 1 : 0);
                    if (comparison != 0) return comparison;
                    // Bond timestamp available above unavailable
                    comparison =
                            (d2.getBondTimestamp() != null ? 1 : 0)
                                    - (d1.getBondTimestamp() != null ? 1 : 0);
                    if (comparison != 0) return comparison;
                    // Order by bond timestamp if it is available
                    // Otherwise order by device name
                    return d1.getBondTimestamp() != null
                            ? d1.getBondTimestamp().compareTo(d2.getBondTimestamp())
                            : d1.getName().compareTo(d2.getName());
                });
        return orderedDevices;
    }

    /**
     * Get the lead device from a list of devices with same group id.
     *
     * @param devices A list of devices with same group id.
     * @return The lead device
     */
    @Nullable
    public static CachedBluetoothDevice getLeadDevice(
            @NonNull List<CachedBluetoothDevice> devices) {
        if (devices.isEmpty()) return null;
        for (CachedBluetoothDevice device : devices) {
            if (!device.getMemberDevice().isEmpty()) {
                return device;
            }
        }
        CachedBluetoothDevice leadDevice = devices.get(0);
        Log.d(
                TAG,
                "No lead device in the group, pick arbitrary device as the lead: "
                        + leadDevice.getDevice().getAnonymizedAddress());
        return leadDevice;
    }

    /**
     * Fetch a list of ordered connected lead {@link AudioSharingDeviceItem}s eligible for audio
     * sharing. The active device is placed in the first place if it exists. The devices can be
     * filtered by whether it is already in the audio sharing session.
     *
     * @param localBtManager The BT manager to provide BT functions. *
     * @param groupedConnectedDevices devices connected to broadcast assistant grouped by CSIP group
     *     id.
     * @param filterByInSharing Whether to filter the device by if is already in the sharing
     *     session.
     * @return A list of ordered connected devices eligible for the audio sharing. The active device
     *     is placed in the first place if it exists.
     */
    @NonNull
    public static List<AudioSharingDeviceItem> buildOrderedConnectedLeadAudioSharingDeviceItem(
            @Nullable LocalBluetoothManager localBtManager,
            Map<Integer, List<CachedBluetoothDevice>> groupedConnectedDevices,
            boolean filterByInSharing) {
        return buildOrderedConnectedLeadDevices(
                        localBtManager, groupedConnectedDevices, filterByInSharing)
                .stream()
                .map(AudioSharingUtils::buildAudioSharingDeviceItem)
                .collect(Collectors.toList());
    }

    /** Build {@link AudioSharingDeviceItem} from {@link CachedBluetoothDevice}. */
    public static AudioSharingDeviceItem buildAudioSharingDeviceItem(
            CachedBluetoothDevice cachedDevice) {
        return new AudioSharingDeviceItem(
                cachedDevice.getName(),
                getGroupId(cachedDevice),
                isActiveLeAudioDevice(cachedDevice));
    }

    /**
     * Check if {@link CachedBluetoothDevice} is an active le audio device.
     *
     * @param cachedDevice The cached bluetooth device to check.
     * @return Whether the device is an active le audio device.
     */
    public static boolean isActiveLeAudioDevice(CachedBluetoothDevice cachedDevice) {
        return BluetoothUtils.isActiveLeAudioDevice(cachedDevice);
    }

    /** Toast message on main thread. */
    public static void toastMessage(Context context, String message) {
        context.getMainExecutor()
                .execute(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    /** Returns if the le audio sharing is enabled. */
    public static boolean isFeatureEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        return Flags.enableLeAudioSharing()
                && adapter.isLeAudioBroadcastSourceSupported()
                        == BluetoothStatusCodes.FEATURE_SUPPORTED
                && adapter.isLeAudioBroadcastAssistantSupported()
                        == BluetoothStatusCodes.FEATURE_SUPPORTED;
    }

    /** Add source to target sinks. */
    public static void addSourceToTargetSinks(
            List<BluetoothDevice> sinks, @Nullable LocalBluetoothManager localBtManager) {
        if (localBtManager == null) {
            Log.d(TAG, "skip addSourceToTargetDevices: LocalBluetoothManager is null!");
            return;
        }
        if (sinks.isEmpty()) {
            Log.d(TAG, "Skip addSourceToTargetDevices. No sinks.");
            return;
        }
        LocalBluetoothLeBroadcast broadcast =
                localBtManager.getProfileManager().getLeAudioBroadcastProfile();
        if (broadcast == null) {
            Log.d(TAG, "skip addSourceToTargetDevices. Broadcast profile is null.");
            return;
        }
        LocalBluetoothLeBroadcastAssistant assistant =
                localBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        if (assistant == null) {
            Log.d(TAG, "skip addSourceToTargetDevices. Assistant profile is null.");
            return;
        }
        BluetoothLeBroadcastMetadata broadcastMetadata =
                broadcast.getLatestBluetoothLeBroadcastMetadata();
        if (broadcastMetadata == null) {
            Log.d(TAG, "skip addSourceToTargetDevices: There is no broadcastMetadata.");
            return;
        }
        List<BluetoothDevice> connectedDevices =
                assistant.getDevicesMatchingConnectionStates(
                        new int[] {BluetoothProfile.STATE_CONNECTED});
        for (BluetoothDevice sink : sinks) {
            if (connectedDevices.contains(sink)) {
                Log.d(
                        TAG,
                        "Add broadcast with broadcastId: "
                                + broadcastMetadata.getBroadcastId()
                                + " to the device: "
                                + sink.getAnonymizedAddress());
                assistant.addSource(sink, broadcastMetadata, /* isGroupOp= */ false);
            } else {
                Log.d(
                        TAG,
                        "Skip add broadcast with broadcastId: "
                                + broadcastMetadata.getBroadcastId()
                                + " to the not connected device: "
                                + sink.getAnonymizedAddress());
            }
        }
    }

    /** Returns if the broadcast is on-going. */
    public static boolean isBroadcasting(@Nullable LocalBluetoothManager manager) {
        if (manager == null) return false;
        LocalBluetoothLeBroadcast broadcast =
                manager.getProfileManager().getLeAudioBroadcastProfile();
        return broadcast != null && broadcast.isEnabled(null);
    }

    /** Stops the latest broadcast. */
    public static void stopBroadcasting(@Nullable LocalBluetoothManager manager) {
        if (manager == null) {
            Log.d(TAG, "Skip stop broadcasting due to bt manager is null");
            return;
        }
        LocalBluetoothLeBroadcast broadcast =
                manager.getProfileManager().getLeAudioBroadcastProfile();
        if (broadcast == null) {
            Log.d(TAG, "Skip stop broadcasting due to broadcast profile is null");
        } else {
            broadcast.stopBroadcast(broadcast.getLatestBroadcastId());
        }
    }

    /**
     * Get CSIP group id for {@link CachedBluetoothDevice}.
     *
     * <p>If CachedBluetoothDevice#getGroupId is invalid, fetch group id from
     * LeAudioProfile#getGroupId.
     */
    public static int getGroupId(CachedBluetoothDevice cachedDevice) {
        int groupId = cachedDevice.getGroupId();
        String anonymizedAddress = cachedDevice.getDevice().getAnonymizedAddress();
        if (groupId != BluetoothCsipSetCoordinator.GROUP_ID_INVALID) {
            Log.d(TAG, "getGroupId by CSIP profile for device: " + anonymizedAddress);
            return groupId;
        }
        for (LocalBluetoothProfile profile : cachedDevice.getProfiles()) {
            if (profile instanceof LeAudioProfile) {
                Log.d(TAG, "getGroupId by LEA profile for device: " + anonymizedAddress);
                return ((LeAudioProfile) profile).getGroupId(cachedDevice.getDevice());
            }
        }
        Log.d(TAG, "getGroupId return invalid id for device: " + anonymizedAddress);
        return BluetoothCsipSetCoordinator.GROUP_ID_INVALID;
    }

    /** Get the fallback active group id from SettingsProvider. */
    public static int getFallbackActiveGroupId(@NonNull Context context) {
        return Settings.Secure.getInt(
                context.getContentResolver(),
                SETTINGS_KEY_FALLBACK_DEVICE_GROUP_ID,
                BluetoothCsipSetCoordinator.GROUP_ID_INVALID);
    }

    /** Post the runnable to main thread. */
    public static void postOnMainThread(@NonNull Context context, @NonNull Runnable runnable) {
        context.getMainExecutor().execute(runnable);
    }

    /** Check if the {@link CachedBluetoothDevice} supports LE Audio profile */
    public static boolean isLeAudioSupported(CachedBluetoothDevice cachedDevice) {
        return cachedDevice.getProfiles().stream()
                .anyMatch(
                        profile ->
                                profile instanceof LeAudioProfile
                                        && profile.isEnabled(cachedDevice.getDevice()));
    }

    /** Check if the LE Audio related profiles ready */
    public static boolean isAudioSharingProfileReady(
            @Nullable LocalBluetoothProfileManager profileManager) {
        if (profileManager == null) return false;
        LocalBluetoothLeBroadcast broadcast = profileManager.getLeAudioBroadcastProfile();
        if (broadcast == null || !broadcast.isProfileReady()) {
            return false;
        }
        LocalBluetoothLeBroadcastAssistant assistant =
                profileManager.getLeAudioBroadcastAssistantProfile();
        if (assistant == null || !assistant.isProfileReady()) {
            return false;
        }
        VolumeControlProfile vc = profileManager.getVolumeControlProfile();
        return vc != null && vc.isProfileReady();
    }

    /**
     * Build audio sharing dialog log event data
     *
     * @param sourcePageId The source page id on which the dialog is shown. *
     * @param pageId The page id of the dialog.
     * @param userTriggered Indicates whether the dialog is triggered by user click.
     * @param deviceCountInSharing The count of the devices joining the audio sharing.
     * @param candidateDeviceCount The count of the eligible devices to join the audio sharing.
     * @return The event data to be attached to the audio sharing action logs.
     */
    @NonNull
    public static Pair<Integer, Object>[] buildAudioSharingDialogEventData(
            int sourcePageId,
            int pageId,
            boolean userTriggered,
            int deviceCountInSharing,
            int candidateDeviceCount) {
        return new Pair[] {
            Pair.create(METRIC_KEY_SOURCE_PAGE_ID.ordinal(), sourcePageId),
            Pair.create(METRIC_KEY_PAGE_ID.ordinal(), pageId),
            Pair.create(METRIC_KEY_USER_TRIGGERED.ordinal(), userTriggered ? 1 : 0),
            Pair.create(METRIC_KEY_DEVICE_COUNT_IN_SHARING.ordinal(), deviceCountInSharing),
            Pair.create(METRIC_KEY_CANDIDATE_DEVICE_COUNT.ordinal(), candidateDeviceCount)
        };
    }
}
