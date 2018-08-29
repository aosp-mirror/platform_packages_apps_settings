/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.TextServicesManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.inputmethod.InputMethodAndSubtypeUtilCompat;

public class SpellCheckerPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    public static final String KEY_SPELL_CHECKERS = "spellcheckers_settings";

    private final TextServicesManager mTextServicesManager;

    public SpellCheckerPreferenceController(Context context) {
        super(context);
        mTextServicesManager = (TextServicesManager) context.getSystemService(
                Context.TEXT_SERVICES_MANAGER_SERVICE);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference preference = screen.findPreference(KEY_SPELL_CHECKERS);
        if (preference != null) {
            InputMethodAndSubtypeUtilCompat.removeUnnecessaryNonPersistentPreference(preference);
        }
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_spellcheckers_settings);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SPELL_CHECKERS;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        if (!mTextServicesManager.isSpellCheckerEnabled()) {
            preference.setSummary(R.string.switch_off_text);
        } else {
            final SpellCheckerInfo sci = mTextServicesManager.getCurrentSpellChecker();
            if (sci != null) {
                preference.setSummary(sci.loadLabel(mContext.getPackageManager()));
            } else {
                preference.setSummary(R.string.spell_checker_not_selected);
            }
        }
    }
}
