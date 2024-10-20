/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.PrimarySwitchPreference;

public class KeyboardAccessibilitySlowKeysController extends
        InputSettingPreferenceController implements
        LifecycleObserver {
    public static final int SLOW_KEYS_THRESHOLD = 500;

    @Nullable
    private PrimarySwitchPreference mPrimarySwitchPreference;

    public KeyboardAccessibilitySlowKeysController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        constructDialog(context, R.string.slow_keys, R.string.slow_keys_summary);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrimarySwitchPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        return InputSettings.isAccessibilitySlowKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        updateInputSettingKeysValue(isChecked ? SLOW_KEYS_THRESHOLD : 0);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return (super.getAvailabilityStatus() == AVAILABLE)
                && InputSettings.isAccessibilitySlowKeysFeatureFlagEnabled() ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    protected void onInputSettingUpdated() {
        if (mPrimarySwitchPreference != null) {
            mPrimarySwitchPreference.setChecked(
                    InputSettings.isAccessibilitySlowKeysEnabled(mContext));
        }
    }

    @Override
    protected Uri getSettingUri() {
        return Settings.Secure.getUriFor(
                Settings.Secure.ACCESSIBILITY_SLOW_KEYS);
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (mAlertDialog != null) {
            mAlertDialog.show();
        }
        return true;
    }

    @Override
    protected void updateInputSettingKeysValue(int thresholdTimeMillis) {
        InputSettings.setAccessibilitySlowKeysThreshold(mContext, thresholdTimeMillis);
    }

    @Override
    protected int getInputSettingKeysValue() {
        return InputSettings.getAccessibilitySlowKeysThreshold(mContext);
    }
}
