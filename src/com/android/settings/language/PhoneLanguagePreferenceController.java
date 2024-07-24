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

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.overlay.FeatureFactory;

import java.util.List;

public class PhoneLanguagePreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin {

    public PhoneLanguagePreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getResources().getBoolean(R.bool.config_show_phone_language)
                && mContext.getAssets().getLocales().length > 1) {
            return AVAILABLE;
        } else {
            return CONDITIONALLY_UNAVAILABLE;
        }
    }

    @Override
    public void updateState(Preference preference) {
        if (preference == null) {
            return;
        }
        final String localeNames = FeatureFactory.getFeatureFactory()
                .getLocaleFeatureProvider().getLocaleNames();
        preference.setSummary(localeNames);
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        // No index needed, because this pref has the same name as the parent page. Indexing it will
        // make search page look like there are duplicate result, creating confusion.
        keys.add(getPreferenceKey());
    }
}
