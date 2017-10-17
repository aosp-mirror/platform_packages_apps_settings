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
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.ThreadedRenderer;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

public class SetGpuRendererPreferenceController extends
        DeveloperOptionsPreferenceController implements Preference.OnPreferenceChangeListener,
        PreferenceControllerMixin {

    private static final String DEBUG_HW_RENDERER_KEY = "debug_hw_renderer";

    private final String[] mListValues;
    private final String[] mListSummaries;
    private ListPreference mPreference;

    public SetGpuRendererPreferenceController(Context context) {
        super(context);

        mListValues = context.getResources().getStringArray(R.array.debug_hw_renderer_values);
        mListSummaries = context.getResources().getStringArray(R.array.debug_hw_renderer_entries);
    }

    @Override
    public String getPreferenceKey() {
        return DEBUG_HW_RENDERER_KEY;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (ListPreference) screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        writeDebugHwRendererOptions(newValue);
        updateDebugHwRendererOptions();
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateDebugHwRendererOptions();
    }

    @Override
    protected void onDeveloperOptionsSwitchEnabled() {
        mPreference.setEnabled(true);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        mPreference.setEnabled(false);
    }

    private void writeDebugHwRendererOptions(Object newValue) {
        SystemProperties.set(ThreadedRenderer.DEBUG_RENDERER_PROPERTY,
                newValue == null ? "" : newValue.toString());
        SystemPropPoker.getInstance().poke();
    }

    private void updateDebugHwRendererOptions() {
        final String value = SystemProperties.get(
                ThreadedRenderer.DEBUG_RENDERER_PROPERTY, "" /* default */);
        int index = 0; // default
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(value, mListValues[i])) {
                index = i;
                break;
            }
        }
        mPreference.setValue(mListValues[index]);
        mPreference.setSummary(mListSummaries[index]);
    }
}
