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
package com.android.settings.connecteddevice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.R;
import com.android.settings.deviceinfo.UsbBackend;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class UsbModePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_USB_MODE = "usb_mode";

    private UsbBackend mUsbBackend;
    private UsbConnectionBroadcastReceiver mUsbReceiver;
    private Preference mUsbPreference;

    public UsbModePreferenceController(Context context, UsbBackend usbBackend) {
        super(context);
        mUsbBackend = usbBackend;
        mUsbReceiver = new UsbConnectionBroadcastReceiver();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mUsbPreference = screen.findPreference(KEY_USB_MODE);
        updataSummary(mUsbPreference);
    }

    @Override
    public void updateState(Preference preference) {
        updataSummary(preference);
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

    @VisibleForTesting
    int getSummary(int mode) {
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
        }
        return 0;
    }

    private void updataSummary(Preference preference) {
        updataSummary(preference, mUsbBackend.getCurrentMode());
    }

    private void updataSummary(Preference preference, int mode) {
        if (preference != null) {
            if (mUsbReceiver.isConnected()) {
                preference.setEnabled(true);
                preference.setSummary(getSummary(mode));
            } else {
                preference.setSummary(R.string.disconnected);
                preference.setEnabled(false);
            }
        }
    }

    private class UsbConnectionBroadcastReceiver extends BroadcastReceiver {
        private boolean mListeningToUsbEvents;
        private boolean mConnected;

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = intent != null
                    && intent.getExtras().getBoolean(UsbManager.USB_CONNECTED);
            if (connected != mConnected) {
                mConnected = connected;
                updataSummary(mUsbPreference);
            }
        }

        public void register() {
            if (!mListeningToUsbEvents) {
                IntentFilter intentFilter = new IntentFilter(UsbManager.ACTION_USB_STATE);
                Intent intent = mContext.registerReceiver(this, intentFilter);
                mConnected = intent != null
                        && intent.getExtras().getBoolean(UsbManager.USB_CONNECTED);
                mListeningToUsbEvents = true;
            }
        }

        public void unregister() {
            if (mListeningToUsbEvents) {
                mContext.unregisterReceiver(this);
                mListeningToUsbEvents = false;
            }
        }

        public boolean isConnected() {
            return mConnected;
        }
    }

}
