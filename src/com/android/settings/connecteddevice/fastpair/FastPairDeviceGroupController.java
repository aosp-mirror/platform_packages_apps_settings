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

package com.android.settings.connecteddevice.fastpair;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.flags.Flags;
import com.android.settings.overlay.FeatureFactory;

/**
 * Controller to maintain the {@link PreferenceGroup} for all Fast Pair devices. It uses {@link
 * DevicePreferenceCallback} to add/remove {@link Preference}
 */
public class FastPairDeviceGroupController extends BasePreferenceController
        implements PreferenceControllerMixin, DefaultLifecycleObserver, DevicePreferenceCallback {

    private static final String TAG = "FastPairDeviceGroupCtr";

    private static final String KEY = "fast_pair_device_list";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting PreferenceGroup mPreferenceGroup;
    private final FastPairDeviceUpdater mFastPairDeviceUpdater;
    private final BluetoothAdapter mBluetoothAdapter;
    @VisibleForTesting IntentFilter mIntentFilter;

    @VisibleForTesting
    BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updatePreferenceVisibility();
                }
            };

    public FastPairDeviceGroupController(Context context) {
        super(context, KEY);
        if (Flags.enableSubsequentPairSettingsIntegration()) {
            FastPairFeatureProvider fastPairFeatureProvider =
                    FeatureFactory.getFeatureFactory().getFastPairFeatureProvider();
            mFastPairDeviceUpdater =
                    fastPairFeatureProvider.getFastPairDeviceUpdater(context, this);
        } else {
            Log.d(TAG, "Flag disabled. Ignored.");
            mFastPairDeviceUpdater = null;
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mFastPairDeviceUpdater != null) {
            mFastPairDeviceUpdater.setPreferenceContext(mContext);
            mFastPairDeviceUpdater.registerCallback();
        } else {
            if (DEBUG) {
                Log.d(TAG, "Callback register: Fast Pair device updater is null. Ignore.");
            }
        }
        mContext.registerReceiver(mReceiver, mIntentFilter, Context.RECEIVER_EXPORTED_UNAUDITED);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        if (mFastPairDeviceUpdater != null) {
            mFastPairDeviceUpdater.setPreferenceContext(null);
            mFastPairDeviceUpdater.unregisterCallback();
        } else {
            if (DEBUG) {
                Log.d(TAG, "Callback unregister: Fast Pair device updater is null. Ignore.");
            }
        }
        mContext.unregisterReceiver(mReceiver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreferenceGroup = screen.findPreference(KEY);
        mPreferenceGroup.setVisible(false);

        if (isAvailable()) {
            final Context context = screen.getContext();
            mFastPairDeviceUpdater.setPreferenceContext(context);
            mFastPairDeviceUpdater.forceUpdate();
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return (mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                        && mFastPairDeviceUpdater != null)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onDeviceAdded(Preference preference) {
        if (preference == null) {
            if (DEBUG) {
                Log.d(TAG, "onDeviceAdded receives null preference. Ignore.");
            }
            return;
        }
        mPreferenceGroup.addPreference(preference);
        updatePreferenceVisibility();
    }

    @Override
    public void onDeviceRemoved(Preference preference) {
        if (preference == null) {
            if (DEBUG) {
                Log.d(TAG, "onDeviceRemoved receives null preference. Ignore.");
            }
            return;
        }
        mPreferenceGroup.removePreference(preference);
        updatePreferenceVisibility();
    }

    private void updatePreferenceVisibility() {
        mPreferenceGroup.setVisible(
                mBluetoothAdapter != null
                        && mBluetoothAdapter.isEnabled()
                        && mPreferenceGroup.getPreferenceCount() > 0);
    }

    @VisibleForTesting
    public void setPreferenceGroup(PreferenceGroup preferenceGroup) {
        mPreferenceGroup = preferenceGroup;
    }
}
