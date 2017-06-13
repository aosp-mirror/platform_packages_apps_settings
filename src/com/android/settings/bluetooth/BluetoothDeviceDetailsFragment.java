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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDeviceDetailsFragment extends RestrictedDashboardFragment {
    public static final String KEY_DEVICE_ADDRESS = "device_address";
    private static final String TAG = "BTDeviceDetailsFrg";

    private String mDeviceAddress;

    public BluetoothDeviceDetailsFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public void onAttach(Context context) {
        mDeviceAddress = getArguments().getString(KEY_DEVICE_ADDRESS);
        super.onAttach(context);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.BLUETOOTH_DEVICE_DETAILS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.bluetooth_device_details_fragment;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        ArrayList<AbstractPreferenceController> controllers = new ArrayList<>();
        LocalBluetoothManager manager = Utils.getLocalBtManager(context);
        BluetoothDevice remoteDevice = manager.getBluetoothAdapter().getRemoteDevice(
                mDeviceAddress);
        CachedBluetoothDevice device = manager.getCachedDeviceManager().findDevice(remoteDevice);
        if (device != null) {
            Lifecycle lifecycle = getLifecycle();
            controllers.add(new BluetoothDetailsHeaderController(context, this, device, lifecycle));
            controllers.add(new BluetoothDetailsButtonsController(context, this, device,
                    lifecycle));
            controllers.add(new BluetoothDetailsProfilesController(context, this, manager, device,
                    lifecycle));
            controllers.add(new BluetoothDetailsMacAddressController(context, this, device,
                    lifecycle));
        }
        return controllers;
    }
}
