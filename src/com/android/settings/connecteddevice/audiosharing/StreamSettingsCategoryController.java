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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager;

public class StreamSettingsCategoryController extends BasePreferenceController
        implements DefaultLifecycleObserver, LocalBluetoothProfileManager.ServiceListener {
    private static final String TAG = "StreamSettingsCategoryController";
    private final BluetoothAdapter mBluetoothAdapter;
    @Nullable private final LocalBluetoothManager mBtManager;
    @Nullable private final LocalBluetoothProfileManager mProfileManager;
    @Nullable private Preference mPreference;
    @VisibleForTesting final IntentFilter mIntentFilter;

    @VisibleForTesting
    BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
                    updateVisibility();
                }
            };

    public StreamSettingsCategoryController(Context context, String key) {
        super(context, key);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBtManager = Utils.getLocalBtManager(context);
        mProfileManager = mBtManager == null ? null : mBtManager.getProfileManager();
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) return;
        mContext.registerReceiver(mReceiver, mIntentFilter, Context.RECEIVER_EXPORTED_UNAUDITED);
        if (!isProfileReady() && mProfileManager != null) {
            mProfileManager.addServiceListener(this);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) return;
        mContext.unregisterReceiver(mReceiver);
        if (mProfileManager != null) {
            mProfileManager.removeServiceListener(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateVisibility();
    }

    @Override
    public int getAvailabilityStatus() {
        return BluetoothUtils.isAudioSharingEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onServiceConnected() {
        if (isAvailable() && isProfileReady()) {
            updateVisibility();
            if (mProfileManager != null) {
                mProfileManager.removeServiceListener(this);
            }
        }
    }

    @Override
    public void onServiceDisconnected() {
        // Do nothing
    }

    private void updateVisibility() {
        if (mPreference == null) {
            Log.w(TAG, "Skip updateVisibility, null preference");
            return;
        }
        if (!isAvailable()) {
            Log.w(TAG, "Skip updateVisibility, unavailable preference");
            AudioSharingUtils.postOnMainThread(
                    mContext,
                    () -> { // Check nullability to pass NullAway check
                        if (mPreference != null) {
                            mPreference.setVisible(false);
                        }
                    });
            return;
        }
        boolean visible = isBluetoothOn() && isProfileReady();
        AudioSharingUtils.postOnMainThread(
                mContext,
                () -> { // Check nullability to pass NullAway check
                    if (mPreference != null) {
                        mPreference.setVisible(visible);
                    }
                });
    }

    private boolean isBluetoothOn() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private boolean isProfileReady() {
        return AudioSharingUtils.isAudioSharingProfileReady(mProfileManager);
    }
}
