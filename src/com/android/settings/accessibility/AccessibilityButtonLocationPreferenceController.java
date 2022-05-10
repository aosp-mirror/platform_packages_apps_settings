/*
 * Copyright (C) 2021 The Android Open Source Project
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

import com.google.common.primitives.Ints;

import java.util.Optional;

/** Preference controller that controls the preferred location in accessibility button page. */
public class AccessibilityButtonLocationPreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    private Optional<Integer> mDefaultLocation = Optional.empty();

    public AccessibilityButtonLocationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AccessibilityUtil.isGestureNavigateEnabled(mContext)
                ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final ListPreference listPreference = (ListPreference) preference;
        final Integer value = Ints.tryParse((String) newValue);
        if (value != null) {
            Settings.Secure.putInt(mContext.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_BUTTON_MODE, value);
            updateState(listPreference);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;

        listPreference.setValue(getCurrentAccessibilityButtonMode());
    }

    private String getCurrentAccessibilityButtonMode() {
        final int mode = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, getDefaultLocationValue());
        return String.valueOf(mode);
    }

    private int getDefaultLocationValue() {
        if (!mDefaultLocation.isPresent()) {
            final String[] valuesList = mContext.getResources().getStringArray(
                    R.array.accessibility_button_location_selector_values);
            mDefaultLocation = Optional.of(Integer.parseInt(valuesList[0]));
        }
        return mDefaultLocation.get();
    }
}
