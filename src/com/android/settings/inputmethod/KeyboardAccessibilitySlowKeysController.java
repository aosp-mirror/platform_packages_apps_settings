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

import static android.app.settings.SettingsEnums.ACTION_SLOW_KEYS_DISABLED;
import static android.app.settings.SettingsEnums.ACTION_SLOW_KEYS_ENABLED;

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

import com.android.settingslib.PrimarySwitchPreference;

public class KeyboardAccessibilitySlowKeysController extends
        InputSettingPreferenceController implements
        LifecycleObserver {
    public static final int SLOW_KEYS_THRESHOLD = 500;
    private static final String KEY_TAG = "slow_keys_dialog_tag";

    @Nullable
    private PrimarySwitchPreference mPrimarySwitchPreference;

    public KeyboardAccessibilitySlowKeysController(@NonNull Context context, @NonNull String key) {
        super(context, key);
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
        mMetricsFeatureProvider.action(mContext,
                isChecked ? ACTION_SLOW_KEYS_ENABLED : ACTION_SLOW_KEYS_DISABLED);
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
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())
                || mFragmentManager == null) {
            return false;
        }
        KeyboardAccessibilitySlowKeysDialogFragment.getInstance().show(mFragmentManager, KEY_TAG);
        return true;
    }

    @Override
    protected void updateInputSettingKeysValue(int thresholdTimeMillis) {
        InputSettings.setAccessibilitySlowKeysThreshold(mContext, thresholdTimeMillis);
    }
}
