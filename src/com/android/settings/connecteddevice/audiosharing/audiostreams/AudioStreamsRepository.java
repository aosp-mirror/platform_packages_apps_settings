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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.settingslib.bluetooth.BluetoothLeBroadcastMetadataExt;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.utils.ThreadUtils;

import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

/** Manages the caching and storage of Bluetooth audio stream metadata. */
public class AudioStreamsRepository {

    private static final String TAG = "AudioStreamsRepository";
    private static final boolean DEBUG = BluetoothUtils.D;

    private static final String PREF_KEY = "bluetooth_audio_stream_pref";
    private static final String METADATA_KEY = "bluetooth_audio_stream_metadata";

    @Nullable private static AudioStreamsRepository sInstance = null;

    private AudioStreamsRepository() {}

    /**
     * Gets the single instance of AudioStreamsRepository.
     *
     * @return The AudioStreamsRepository instance.
     */
    public static synchronized AudioStreamsRepository getInstance() {
        if (sInstance == null) {
            sInstance = new AudioStreamsRepository();
        }
        return sInstance;
    }

    private final ConcurrentHashMap<Integer, BluetoothLeBroadcastMetadata>
            mBroadcastIdToMetadataCacheMap = new ConcurrentHashMap<>();

    /**
     * Caches BluetoothLeBroadcastMetadata in a local cache.
     *
     * @param metadata The BluetoothLeBroadcastMetadata to be cached.
     */
    void cacheMetadata(BluetoothLeBroadcastMetadata metadata) {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "cacheMetadata(): broadcastId "
                            + metadata.getBroadcastId()
                            + " saved in local cache.");
        }
        mBroadcastIdToMetadataCacheMap.put(metadata.getBroadcastId(), metadata);
    }

    /**
     * Gets cached BluetoothLeBroadcastMetadata by broadcastId.
     *
     * @param broadcastId The broadcastId to look up in the cache.
     * @return The cached BluetoothLeBroadcastMetadata or null if not found.
     */
    @Nullable
    BluetoothLeBroadcastMetadata getCachedMetadata(int broadcastId) {
        var metadata = mBroadcastIdToMetadataCacheMap.get(broadcastId);
        if (metadata == null) {
            Log.w(
                    TAG,
                    "getCachedMetadata(): broadcastId not found in"
                            + " mBroadcastIdToMetadataCacheMap.");
            return null;
        }
        return metadata;
    }

    /**
     * Saves metadata to SharedPreferences asynchronously.
     *
     * @param context The context.
     * @param metadata The BluetoothLeBroadcastMetadata to be saved.
     */
    void saveMetadata(Context context, BluetoothLeBroadcastMetadata metadata) {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            SharedPreferences sharedPref =
                                    context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
                            if (sharedPref != null) {
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putString(
                                        METADATA_KEY,
                                        BluetoothLeBroadcastMetadataExt.INSTANCE.toQrCodeString(
                                                metadata));
                                editor.apply();
                                if (DEBUG) {
                                    Log.d(
                                            TAG,
                                            "saveMetadata(): broadcastId "
                                                    + metadata.getBroadcastId()
                                                    + " metadata saved in storage.");
                                }
                            }
                        });
    }

    /**
     * Gets saved metadata from SharedPreferences.
     *
     * @param context The context.
     * @param broadcastId The broadcastId to retrieve metadata for.
     * @return The saved BluetoothLeBroadcastMetadata or null if not found.
     */
    @Nullable
    BluetoothLeBroadcastMetadata getSavedMetadata(Context context, int broadcastId) {
        SharedPreferences sharedPref = context.getSharedPreferences(PREF_KEY, Context.MODE_PRIVATE);
        if (sharedPref != null) {
            String savedMetadataStr = sharedPref.getString(METADATA_KEY, null);
            if (savedMetadataStr == null) {
                Log.w(TAG, "getSavedMetadata(): savedMetadataStr is null");
                return null;
            }
            var savedMetadata =
                    BluetoothLeBroadcastMetadataExt.INSTANCE.convertToBroadcastMetadata(
                            savedMetadataStr);
            if (savedMetadata == null || savedMetadata.getBroadcastId() != broadcastId) {
                Log.w(TAG, "getSavedMetadata(): savedMetadata doesn't match broadcast Id.");
                return null;
            }
            if (DEBUG) {
                Log.d(
                        TAG,
                        "getSavedMetadata(): broadcastId "
                                + savedMetadata.getBroadcastId()
                                + " metadata found in storage.");
            }
            return savedMetadata;
        }
        return null;
    }
}
