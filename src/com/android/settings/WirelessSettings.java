/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.settings;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.bluetooth.BluetoothEnabler;
import com.android.settings.wifi.WifiEnabler;
import com.android.settings.nfc.NfcEnabler;

public class WirelessSettings extends PreferenceActivity {

    private static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String KEY_TOGGLE_BLUETOOTH = "toggle_bluetooth";
    private static final String KEY_TOGGLE_WIFI = "toggle_wifi";
    private static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private static final String KEY_WIFI_SETTINGS = "wifi_settings";
    private static final String KEY_WIMAX_SETTINGS = "wimax_settings";
    private static final String KEY_BT_SETTINGS = "bt_settings";
    private static final String KEY_VPN_SETTINGS = "vpn_settings";
    private static final String KEY_TETHER_SETTINGS = "tether_settings";

    public static final String EXIT_ECM_RESULT = "exit_ecm_result";
    public static final int REQUEST_CODE_EXIT_ECM = 1;

    private AirplaneModeEnabler mAirplaneModeEnabler;
    private CheckBoxPreference mAirplaneModePreference;
    private WifiEnabler mWifiEnabler;
    private NfcEnabler mNfcEnabler;
    private BluetoothEnabler mBtEnabler;

    /**
     * Invoked on each preference click in this hierarchy, overrides
     * PreferenceActivity's implementation.  Used to make sure we track the
     * preference click events.
     */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mAirplaneModePreference && Boolean.parseBoolean(
                SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE))) {
            // In ECM mode launch ECM app dialog
            startActivityForResult(
                new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                REQUEST_CODE_EXIT_ECM);
            return true;
        }
        // Let the intents be launched by the Preference manager
        return false;
    }

    public static boolean isRadioAllowed(Context context, String type) {
        if (!AirplaneModeEnabler.isAirplaneModeOn(context)) {
            return true;
        }
        // Here we use the same logic in onCreate().
        String toggleable = Settings.System.getString(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_TOGGLEABLE_RADIOS);
        return toggleable != null && toggleable.contains(type);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wireless_settings);

        CheckBoxPreference airplane = (CheckBoxPreference) findPreference(KEY_TOGGLE_AIRPLANE);
        CheckBoxPreference wifi = (CheckBoxPreference) findPreference(KEY_TOGGLE_WIFI);
        CheckBoxPreference bt = (CheckBoxPreference) findPreference(KEY_TOGGLE_BLUETOOTH);
        CheckBoxPreference nfc = (CheckBoxPreference) findPreference(KEY_TOGGLE_NFC);

        mAirplaneModeEnabler = new AirplaneModeEnabler(this, airplane);
        mAirplaneModePreference = (CheckBoxPreference) findPreference(KEY_TOGGLE_AIRPLANE);
        mWifiEnabler = new WifiEnabler(this, wifi);
        mBtEnabler = new BluetoothEnabler(this, bt);
        mNfcEnabler = new NfcEnabler(this, nfc);

        String toggleable = Settings.System.getString(getContentResolver(),
                Settings.System.AIRPLANE_MODE_TOGGLEABLE_RADIOS);

        //enable/disable wimax depending on the value in config.xml
        boolean isWimaxEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (!isWimaxEnabled) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
            if (ps != null)
                root.removePreference(ps);
        } else {
            if (toggleable == null || !toggleable.contains(Settings.System.RADIO_WIMAX )
                    && isWimaxEnabled) {
                Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
                ps.setDependency(KEY_TOGGLE_AIRPLANE);
            }
        }

        // Manually set dependencies for Wifi when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.System.RADIO_WIFI)) {
            wifi.setDependency(KEY_TOGGLE_AIRPLANE);
            findPreference(KEY_WIFI_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
            findPreference(KEY_VPN_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }

        // Manually set dependencies for Bluetooth when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.System.RADIO_BLUETOOTH)) {
            bt.setDependency(KEY_TOGGLE_AIRPLANE);
            findPreference(KEY_BT_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }

        // Remove Bluetooth Settings if Bluetooth service is not available.
        if (ServiceManager.getService(BluetoothAdapter.BLUETOOTH_SERVICE) == null) {
            getPreferenceScreen().removePreference(bt);
        }

        // Remove NFC if its not available
        if (NfcAdapter.getDefaultAdapter(this) == null) {
            getPreferenceScreen().removePreference(nfc);
        }

        // Disable Tethering if it's not allowed
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!cm.isTetheringSupported()) {
            getPreferenceScreen().removePreference(findPreference(KEY_TETHER_SETTINGS));
        } else {
            String[] usbRegexs = cm.getTetherableUsbRegexs();
            String[] wifiRegexs = cm.getTetherableWifiRegexs();
            Preference p = findPreference(KEY_TETHER_SETTINGS);
            if (wifiRegexs.length == 0) {
                p.setTitle(R.string.tether_settings_title_usb);
                p.setSummary(R.string.tether_settings_summary_usb);
            } else {
                if (usbRegexs.length == 0) {
                    p.setTitle(R.string.tether_settings_title_wifi);
                    p.setSummary(R.string.tether_settings_summary_wifi);
                } else {
                    p.setTitle(R.string.tether_settings_title_both);
                    p.setSummary(R.string.tether_settings_summary_both);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mAirplaneModeEnabler.resume();
        mWifiEnabler.resume();
        mBtEnabler.resume();
        mNfcEnabler.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mAirplaneModeEnabler.pause();
        mWifiEnabler.pause();
        mBtEnabler.pause();
        mNfcEnabler.pause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            Boolean isChoiceYes = data.getBooleanExtra(EXIT_ECM_RESULT, false);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        }
    }
}
