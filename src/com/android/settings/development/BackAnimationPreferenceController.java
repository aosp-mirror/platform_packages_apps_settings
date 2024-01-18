/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.android.window.flags.Flags.predictiveBackSystemAnims;

import android.content.Context;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.TwoStatePreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

/**
 * PreferenceController for enabling/disabling animation related to back button and back gestures.
 */
public class BackAnimationPreferenceController extends DeveloperOptionsPreferenceController
        implements Preference.OnPreferenceChangeListener, PreferenceControllerMixin {

    private static final String BACK_NAVIGATION_ANIMATION_KEY =
            "back_navigation_animation";

    private static final int SETTING_VALUE_OFF = 0;
    private static final int SETTING_VALUE_ON = 1;
    private final DevelopmentSettingsDashboardFragment mFragment;

    @VisibleForTesting
    BackAnimationPreferenceController(Context context) {
        super(context);
        mFragment = null;
    }


    public BackAnimationPreferenceController(Context context,
            @Nullable DevelopmentSettingsDashboardFragment fragment) {
        super(context);
        mFragment = fragment;
    }

    @Override
    public boolean isAvailable() {
        return !predictiveBackSystemAnims();
    }

    @Override
    public String getPreferenceKey() {
        return BACK_NAVIGATION_ANIMATION_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isEnabled = (Boolean) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_BACK_ANIMATION,
                isEnabled ? SETTING_VALUE_ON : SETTING_VALUE_OFF);
        if (mFragment != null && isEnabled) {
            BackAnimationPreferenceDialog.show(mFragment);
        }
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        final int mode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_BACK_ANIMATION, SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(mode != SETTING_VALUE_OFF);
    }

    @Override
    protected void onDeveloperOptionsSwitchDisabled() {
        super.onDeveloperOptionsSwitchDisabled();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.ENABLE_BACK_ANIMATION, SETTING_VALUE_OFF);
        ((TwoStatePreference) mPreference).setChecked(false);
    }
}
