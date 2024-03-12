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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothLeBroadcastAssistant;
import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothLeBroadcastReceiveState;
import android.util.Log;

import com.android.settingslib.bluetooth.BluetoothUtils;

import java.util.Locale;

public class AudioStreamsBroadcastAssistantCallback
        implements BluetoothLeBroadcastAssistant.Callback {

    private static final String TAG = "AudioStreamsBroadcastAssistantCallback";
    private static final boolean DEBUG = BluetoothUtils.D;

    private final AudioStreamsProgressCategoryController mCategoryController;

    public AudioStreamsBroadcastAssistantCallback(
            AudioStreamsProgressCategoryController audioStreamsProgressCategoryController) {
        mCategoryController = audioStreamsProgressCategoryController;
    }

    @Override
    public void onReceiveStateChanged(
            BluetoothDevice sink, int sourceId, BluetoothLeBroadcastReceiveState state) {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onReceiveStateChanged() sink : "
                            + sink.getAddress()
                            + " sourceId: "
                            + sourceId
                            + " state: "
                            + state);
        }
        mCategoryController.handleSourceConnected(state);
    }

    @Override
    public void onSearchStartFailed(int reason) {
        Log.w(TAG, "onSearchStartFailed() reason : " + reason);
        mCategoryController.showToast(
                String.format(Locale.US, "Failed to start scanning, reason %d", reason));
    }

    @Override
    public void onSearchStarted(int reason) {
        if (mCategoryController == null) {
            Log.w(TAG, "onSearchStarted() : mCategoryController is null!");
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "onSearchStarted() reason : " + reason);
        }
        mCategoryController.setScanning(true);
    }

    @Override
    public void onSearchStopFailed(int reason) {
        Log.w(TAG, "onSearchStopFailed() reason : " + reason);
        mCategoryController.showToast(
                String.format(Locale.US, "Failed to stop scanning, reason %d", reason));
    }

    @Override
    public void onSearchStopped(int reason) {
        if (mCategoryController == null) {
            Log.w(TAG, "onSearchStopped() : mCategoryController is null!");
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "onSearchStopped() reason : " + reason);
        }
        mCategoryController.setScanning(false);
    }

    @Override
    public void onSourceAddFailed(
            BluetoothDevice sink, BluetoothLeBroadcastMetadata source, int reason) {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onSourceAddFailed() sink : "
                            + sink.getAddress()
                            + " source: "
                            + source
                            + " reason: "
                            + reason);
        }
        mCategoryController.showToast(
                String.format(Locale.US, "Failed to join broadcast, reason %d", reason));
    }

    @Override
    public void onSourceAdded(BluetoothDevice sink, int sourceId, int reason) {
        if (DEBUG) {
            Log.d(
                    TAG,
                    "onSourceAdded() sink : "
                            + sink.getAddress()
                            + " sourceId: "
                            + sourceId
                            + " reason: "
                            + reason);
        }
    }

    @Override
    public void onSourceFound(BluetoothLeBroadcastMetadata source) {
        if (mCategoryController == null) {
            Log.w(TAG, "onSourceFound() : mCategoryController is null!");
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "onSourceFound() broadcastId : " + source.getBroadcastId());
        }
        mCategoryController.handleSourceFound(source);
    }

    @Override
    public void onSourceLost(int broadcastId) {
        if (DEBUG) {
            Log.d(TAG, "onSourceLost() broadcastId : " + broadcastId);
        }
        mCategoryController.handleSourceLost(broadcastId);
    }

    @Override
    public void onSourceModified(BluetoothDevice sink, int sourceId, int reason) {}

    @Override
    public void onSourceModifyFailed(BluetoothDevice sink, int sourceId, int reason) {}

    @Override
    public void onSourceRemoveFailed(BluetoothDevice sink, int sourceId, int reason) {
        Log.w(TAG, "onSourceRemoveFailed() sourceId : " + sourceId + " reason : " + reason);
        mCategoryController.showToast(
                String.format(
                        Locale.US,
                        "Failed to remove source %d for sink %s",
                        sourceId,
                        sink.getAddress()));
    }

    @Override
    public void onSourceRemoved(BluetoothDevice sink, int sourceId, int reason) {
        if (DEBUG) {
            Log.d(TAG, "onSourceRemoved() sourceId : " + sourceId + " reason : " + reason);
        }
        mCategoryController.showToast(
                String.format(
                        Locale.US, "Source %d removed for sink %s", sourceId, sink.getAddress()));
    }
}
