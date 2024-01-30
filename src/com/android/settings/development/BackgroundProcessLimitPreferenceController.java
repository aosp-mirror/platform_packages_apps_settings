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
 * limitations under the License.
 */

package com.android.settings.development;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Context;
import android.os.RemoteException;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.R;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class BackgroundProcessLimitPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String APP_PROCESS_LIMIT_KEY = "app_process_limit";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public BackgroundProcessLimitPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.app_process_limit_values);
        mListSummaries = context.getResources().getStringArray(R.array.app_process_limit_entries);
    }

    @Override
    public String getPreferenceKey() {
        return APP_PROCESS_LIMIT_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeAppProcessLimitOptions(newValue);
        updateAppProcessLimitOptions();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateAppProcessLimitOptions();
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        writeAppProcessLimitOptions(null);
    }

    private void updateAppProcessLimitOptions() {
        try {
            final int limit = getActivityManagerService().getProcessLimit();
            int index = 0; // default
            for (int i = 0; i < mListValues.length; i++) {
                int val = Integer.parseInt(mListValues[i]);
                if (val >= limit) {
                    index = i;
                    break;
                }
            }
            final ListPreference listPreference = (ListPreference) mPreference;
            listPreference.setValue(mListValues[index]);
            listPreference.setSummary(mListSummaries[index]);
        } catch (RemoteException e) {
            // intentional no-op
        }
    }

    private void writeAppProcessLimitOptions(Object newValue) {
        try {
            final int limit = newValue != null ? Integer.parseInt(newValue.toString()) : -1;
            getActivityManagerService().setProcessLimit(limit);
            updateAppProcessLimitOptions();
        } catch (RemoteException e) {
            // intentional no-op
        }
    }

    @VisibleForTesting
    IActivityManager getActivityManagerService() {
        return ActivityManager.getService();
    }
}
