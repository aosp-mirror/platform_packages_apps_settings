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

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.AbstractPreferenceController;

public class PhoneLanguagePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_PHONE_LANGUAGE = "phone_language";

    public PhoneLanguagePreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return mContext.getAssets().getLocales().length > 1;
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
    public String getPreferenceKey() {
        return KEY_PHONE_LANGUAGE;
    }
}
