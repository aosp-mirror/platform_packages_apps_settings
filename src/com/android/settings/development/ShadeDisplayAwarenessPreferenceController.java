/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.R;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class ShadeDisplayAwarenessPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin,
        RebootConfirmationDialogHost {

    private static final int SHADE_DISPLAY_AWARENESS_DEFAULT = 0;
    private static final String SHADE_DISPLAY_AWARENESS_KEY = "shade_display_awareness";

    private final String[] mListValues;
    private final String[] mListSummaries;

    public ShadeDisplayAwarenessPreferenceController(Context context) {
        super(context);

        mListValues = mContext.getResources().getStringArray(
                R.array.shade_display_awareness_values);
        mListSummaries = mContext.getResources().getStringArray(
                R.array.shade_display_awareness_summaries);
    }

    @Override
    public String getPreferenceKey() {
        return SHADE_DISPLAY_AWARENESS_KEY;
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SHADE_DISPLAY_AWARENESS, newValue.toString());
        updateShadeDisplayAwareness((ListPreference) mPreference);
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        updateShadeDisplayAwareness((ListPreference) mPreference);
    }

    private void updateShadeDisplayAwareness(ListPreference preference) {
        String currentValue = Settings.Global.getString(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SHADE_DISPLAY_AWARENESS);
        int index = SHADE_DISPLAY_AWARENESS_DEFAULT; // Defaults to value is device-display (0)
        for (int i = 0; i < mListValues.length; i++) {
            if (TextUtils.equals(currentValue, mListValues[i])) {
                index = i;
                break;
            }
        }
        preference.setValue(mListValues[index]);
        preference.setSummary(mListSummaries[index]);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SHADE_DISPLAY_AWARENESS,
                mListValues[SHADE_DISPLAY_AWARENESS_DEFAULT]);
    }
}
