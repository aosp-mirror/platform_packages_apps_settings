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

import android.app.backup.BackupManager;
import android.content.Context;
import android.os.UserManager;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class BackupSettingsActivityPreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin {
    private static final String KEY_BACKUP_SETTINGS = "backup_settings";
    private static final String TAG = "BackupSettingActivityPC" ;

    private final UserManager mUm;
    private final BackupManager mBackupManager;

    public BackupSettingsActivityPreferenceController(Context context) {
        super(context);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mBackupManager = new BackupManager(context);
    }

    @Override
    public boolean isAvailable() {
        return mUm.isAdminUser();
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BACKUP_SETTINGS;
    }

    @Override
    public void updateState(Preference preference) {
        final boolean backupEnabled = mBackupManager.isBackupEnabled();

        preference.setSummary(backupEnabled
                ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off);
    }
}
