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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamMediaService.BROADCAST_ID;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamMediaService.BROADCAST_TITLE;
import static com.android.settings.connecteddevice.audiosharing.audiostreams.AudioStreamMediaService.DEVICES;
import static com.android.settingslib.bluetooth.BluetoothUtils.isAudioSharingHysteresisModeFixAvailable;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.DECRYPTION_FAILED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.PAUSED;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.LocalBluetoothLeBroadcastSourceState.STREAMING;
import static com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant.getLocalSourceState;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeAudioContentMetadata;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.utils.ThreadUtils;

import com.google.android.material.appbar.AppBarLayout;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * A helper class that adds, removes and retrieves LE broadcast sources for all active sink devices.
 */
public class AudioStreamsHelper {

    private static final String TAG = "AudioStreamsHelper";
    private static final boolean DEBUG = BluetoothUtils.D;

    private final @Nullable LocalBluetoothManager mBluetoothManager;
    private final @Nullable LocalBluetoothLeBroadcastAssistant mLeBroadcastAssistant;

    AudioStreamsHelper(@Nullable LocalBluetoothManager bluetoothManager) {
        mBluetoothManager = bluetoothManager;
        mLeBroadcastAssistant = getLeBroadcastAssistant(mBluetoothManager);
    }

    /**
     * Adds the specified LE broadcast source to all active sinks.
     *
     * @param source The LE broadcast metadata representing the audio source.
     */
    @VisibleForTesting
    public void addSource(BluetoothLeBroadcastMetadata source) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "addSource(): LeBroadcastAssistant is null!");
            return;
        }
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            for (var sink :
                                    getConnectedBluetoothDevices(
                                            mBluetoothManager, /* inSharingOnly= */ false)) {
                                if (DEBUG) {
                                    Log.d(
                                            TAG,
                                            "addSource(): join broadcast broadcastId"
                                                    + " : "
                                                    + source.getBroadcastId()
                                                    + " sink : "
                                                    + sink.getAddress());
                                }
                                mLeBroadcastAssistant.addSource(sink, source, false);
                            }
                        });
    }

    /** Removes sources from LE broadcasts associated for all active sinks based on broadcast Id. */
    @VisibleForTesting
    public void removeSource(int broadcastId) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "removeSource(): LeBroadcastAssistant is null!");
            return;
        }
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            for (var sink :
                                    getConnectedBluetoothDevices(
                                            mBluetoothManager, /* inSharingOnly= */ true)) {
                                if (DEBUG) {
                                    Log.d(
                                            TAG,
                                            "removeSource(): remove all sources with broadcast id :"
                                                    + broadcastId
                                                    + " from sink : "
                                                    + sink.getAddress());
                                }
                                mLeBroadcastAssistant.getAllSources(sink).stream()
                                        .filter(state -> state.getBroadcastId() == broadcastId)
                                        .forEach(
                                                state ->
                                                        mLeBroadcastAssistant.removeSource(
                                                                sink, state.getSourceId()));
                            }
                        });
    }

    /**
     * Gets a map of connected broadcast IDs to their corresponding local broadcast source states.
     *
     * <p>If multiple sources have the same broadcast ID, the state of the source that is
     * {@code STREAMING} is preferred.
     */
    public Map<Integer, LocalBluetoothLeBroadcastSourceState> getConnectedBroadcastIdAndState(
            boolean hysteresisModeFixAvailable) {
        if (mBluetoothManager == null || mLeBroadcastAssistant == null) {
            Log.w(TAG,
                    "getConnectedBroadcastIdAndState(): BluetoothManager or LeBroadcastAssistant "
                            + "is null!");
            return emptyMap();
        }
        return getConnectedBluetoothDevices(mBluetoothManager, /* inSharingOnly= */ true).stream()
                .flatMap(sink -> mLeBroadcastAssistant.getAllSources(sink).stream())
                .map(state -> new Pair<>(state.getBroadcastId(), getLocalSourceState(state)))
                .filter(pair -> pair.second == STREAMING
                        || (hysteresisModeFixAvailable && pair.second == PAUSED))
                .collect(toMap(
                        p -> p.first,
                        p -> p.second,
                        (existingState, newState) -> existingState == STREAMING ? existingState
                                : newState
                ));
    }

    /** Retrieves a list of all LE broadcast receive states keyed by each active device. */
    public Map<BluetoothDevice, List<BluetoothLeBroadcastReceiveState>> getAllSourcesByDevice() {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "getAllSourcesByDevice(): LeBroadcastAssistant is null!");
            return emptyMap();
        }
        return getConnectedBluetoothDevices(mBluetoothManager, /* inSharingOnly= */ true).stream()
                .collect(toMap(Function.identity(), mLeBroadcastAssistant::getAllSources));
    }

    /** Retrieves LocalBluetoothLeBroadcastAssistant. */
    @Nullable
    public LocalBluetoothLeBroadcastAssistant getLeBroadcastAssistant() {
        return mLeBroadcastAssistant;
    }

    /**
     * Returns a {@code CachedBluetoothDevice} that is either connected to a broadcast source or is
     * a connected LE device.
     */
    public static Optional<CachedBluetoothDevice> getCachedBluetoothDeviceInSharingOrLeConnected(
            @androidx.annotation.Nullable LocalBluetoothManager manager) {
        if (manager == null) {
            Log.w(
                    TAG,
                    "getCachedBluetoothDeviceInSharingOrLeConnected(): LocalBluetoothManager is"
                            + " null!");
            return Optional.empty();
        }
        var groupedDevices = AudioSharingUtils.fetchConnectedDevicesByGroupId(manager);
        var leadDevices =
                AudioSharingUtils.buildOrderedConnectedLeadDevices(manager, groupedDevices, false);
        if (leadDevices.isEmpty()) {
            Log.w(TAG, "getCachedBluetoothDeviceInSharingOrLeConnected(): No lead device!");
            return Optional.empty();
        }
        var deviceHasSource =
                leadDevices.stream()
                        .filter(device -> hasBroadcastSource(device, manager))
                        .findFirst();
        if (deviceHasSource.isPresent()) {
            Log.d(
                    TAG,
                    "getCachedBluetoothDeviceInSharingOrLeConnected(): Device has connected source"
                            + " found: "
                            + deviceHasSource.get().getAddress());
            return deviceHasSource;
        }
        Log.d(
                TAG,
                "getCachedBluetoothDeviceInSharingOrLeConnected(): Device connected found: "
                        + leadDevices.get(0).getAddress());
        return Optional.of(leadDevices.get(0));
    }

    /** Returns a {@code CachedBluetoothDevice} that has a connected broadcast source. */
    static Optional<CachedBluetoothDevice> getCachedBluetoothDeviceInSharing(
            @androidx.annotation.Nullable LocalBluetoothManager manager) {
        if (manager == null) {
            Log.w(TAG, "getCachedBluetoothDeviceInSharing(): LocalBluetoothManager is null!");
            return Optional.empty();
        }
        var groupedDevices = AudioSharingUtils.fetchConnectedDevicesByGroupId(manager);
        var leadDevices =
                AudioSharingUtils.buildOrderedConnectedLeadDevices(manager, groupedDevices, false);
        if (leadDevices.isEmpty()) {
            Log.w(TAG, "getCachedBluetoothDeviceInSharing(): No lead device!");
            return Optional.empty();
        }
        return leadDevices.stream()
                .filter(device -> hasBroadcastSource(device, manager))
                .findFirst();
    }

    /**
     * Check if {@link CachedBluetoothDevice} has a broadcast source that is in STREAMING, PAUSED
     * or DECRYPTION_FAILED state.
     *
     * @param cachedDevice   The cached bluetooth device to check.
     * @param localBtManager The BT manager to provide BT functions.
     * @return Whether the device has a broadcast source.
     */
    public static boolean hasBroadcastSource(
            CachedBluetoothDevice cachedDevice, LocalBluetoothManager localBtManager) {
        if (localBtManager == null) {
            Log.d(TAG, "Skip check hasBroadcastSource due to bt manager is null");
            return false;
        }
        LocalBluetoothLeBroadcastAssistant assistant =
                localBtManager.getProfileManager().getLeAudioBroadcastAssistantProfile();
        if (assistant == null) {
            Log.d(TAG, "Skip check hasBroadcastSource due to assistant profile is null");
            return false;
        }
        List<BluetoothLeBroadcastReceiveState> sourceList =
                assistant.getAllSources(cachedDevice.getDevice());
        boolean hysteresisModeFixAvailable = isAudioSharingHysteresisModeFixAvailable(
                localBtManager.getContext());
        if (hasReceiveState(sourceList, hysteresisModeFixAvailable)) {
            Log.d(
                    TAG,
                    "Lead device has broadcast source, device = "
                            + cachedDevice.getDevice().getAnonymizedAddress());
            return true;
        }
        // Return true if member device is in broadcast.
        for (CachedBluetoothDevice device : cachedDevice.getMemberDevice()) {
            List<BluetoothLeBroadcastReceiveState> list =
                    assistant.getAllSources(device.getDevice());
            if (hasReceiveState(list, hysteresisModeFixAvailable)) {
                Log.d(
                        TAG,
                        "Member device has broadcast source, device = "
                                + device.getDevice().getAnonymizedAddress());
                return true;
            }
        }
        return false;
    }

    private static boolean hasReceiveState(List<BluetoothLeBroadcastReceiveState> states,
            boolean hysteresisModeFixAvailable) {
        return states.stream().anyMatch(state -> {
            var localSourceState = getLocalSourceState(state);
            if (hysteresisModeFixAvailable) {
                return localSourceState == STREAMING || localSourceState == DECRYPTION_FAILED
                        || localSourceState == PAUSED;
            }
            return localSourceState == STREAMING || localSourceState == DECRYPTION_FAILED;
        });
    }

    /**
     * Retrieves a list of connected Bluetooth devices that belongs to one {@link
     * CachedBluetoothDevice} that's either connected to a broadcast source or is a connected LE
     * audio device.
     */
    static List<BluetoothDevice> getConnectedBluetoothDevices(
            @Nullable LocalBluetoothManager manager, boolean inSharingOnly) {
        if (manager == null) {
            Log.w(TAG, "getConnectedBluetoothDevices(): LocalBluetoothManager is null!");
            return emptyList();
        }
        var leBroadcastAssistant = getLeBroadcastAssistant(manager);
        if (leBroadcastAssistant == null) {
            Log.w(TAG, "getConnectedBluetoothDevices(): LeBroadcastAssistant is null!");
            return emptyList();
        }
        List<BluetoothDevice> connectedDevices = leBroadcastAssistant.getAllConnectedDevices();
        Optional<CachedBluetoothDevice> cachedBluetoothDevice =
                inSharingOnly
                        ? getCachedBluetoothDeviceInSharing(manager)
                        : getCachedBluetoothDeviceInSharingOrLeConnected(manager);
        List<BluetoothDevice> bluetoothDevices =
                cachedBluetoothDevice
                        .map(
                                c ->
                                        Stream.concat(
                                                        Stream.of(c.getDevice()),
                                                        c.getMemberDevice().stream()
                                                                .map(
                                                                        CachedBluetoothDevice
                                                                                ::getDevice))
                                                .filter(connectedDevices::contains)
                                                .toList())
                        .orElse(emptyList());
        Log.d(TAG, "getConnectedBluetoothDevices() devices: " + bluetoothDevices);
        return bluetoothDevices;
    }

    private static @Nullable LocalBluetoothLeBroadcastAssistant getLeBroadcastAssistant(
            @Nullable LocalBluetoothManager manager) {
        if (manager == null) {
            Log.w(TAG, "getLeBroadcastAssistant(): LocalBluetoothManager is null!");
            return null;
        }

        LocalBluetoothProfileManager profileManager = manager.getProfileManager();
        if (profileManager == null) {
            Log.w(TAG, "getLeBroadcastAssistant(): LocalBluetoothProfileManager is null!");
            return null;
        }

        return profileManager.getLeAudioBroadcastAssistantProfile();
    }

    static String getBroadcastName(BluetoothLeBroadcastMetadata source) {
        String broadcastName = source.getBroadcastName();
        if (broadcastName != null && !broadcastName.isEmpty()) {
            return broadcastName;
        }
        return source.getSubgroups().stream()
                .map(subgroup -> subgroup.getContentMetadata().getProgramInfo())
                .filter(programInfo -> !Strings.isNullOrEmpty(programInfo))
                .findFirst()
                .orElse("Broadcast Id: " + source.getBroadcastId());
    }

    static String getBroadcastName(BluetoothLeBroadcastReceiveState state) {
        return state.getSubgroupMetadata().stream()
                .map(BluetoothLeAudioContentMetadata::getProgramInfo)
                .filter(i -> !Strings.isNullOrEmpty(i))
                .findFirst()
                .orElse("Broadcast Id: " + state.getBroadcastId());
    }

    void startMediaService(Context context, int audioStreamBroadcastId, String title) {
        List<BluetoothDevice> devices =
                getConnectedBluetoothDevices(mBluetoothManager, /* inSharingOnly= */ true);
        if (devices.isEmpty()) {
            return;
        }
        var intent = new Intent(context, AudioStreamMediaService.class);
        intent.putExtra(BROADCAST_ID, audioStreamBroadcastId);
        intent.putExtra(BROADCAST_TITLE, title);
        intent.putParcelableArrayListExtra(DEVICES, new ArrayList<>(devices));
        context.startService(intent);
    }

    static void configureAppBarByOrientation(@Nullable FragmentActivity activity) {
        if (activity != null) {
            AppBarLayout appBarLayout = activity.findViewById(R.id.app_bar);
            if (appBarLayout != null) {
                boolean canAppBarExpand =
                        activity.getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_PORTRAIT;
                appBarLayout.setExpanded(canAppBarExpand);
            }
        }
    }
}
