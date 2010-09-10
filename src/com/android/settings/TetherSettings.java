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

import com.android.settings.wifi.WifiApEnabler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.webkit.WebView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

/*
 * Displays preferences for Tethering.
 */
public class TetherSettings extends SettingsPreferenceFragment {
    private static final String TAG = "TetheringSettings";

    private static final String USB_TETHER_SETTINGS = "usb_tether_settings";
    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";
    private static final String WIFI_AP_SETTINGS = "wifi_ap_settings";
    private static final String ENABLE_BLUETOOTH_TETHERING = "enable_bluetooth_tethering";
    private static final String BLUETOOTH_TETHER_SETTINGS = "bluetooth_tether_settings";
    private static final String TETHERING_HELP = "tethering_help";
    private static final String USB_HELP_MODIFIER = "usb_";
    private static final String WIFI_HELP_MODIFIER = "wifi_";
    private static final String HELP_URL = "file:///android_asset/html/%y%z/tethering_%xhelp.html";
    private static final String HELP_PATH = "html/%y%z/tethering_help.html";

    private static final int DIALOG_TETHER_HELP = 1;

    private WebView mView;
    private CheckBoxPreference mUsbTether;

    private CheckBoxPreference mEnableWifiAp;
    private PreferenceScreen mWifiApSettings;
    private WifiApEnabler mWifiApEnabler;

    private CheckBoxPreference mBluetoothTether;
    private PreferenceScreen mBluetoothSettings;

    private PreferenceScreen mTetherHelp;

    private BroadcastReceiver mTetherChangeReceiver;

    private String[] mUsbRegexs;

    private String[] mWifiRegexs;

