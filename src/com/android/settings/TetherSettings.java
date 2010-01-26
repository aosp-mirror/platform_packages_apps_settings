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

/*
 * Displays preferences for Tethering.
 */
public class TetherSettings extends PreferenceActivity {

    private static final String ENABLE_TETHER_NOTICE = "enable_tether_notice";
    private static final String USB_TETHER_SETTINGS = "usb_tether_settings";

    private CheckBoxPreference mEnableTetherNotice;
    private PreferenceScreen mUsbTether;

    private BroadcastReceiver mTetherChangeReceiver;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.tether_prefs);

        mEnableTetherNotice = (CheckBoxPreference) findPreference(ENABLE_TETHER_NOTICE);
        mUsbTether = (PreferenceScreen) findPreference(USB_TETHER_SETTINGS);
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        public void onReceive(Context content, Intent intent) {
            updateState(intent.getIntExtra(ConnectivityManager.EXTRA_AVAILABLE_TETHER_COUNT,0)>0,
                    intent.getIntExtra(ConnectivityManager.EXTRA_ACTIVE_TETHER_COUNT,0)>0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEnableTetherNotice.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.TETHER_NOTIFY, 0) != 0);

        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        mTetherChangeReceiver = new TetherChangeReceiver();
        registerReceiver(mTetherChangeReceiver, filter);

        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        updateState(cm.getTetherableIfaces().length>0, cm.getTetheredIfaces().length>0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
    }

    private void updateState(boolean isAvailable, boolean isTethered) {
        if (isTethered) {
            mUsbTether.setSummary(R.string.usb_tethering_active_subtext);
            mUsbTether.setEnabled(true);
        } else if (isAvailable) {
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
