/*
 * Copyright (C) 2007 The Android Open Source Project
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

import com.android.settings.R;

import android.content.ContentResolver;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.provider.Settings.System;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.os.SystemProperties;

public class AdvancedSettings extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_MAC_ADDRESS = "mac_address";
    private static final String KEY_CURRENT_IP_ADDRESS = "current_ip_address";
    private static final String KEY_USE_STATIC_IP = "use_static_ip";
    private static final String KEY_NUM_CHANNELS = "num_channels";
    private static final String KEY_SLEEP_POLICY = "sleep_policy";
    
    private String[] mSettingNames = {
            System.WIFI_STATIC_IP, System.WIFI_STATIC_GATEWAY, System.WIFI_STATIC_NETMASK,
            System.WIFI_STATIC_DNS1, System.WIFI_STATIC_DNS2
    };
    
    private String[] mPreferenceKeys = {
            "ip_address", "gateway", "netmask", "dns1", "dns2"
    };
    
    private CheckBoxPreference mUseStaticIpCheckBox;
    
    private static final int MENU_ITEM_SAVE = Menu.FIRST;
    private static final int MENU_ITEM_CANCEL = Menu.FIRST + 1;
    
    //Tracks ro.debuggable (1 on userdebug builds)
    private static int DEBUGGABLE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.wifi_advanced_settings);
        
        mUseStaticIpCheckBox = (CheckBoxPreference) findPreference(KEY_USE_STATIC_IP);
        mUseStaticIpCheckBox.setOnPreferenceChangeListener(this);

        for (int i = 0; i < mPreferenceKeys.length; i++) {
            Preference preference = findPreference(mPreferenceKeys[i]);
            preference.setOnPreferenceChangeListener(this);
        }

        DEBUGGABLE = SystemProperties.getInt("ro.debuggable", 0);

        /**
         * Remove user control of regulatory domain
         * channel count settings in non userdebug builds
         */
        if (DEBUGGABLE == 1) {
            /*
             * Fix the Run-time IllegalStateException that ListPreference requires an entries
             * array and an entryValues array, this exception occurs when user open/close the
             * slider in the Regulatory domain dialog.
             */
            initNumChannelsPreference();
        } else {
            Preference chanPref = findPreference(KEY_NUM_CHANNELS);
            if (chanPref != null) {
              getPreferenceScreen().removePreference(chanPref);
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        updateUi();
        /**
         * Remove user control of regulatory domain
         * channel count settings in non userdebug builds
         */
        if (DEBUGGABLE == 1) {
            initNumChannelsPreference();
        }
        initSleepPolicyPreference();
        refreshWifiInfo();
    }

    private void initNumChannelsPreference() {
        ListPreference pref = (ListPreference) findPreference(KEY_NUM_CHANNELS);
        pref.setOnPreferenceChangeListener(this);

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        /*
         * Generate the list of valid channel counts to show in the ListPreference.
         * The values are numerical, so the only text to be localized is the
         * "channel_word" resource.
         */
        int[] validChannelCounts = wifiManager.getValidChannelCounts();
        if (validChannelCounts == null) {
            Toast.makeText(this, R.string.wifi_setting_num_channels_error,
                           Toast.LENGTH_SHORT).show();
            pref.setEnabled(false);
            return;
        }
        String[] entries = new String[validChannelCounts.length];
        String[] entryValues = new String[validChannelCounts.length];

        for (int i = 0; i < validChannelCounts.length; i++) {
            entryValues[i] = String.valueOf(validChannelCounts[i]);
            entries[i] = getString(R.string.wifi_setting_num_channels_channel_phrase,
                                   validChannelCounts[i]);
        }
        pref.setEntries(entries);
        pref.setEntryValues(entryValues);
        pref.setEnabled(true);
        int numChannels = wifiManager.getNumAllowedChannels();
        if (numChannels >= 0) {
            pref.setValue(String.valueOf(numChannels));
        }
    }
    
    private void initSleepPolicyPreference() {
        ListPreference pref = (ListPreference) findPreference(KEY_SLEEP_POLICY);
        pref.setOnPreferenceChangeListener(this);
        int value = Settings.System.getInt(getContentResolver(),
                Settings.System.WIFI_SLEEP_POLICY,Settings. System.WIFI_SLEEP_POLICY_DEFAULT);
        pref.setValue(String.valueOf(value));
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            updateSettingsProvider();
        }
    
        return super.onKeyDown(keyCode, event);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (key == null) return true;

        if (key.equals(KEY_NUM_CHANNELS)) {
            try {
                int numChannels = Integer.parseInt((String) newValue);
                WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                if (!wifiManager.setNumAllowedChannels(numChannels, true)) {
                    Toast.makeText(this, R.string.wifi_setting_num_channels_error,
                            Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.wifi_setting_num_channels_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }
            
        } else if (key.equals(KEY_SLEEP_POLICY)) {
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.WIFI_SLEEP_POLICY, Integer.parseInt(((String) newValue)));
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.wifi_setting_sleep_policy_error,
                        Toast.LENGTH_SHORT).show();
                return false;
            }

        } else if (key.equals(KEY_USE_STATIC_IP)) {
            boolean value = ((Boolean) newValue).booleanValue();

            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.WIFI_USE_STATIC_IP, value ? 1 : 0);
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            String value = (String) newValue;
            
            if (!isIpAddress(value)) {
                Toast.makeText(this, R.string.wifi_ip_settings_invalid_ip, Toast.LENGTH_LONG).show();
                return false;
            }
            
            preference.setSummary(value);
            for (int i = 0; i < mSettingNames.length; i++) {
                if (key.equals(mPreferenceKeys[i])) {
                    Settings.System.putString(getContentResolver(), mSettingNames[i], value);
                    break;
                }
            }
        }
        
        return true;
    }

    private boolean isIpAddress(String value) {
        
        int start = 0;
        int end = value.indexOf('.');
        int numBlocks = 0;
        
        while (start < value.length()) {
            
            if (end == -1) {
                end = value.length();
            }

            try {
                int block = Integer.parseInt(value.substring(start, end));
                if ((block > 255) || (block < 0)) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
            
            numBlocks++;
            
            start = end + 1;
            end = value.indexOf('.', start);
        }
        
        return numBlocks == 4;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        menu.add(0, MENU_ITEM_SAVE, 0, R.string.wifi_ip_settings_menu_save)
                .setIcon(android.R.drawable.ic_menu_save);

        menu.add(0, MENU_ITEM_CANCEL, 0, R.string.wifi_ip_settings_menu_cancel)
                .setIcon(android.R.drawable.ic_menu_close_clear_cancel);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
        
            case MENU_ITEM_SAVE:
                updateSettingsProvider();
                finish();
                return true;
                
            case MENU_ITEM_CANCEL:
                finish();
                return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void updateUi() {
        ContentResolver contentResolver = getContentResolver();
        
        mUseStaticIpCheckBox.setChecked(System.getInt(contentResolver,
                System.WIFI_USE_STATIC_IP, 0) != 0);
        
        for (int i = 0; i < mSettingNames.length; i++) {
            EditTextPreference preference = (EditTextPreference) findPreference(mPreferenceKeys[i]);
            String settingValue = System.getString(contentResolver, mSettingNames[i]);
            preference.setText(settingValue);
            preference.setSummary(settingValue);
        }
    }
    
    private void updateSettingsProvider() {
        ContentResolver contentResolver = getContentResolver();

        System.putInt(contentResolver, System.WIFI_USE_STATIC_IP,
                mUseStaticIpCheckBox.isChecked() ? 1 : 0);
        
        for (int i = 0; i < mSettingNames.length; i++) {
            EditTextPreference preference = (EditTextPreference) findPreference(mPreferenceKeys[i]);
            System.putString(contentResolver, mSettingNames[i], preference.getText());
        }
    }
    
    private void refreshWifiInfo() {
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Preference wifiMacAddressPref = findPreference(KEY_MAC_ADDRESS);
        String macAddress = wifiInfo == null ? null : wifiInfo.getMacAddress();
        wifiMacAddressPref.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress 
                : getString(R.string.status_unavailable));

        Preference wifiIpAddressPref = findPreference(KEY_CURRENT_IP_ADDRESS);
        String ipAddress = null;
        if (wifiInfo != null) {
            long addr = wifiInfo.getIpAddress();
            if (addr != 0) {
                // handle negative values whe first octet > 127
                if (addr < 0) addr += 0x100000000L;
                ipAddress = String.format("%d.%d.%d.%d",
                        addr & 0xFF, (addr >> 8) & 0xFF, (addr >> 16) & 0xFF, (addr >> 24) & 0xFF);
            }
        }
        wifiIpAddressPref.setSummary(ipAddress == null ?
                getString(R.string.status_unavailable) : ipAddress);
    }

}
