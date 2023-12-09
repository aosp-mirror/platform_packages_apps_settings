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

import android.bluetooth.BluetoothLeBroadcastMetadata;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.audiosharing.AudioSharingUtils;
import com.android.settings.connecteddevice.audiosharing.audiostreams.qrcode.QrCodeScanModeActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.BluetoothBroadcastUtils;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

public class AudioStreamsScanQrCodeController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    static final int REQUEST_SCAN_BT_BROADCAST_QR_CODE = 0;
    private static final String TAG = "AudioStreamsProgressCategoryController";
    private static final boolean DEBUG = BluetoothUtils.D;
    private static final String KEY = "audio_streams_scan_qr_code";
    private final BluetoothCallback mBluetoothCallback =
            new BluetoothCallback() {
                @Override
                public void onActiveDeviceChanged(
                        @Nullable CachedBluetoothDevice activeDevice, int bluetoothProfile) {
                    if (bluetoothProfile == BluetoothProfile.LE_AUDIO) {
                        updateVisibility();
                    }
                }
            };

    private final LocalBluetoothManager mLocalBtManager;
    private final AudioStreamsHelper mAudioStreamsHelper;
    private AudioStreamsDashboardFragment mFragment;
    private Preference mPreference;

    public AudioStreamsScanQrCodeController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLocalBtManager = Utils.getLocalBtManager(mContext);
        mAudioStreamsHelper = new AudioStreamsHelper(mLocalBtManager);
    }

    public void setFragment(AudioStreamsDashboardFragment fragment) {
        mFragment = fragment;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().registerCallback(mBluetoothCallback);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mLocalBtManager != null) {
            mLocalBtManager.getEventManager().unregisterCallback(mBluetoothCallback);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference == null) {
            Log.w(TAG, "displayPreference() mPreference is null!");
            return;
        }
        mPreference.setOnPreferenceClickListener(
                preference -> {
                    if (mFragment == null) {
                        Log.w(TAG, "displayPreference() mFragment is null!");
                        return false;
                    }
                    if (preference.getKey().equals(KEY)) {
                        Intent intent = new Intent(mContext, QrCodeScanModeActivity.class);
                        intent.setAction(
                                BluetoothBroadcastUtils.ACTION_BLUETOOTH_LE_AUDIO_QR_CODE_SCANNER);
                        mFragment.startActivityForResult(intent, REQUEST_SCAN_BT_BROADCAST_QR_CODE);
                        if (DEBUG) {
                            Log.w(TAG, "displayPreference() sent intent : " + intent);
                        }
                        return true;
                    }
                    return false;
                });
    }

    void addSource(BluetoothLeBroadcastMetadata source) {
        mAudioStreamsHelper.addSource(source);
    }

    private void updateVisibility() {
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    boolean hasActiveLe =
                            AudioSharingUtils.getActiveSinkOnAssistant(mLocalBtManager).isPresent();
                    ThreadUtils.postOnMainThread(() -> mPreference.setVisible(hasActiveLe));
                });
    }
}
