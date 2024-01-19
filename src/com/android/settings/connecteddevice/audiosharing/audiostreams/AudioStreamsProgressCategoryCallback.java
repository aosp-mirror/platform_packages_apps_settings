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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.util.Log;

import java.util.Locale;

public class AudioStreamsProgressCategoryCallback extends AudioStreamsBroadcastAssistantCallback {
    private static final String TAG = "AudioStreamsProgressCategoryCallback";

    private final AudioStreamsProgressCategoryController mCategoryController;

    public AudioStreamsProgressCategoryCallback(
            AudioStreamsProgressCategoryController audioStreamsProgressCategoryController) {
        mCategoryController = audioStreamsProgressCategoryController;
    }

    @Override
    public void onReceiveStateChanged(
            BluetoothDevice sink, int sourceId, BluetoothLeBroadcastReceiveState state) {
        super.onReceiveStateChanged(sink, sourceId, state);
        mCategoryController.handleSourceConnected(state);
    }

    @Override
    public void onSearchStartFailed(int reason) {
        super.onSearchStartFailed(reason);
        mCategoryController.showToast(
                String.format(Locale.US, "Failed to start scanning, reason %d", reason));
    }

    @Override
    public void onSearchStarted(int reason) {
        super.onSearchStarted(reason);
        if (mCategoryController == null) {
            Log.w(TAG, "onSearchStarted() : mCategoryController is null!");
            return;
        }
        mCategoryController.setScanning(true);
    }

    @Override
    public void onSearchStopFailed(int reason) {
        super.onSearchStopFailed(reason);
        mCategoryController.showToast(
                String.format(Locale.US, "Failed to stop scanning, reason %d", reason));
    }

    @Override
    public void onSearchStopped(int reason) {
        super.onSearchStopped(reason);
        if (mCategoryController == null) {
            Log.w(TAG, "onSearchStopped() : mCategoryController is null!");
            return;
        }
        mCategoryController.setScanning(false);
    }

    @Override
    public void onSourceAddFailed(
            BluetoothDevice sink, BluetoothLeBroadcastMetadata source, int reason) {
        super.onSourceAddFailed(sink, source, reason);
        mCategoryController.showToast(
                String.format(Locale.US, "Failed to join broadcast, reason %d", reason));
    }

    @Override
    public void onSourceFound(BluetoothLeBroadcastMetadata source) {
        super.onSourceFound(source);
        if (mCategoryController == null) {
            Log.w(TAG, "onSourceFound() : mCategoryController is null!");
            return;
        }
        mCategoryController.handleSourceFound(source);
    }

    @Override
    public void onSourceLost(int broadcastId) {
        super.onSourceLost(broadcastId);
        mCategoryController.handleSourceLost(broadcastId);
    }

    @Override
    public void onSourceRemoveFailed(BluetoothDevice sink, int sourceId, int reason) {
        super.onSourceRemoveFailed(sink, sourceId, reason);
        mCategoryController.showToast(
                String.format(
                        Locale.US,
                        "Failed to remove source %d for sink %s",
                        sourceId,
                        sink.getAddress()));
    }

    @Override
    public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
        super.onSourceRemoved(sink, sourceId, reason);
        mCategoryController.handleSourceRemoved();
    }
}
