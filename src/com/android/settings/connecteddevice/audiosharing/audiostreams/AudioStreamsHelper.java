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

import static java.util.Collections.emptyList;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.util.Log;

import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothLeBroadcastAssistant;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nullable;

/**
 * A helper class that adds, removes and retrieves LE broadcast sources for all active sink devices.
 */
class AudioStreamsHelper {

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
    void addSource(BluetoothLeBroadcastMetadata source) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "addSource(): LeBroadcastAssistant is null!");
            return;
        }
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            for (var sink : getActiveSinksOnAssistant(mBluetoothManager)) {
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
    void removeSource(int broadcastId) {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "removeSource(): LeBroadcastAssistant is null!");
            return;
        }
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            for (var sink : getActiveSinksOnAssistant(mBluetoothManager)) {
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

    /** Retrieves a list of all LE broadcast receive states from active sinks. */
    List<BluetoothLeBroadcastReceiveState> getAllSources() {
        if (mLeBroadcastAssistant == null) {
            Log.w(TAG, "getAllSources(): LeBroadcastAssistant is null!");
            return emptyList();
        }
        return getActiveSinksOnAssistant(mBluetoothManager).stream()
                .flatMap(sink -> mLeBroadcastAssistant.getAllSources(sink).stream())
                .toList();
    }

    @Nullable
    LocalBluetoothLeBroadcastAssistant getLeBroadcastAssistant() {
        return mLeBroadcastAssistant;
    }

    static boolean isConnected(BluetoothLeBroadcastReceiveState state) {
        return state.getPaSyncState() == BluetoothLeBroadcastReceiveState.PA_SYNC_STATE_SYNCHRONIZED
                && state.getBigEncryptionState()
                        == BluetoothLeBroadcastReceiveState.BIG_ENCRYPTION_STATE_DECRYPTING;
    }

    private static List<BluetoothDevice> getActiveSinksOnAssistant(
            @Nullable LocalBluetoothManager manager) {
        if (manager == null) {
            Log.w(TAG, "getActiveSinksOnAssistant(): LocalBluetoothManager is null!");
            return emptyList();
        }
        return AudioSharingUtils.getActiveSinkOnAssistant(manager)
                .map(
                        cachedBluetoothDevice ->
                                Stream.concat(
                                                Stream.of(cachedBluetoothDevice.getDevice()),
                                                cachedBluetoothDevice.getMemberDevice().stream()
                                                        .map(CachedBluetoothDevice::getDevice))
                                        .toList())
                .orElse(emptyList());
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
}
