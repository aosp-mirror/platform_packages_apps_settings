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

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.android.settings.R;

import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;


/**
 * The activity used to launch the configured Backup activity or the preference screen
 * if the manufacturer provided their backup settings.
 */
public class BackupSettingsActivity extends Activity implements Indexable {
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
            // enable the activity before launching it
            getPackageManager().setComponentEnabledSetting(intent.getComponent(),
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            // use startActivityForResult to let the activity check the caller signature
            startActivityForResult(intent, 1);
            finish();
        } else {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Manufacturer provided backup settings, showing the preference screen");
            }
            // mFragmentManager can be set by {@link #setFragmentManager()} for testing
            if (mFragmentManager == null) {
                mFragmentManager = getFragmentManager();
            }
            mFragmentManager.beginTransaction()
                    .replace(android.R.id.content, new BackupSettingsFragment())
                    .commit();
        }
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                private static final String BACKUP_SEARCH_INDEX_KEY = "backup";

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {

                    final List<SearchIndexableRaw> result = new ArrayList<>();

                    // Add the activity title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = context.getString(R.string.privacy_settings_title);
                    data.screenTitle = context.getString(R.string.settings_label);
                    data.keywords = context.getString(R.string.keywords_backup);
                    data.intentTargetPackage = context.getPackageName();
                    data.intentTargetClass = BackupSettingsActivity.class.getName();
                    data.intentAction = Intent.ACTION_MAIN;
                    data.key = BACKUP_SEARCH_INDEX_KEY;
                    result.add(data);

                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> keys = super.getNonIndexableKeys(context);

                    // For non-primary user, no backup is available, so don't show it in search
                    // TODO: http://b/22388012
                    if (UserHandle.myUserId() != UserHandle.USER_SYSTEM) {
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "Not a system user, not indexing the screen");
                        }
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