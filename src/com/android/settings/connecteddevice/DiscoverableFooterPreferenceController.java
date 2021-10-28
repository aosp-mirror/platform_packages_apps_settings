/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.text.BidiFormatter;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.AlwaysDiscoverable;
import com.android.settings.bluetooth.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixinCompat;

/**
 * Controller that shows and updates the bluetooth device name
 */
public class DiscoverableFooterPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {
    private static final String KEY = "discoverable_footer_preference";

    @VisibleForTesting
    BroadcastReceiver mBluetoothChangedReceiver;
    @VisibleForTesting
    LocalBluetoothManager mLocalManager;
    private FooterPreferenceMixinCompat mFooterPreferenceMixin;
    private FooterPreference mPreference;
    private BluetoothAdapter mBluetoothAdapter;
    private AlwaysDiscoverable mAlwaysDiscoverable;
    private boolean mIsAlwaysDiscoverable;

    public DiscoverableFooterPreferenceController(Context context) {
        super(context, KEY);
        mLocalManager = Utils.getLocalBtManager(context);
        if (mLocalManager == null) {
            return;
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAlwaysDiscoverable = new AlwaysDiscoverable(context);
        initReceiver();
    }

    private void initReceiver() {
        mBluetoothChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR);
                    updateFooterPreferenceTitle(state);
                }
            }
        };
    }

    public void init(DashboardFragment fragment) {
        mFooterPreferenceMixin = new FooterPreferenceMixinCompat(fragment,
                fragment.getSettingsLifecycle());
    }

    @VisibleForTesting
    void init(FooterPreferenceMixinCompat footerPreferenceMixin, FooterPreference preference,
            AlwaysDiscoverable alwaysDiscoverable) {
        mFooterPreferenceMixin = footerPreferenceMixin;
        mPreference = preference;
        mAlwaysDiscoverable = alwaysDiscoverable;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        addFooterPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    private void addFooterPreference(PreferenceScreen screen) {
        mPreference = mFooterPreferenceMixin.createFooterPreference();
        mPreference.setKey(KEY);
        screen.addPreference(mPreference);
    }

    @Override
    public void onResume() {
        if (mLocalManager == null) {
            return;
        }
        mContext.registerReceiver(mBluetoothChangedReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (mIsAlwaysDiscoverable) {
            mAlwaysDiscoverable.start();
        }
        updateFooterPreferenceTitle(mBluetoothAdapter.getState());
    }

    @Override
    public void onPause() {
        if (mLocalManager == null) {
            return;
        }
        mContext.unregisterReceiver(mBluetoothChangedReceiver);
        if (mIsAlwaysDiscoverable) {
            mAlwaysDiscoverable.stop();
        }
    }

    /**
     * Set whether the device can be discovered. By default the value will be {@code false}.
     *
     * @param isAlwaysDiscoverable {@code true} if the device can be discovered,
     *     otherwise {@code false}
     */
    public void setAlwaysDiscoverable(boolean isAlwaysDiscoverable) {
        mIsAlwaysDiscoverable = isAlwaysDiscoverable;
    }

    private void updateFooterPreferenceTitle (int bluetoothState) {
        if (bluetoothState == BluetoothAdapter.STATE_ON) {
            mPreference.setTitle(getPreferenceTitle());
        } else {
            mPreference.setTitle(R.string.bluetooth_off_footer);
        }
    }

    private CharSequence getPreferenceTitle() {
        final String deviceName = mBluetoothAdapter.getName();
        if (TextUtils.isEmpty(deviceName)) {
            return null;
        }

        return TextUtils.expandTemplate(
                mContext.getText(R.string.bluetooth_device_name_summary),
                BidiFormatter.getInstance().unicodeWrap(deviceName));
    }
}