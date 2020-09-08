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

import android.content.Context;
import android.os.SystemProperties;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;
import com.android.settingslib.development.SystemPropPoker;

/**
 * Controller that toggles window blurs on SurfaceFlinger on devices that support it.
 */
public final class EnableBlursPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    @VisibleForTesting
    static final String DISABLE_BLURS_SYSPROP = "persist.sys.sf.disable_blurs";
    private static final String ENABLE_BLURS_ON_WINDOWS = "enable_blurs_on_windows";
    private final boolean mBlurSupported;

    public EnableBlursPreferenceController(Context context) {
        this(context, SystemProperties
                .getBoolean("ro.surface_flinger.supports_background_blur", false));
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
        final boolean isDisabled = !(Boolean) newValue;
        SystemProperties.set(DISABLE_BLURS_SYSPROP, isDisabled ? "1" : "0");
        SystemPropPoker.getInstance().poke();
        return true;
    }

    @Override
    public boolean isAvailable() {
        return mBlurSupported;
    }

    @Override
    public void updateState(Preference preference) {
        boolean isEnabled = !SystemProperties.getBoolean(
                DISABLE_BLURS_SYSPROP, false /* default */);
        ((SwitchPreference) mPreference).setChecked(isEnabled);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        SystemProperties.set(DISABLE_BLURS_SYSPROP, null);
        updateState(null);
    }
}