    private String[] mBluetoothRegexs;
    private BluetoothPan mBluetoothPan;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.tether_prefs);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Activity activity = getActivity();
        mBluetoothPan = new BluetoothPan(activity);

        mEnableWifiAp = (CheckBoxPreference) findPreference(ENABLE_WIFI_AP);
        mWifiApSettings = (PreferenceScreen) findPreference(WIFI_AP_SETTINGS);
        mUsbTether = (CheckBoxPreference) findPreference(USB_TETHER_SETTINGS);
        mBluetoothTether = (CheckBoxPreference) findPreference(ENABLE_BLUETOOTH_TETHERING);
        mBluetoothSettings = (PreferenceScreen) findPreference(BLUETOOTH_TETHER_SETTINGS);
        mTetherHelp = (PreferenceScreen) findPreference(TETHERING_HELP);

        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        mUsbRegexs = cm.getTetherableUsbRegexs();
        mWifiRegexs = cm.getTetherableWifiRegexs();
        mBluetoothRegexs = cm.getTetherableBluetoothRegexs();

        boolean usbAvailable = mUsbRegexs.length != 0;
        boolean wifiAvailable = mWifiRegexs.length != 0;
        boolean bluetoothAvailable = mBluetoothRegexs.length != 0;


        if (!usbAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(mUsbTether);
        }
        if (!wifiAvailable) {
            getPreferenceScreen().removePreference(mEnableWifiAp);
            getPreferenceScreen().removePreference(mWifiApSettings);
        }
        if (!bluetoothAvailable) {
            getPreferenceScreen().removePreference(mBluetoothTether);
            getPreferenceScreen().removePreference(mBluetoothSettings);
        } else {
            if (mBluetoothPan.isTetheringOn()) {
                mBluetoothTether.setChecked(true);
                mBluetoothSettings.setEnabled(true);
            } else {
                mBluetoothTether.setChecked(false);
                mBluetoothSettings.setEnabled(false);
            }
        }
        if (wifiAvailable && usbAvailable && bluetoothAvailable){
            activity.setTitle(R.string.tether_settings_title_all);
        } else if (wifiAvailable && usbAvailable){
            activity.setTitle(R.string.tether_settings_title_all);
        } else if (wifiAvailable && bluetoothAvailable){
            activity.setTitle(R.string.tether_settings_title_all);
        } else if (wifiAvailable) {
            activity.setTitle(R.string.tether_settings_title_wifi);
        } else if (usbAvailable && bluetoothAvailable) {
            activity.setTitle(R.string.tether_settings_title_usb_bluetooth);
        } else if (usbAvailable) {
            activity.setTitle(R.string.tether_settings_title_usb);
        } else {
            activity.setTitle(R.string.tether_settings_title_bluetooth);
        }
        mWifiApEnabler = new WifiApEnabler(activity, mEnableWifiAp);
        mView = new WebView(activity);
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_TETHER_HELP) {
            Locale locale = Locale.getDefault();

            // check for the full language + country resource, if not there, try just language
            final AssetManager am = getActivity().getAssets();
            String path = HELP_PATH.replace("%y", locale.getLanguage().toLowerCase());
            path = path.replace("%z", "_"+locale.getCountry().toLowerCase());
            boolean useCountry = true;
            InputStream is = null;
            try {
                is = am.open(path);
            } catch (Exception e) {
                useCountry = false;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (Exception e) {}
                }
            }
            String url = HELP_URL.replace("%y", locale.getLanguage().toLowerCase());
            url = url.replace("%z", (useCountry ? "_"+locale.getCountry().toLowerCase() : ""));
            if ((mUsbRegexs.length != 0) && (mWifiRegexs.length == 0)) {
                url = url.replace("%x", USB_HELP_MODIFIER);
            } else if ((mWifiRegexs.length != 0) && (mUsbRegexs.length == 0)) {
                url = url.replace("%x", WIFI_HELP_MODIFIER);
            } else {
                // could assert that both wifi and usb have regexs, but the default
                // is to use this anyway so no check is needed
                url = url.replace("%x", "");
            }

            mView.loadUrl(url);

            return new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.tethering_help_button_text)
                .setView(mView)
                .create();
        }
        return null;
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                // TODO - this should understand the interface types
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateState(available.toArray(new String[available.size()]),
                        active.toArray(new String[active.size()]),
                        errored.toArray(new String[errored.size()]));
            } else if (intent.getAction().equals(Intent.ACTION_MEDIA_SHARED) ||
                       intent.getAction().equals(Intent.ACTION_MEDIA_UNSHARED)) {
                updateState();
            } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                updateState();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        final Activity activity = getActivity();

        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        Intent intent = activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        if (intent != null) mTetherChangeReceiver.onReceive(activity, intent);
        mWifiApEnabler.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
        mWifiApEnabler.pause();
    }

    private void updateState() {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        String[] available = cm.getTetherableIfaces();
        String[] tethered = cm.getTetheredIfaces();
        String[] errored = cm.getTetheringErroredIfaces();
        updateState(available, tethered, errored);
    }

    private void updateState(String[] available, String[] tethered,
            String[] errored) {
        updateUsbState(available, tethered, errored);
        updateBluetoothState(available, tethered, errored);
    }


    private void updateUsbState(String[] available, String[] tethered,
            String[] errored) {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean usbTethered = false;
        boolean usbAvailable = false;
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        boolean usbErrored = false;
        boolean massStorageActive =
                Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    usbAvailable = true;
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = cm.getLastTetherError(s);
                    }
                }
            }
        }
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
        }
        for (String s: errored) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbErrored = true;
            }
        }

        if (usbTethered) {
            mUsbTether.setSummary(R.string.usb_tethering_active_subtext);
            mUsbTether.setEnabled(true);
            mUsbTether.setChecked(true);
        } else if (usbAvailable) {
            if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                mUsbTether.setSummary(R.string.usb_tethering_available_subtext);
            } else {
                mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            }
            mUsbTether.setEnabled(true);
            mUsbTether.setChecked(false);
        } else if (usbErrored) {
            mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        } else if (massStorageActive) {
            mUsbTether.setSummary(R.string.usb_tethering_storage_active_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        } else {
            mUsbTether.setSummary(R.string.usb_tethering_unavailable_subtext);
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
        }
    }

    private void updateBluetoothState(String[] available, String[] tethered,
            String[] errored) {
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean bluetoothTethered = false;
        boolean bluetoothAvailable = false;
        int bluetoothError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        boolean bluetoothErrored = false;
        for (String s : available) {
            for (String regex : mBluetoothRegexs) {
                if (s.matches(regex)) {
                    bluetoothAvailable = true;
                    if (bluetoothError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        bluetoothError = cm.getLastTetherError(s);
                    }
                }
            }
        }
        for (String s : tethered) {
            for (String regex : mBluetoothRegexs) {
                if (s.matches(regex)) bluetoothTethered = true;
            }
        }
        for (String s: errored) {
            for (String regex : mBluetoothRegexs) {
                if (s.matches(regex)) bluetoothErrored = true;
            }
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int btState = adapter.getState();
        if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
            mBluetoothTether.setEnabled(false);
            mBluetoothSettings.setEnabled(false);
            mBluetoothTether.setSummary(R.string.wifi_stopping);
        } else if (btState == BluetoothAdapter.STATE_TURNING_ON) {
            mBluetoothTether.setEnabled(false);
            mBluetoothSettings.setEnabled(false);
            mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
        } else if (mBluetoothPan.isTetheringOn()) {
            mBluetoothTether.setChecked(true);
            if (btState == BluetoothAdapter.STATE_ON) {
                mBluetoothTether.setEnabled(true);
                mBluetoothSettings.setEnabled(true);
                if (bluetoothTethered) {
                    mBluetoothTether.setSummary(R.string.bluetooth_tethering_connected_subtext);
                } else if (bluetoothErrored) {
                    mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                } else {
                    mBluetoothTether.setSummary(R.string.bluetooth_tethering_available_subtext);
                }
            }
        } else {
            mBluetoothTether.setEnabled(true);
            mBluetoothTether.setChecked(false);
            mBluetoothSettings.setEnabled(false);
            mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mUsbTether) {
            boolean newState = mUsbTether.isChecked();

            ConnectivityManager cm =
                    (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

            if (newState) {
                String[] available = cm.getTetherableIfaces();

                String usbIface = findIface(available, mUsbRegexs);
                if (usbIface == null) {
                    updateState();
                    return true;
                }
                if (cm.tether(usbIface) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                    mUsbTether.setChecked(false);
                    mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
                    return true;
                }
                mUsbTether.setSummary("");
            } else {
                String [] tethered = cm.getTetheredIfaces();

                String usbIface = findIface(tethered, mUsbRegexs);
                if (usbIface == null) {
                    updateState();
                    return true;
                }
                if (cm.untether(usbIface) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                    mUsbTether.setSummary(R.string.usb_tethering_errored_subtext);
                    return true;
                }
                mUsbTether.setSummary("");
            }
        } else if(preference == mBluetoothTether) {
            boolean bluetoothTetherState = mBluetoothTether.isChecked();

            if (bluetoothTetherState) {
                // turn on Bluetooth first
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
                    adapter.enable();
                    mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
                    mBluetoothTether.setEnabled(false);
                    mBluetoothSettings.setEnabled(false);
                } else {
                    mBluetoothSettings.setEnabled(true);
                }

                mBluetoothPan.setBluetoothTethering(true);
                mBluetoothTether.setSummary(R.string.bluetooth_tethering_available_subtext);
            } else {
                boolean errored = false;

                ConnectivityManager cm =
                    (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                String [] tethered = cm.getTetheredIfaces();
                String bluetoothIface = findIface(tethered, mBluetoothRegexs);
                if (bluetoothIface != null &&
                        cm.untether(bluetoothIface) != ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                    errored = true;
                }

                mBluetoothPan.setBluetoothTethering(false);
                mBluetoothSettings.setEnabled(false);
                if (errored) {
                    mBluetoothTether.setSummary(R.string.bluetooth_tethering_errored_subtext);
                } else {
                    mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
                }
            }
        } else if (preference == mTetherHelp) {
            showDialog(DIALOG_TETHER_HELP);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private String findIface(String[] ifaces, String[] regexes) {
        for (String iface : ifaces) {
            for (String regex : regexes) {
                if (iface.matches(regex)) {
                    return iface;
                }
            }
        }
        return null;
    }
}
