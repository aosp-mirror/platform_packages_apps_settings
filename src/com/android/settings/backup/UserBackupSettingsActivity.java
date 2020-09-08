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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;


/**
 * The activity used to launch the configured Backup activity or the preference screen
 * if the manufacturer provided their backup settings.
 * Pre-Q, BackupSettingsActivity was disabled for non-system users. Therefore, for phones which
 * upgrade to Q, BackupSettingsActivity is disabled for those users. However, we cannot simply
 * enable it in Q since component enable can only be done by the user itself; which is not
 * enough in Q we want it to be enabled for all profile users of the user.
 * Therefore, as a simple workaround, we use a new class which is enabled by default.
 */
@SearchIndexable
public class UserBackupSettingsActivity extends FragmentActivity implements Indexable {
    private static final String TAG = "BackupSettingsActivity";
    private FragmentManager mFragmentManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BackupSettingsHelper backupHelper = new BackupSettingsHelper(this);

        if (!backupHelper.isBackupProvidedByManufacturer()) {
            // If manufacturer specific backup settings are not provided then launch
            // the backup settings provided by backup transport or the default one directly.
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG,
                        "No manufacturer settings found, launching the backup settings directly");
            }
            Intent intent = backupHelper.getIntentForBackupSettings();
            try {
                // enable the activity before launching it
                getPackageManager().setComponentEnabledSetting(
                        intent.getComponent(),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
            } catch (SecurityException e) {
                Log.w(TAG, "Trying to enable activity " + intent.getComponent() + " but couldn't: "
                        + e.getMessage());
                // the activity may already be enabled
            }

            // use startActivityForResult to let the activity check the caller signature
            startActivityForResult(intent, 1);
            finish();
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Manufacturer provided backup settings, showing the preference screen");
            }
            // mFragmentManager can be set by {@link #setFragmentManager()} for testing
            if (mFragmentManager == null) {
                mFragmentManager = getSupportFragmentManager();
            }
            mFragmentManager.beginTransaction()
                    .replace(android.R.id.content, new BackupSettingsFragment())
                    .commit();
        }
    }

    /**
     * For Search.
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                private static final String BACKUP_SEARCH_INDEX_KEY = "Backup";

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<>();

                    // Add the activity title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.privacy_settings_title);
                    data.screenTitle = context.getString(R.string.privacy_settings_title);
                    data.keywords = context.getString(R.string.keywords_backup);
                    data.intentTargetPackage = context.getPackageName();
                    data.intentTargetClass = UserBackupSettingsActivity.class.getName();
                    data.intentAction = Intent.ACTION_MAIN;
                    data.key = BACKUP_SEARCH_INDEX_KEY;
                    result.add(data);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);
                    if (!new BackupSettingsHelper(context).isBackupServiceActive()) {
                        keys.add(BACKUP_SEARCH_INDEX_KEY);
                    }
                    return keys;
                }
            };

    @VisibleForTesting
    void setFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }
}