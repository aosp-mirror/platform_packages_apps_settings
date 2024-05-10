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

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.view.ThreadedRenderer;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class DebugGpuOverdrawPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String DEBUG_HW_OVERDRAW_KEY = "debug_hw_overdraw";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public DebugGpuOverdrawPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(
                com.android.settingslib.R.array.debug_hw_overdraw_values);
        mListSummaries = context.getResources().getStringArray(
                com.android.settingslib.R.array.debug_hw_overdraw_entries);
    }

    @Override
    public String getPreferenceKey() {
        return DEBUG_HW_OVERDRAW_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeDebugHwOverdrawOptions(newValue);
        updateDebugHwOverdrawOptions();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateDebugHwOverdrawOptions();
    }

    private void writeDebugHwOverdrawOptions(Object newValue) {
        SystemProperties.set(ThreadedRenderer.DEBUG_OVERDRAW_PROPERTY,
                newValue == null ? "" : newValue.toString());
        SystemPropPoker.getInstance().poke();
    }

    private void updateDebugHwOverdrawOptions() {
        final String value = SystemProperties.get(
                ThreadedRenderer.DEBUG_OVERDRAW_PROPERTY, "" /* default */);

        int index = 0; // default
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(value, mListValues[i])) {
                index = i;
                break;
            }
        }
        final ListPreference listPreference = (ListPreference) mPreference;
        listPreference.setValue(mListValues[index]);
        listPreference.setSummary(mListSummaries[index]);
    }
}
