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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingBasePreferenceController;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.flags.Flags;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AudioStreamsCategoryController extends AudioSharingBasePreferenceController {
    private static final String TAG = "AudioStreamsCategoryController";
    private static final boolean DEBUG = BluetoothUtils.D;
    private final LocalBluetoothManager mLocalBtManager;
    private final Executor mExecutor;
    private final BluetoothCallback mBluetoothCallback =
            new BluetoothCallback() {
                @Override
                public void onProfileConnectionStateChanged(
                        @NonNull CachedBluetoothDevice cachedDevice,
                        @ConnectionState int state,
                        int bluetoothProfile) {
                    if (bluetoothProfile == BluetoothProfile.LE_AUDIO_BROADCAST_ASSISTANT
                            && (state == BluetoothAdapter.STATE_CONNECTED
                                    || state == BluetoothAdapter.STATE_DISCONNECTED)) {
                        updateVisibility();
                    }
                }
            };

    public AudioStreamsCategoryController(Context context, String key) {
        super(context, key);
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        mExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) return;
        super.onStart(owner);
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().registerCallback(mBluetoothCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) return;
        super.onStop(owner);
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().unregisterCallback(mBluetoothCallback);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return Flags.enableLeAudioQrCodePrivateBroadcastSharing()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void updateVisibility() {
        if (mPreference == null) return;
        mExecutor.execute(
                () -> {
                    if (!isAvailable()) {
                        Log.d(TAG, "skip updateVisibility, unavailable preference");
                        AudioSharingUtils.postOnMainThread(
                                mContext,
                                () -> { // Check nullability to pass NullAway check
                                    if (mPreference != null) {
                                        mPreference.setVisible(false);
                                    }
                                });
                        return;
                    }
                    boolean hasConnectedLe =
                            AudioStreamsHelper.getCachedBluetoothDeviceInSharingOrLeConnected(
                                            mLocalBtManager)
                                    .isPresent();
                    boolean isProfileReady =
                            AudioSharingUtils.isAudioSharingProfileReady(
                                    mLocalBtManager.getProfileManager());
                    boolean isBroadcasting = isBroadcasting();
                    boolean isBluetoothOn = isBluetoothStateOn();
                    if (DEBUG) {
                        Log.d(
                                TAG,
                                "updateVisibility() isBroadcasting : "
                                        + isBroadcasting
                                        + " hasConnectedLe : "
                                        + hasConnectedLe
                                        + " is BT on : "
                                        + isBluetoothOn
                                        + " is profile ready : "
                                        + isProfileReady);
                    }
                    AudioSharingUtils.postOnMainThread(
                            mContext,
                            () -> { // Check nullability to pass NullAway check
                                if (mPreference != null) {
                                    mPreference.setVisible(
                                            isProfileReady
                                                    && isBluetoothOn
                                                    && hasConnectedLe
                                                    && !isBroadcasting);
                                }
                            });
                });
    }
}
