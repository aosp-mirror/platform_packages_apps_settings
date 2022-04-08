/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network;

import static com.android.settings.network.TetherEnabler.TETHERING_BLUETOOTH_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_ETHERNET_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_USB_ON;
import static com.android.settings.network.TetherEnabler.TETHERING_WIFI_ON;

import android.content.Context;
import android.net.ConnectivityManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;

/**
 * This controller helps to manage the switch state and visibility of wifi tether disable switch
 * preference. When the preference checked, wifi tether will be disabled.
 *
 * @see BluetoothTetherPreferenceController
 * @see UsbTetherPreferenceController
 */
public final class WifiTetherDisablePreferenceController extends TetherBasePreferenceController {

    private static final String TAG = "WifiTetherDisablePreferenceController";

    private PreferenceScreen mScreen;

    public WifiTetherDisablePreferenceController(Context context, String prefKey) {
        super(context, prefKey);
    }

    @Override
    public boolean isChecked() {
        return !super.isChecked();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return super.setChecked(!isChecked);
    }

    private int getTetheringStateOfOtherInterfaces() {
        return mTetheringState & (~TETHERING_WIFI_ON);
    }

    @Override
    public boolean shouldEnable() {
        return true;
    }

    @Override
    public boolean shouldShow() {
        final String[] wifiRegexs = mCm.getTetherableWifiRegexs();
        return wifiRegexs != null && wifiRegexs.length != 0 && !Utils.isMonkeyRunning()
                && getTetheringStateOfOtherInterfaces() != TetherEnabler.TETHERING_OFF;
    }

    @Override
    public int getTetherType() {
        return ConnectivityManager.TETHERING_WIFI;
    }

    @Override
    public CharSequence getSummary() {
        switch (getTetheringStateOfOtherInterfaces()) {
            case TETHERING_USB_ON:
                return mContext.getString(R.string.disable_wifi_hotspot_when_usb_on);
            case TETHERING_BLUETOOTH_ON:
                return mContext.getString(R.string.disable_wifi_hotspot_when_bluetooth_on);
            case TETHERING_ETHERNET_ON:
                return mContext.getString(R.string.disable_wifi_hotspot_when_ethernet_on);
            case TETHERING_USB_ON | TETHERING_BLUETOOTH_ON:
                return mContext.getString(R.string.disable_wifi_hotspot_when_usb_and_bluetooth_on);
            case TETHERING_USB_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(R.string.disable_wifi_hotspot_when_usb_and_ethernet_on);
            case TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(
                        R.string.disable_wifi_hotspot_when_bluetooth_and_ethernet_on);
            case TETHERING_USB_ON | TETHERING_BLUETOOTH_ON | TETHERING_ETHERNET_ON:
                return mContext.getString(
                        R.string.disable_wifi_hotspot_when_usb_and_bluetooth_and_ethernet_on);
            default:
                return mContext.getString(R.string.summary_placeholder);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mScreen = screen;
        if (mPreference != null) {
            mPreference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setVisible(isAvailable());
        refreshSummary(preference);
    }
}
