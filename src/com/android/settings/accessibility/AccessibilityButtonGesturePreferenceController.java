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

import androidx.annotation.IntDef;
import androidx.preference.ListPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

import com.google.common.primitives.Ints;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Preference controller that controls the button or gesture in accessibility button page. */
public class AccessibilityButtonGesturePreferenceController extends BasePreferenceController
        implements Preference.OnPreferenceChangeListener {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            Mode.BUTTON,
            Mode.GESTURE,
    })
    private @interface Mode {
        int BUTTON = 1;
        int GESTURE = 2;
    }

    public AccessibilityButtonGesturePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return AccessibilityUtil.isGestureNavigateEnabled(mContext)
                ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Integer value = Ints.tryParse((String) newValue);
        if (value != null) {
            putCurrentAccessibilityButtonMode(value);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        final ListPreference listPreference = (ListPreference) preference;

        listPreference.setValue(String.valueOf(getCurrentAccessibilityButtonMode()));
    }

    @Mode
    private int getCurrentAccessibilityButtonMode() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, Mode.BUTTON);
    }

    private void putCurrentAccessibilityButtonMode(@Mode int mode) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, mode);
    }
}
