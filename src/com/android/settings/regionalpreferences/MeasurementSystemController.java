/**
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context;
import android.os.LocaleList;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;

import java.util.Locale;

/** A controller for the entry of measurement system page */
public class MeasurementSystemController extends BasePreferenceController {
    private static final String TAG = "MeasurementSystemController";
    public MeasurementSystemController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.regionalPreferencesApiEnabled()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    @NonNull
    public CharSequence getSummary() {
        LocaleList localeList = LocaleList.getDefault();
        Locale locale = localeList.get(0);
        return getMeasurementSystem(locale);
    }

    private String getMeasurementSystem(Locale locale) {
        String type = locale.getUnicodeLocaleType(
                RegionalPreferencesDataUtils.EXTENSION_TYPE_MEASUREMENT_SYSTEM);
        if (type != null) {
            if (type.equals(RegionalPreferencesDataUtils.MEASUREMENT_SYSTEM_METRIC)) {
                return mContext.getString(R.string.metric_measurement_system);
            }
            if (type.equals(RegionalPreferencesDataUtils.MEASUREMENT_SYSTEM_UK)) {
                return mContext.getString(R.string.uk_measurement_system);
            }
            return mContext.getString(R.string.us_measurement_system);
        } else {
            return mContext.getString(R.string.default_string_of_regional_preference);
        }
    }
}
