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

import static android.app.settings.SettingsEnums.ACTION_MOUSE_KEYS_DISABLED;
import static android.app.settings.SettingsEnums.ACTION_MOUSE_KEYS_ENABLED;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.widget.MainSwitchPreference;

public class KeyboardAccessibilityMouseKeysController extends
        InputSettingPreferenceController implements
        LifecycleObserver {
    private static final String KEY_MOUSE_KEY = "keyboard_a11y_page_mouse_keys";
    private static final String KEY_MOUSE_KEY_MAIN_PAGE = "mouse_keys_main_switch";

    @Nullable
    private PrimarySwitchPreference mPrimaryPreference;
    @Nullable
    private MainSwitchPreference mMainSwitchPreference;

    public KeyboardAccessibilityMouseKeysController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        if (KEY_MOUSE_KEY.equals(getPreferenceKey())) {
            mPrimaryPreference = screen.findPreference(getPreferenceKey());
        } else if (KEY_MOUSE_KEY_MAIN_PAGE.equals(getPreferenceKey())) {
            mMainSwitchPreference = screen.findPreference(getPreferenceKey());
        }
    }

    @Override
    public boolean isChecked() {
        return InputSettings.isAccessibilityMouseKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setAccessibilityMouseKeysEnabled(mContext,
                isChecked);
        mMetricsFeatureProvider.action(mContext,
                isChecked ? ACTION_MOUSE_KEYS_ENABLED : ACTION_MOUSE_KEYS_DISABLED);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        return (super.getAvailabilityStatus() == AVAILABLE)
                && InputSettings.isAccessibilityMouseKeysFeatureFlagEnabled() ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    protected void onInputSettingUpdated() {
        if (mPrimaryPreference != null) {
            mPrimaryPreference.setChecked(
                    InputSettings.isAccessibilityMouseKeysEnabled(mContext));
        } else if (mMainSwitchPreference != null) {
            mMainSwitchPreference.setChecked(
                    InputSettings.isAccessibilityMouseKeysEnabled(mContext));
        }
    }

    @Override
    protected Uri getSettingUri() {
        return Settings.Secure.getUriFor(
                Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ENABLED);
    }
}
