/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.regionalpreferences;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.SubSettingLauncher;

import java.util.Locale;

/** A controller for the entry of Temperature units' page */
public class TemperatureUnitController extends BasePreferenceController {
    private static final String TAG = TemperatureUnitController.class.getSimpleName();
    public TemperatureUnitController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * @return {@link AvailabilityStatus} for the Setting. This status is used to determine if the
     * Setting should be shown or disabled in Settings. Further, it can be used to produce
     * appropriate error / warning Slice in the case of unavailability.
     * </p>
     * The status is used for the convenience methods: {@link #isAvailable()}, {@link
     * #isSupported()}
     * </p>
     * The inherited class doesn't need to check work profile if android:forWork="true" is set in
     * preference xml.
     */
    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        String record = Settings.System.getString(
                mContext.getContentResolver(), Settings.System.LOCALE_PREFERENCES);
        String result = "";
        if (record != null) {
            result = LocalePreferences.getTemperatureUnit(Locale.forLanguageTag(record), false);
        }

        if (result.isEmpty()) {
            result = LocalePreferences.getTemperatureUnit(false);
        }

        return result.isEmpty()
                ? mContext.getString(R.string.default_string_of_regional_preference)
                : RegionalPreferencesDataUtils.temperatureUnitsConverter(mContext, result);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), mPreferenceKey)) {
            Log.e(TAG, "not the key " + preference.getKey() + " / " + mPreferenceKey);
            return false;
        }
        final Bundle extra = new Bundle();
        extra.putString(RegionalPreferencesFragment.TYPE_OF_REGIONAL_PREFERENCE,
                RegionalPreferencesFragment.TYPE_TEMPERATURE);
        new SubSettingLauncher(preference.getContext())
                .setDestination(RegionalPreferencesFragment.class.getName())
                .setSourceMetricsCategory(SettingsEnums.REGIONAL_PREFERENCE)
                .setArguments(extra)
                .launch();
        return true;
    }
}
