/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.os.Bundle;
import android.os.SystemProperties;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.preference.CheckBoxPreference;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
/*
 * Displays preferences for Tethering.
 */
public class TetherSettings extends PreferenceActivity {

    private static final String ENABLE_TETHER_NOTICE = "enable_tether_notice";
    private static final String USB_TETHER_SETTINGS = "usb_tether_settings";

    private CheckBoxPreference mEnableTetherNotice;
    private PreferenceScreen mUsbTether;

    private BroadcastReceiver mTetherChangeReceiver;

    private String[] mUsbRegexs;
    private ArrayList mUsbIfaces;

    private String[] mWifiRegexs;
    private ArrayList mWifiIfaces;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.tether_prefs);

        mEnableTetherNotice = (CheckBoxPreference) findPreference(ENABLE_TETHER_NOTICE);
        mUsbTether = (PreferenceScreen) findPreference(USB_TETHER_SETTINGS);

        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        mUsbRegexs = cm.getTetherableUsbRegexs();
        if (mUsbRegexs.length == 0) {
            getPreferenceScreen().removePreference(mUsbTether);
            getPreferenceScreen().removePreference(mEnableTetherNotice);
        }
        mWifiRegexs = cm.getTetherableWifiRegexs();
    }


    private class TetherChangeReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            // TODO - this should understand the interface types
            ArrayList<String> available = intent.getStringArrayListExtra(
                    ConnectivityManager.EXTRA_AVAILABLE_TETHER);
            ArrayList<String> active = intent.getStringArrayListExtra(
                    ConnectivityManager.EXTRA_ACTIVE_TETHER);

            updateState(available, active);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEnableTetherNotice.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.TETHER_NOTIFY, 0) != 0);

        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        mTetherChangeReceiver = new TetherChangeReceiver();
        Intent intent = registerReceiver(mTetherChangeReceiver, filter);

        if (intent != null) mTetherChangeReceiver.onReceive(this, intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
    }

    private void updateState(ArrayList<String> available, ArrayList<String> tethered) {
        boolean usbTethered = false;
        boolean usbAvailable = false;
        boolean wifiTethered = false;
        boolean wifiAvailable = false;

        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbAvailable = true;
            }
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) wifiAvailable = true;
            }
        }
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
            for (String regex : mWifiRegexs) {
                if (s.matches(regex)) wifiTethered = true;
            }
        }

        if (usbTethered) {
            mUsbTether.setSummary(R.string.usb_tethering_active_subtext);
            mUsbTether.setEnabled(true);
        } else if (usbAvailable) {
            mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            mUsbTether.setEnabled(true);
        } else {
            mUsbTether.setSummary(R.string.usb_tethering_unavailable_subtext);
            mUsbTether.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mEnableTetherNotice) {
            boolean newState = mEnableTetherNotice.isChecked();
            Settings.Secure.putInt(getContentResolver(),
                    Settings.Secure.TETHER_NOTIFY, newState ? 1 : 0);
            return true;
        }
        return false;
    }

}
