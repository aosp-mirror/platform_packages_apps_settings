/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.backup;


import android.content.Context;
import android.content.Intent;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class BackupSettingsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {
    private static final String BACKUP_SETTINGS = "backup_settings";
    private static final  String MANUFACTURER_SETTINGS = "manufacturer_backup";
    private final BackupSettingsHelper settingsHelper;
    private Intent mManufacturerIntent;
    private String mManufacturerLabel;

    public BackupSettingsPreferenceController(Context context) {
        super(context);
        settingsHelper = new BackupSettingsHelper(context);
        mManufacturerIntent = settingsHelper.getIntentProvidedByManufacturer();
        mManufacturerLabel = settingsHelper.getLabelProvidedByManufacturer();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        // we don't get these in the constructor, so we can get updates for them later
        Intent mBackupSettingsIntent = settingsHelper.getIntentForBackupSettings();
        CharSequence mBackupSettingsTitle = settingsHelper.getLabelForBackupSettings();
        String mBackupSettingsSummary = settingsHelper.getSummaryForBackupSettings();

        Preference backupSettings = screen.findPreference(BACKUP_SETTINGS);
        Preference manufacturerSettings = screen.findPreference(MANUFACTURER_SETTINGS);
        backupSettings.setIntent(mBackupSettingsIntent);
        backupSettings.setTitle(mBackupSettingsTitle);
        backupSettings.setSummary(mBackupSettingsSummary);
        manufacturerSettings.setIntent(mManufacturerIntent);
        manufacturerSettings.setTitle(mManufacturerLabel);
    }

    /**
     * Returns true if preference is available (should be displayed)
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * Returns the key for this preference.
     */
    @Override
    public String getPreferenceKey() {
        return null;
    }
}
