/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable
public class PrivacySettings extends DashboardFragment {
    private static final String TAG = "PrivacySettings";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVACY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.privacy_settings;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_backup_reset;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        updatePrivacySettingsConfigData(context);
    }

    @Override
    protected void updatePreferenceStates() {
        updatePrivacySettingsConfigData(getContext());
        super.updatePreferenceStates();
    }

    private void updatePrivacySettingsConfigData(final Context context) {
        if (PrivacySettingsUtils.isAdminUser(context)) {
            PrivacySettingsUtils.updatePrivacyBuffer(context,
                    PrivacySettingsConfigData.getInstance());
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.privacy_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    final BackupSettingsHelper backupHelper = new BackupSettingsHelper(context);
                    return !backupHelper.isBackupProvidedByManufacturer() &&
                            !backupHelper.isIntentProvidedByTransport();
                }
            };
}
