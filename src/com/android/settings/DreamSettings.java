/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import static android.provider.Settings.Secure.DREAM_TIMEOUT;

import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;

import java.util.ArrayList;

public class DreamSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "DreamSettings";

    private static final String KEY_DREAM_TIMEOUT = "dream_timeout";

    private ListPreference mScreenTimeoutPreference;
    private ListPreference mDreamTimeoutPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.dream_settings);

        mDreamTimeoutPreference = (ListPreference) findPreference(KEY_DREAM_TIMEOUT);
        final long currentSaverTimeout = Settings.Secure.getLong(resolver, DREAM_TIMEOUT,
                0);
        mDreamTimeoutPreference.setValue(String.valueOf(currentSaverTimeout));
        mDreamTimeoutPreference.setOnPreferenceChangeListener(this);
        updateTimeoutPreferenceDescription(resolver, mDreamTimeoutPreference,
                R.string.dream_timeout_summary,
                R.string.dream_timeout_zero_summary,
                currentSaverTimeout);
    }

    private void updateTimeoutPreferenceDescription(
            ContentResolver resolver,
            ListPreference pref, 
            int summaryStrings,
            long currentTimeout) {
        updateTimeoutPreferenceDescription(resolver, pref, summaryStrings, 0, currentTimeout);
    }
    private void updateTimeoutPreferenceDescription(
            ContentResolver resolver,
            ListPreference pref, 
            int summaryStrings,
            int zeroString,
            long currentTimeout) {
        String summary;
        if (currentTimeout == 0) {
            summary = pref.getContext().getString(zeroString);
        } else {
            final CharSequence[] entries = pref.getEntries();
            final CharSequence[] values = pref.getEntryValues();
            int best = 0;
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.valueOf(values[i].toString());
                if (currentTimeout >= timeout) {
                    best = i;
                }
            }
            summary = pref.getContext().getString(summaryStrings, entries[best]);
        }
        pref.setSummary(summary);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_DREAM_TIMEOUT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.Secure.putInt(getContentResolver(),
                        DREAM_TIMEOUT, value);
                updateTimeoutPreferenceDescription(getContentResolver(),
                        mDreamTimeoutPreference,
                        R.string.dream_timeout_summary, 
                        R.string.dream_timeout_zero_summary, 
                        value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist dream timeout setting", e);
            }
        }

        return true;
    }
}
