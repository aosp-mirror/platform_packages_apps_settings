/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.development.bluetooth;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.development.BluetoothA2dpConfigStore;
import com.android.settings.development.BluetoothServiceConnectionListener;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnDestroy;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

import java.util.List;

/**
 * Abstract class for Bluetooth A2DP config controller in developer option.
 */
public abstract class AbstractBluetoothPreferenceController extends
        DeveloperOptionsPreferenceController implements BluetoothServiceConnectionListener,
        LifecycleObserver, OnDestroy, PreferenceControllerMixin {

    protected volatile BluetoothA2dp mBluetoothA2dp;

    @VisibleForTesting
    BluetoothAdapter mBluetoothAdapter;

    public AbstractBluetoothPreferenceController(Context context, Lifecycle lifecycle,
                                                 BluetoothA2dpConfigStore store) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mBluetoothAdapter = context.getSystemService(BluetoothManager.class).getAdapter();
    }

    @Override
    public void onBluetoothServiceConnected(BluetoothA2dp bluetoothA2dp) {
        mBluetoothA2dp = bluetoothA2dp;
        updateState(mPreference);
    }

    @Override
    public void onBluetoothCodecUpdated() {
        updateState(mPreference);
    }

    @Override
    public void onBluetoothServiceDisconnected() {
        mBluetoothA2dp = null;
        updateState(mPreference);
    }

    @Override
    public void onDestroy() {
        mBluetoothA2dp = null;
    }

    /**
     * Callback interface for this class to manipulate data from controller.
     */
    public interface Callback {
        /**
         * Callback method to notify preferences when the Bluetooth A2DP config is changed.
         */
        void onBluetoothCodecChanged();

        /**
         * Callback method to notify preferences when the HD audio(optional codec) state is changed.
         *
         * @param enabled Is {@code true} when the setting is enabled.
         */
        void onBluetoothHDAudioEnabled(boolean enabled);
    }

    protected BluetoothDevice getA2dpActiveDevice() {
        if (mBluetoothAdapter == null) {
            return null;
        }
        List<BluetoothDevice> activeDevices =
                mBluetoothAdapter.getActiveDevices(BluetoothProfile.A2DP);
        return (activeDevices.size() > 0) ? activeDevices.get(0) : null;
    }
}
