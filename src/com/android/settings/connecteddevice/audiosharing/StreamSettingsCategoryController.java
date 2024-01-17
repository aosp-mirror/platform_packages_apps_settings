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
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

public class StreamSettingsCategoryController extends BasePreferenceController
        implements DefaultLifecycleObserver {
    private static final String TAG = "StreamSettingsCategoryController";
    private final BluetoothAdapter mBluetoothAdapter;
    private final IntentFilter mIntentFilter;
    private @Nullable Preference mPreference;
    private BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;
                    int adapterState =
                            intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothDevice.ERROR);
                    mContext.getMainExecutor()
                            .execute(
                                    () -> {
                                        if (mPreference == null) {
                                            Log.w(
                                                    TAG,
                                                    "Skip BT state change due to mPreference "
                                                            + "is null");
                                        } else {
                                            mPreference.setVisible(
                                                    adapterState == BluetoothAdapter.STATE_ON);
                                        }
                                    });
                }
            };

    public StreamSettingsCategoryController(Context context, String key) {
        super(context, key);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mContext.registerReceiver(mReceiver, mIntentFilter, Context.RECEIVER_EXPORTED_UNAUDITED);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        if (mPreference != null) {
            mPreference.setVisible(isBluetoothStateOn());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AudioSharingUtils.isFeatureEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    private boolean isBluetoothStateOn() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }
}
