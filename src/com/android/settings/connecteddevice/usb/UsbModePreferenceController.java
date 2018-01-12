/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class UsbModePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_USB_MODE = "usb_mode";

    private UsbBackend mUsbBackend;
    @VisibleForTesting
    UsbConnectionBroadcastReceiver mUsbReceiver;
    private Preference mUsbPreference;

    public UsbModePreferenceController(Context context, UsbBackend usbBackend) {
        super(context);
        mUsbBackend = usbBackend;
        mUsbReceiver = new UsbConnectionBroadcastReceiver(mContext, (connected, newMode) -> {
            updateSummary(mUsbPreference, connected, newMode);
        }, mUsbBackend);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mUsbPreference = screen.findPreference(KEY_USB_MODE);
    }

    @Override
    public void updateState(Preference preference) {
        updateSummary(preference, mUsbReceiver.isConnected(), mUsbBackend.getCurrentMode());
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_USB_MODE;
    }

    @Override
    public void onPause() {
        mUsbReceiver.unregister();
    }

    @Override
    public void onResume() {
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

    private void updateSummary(Preference preference, boolean connected, int mode) {
        if (preference != null) {
            if (connected) {
                preference.setEnabled(true);
                preference.setSummary(getSummary(mode));
            } else {
                preference.setSummary(R.string.disconnected);
                preference.setEnabled(false);
            }
        }
    }
}
