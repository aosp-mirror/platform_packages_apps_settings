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
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.utils.ThreadUtils;

public class AudioStreamsScanQrCodeController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    static final int REQUEST_SCAN_BT_BROADCAST_QR_CODE = 0;
    private static final String TAG = "AudioStreamsProgressCategoryController";
    @VisibleForTesting static final String KEY = "audio_streams_scan_qr_code";

    @VisibleForTesting
    final BluetoothCallback mBluetoothCallback =
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

    @Nullable private final LocalBluetoothManager mLocalBtManager;
    @Nullable private AudioStreamsDashboardFragment mFragment;
    @Nullable private Preference mPreference;

    public AudioStreamsScanQrCodeController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLocalBtManager = Utils.getLocalBtManager(mContext);
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
                        new SubSettingLauncher(mContext)
                                .setTitleRes(R.string.audio_streams_main_page_scan_qr_code_title)
                                .setDestination(AudioStreamsQrCodeScanFragment.class.getName())
                                .setResultListener(mFragment, REQUEST_SCAN_BT_BROADCAST_QR_CODE)
                                .setSourceMetricsCategory(mFragment.getMetricsCategory())
                                .launch();
                        return true;
                    }
                    return false;
                });
    }

    private void updateVisibility() {
        var unused =
                ThreadUtils.postOnBackgroundThread(
                        () -> {
                            boolean hasConnectedLe =
                                    AudioStreamsHelper
                                            .getCachedBluetoothDeviceInSharingOrLeConnected(
                                                    mLocalBtManager)
                                            .isPresent();
                            ThreadUtils.postOnMainThread(
                                    () -> {
                                        if (mPreference != null) {
                                            mPreference.setVisible(hasConnectedLe);
                                        }
                                    });
                        });
    }
}
