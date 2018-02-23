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
package com.android.settings.connecteddevice.usb;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.GearPreference;

/**
 * Controller to maintain connected usb device
 */
public class ConnectedUsbDeviceUpdater {
    private DashboardFragment mFragment;
    private UsbBackend mUsbBackend;
    private DevicePreferenceCallback mDevicePreferenceCallback;
    @VisibleForTesting
    GearPreference mUsbPreference;
    @VisibleForTesting
    UsbConnectionBroadcastReceiver mUsbReceiver;

    @VisibleForTesting
    UsbConnectionBroadcastReceiver.UsbConnectionListener mUsbConnectionListener =
            (connected, newMode) -> {
                if (connected) {
                    mUsbPreference.setSummary(getSummary(mUsbBackend.getCurrentMode()));
                    mDevicePreferenceCallback.onDeviceAdded(mUsbPreference);
                } else {
                    mDevicePreferenceCallback.onDeviceRemoved(mUsbPreference);
                }
            };

    public ConnectedUsbDeviceUpdater(DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback) {
        this(fragment, devicePreferenceCallback, new UsbBackend(fragment.getContext()));
    }

    @VisibleForTesting
    ConnectedUsbDeviceUpdater(DashboardFragment fragment,
            DevicePreferenceCallback devicePreferenceCallback, UsbBackend usbBackend) {
        mFragment = fragment;
        mDevicePreferenceCallback = devicePreferenceCallback;
        mUsbBackend = usbBackend;
        mUsbReceiver = new UsbConnectionBroadcastReceiver(fragment.getContext(),
                mUsbConnectionListener, mUsbBackend);
    }

    public void registerCallback() {
        // This method could handle multiple register
        mUsbReceiver.register();
    }

    public void unregisterCallback() {
        mUsbReceiver.unregister();
    }

    public void initUsbPreference(Context context) {
        mUsbPreference = new GearPreference(context, null /* AttributeSet */);
        mUsbPreference.setTitle(R.string.usb_pref);
        mUsbPreference.setIcon(R.drawable.ic_usb);
        mUsbPreference.setSelectable(false);
        mUsbPreference.setOnGearClickListener((GearPreference p) -> {
            // New version - uses a separate screen.
            new SubSettingLauncher(mFragment.getContext())
                    .setDestination(UsbDetailsFragment.class.getName())
                    .setTitle(R.string.device_details_title)
                    .setSourceMetricsCategory(mFragment.getMetricsCategory())
                    .launch();
        });

        forceUpdate();
    }

    private void forceUpdate() {
        // Register so we can get the connection state from sticky intent.
        //TODO(b/70336520): Use an API to get data instead of sticky intent
        mUsbReceiver.register();
    }

    public static int getSummary(int mode) {
        switch (mode) {
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_summary_charging_only;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_NONE:
                return R.string.usb_summary_power_only;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MTP:
                return R.string.usb_summary_file_transfers;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_PTP:
                return R.string.usb_summary_photo_transfers;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_MIDI:
                return R.string.usb_summary_MIDI;
            case UsbBackend.MODE_POWER_SINK | UsbBackend.MODE_DATA_TETHER:
                return R.string.usb_summary_tether;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_MTP:
                return R.string.usb_summary_file_transfers_power;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_PTP:
                return R.string.usb_summary_photo_transfers_power;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_MIDI:
                return R.string.usb_summary_MIDI_power;
            case UsbBackend.MODE_POWER_SOURCE | UsbBackend.MODE_DATA_TETHER:
                return R.string.usb_summary_tether_power;
            default:
                return R.string.usb_summary_charging_only;
        }
    }
}
