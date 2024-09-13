/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.android.settings.network.SatelliteWarningDialogActivity.EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG;
import static com.android.settings.network.SatelliteWarningDialogActivity.TYPE_IS_BLUETOOTH;

import android.app.settings.SettingsEnums;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.network.SatelliteRepository;
import com.android.settings.network.SatelliteWarningDialogActivity;
import com.android.settingslib.bluetooth.BluetoothDeviceFilter;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.widget.FooterPreference;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * BluetoothPairingDetail is a page to scan bluetooth devices and pair them.
 */
public class BluetoothPairingDetail extends BluetoothDevicePairingDetailBase implements
        Indexable {
    private static final String TAG = "BluetoothPairingDetail";

    @VisibleForTesting
    static final String KEY_AVAIL_DEVICES = "available_devices";
    @VisibleForTesting
    static final String KEY_FOOTER_PREF = "footer_preference";

    @VisibleForTesting
    FooterPreference mFooterPreference;
    @VisibleForTesting
    AlwaysDiscoverable mAlwaysDiscoverable;

    public BluetoothPairingDetail() {
        super();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mayStartSatelliteWarningDialog()) {
            finish();
            return;
        }
        use(BluetoothDeviceRenamePreferenceController.class).setFragment(this);
    }

    private boolean mayStartSatelliteWarningDialog() {
        SatelliteRepository satelliteRepository = new SatelliteRepository(this.getContext());
        boolean isSatelliteOn = true;
        try {
            isSatelliteOn =
                    satelliteRepository.requestIsSessionStarted(
                            Executors.newSingleThreadExecutor()).get(3000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Error to get satellite status : " + e);
        }
        if (!isSatelliteOn) {
            return false;
        }
        startActivity(
                new Intent(getContext(), SatelliteWarningDialogActivity.class)
                        .putExtra(
                                EXTRA_TYPE_OF_SATELLITE_WARNING_DIALOG,
                                TYPE_IS_BLUETOOTH)
        );
        return true;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mAlwaysDiscoverable = new AlwaysDiscoverable(getContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        mAvailableDevicesCategory.setProgress(mBluetoothAdapter.isDiscovering());
    }

    @Override
    public void onStop() {
        super.onStop();
        // Make the device only visible to connected devices.
        mAlwaysDiscoverable.stop();
    }

    @Override
    public void initPreferencesFromPreferenceScreen() {
        super.initPreferencesFromPreferenceScreen();
        mFooterPreference = findPreference(KEY_FOOTER_PREF);
        mFooterPreference.setSelectable(false);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BLUETOOTH_PAIRING;
    }

    /**
     * {@inheritDoc}
     *
     * Will update footer and keep the device discoverable as long as the page is visible.
     */
    @VisibleForTesting
    @Override
    public void updateContent(int bluetoothState) {
        super.updateContent(bluetoothState);
        if (bluetoothState == BluetoothAdapter.STATE_ON) {
            if (mInitialScanStarted) {
                // Don't show bonded devices when screen turned back on
                addCachedDevices(BluetoothDeviceFilter.UNBONDED_DEVICE_FILTER);
            }
            updateFooterPreference(mFooterPreference);
            mAlwaysDiscoverable.start();
        }
    }

    @Override
    public void onScanningStateChanged(boolean started) {
        super.onScanningStateChanged(started);
        started |= mScanEnabled;
        mAvailableDevicesCategory.setProgress(started);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_bluetooth;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_pairing_detail;
    }

    @Override
    public String getDeviceListKey() {
        return KEY_AVAIL_DEVICES;
    }
}
