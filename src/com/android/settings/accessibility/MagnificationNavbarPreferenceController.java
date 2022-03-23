/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.accessibility;

import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

public class MagnificationNavbarPreferenceController extends TogglePreferenceController {

    private boolean mIsFromSUW = false;

    public MagnificationNavbarPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return MagnificationPreferenceFragment.isChecked(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return MagnificationPreferenceFragment.setChecked(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, isChecked);
    }

    public void setIsFromSUW(boolean fromSUW) {
        mIsFromSUW = fromSUW;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            Bundle extras = preference.getExtras();
            extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED);
            extras.putInt(AccessibilitySettings.EXTRA_TITLE_RES,
                    R.string.accessibility_screen_magnification_navbar_title);
            extras.putCharSequence(AccessibilitySettings.EXTRA_HTML_DESCRIPTION,
                    mContext.getText(R.string.accessibility_screen_magnification_navbar_summary));
            extras.putBoolean(AccessibilitySettings.EXTRA_CHECKED, isChecked());
            extras.putBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW, mIsFromSUW);
            return true;
        }
        return false;
    }

    @Override
    public int getAvailabilityStatus() {
        return MagnificationPreferenceFragment.isApplicable(mContext.getResources())
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(),
                "screen_magnification_navbar_preference_screen");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        int resId = 0;
        if (mIsFromSUW) {
            resId = R.string.accessibility_screen_magnification_navbar_short_summary;
        } else {
            final boolean enabled = isChecked();
            resId = (enabled ? R.string.accessibility_feature_state_on :
                    R.string.accessibility_feature_state_off);
        }
        return mContext.getText(resId);
    }
}
