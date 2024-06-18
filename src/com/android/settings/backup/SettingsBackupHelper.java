/**
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.backup;

import static com.android.settings.localepicker.LocaleNotificationDataManager.LOCALE_NOTIFICATION;

import android.app.backup.BackupAgentHelper;
import android.app.backup.SharedPreferencesBackupHelper;

import com.android.settings.flags.Flags;
import com.android.settings.onboarding.OnboardingFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.shortcut.CreateShortcutPreferenceController;
import com.android.settingslib.datastore.BackupRestoreStorageManager;

/** Backup agent for Settings APK */
public class SettingsBackupHelper extends BackupAgentHelper {
    private static final String PREF_LOCALE_NOTIFICATION = "localeNotificationSharedPref";
    public static final String SOUND_BACKUP_HELPER = "SoundSettingsBackup";

    @Override
    public void onCreate() {
        super.onCreate();
        BackupRestoreStorageManager.getInstance(this).addBackupAgentHelpers(this);
        addHelper(PREF_LOCALE_NOTIFICATION,
                new SharedPreferencesBackupHelper(this, LOCALE_NOTIFICATION));
        if (Flags.enableSoundBackup()) {
            OnboardingFeatureProvider onboardingFeatureProvider =
                    FeatureFactory.getFeatureFactory().getOnboardingFeatureProvider();
            if (onboardingFeatureProvider != null) {
                addHelper(SOUND_BACKUP_HELPER, onboardingFeatureProvider.
                        getSoundBackupHelper(this, this.getBackupRestoreEventLogger()));
            }
        }
    }

    @Override
    public void onRestoreFinished() {
        super.onRestoreFinished();
        BackupRestoreStorageManager.getInstance(this).onRestoreFinished();
        CreateShortcutPreferenceController.updateRestoredShortcuts(this);
    }
}
