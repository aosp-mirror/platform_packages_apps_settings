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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.PrimarySwitchPreference;
import com.android.settingslib.widget.MainSwitchPreference;

public class KeyboardRepeatKeysController extends
        InputSettingPreferenceController implements
        LifecycleObserver {
    private static final String KEY_REPEAT_KEY = "physical_keyboard_repeat_keys";
    private static final String KEY_REPEAT_KEY_MAIN_PAGE = "repeat_key_main_switch";

    @Nullable
    private PrimarySwitchPreference mPrimarySwitchPreference;
    @Nullable
    private MainSwitchPreference mMainSwitchPreference;

    public KeyboardRepeatKeysController(@NonNull Context context,
            @NonNull String key) {
        super(context, key);
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        if (KEY_REPEAT_KEY.equals(getPreferenceKey())) {
            mPrimarySwitchPreference = screen.findPreference(getPreferenceKey());
        } else if (KEY_REPEAT_KEY_MAIN_PAGE.equals(getPreferenceKey())) {
            mMainSwitchPreference = screen.findPreference(getPreferenceKey());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return InputSettings.isRepeatKeysFeatureFlagEnabled() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return InputSettings.isRepeatKeysEnabled(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        InputSettings.setRepeatKeysEnabled(mContext, isChecked);
        return true;
    }

    @Override
    protected void onInputSettingUpdated() {
        if (mPrimarySwitchPreference != null) {
            mPrimarySwitchPreference.setChecked(InputSettings.isRepeatKeysEnabled(mContext));
        } else if (mMainSwitchPreference != null) {
            mMainSwitchPreference.setChecked(InputSettings.isRepeatKeysEnabled(mContext));
        }
    }

    @Override
    protected Uri getSettingUri() {
        return Settings.Secure.getUriFor(
                Settings.Secure.KEY_REPEAT_ENABLED);
    }
}
