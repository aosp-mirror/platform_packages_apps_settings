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

import static android.app.settings.SettingsEnums.ACTION_BOUNCE_KEYS_CUSTOM_VALUE_CHANGE;
import static android.app.settings.SettingsEnums.ACTION_BOUNCE_KEYS_DISABLED;
import static android.app.settings.SettingsEnums.ACTION_BOUNCE_KEYS_ENABLED;

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

public class KeyboardAccessibilityBounceKeysController extends
        InputSettingPreferenceController implements
        LifecycleObserver {
    public static final int BOUNCE_KEYS_THRESHOLD = 500;

    @Nullable
    private PrimarySwitchPreference mPrimaryPreference;

    public KeyboardAccessibilityBounceKeysController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
        constructDialog(context, R.string.bounce_keys_dialog_title,
                R.string.bounce_keys_dialog_subtitle);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPrimaryPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return (super.getAvailabilityStatus() == AVAILABLE)
                && InputSettings.isAccessibilityBounceKeysFeatureEnabled() ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
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
    public boolean isChecked() {
        return InputSettings.isAccessibilityBounceKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        updateInputSettingKeysValue(isChecked ? BOUNCE_KEYS_THRESHOLD : 0);
        mMetricsFeatureProvider.action(mContext,
                isChecked ? ACTION_BOUNCE_KEYS_ENABLED : ACTION_BOUNCE_KEYS_DISABLED);
        return true;
    }

    @Override
    protected void onCustomValueUpdated(int thresholdTimeMillis) {
        mMetricsFeatureProvider.action(mContext, ACTION_BOUNCE_KEYS_CUSTOM_VALUE_CHANGE,
                thresholdTimeMillis);
    }

    @Override
    protected void onInputSettingUpdated() {
        if (mPrimaryPreference != null) {
            mPrimaryPreference.setChecked(
                    InputSettings.isAccessibilityBounceKeysEnabled(mContext));
        }
    }

    @Override
    protected Uri getSettingUri() {
        return Settings.Secure.getUriFor(
                Settings.Secure.ACCESSIBILITY_BOUNCE_KEYS);
    }

    @Override
    protected void updateInputSettingKeysValue(int thresholdTimeMillis) {
        InputSettings.setAccessibilityBounceKeysThreshold(mContext, thresholdTimeMillis);
    }

    @Override
    protected int getInputSettingKeysValue() {
        return InputSettings.getAccessibilityBounceKeysThreshold(mContext);
    }
}
