/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Intent;

public class PrivacySettingsConfigData {

    private static PrivacySettingsConfigData sInstance;

    private boolean mBackupEnabled;
    private boolean mBackupGray;
    private Intent mConfigIntent;
    private String mConfigSummary;
    private Intent mManageIntent;
    private CharSequence mManageLabel;

    private PrivacySettingsConfigData() {
        mBackupEnabled = false;
        mBackupGray = false;
        mConfigIntent = null;
        mConfigSummary = null;
        mManageIntent = null;
        mManageLabel = null;
    }

    public static PrivacySettingsConfigData getInstance() {
        if (sInstance == null) {
            sInstance = new PrivacySettingsConfigData();
        }
        return sInstance;
    }

    public boolean isBackupEnabled() {
        return mBackupEnabled;
    }

    public void setBackupEnabled(final boolean backupEnabled) {
        mBackupEnabled = backupEnabled;
    }

    public boolean isBackupGray() {
        return mBackupGray;
    }

    public void setBackupGray(final boolean backupGray) {
        mBackupGray = backupGray;
    }

    public Intent getConfigIntent() {
        return mConfigIntent;
    }

    public void setConfigIntent(final Intent configIntent) {
        mConfigIntent = configIntent;
    }

    public String getConfigSummary() {
        return mConfigSummary;
    }

    public void setConfigSummary(final String configSummary) {
        mConfigSummary = configSummary;
    }

    public Intent getManageIntent() {
        return mManageIntent;
    }

    public void setManageIntent(final Intent manageIntent) {
        mManageIntent = manageIntent;
    }

    public CharSequence getManageLabel() {
        return mManageLabel;
    }

    public void setManageLabel(final CharSequence manageLabel) {
        mManageLabel = manageLabel;
    }
}
