/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import java.util.HashMap;
import java.util.Map;

public class SelectLongPressTimeoutPreferenceController extends BasePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final Map<String, String> mLongPressTimeoutValueToTitleMap;
    private int mLongPressTimeoutDefault;

    public SelectLongPressTimeoutPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mLongPressTimeoutValueToTitleMap = new HashMap<>();
        initLongPressTimeoutValueToTitleMap();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object object) {
        if (!(preference instanceof ListPreference)) {
            return false;
        }
        final ListPreference listPreference = (ListPreference) preference;
        final int newValue = Integer.parseInt((String) object);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, newValue);
        updateState(listPreference);
        return true;

    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!(preference instanceof ListPreference)) {
            return;
        }
        final ListPreference listPreference = (ListPreference) preference;
        listPreference.setValue(getLongPressTimeoutValue());
    }

    @Override
    public CharSequence getSummary() {
        return mLongPressTimeoutValueToTitleMap.get(getLongPressTimeoutValue());
    }

    private String getLongPressTimeoutValue() {
        final int longPressTimeout = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, mLongPressTimeoutDefault);
        return String.valueOf(longPressTimeout);
    }

    private void initLongPressTimeoutValueToTitleMap() {
        if (mLongPressTimeoutValueToTitleMap.size() == 0) {
            final String[] timeoutValues = mContext.getResources().getStringArray(
                    R.array.long_press_timeout_selector_values);
            mLongPressTimeoutDefault = Integer.parseInt(timeoutValues[0]);
            final String[] timeoutTitles = mContext.getResources().getStringArray(
                    R.array.long_press_timeout_selector_titles);
            final int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                mLongPressTimeoutValueToTitleMap.put(timeoutValues[i], timeoutTitles[i]);
            }
        }
    }
}
