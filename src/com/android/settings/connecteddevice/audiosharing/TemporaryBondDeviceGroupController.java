/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothEventManager;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.flags.Flags;
import com.android.settingslib.utils.ThreadUtils;

/**
 * Controller to maintain the {@link androidx.preference.PreferenceGroup} for all connected
 * temporary bond devices. It uses {@link DevicePreferenceCallback} to add/remove
 * {@link Preference}
 */
public class TemporaryBondDeviceGroupController extends BasePreferenceController implements
        DefaultLifecycleObserver, DevicePreferenceCallback, BluetoothCallback {
    private static final String TAG = "TemporaryBondDeviceGroupController";
    private static final String KEY = "temp_bond_device_list";

    @Nullable
    private final BluetoothEventManager mEventManager;
    @Nullable
    private PreferenceGroup mPreferenceGroup;
    @Nullable
    private BluetoothDeviceUpdater mBluetoothDeviceUpdater;


    public TemporaryBondDeviceGroupController(@NonNull Context context) {
        super(context, KEY);
        LocalBluetoothManager btManager = Utils.getLocalBtManager(mContext);
        mEventManager = btManager == null ? null : btManager.getEventManager();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (!isAvailable()) {
            Log.d(TAG, "Skip onStart(), feature is not supported.");
            return;
        }
        if (mEventManager == null) {
            Log.d(TAG, "onStart() Bluetooth is not supported on this device");
            return;
        }
        var unused = ThreadUtils.postOnBackgroundThread(() -> {
            mEventManager.registerCallback(this);
            if (mBluetoothDeviceUpdater != null) {
                mBluetoothDeviceUpdater.registerCallback();
                mBluetoothDeviceUpdater.refreshPreference();
            }
        });
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        var unused = ThreadUtils.postOnBackgroundThread(() -> {
            if (mBluetoothDeviceUpdater != null) {
                mBluetoothDeviceUpdater.unregisterCallback();
            }
            if (mEventManager != null) {
                mEventManager.unregisterCallback(this);
                return;
            }
            Log.d(TAG, "onStop() Bluetooth is not supported on this device");
        });
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(KEY);
        if (mPreferenceGroup != null) {
            mPreferenceGroup.setVisible(false);
        }

        if (isAvailable() && mBluetoothDeviceUpdater != null) {
            mBluetoothDeviceUpdater.setPrefContext(screen.getContext());
            mBluetoothDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public void onDeviceAdded(@NonNull Preference preference) {
        if (mPreferenceGroup != null) {
            mPreferenceGroup.addPreference(preference);
            Log.d(TAG, "Temporary bond device added");
            if (mPreferenceGroup.getPreferenceCount() == 1) {
                mPreferenceGroup.setVisible(true);
            }
        }
    }

    @Override
    public void onDeviceRemoved(@NonNull Preference preference) {
        if (mPreferenceGroup != null) {
            mPreferenceGroup.removePreference(preference);
            Log.d(TAG, "Temporary bond device removed");
            if (mPreferenceGroup.getPreferenceCount() == 0) {
                mPreferenceGroup.setVisible(false);
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return (BluetoothUtils.isAudioSharingUIAvailable(mContext)
                && mBluetoothDeviceUpdater != null && Flags.enableTemporaryBondDevicesUi())
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    /**
     * Initialize the controller.
     *
     * @param fragment The fragment to provide the context and metrics category for {@link
     *     TemporaryBondDeviceGroupUpdater} and provide the host for dialogs.
     */
    public void init(@NonNull DashboardFragment fragment) {
        mBluetoothDeviceUpdater = new TemporaryBondDeviceGroupUpdater(fragment.getContext(),
                TemporaryBondDeviceGroupController.this,
                fragment.getMetricsCategory());
    }

    @VisibleForTesting
    void setBluetoothDeviceUpdater(@Nullable BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    @VisibleForTesting
    void setPreferenceGroup(@Nullable PreferenceGroup preferenceGroup) {
        mPreferenceGroup = preferenceGroup;
    }
}
