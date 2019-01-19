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

package com.android.settings.language;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.localepicker.LocaleListEditor;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

public class PhoneLanguagePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_PHONE_LANGUAGE = "phone_language";

    public PhoneLanguagePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.config_show_phone_language)
                && mContext.getAssets().getLocales().length > 1;
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        final String localeNames = FeatureFactory.getFactory(mContext)
                .getLocaleFeatureProvider().getLocaleNames();
        preference.setSummary(localeNames);
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        // No index needed, because this pref has the same name as the parent page. Indexing it will
        // make search page look like there are duplicate result, creating confusion.
        keys.add(getPreferenceKey());
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PHONE_LANGUAGE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_PHONE_LANGUAGE.equals(preference.getKey())) {
            return false;
        }
        new SubSettingLauncher(mContext)
                .setDestination(LocaleListEditor.class.getName())
                .setSourceMetricsCategory(SettingsEnums.SETTINGS_LANGUAGE_CATEGORY)
                .setTitleRes(R.string.language_picker_title)
                .launch();
        return true;
    }

}
