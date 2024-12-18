/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.regionalpreferences;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

/** A base controller for handling all regional preferences controllers. */
public abstract class RegionalPreferenceListBasePreferenceController extends
        BasePreferenceController {

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private PreferenceCategory mPreferenceCategory;

    public RegionalPreferenceListBasePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(getPreferenceCategoryKey());
        initPreferences();
    }

    private void initPreferences() {
        if (mPreferenceCategory == null) {
            return;
        }

        String[] unitValues = getUnitValues();

        for (int i = 0; i < unitValues.length; i++) {
            SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(mContext);
            mPreferenceCategory.addPreference(pref);
            final String item = unitValues[i];
            final String value = RegionalPreferencesDataUtils.getDefaultUnicodeExtensionData(
                    mContext, getExtensionTypes());
            pref.setTitle(getPreferenceTitle(item));
            pref.setKey(item);
            pref.setOnClickListener(v -> {
                RegionalPreferencesDataUtils.savePreference(mContext, getExtensionTypes(),
                        item.equals(RegionalPreferencesDataUtils.DEFAULT_VALUE)
                                ? null : item);
                String metrics = shouldUseEmptyMetrics()
                                ? ""
                                : getPreferenceTitle(value) + " > " + getPreferenceTitle(item);
                mMetricsFeatureProvider.action(mContext, getMetricsActionKey(), metrics);
            });
            pref.setChecked(!value.isEmpty() && item.equals(value));
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    private boolean shouldUseEmptyMetrics() {
        if (getMetricsActionKey() == SettingsEnums.ACTION_SET_FIRST_DAY_OF_WEEK
                || getMetricsActionKey() == SettingsEnums.ACTION_SET_MEASUREMENT_SYSTEM) {
            return true;
        }
        return false;
    }

    protected abstract String getPreferenceTitle(String item);

    protected abstract String getPreferenceCategoryKey();

    protected abstract String getExtensionTypes();

    protected abstract String[] getUnitValues();

    protected abstract int getMetricsActionKey();


}
