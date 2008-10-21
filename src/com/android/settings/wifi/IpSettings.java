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
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.provider.Settings.System;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class IpSettings extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String KEY_USE_STATIC_IP = "use_static_ip";

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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.ip_settings);
        
        mUseStaticIpCheckBox = (CheckBoxPreference) findPreference(KEY_USE_STATIC_IP);

        for (int i = 0; i < mPreferenceKeys.length; i++) {
            Preference preference = findPreference(mPreferenceKeys[i]);
            preference.setOnPreferenceChangeListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        updateUi();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            updateSettingsProvider();
        }
    
        return super.onKeyDown(keyCode, event);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        
        if (!isIpAddress(value)) {
            Toast.makeText(this, R.string.wifi_ip_settings_invalid_ip, Toast.LENGTH_LONG).show();
            return false;
        }
        
        preference.setSummary(value);
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
    
}
