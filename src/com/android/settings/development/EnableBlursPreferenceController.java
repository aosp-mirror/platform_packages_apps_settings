/*
 * Copyright 2020 The Android Open Source Project
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

import static android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * Controller that toggles window blurs on devices that support it.
 */
public final class EnableBlursPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String ENABLE_BLURS_ON_WINDOWS = "enable_blurs_on_windows";
    private final boolean mBlurSupported;

    public EnableBlursPreferenceController(Context context) {
        this(context, CROSS_WINDOW_BLUR_SUPPORTED);
    }

    @VisibleForTesting
    public EnableBlursPreferenceController(Context context, boolean blurSupported) {
        super(context);
        mBlurSupported = blurSupported;
    }

    @Override
    public String getPreferenceKey() {
        return ENABLE_BLURS_ON_WINDOWS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean enabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DISABLE_WINDOW_BLURS, enabled ? 0 : 1);
        return true;
    }

    @Override
    public boolean isAvailable() {
        return mBlurSupported;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = Settings.Global.getInt(mContext.getContentResolver(),
                    Settings.Global.DISABLE_WINDOW_BLURS, 0) == 0;
        ((TwoStatePreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DISABLE_WINDOW_BLURS, 0);
        updateState(null);
    }
}
