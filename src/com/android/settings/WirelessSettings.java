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

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Switch;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.nfc.NfcEnabler;
import com.android.settings.NsdEnabler;

public class WirelessSettings extends SettingsPreferenceFragment {

    private static final String KEY_TOGGLE_AIRPLANE = "toggle_airplane";
    private static final String KEY_TOGGLE_NFC = "toggle_nfc";
    private static final String KEY_WIMAX_SETTINGS = "wimax_settings";
    private static final String KEY_ANDROID_BEAM_SETTINGS = "android_beam_settings";
    private static final String KEY_VPN_SETTINGS = "vpn_settings";
    private static final String KEY_TETHER_SETTINGS = "tether_settings";
    private static final String KEY_PROXY_SETTINGS = "proxy_settings";
    private static final String KEY_MOBILE_NETWORK_SETTINGS = "mobile_network_settings";
    private static final String KEY_TOGGLE_NSD = "toggle_nsd"; //network service discovery
    private static final String KEY_CELL_BROADCAST_SETTINGS = "cell_broadcast_settings";

    public static final String EXIT_ECM_RESULT = "exit_ecm_result";
    public static final int REQUEST_CODE_EXIT_ECM = 1;

    private AirplaneModeEnabler mAirplaneModeEnabler;
    private CheckBoxPreference mAirplaneModePreference;
    private NfcEnabler mNfcEnabler;
    private NfcAdapter mNfcAdapter;
    private NsdEnabler mNsdEnabler;

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
        return super.onPreferenceTreeClick(preferenceScreen, preference);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wireless_settings);

        final Activity activity = getActivity();
        mAirplaneModePreference = (CheckBoxPreference) findPreference(KEY_TOGGLE_AIRPLANE);
        CheckBoxPreference nfc = (CheckBoxPreference) findPreference(KEY_TOGGLE_NFC);
        PreferenceScreen androidBeam = (PreferenceScreen) findPreference(KEY_ANDROID_BEAM_SETTINGS);
        CheckBoxPreference nsd = (CheckBoxPreference) findPreference(KEY_TOGGLE_NSD);

        mAirplaneModeEnabler = new AirplaneModeEnabler(activity, mAirplaneModePreference);
        mNfcEnabler = new NfcEnabler(activity, nfc, androidBeam);

        // Remove NSD checkbox by default
        getPreferenceScreen().removePreference(nsd);
        //mNsdEnabler = new NsdEnabler(activity, nsd);

        String toggleable = Settings.System.getString(activity.getContentResolver(),
                Settings.System.AIRPLANE_MODE_TOGGLEABLE_RADIOS);

        //enable/disable wimax depending on the value in config.xml
        boolean isWimaxEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_wimaxEnabled);
        if (!isWimaxEnabled) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
            if (ps != null) root.removePreference(ps);
        } else {
            if (toggleable == null || !toggleable.contains(Settings.System.RADIO_WIMAX )
                    && isWimaxEnabled) {
                Preference ps = (Preference) findPreference(KEY_WIMAX_SETTINGS);
                ps.setDependency(KEY_TOGGLE_AIRPLANE);
            }
        }
        // Manually set dependencies for Wifi when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.System.RADIO_WIFI)) {
            findPreference(KEY_VPN_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }

        // Manually set dependencies for Bluetooth when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.System.RADIO_BLUETOOTH)) {
            // No bluetooth-dependent items in the list. Code kept in case one is added later.
        }

        // Manually set dependencies for NFC when not toggleable.
        if (toggleable == null || !toggleable.contains(Settings.System.RADIO_NFC)) {
            findPreference(KEY_TOGGLE_NFC).setDependency(KEY_TOGGLE_AIRPLANE);
            findPreference(KEY_ANDROID_BEAM_SETTINGS).setDependency(KEY_TOGGLE_AIRPLANE);
        }

        // Remove NFC if its not available
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (mNfcAdapter == null) {
            getPreferenceScreen().removePreference(nfc);
            getPreferenceScreen().removePreference(androidBeam);
            mNfcEnabler = null;
        }

        // Remove Mobile Network Settings if it's a wifi-only device.
        if (Utils.isWifiOnly(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(KEY_MOBILE_NETWORK_SETTINGS));
        }

        // Enable Proxy selector settings if allowed.
        Preference mGlobalProxy = findPreference(KEY_PROXY_SETTINGS);
        DevicePolicyManager mDPM = (DevicePolicyManager)
                activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        // proxy UI disabled until we have better app support
        getPreferenceScreen().removePreference(mGlobalProxy);
        mGlobalProxy.setEnabled(mDPM.getGlobalProxyAdmin() == null);

        // Disable Tethering if it's not allowed or if it's a wifi-only device
        ConnectivityManager cm =
                (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!cm.isTetheringSupported()) {
            getPreferenceScreen().removePreference(findPreference(KEY_TETHER_SETTINGS));
        } else {
            Preference p = findPreference(KEY_TETHER_SETTINGS);
            p.setTitle(Utils.getTetheringLabel(cm));
        }

        // Enable link to CMAS app settings depending on the value in config.xml.
        boolean isCellBroadcastAppLinkEnabled = this.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);
        try {
            if (isCellBroadcastAppLinkEnabled) {
                PackageManager pm = getPackageManager();
                if (pm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    isCellBroadcastAppLinkEnabled = false;  // CMAS app disabled
                }
            }
        } catch (IllegalArgumentException ignored) {
            isCellBroadcastAppLinkEnabled = false;  // CMAS app not installed
        }
        if (!isCellBroadcastAppLinkEnabled) {
            PreferenceScreen root = getPreferenceScreen();
            Preference ps = findPreference(KEY_CELL_BROADCAST_SETTINGS);
            if (ps != null) root.removePreference(ps);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mAirplaneModeEnabler.resume();
        if (mNfcEnabler != null) {
            mNfcEnabler.resume();
        }
        if (mNsdEnabler != null) {
            mNsdEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mAirplaneModeEnabler.pause();
        if (mNfcEnabler != null) {
            mNfcEnabler.pause();
        }
        if (mNsdEnabler != null) {
            mNsdEnabler.pause();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            Boolean isChoiceYes = data.getBooleanExtra(EXIT_ECM_RESULT, false);
            // Set Airplane mode based on the return value and checkbox state
            mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes,
                    mAirplaneModePreference.isChecked());
        }
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_more_networks;
    }
}
