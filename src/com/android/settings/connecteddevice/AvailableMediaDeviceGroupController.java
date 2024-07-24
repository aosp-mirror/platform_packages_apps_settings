/*
 * Copyright 2018 The Android Open Source Project
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
package com.android.settings.connecteddevice;

import static com.android.settingslib.Utils.isAudioModeOngoingCall;

import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.accessibility.HearingAidUtils;
import com.android.settings.bluetooth.AvailableMediaBluetoothDeviceUpdater;
import com.android.settings.bluetooth.BluetoothDeviceUpdater;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.BluetoothCallback;
import com.android.settingslib.bluetooth.BluetoothUtils;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.Lifecycle;

/**
 * Controller to maintain the {@link androidx.preference.PreferenceGroup} for all available media
 * devices. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class AvailableMediaDeviceGroupController extends BasePreferenceController
        implements DefaultLifecycleObserver, DevicePreferenceCallback, BluetoothCallback {
    private static final boolean DEBUG = BluetoothUtils.D;

    private static final String TAG = "AvailableMediaDeviceGroupController";
    private static final String KEY = "available_device_list";

    @VisibleForTesting @Nullable PreferenceGroup mPreferenceGroup;
    @VisibleForTesting LocalBluetoothManager mLocalBluetoothManager;
    @Nullable private BluetoothDeviceUpdater mBluetoothDeviceUpdater;
    @Nullable private FragmentManager mFragmentManager;

    public AvailableMediaDeviceGroupController(
            Context context,
            @Nullable DashboardFragment fragment,
            @Nullable Lifecycle lifecycle) {
        super(context, KEY);
        if (fragment != null) {
            init(fragment);
        }
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "onStart() Bluetooth is not supported on this device");
            return;
        }
        mLocalBluetoothManager.getEventManager().registerCallback(this);
        if (mBluetoothDeviceUpdater != null) {
            mBluetoothDeviceUpdater.registerCallback();
            mBluetoothDeviceUpdater.refreshPreference();
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mLocalBluetoothManager == null) {
            Log.e(TAG, "onStop() Bluetooth is not supported on this device");
            return;
        }
        if (mBluetoothDeviceUpdater != null) {
            mBluetoothDeviceUpdater.unregisterCallback();
        }
        mLocalBluetoothManager.getEventManager().unregisterCallback(this);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreferenceGroup = screen.findPreference(KEY);
        if (mPreferenceGroup != null) {
            mPreferenceGroup.setVisible(false);
        }

        if (isAvailable()) {
            updateTitle();
            if (mBluetoothDeviceUpdater != null) {
                mBluetoothDeviceUpdater.setPrefContext(screen.getContext());
                mBluetoothDeviceUpdater.forceUpdate();
            }
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (mPreferenceGroup != null) {
            if (mPreferenceGroup.getPreferenceCount() == 0) {
                mPreferenceGroup.setVisible(true);
            }
            mPreferenceGroup.addPreference(preference);
        }
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        if (mPreferenceGroup != null) {
            mPreferenceGroup.removePreference(preference);
            if (mPreferenceGroup.getPreferenceCount() == 0) {
                mPreferenceGroup.setVisible(false);
            }
        }
    }

    public void init(DashboardFragment fragment) {
        mFragmentManager = fragment.getParentFragmentManager();
        mBluetoothDeviceUpdater =
                new AvailableMediaBluetoothDeviceUpdater(
                        fragment.getContext(),
                        AvailableMediaDeviceGroupController.this,
                        fragment.getMetricsCategory());
    }

    @VisibleForTesting
    public void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    @VisibleForTesting
    public void setBluetoothDeviceUpdater(BluetoothDeviceUpdater bluetoothDeviceUpdater) {
        mBluetoothDeviceUpdater = bluetoothDeviceUpdater;
    }

    @Override
    public void onAudioModeChanged() {
        updateTitle();
    }

    @Override
    public void onActiveDeviceChanged(CachedBluetoothDevice activeDevice, int bluetoothProfile) {
        // exclude inactive device
        if (activeDevice == null) {
            return;
        }

        if (bluetoothProfile == BluetoothProfile.HEARING_AID) {
            HearingAidUtils.launchHearingAidPairingDialog(
                    mFragmentManager, activeDevice, getMetricsCategory());
        }
    }

    private void updateTitle() {
        if (mPreferenceGroup != null) {
            if (isAudioModeOngoingCall(mContext)) {
                // in phone call
                mPreferenceGroup.setTitle(
                        mContext.getString(R.string.connected_device_call_device_title));
            } else {
                // without phone call
                mPreferenceGroup.setTitle(
                        mContext.getString(R.string.connected_device_media_device_title));
            }
        }
    }
}
