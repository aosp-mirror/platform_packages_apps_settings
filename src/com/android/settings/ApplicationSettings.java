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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class ApplicationSettings extends PreferenceActivity implements
        DialogInterface.OnClickListener {
    
    private static final String KEY_TOGGLE_INSTALL_APPLICATIONS = "toggle_install_applications";
    private static final String KEY_QUICK_LAUNCH = "quick_launch";

    private CheckBoxPreference mToggleAppInstallation;
    
    private DialogInterface mWarnInstallApps;
    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.application_settings);

        mToggleAppInstallation = (CheckBoxPreference) findPreference(KEY_TOGGLE_INSTALL_APPLICATIONS);
        mToggleAppInstallation.setChecked(isNonMarketAppsAllowed());

        if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS) {
            // No hard keyboard, remove the setting for quick launch
            Preference quickLaunchSetting = findPreference(KEY_QUICK_LAUNCH);
            getPreferenceScreen().removePreference(quickLaunchSetting);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mToggleAppInstallation) {
            if (mToggleAppInstallation.isChecked()) {
                mToggleAppInstallation.setChecked(false);
                warnAppInstallation();
            } else {
                setNonMarketAppsAllowed(false);
            }
        }
        
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (dialog == mWarnInstallApps && which == DialogInterface.BUTTON1) {
            setNonMarketAppsAllowed(true);
            mToggleAppInstallation.setChecked(true);
        }
    }

    private void setNonMarketAppsAllowed(boolean enabled) {
        // Change the system setting
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 
                                enabled ? 1 : 0);
    }
    
    private boolean isNonMarketAppsAllowed() {
        return Settings.Secure.getInt(getContentResolver(), 
                                      Settings.Secure.INSTALL_NON_MARKET_APPS, 0) > 0;
    }

    private void warnAppInstallation() {
        mWarnInstallApps = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error_title))
                .setIcon(com.android.internal.R.drawable.ic_dialog_alert)
                .setMessage(getResources().getString(R.string.install_all_warning))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, null)
                .show();
    }
    
    
}
