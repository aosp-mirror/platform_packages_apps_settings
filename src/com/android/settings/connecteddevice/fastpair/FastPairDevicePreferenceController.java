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
import com.android.settings.flags.Flags;
import com.android.settings.overlay.FeatureFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Controller to maintain the {@link PreferenceGroup} for all Fast Pair devices and a "See all"
 * Preference. It uses {@link DevicePreferenceCallback} to add/remove {@link Preference}.
 */
public class FastPairDevicePreferenceController extends BasePreferenceController
        implements DefaultLifecycleObserver, DevicePreferenceCallback {

    private static final String TAG = "FastPairDevicePrefCtr";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final int MAX_DEVICE_NUM = 3;
    private static final String KEY_SEE_ALL = "fast_pair_devices_see_all";

    private final List<Preference> mPreferenceList = new ArrayList<>();

    private PreferenceGroup mPreferenceGroup;
    private FastPairDeviceUpdater mFastPairDeviceUpdater;
    private BluetoothAdapter mBluetoothAdapter;

    @VisibleForTesting Preference mSeeAllPreference;
    @VisibleForTesting IntentFilter mIntentFilter;

    @VisibleForTesting
    BroadcastReceiver mReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    updatePreferenceVisibility();
                }
            };

    public FastPairDevicePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);

        if (Flags.enableSubsequentPairSettingsIntegration()) {
            FastPairFeatureProvider fastPairFeatureProvider =
                    FeatureFactory.getFeatureFactory().getFastPairFeatureProvider();
            mFastPairDeviceUpdater =
                    fastPairFeatureProvider.getFastPairDeviceUpdater(context, this);
        } else {
            Log.d(TAG, "Flag disabled. Ignore.");
            mFastPairDeviceUpdater = null;
        }
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
        mSeeAllPreference = mPreferenceGroup.findPreference(KEY_SEE_ALL);
        updatePreferenceVisibility();
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
    public void onDeviceAdded(Preference preference) {
        if (preference == null) {
            if (DEBUG) {
                Log.d(TAG, "onDeviceAdd receives null preference. Ignore.");
            }
            return;
        }

        // Keep showing the latest MAX_DEVICE_NUM devices.
        // The preference for the latest device has top preference order.
        int idx = Collections.binarySearch(mPreferenceList, preference);
        // Binary search returns the index of the search key if it is contained in the list;
        // otherwise, (-(insertion point) - 1).
        // The insertion point is defined as the point at which the key would be inserted into the
        // list: the index of the first element greater than the key, or list.size() if all elements
        // in the list are less than the specified key.
        if (idx >= 0) {
            if (DEBUG) {
                Log.d(TAG, "onDeviceAdd receives duplicate preference. Ignore.");
            }
            return;
        }
        idx = -1 * (idx + 1);
        mPreferenceList.add(idx, preference);
        if (idx < MAX_DEVICE_NUM) {
            if (mPreferenceList.size() > MAX_DEVICE_NUM) {
                mPreferenceGroup.removePreference(mPreferenceList.get(MAX_DEVICE_NUM));
            }
            mPreferenceGroup.addPreference(preference);
        }
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

        // Keep showing the latest MAX_DEVICE_NUM devices.
        // The preference for the latest device has top preference order.
        final int idx = mPreferenceList.indexOf(preference);
        mPreferenceList.remove(preference);
        if (idx < MAX_DEVICE_NUM) {
            mPreferenceGroup.removePreference(preference);
            if (mPreferenceList.size() >= MAX_DEVICE_NUM) {
                mPreferenceGroup.addPreference(mPreferenceList.get(MAX_DEVICE_NUM - 1));
            }
        }
        updatePreferenceVisibility();
    }

    @VisibleForTesting
    void setPreferenceGroup(PreferenceGroup preferenceGroup) {
        mPreferenceGroup = preferenceGroup;
    }

    @VisibleForTesting
    void updatePreferenceVisibility() {
        if (mBluetoothAdapter != null
                && mBluetoothAdapter.isEnabled()
                && mPreferenceList.size() > 0) {
            mPreferenceGroup.setVisible(true);
            mSeeAllPreference.setVisible(mPreferenceList.size() > MAX_DEVICE_NUM);
        } else {
            mPreferenceGroup.setVisible(false);
            mSeeAllPreference.setVisible(false);
        }
    }
}
