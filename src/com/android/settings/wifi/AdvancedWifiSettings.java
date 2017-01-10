/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.security.Credentials;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.RestrictedLockUtils;

public class AdvancedWifiSettings extends RestrictedSettingsFragment {
    private static final String TAG = "AdvancedWifiSettings";

    private static final String KEY_INSTALL_CREDENTIALS = "install_credentials";
    private static final String KEY_WIFI_DIRECT = "wifi_direct";
    private static final String KEY_WPS_PUSH = "wps_push_button";
    private static final String KEY_WPS_PIN = "wps_pin_entry";

    private boolean mUnavailable;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            initPreferences();
        }
    };

    public AdvancedWifiSettings() {
        super(UserManager.DISALLOW_CONFIG_WIFI);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.WIFI_ADVANCED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isUiRestricted()) {
            mUnavailable = true;
            setPreferenceScreen(new PreferenceScreen(getPrefContext(), null));
        } else {
            addPreferencesFromResource(R.xml.wifi_advanced_settings);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getEmptyTextView().setText(R.string.wifi_advanced_not_available);
        if (mUnavailable) {
            getPreferenceScreen().removeAll();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mUnavailable) {
            getActivity().registerReceiver(mReceiver,
                    new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
            initPreferences();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!mUnavailable) {
            getActivity().unregisterReceiver(mReceiver);
        }
    }

    private void initPreferences() {
        final Context context = getActivity();
        Intent intent = new Intent(Credentials.INSTALL_AS_USER_ACTION);
        intent.setClassName("com.android.certinstaller",
                "com.android.certinstaller.CertInstallerMain");
        intent.putExtra(Credentials.EXTRA_INSTALL_AS_UID, android.os.Process.WIFI_UID);
        Preference pref = findPreference(KEY_INSTALL_CREDENTIALS);
        pref.setIntent(intent);

        final WifiManager wifiManager =
                (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
        Intent wifiDirectIntent = new Intent(context,
                com.android.settings.Settings.WifiP2pSettingsActivity.class);
        Preference wifiDirectPref = findPreference(KEY_WIFI_DIRECT);
        wifiDirectPref.setIntent(wifiDirectIntent);
        wifiDirectPref.setEnabled(wifiManager.isWifiEnabled());

        // WpsDialog: Create the dialog like WifiSettings does.
        Preference wpsPushPref = findPreference(KEY_WPS_PUSH);
        wpsPushPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference arg0) {
                    WpsFragment wpsFragment = new WpsFragment(WpsInfo.PBC);
                    wpsFragment.show(getFragmentManager(), KEY_WPS_PUSH);
                    return true;
                }
        });
        wpsPushPref.setEnabled(wifiManager.isWifiEnabled());

        // WpsDialog: Create the dialog like WifiSettings does.
        Preference wpsPinPref = findPreference(KEY_WPS_PIN);
        wpsPinPref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
                public boolean onPreferenceClick(Preference arg0) {
                    WpsFragment wpsFragment = new WpsFragment(WpsInfo.DISPLAY);
                    wpsFragment.show(getFragmentManager(), KEY_WPS_PIN);
                    return true;
                }
        });
        wpsPinPref.setEnabled(wifiManager.isWifiEnabled());
    }

    /* Wrapper class for the WPS dialog to properly handle life cycle events like rotation. */
    public static class WpsFragment extends InstrumentedDialogFragment {
        private static int mWpsSetup;

        // Public default constructor is required for rotation.
        public WpsFragment() {
            super();
        }

        public WpsFragment(int wpsSetup) {
            super();
            mWpsSetup = wpsSetup;
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.DIALOG_WPS_SETUP;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new WpsDialog(getActivity(), mWpsSetup);
        }
    }

}
