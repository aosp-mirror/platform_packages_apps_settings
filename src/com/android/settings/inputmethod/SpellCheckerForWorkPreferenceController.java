/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.view.inputmethod.InputMethodSystemProperty;

import com.android.settings.R;
import com.android.settings.core.WorkProfilePreferenceController;

/**
 * Preference controller for "Spell checker for work".
 *
 * @see SpellCheckerPreferenceController
 */
public final class SpellCheckerForWorkPreferenceController extends WorkProfilePreferenceController {

    public SpellCheckerForWorkPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    protected int getSourceMetricsCategory() {
        return SettingsEnums.SETTINGS_LANGUAGE_CATEGORY;
    }

    @AvailabilityStatus
    @Override
    public int getAvailabilityStatus() {
        if (!mContext.getResources().getBoolean(R.bool.config_show_spellcheckers_settings)
                || !InputMethodSystemProperty.PER_PROFILE_IME_ENABLED) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return super.getAvailabilityStatus();
    }
}
