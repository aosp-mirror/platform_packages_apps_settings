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
 * limitations under the License.
 */

package com.android.settings.notification;

import android.app.NotificationManager;
import android.content.Context;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

public class ZenModePriorityCallsPreferenceController extends AbstractZenModePreferenceController
        implements Preference.OnPreferenceChangeListener {

    protected static final String KEY = "zen_mode_calls";
    private final ZenModeBackend mBackend;
    private ListPreference mPreference;
    private final String[] mListValues;

    public ZenModePriorityCallsPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY, lifecycle);
        mBackend = ZenModeBackend.getInstance(context);
        mListValues = context.getResources().getStringArray(R.array.zen_mode_contacts_values);
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updateFromContactsValue(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object selectedContactsFrom) {
        mBackend.saveSenders(NotificationManager.Policy.PRIORITY_CATEGORY_CALLS,
                ZenModeBackend.getSettingFromPrefKey(selectedContactsFrom.toString()));
        updateFromContactsValue(preference);
        return true;
    }

    private void updateFromContactsValue(Preference preference) {
        mPreference = (ListPreference) preference;
        switch (getZenMode()) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
            case Settings.Global.ZEN_MODE_ALARMS:
                mPreference.setEnabled(false);
                mPreference.setValue(ZenModeBackend.ZEN_MODE_FROM_NONE);
                mPreference.setSummary(mBackend.getAlarmsTotalSilenceCallsMessagesSummary(
                        NotificationManager.Policy.PRIORITY_CATEGORY_CALLS));
                break;
            default:
                preference.setEnabled(true);
                preference.setSummary(mBackend.getContactsSummary(
                        NotificationManager.Policy.PRIORITY_CATEGORY_CALLS));

                final String currentVal = ZenModeBackend.getKeyFromSetting(
                        mBackend.getPriorityCallSenders());
                mPreference.setValue(mListValues[getIndexOfSendersValue(currentVal)]);
        }
    }

    @VisibleForTesting
    protected int getIndexOfSendersValue(String currentVal) {
        int index = 3; // defaults to "none" based on R.array.zen_mode_contacts_values
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentVal, mListValues[i])) {
                return i;
            }
        }

        return index;
    }
}
