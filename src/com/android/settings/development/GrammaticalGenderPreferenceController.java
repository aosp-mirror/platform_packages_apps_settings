/*
 * Copyright (C) 2020 The Android Open Source Project
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
import android.content.res.Configuration;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Preference controller to control Grammatical Gender
 */
public class GrammaticalGenderPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String GRAMMATICAL_GENDER_KEY =
            "grammatical_gender";
    @VisibleForTesting
    static final String GRAMMATICAL_GENDER_PROPERTY = "persist.sys.grammatical_gender";
    private final String[] mListValues;
    private final String[] mListSummaries;

    private IActivityManager mActivityManager;

    public GrammaticalGenderPreferenceController(Context context) {
        this(context, ActivityManager.getService());
    }

    @VisibleForTesting
    GrammaticalGenderPreferenceController(Context context,
            IActivityManager activityManager) {
        super(context);

        mListValues = context.getResources()
                .getStringArray(com.android.settingslib.R.array.grammatical_gender_values);
        mListSummaries = context.getResources()
                .getStringArray(com.android.settingslib.R.array.grammatical_gender_entries);
        mActivityManager = activityManager;
    }

    @Override
    public String getPreferenceKey() {
        return GRAMMATICAL_GENDER_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SystemProperties.set(GRAMMATICAL_GENDER_PROPERTY, newValue.toString());
        updateState(mPreference);
        try {
            Configuration config = mActivityManager.getConfiguration();
            config.setGrammaticalGender(Integer.parseInt(newValue.toString()));
            mActivityManager.updatePersistentConfiguration(config);
        } catch (RemoteException ex) {
            // intentional no-op
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final ListPreference listPreference = (ListPreference) preference;
        final String currentValue = SystemProperties.get(GRAMMATICAL_GENDER_PROPERTY);
        int index = 0; // Defaults to Not Selected
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListSummaries[index]);
    }

    @Override
    public boolean isAvailable() {
        return android.app.Flags.systemTermsOfAddressEnabled();
    }
}
