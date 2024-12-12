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

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.R;

/** A controller for handling all measurement system preferences. */
public class MeasurementSystemItemListController extends
        RegionalPreferenceListBasePreferenceController {

    private static final String KEY_PREFERENCE_CATEGORY_MEASUREMENT_SYSTEM_ITEM =
            "measurement_system_item_category";
    private static final String KEY_PREFERENCE_MEASUREMENT_SYSTEM_ITEM =
            "measurement_system_item_list";

    public MeasurementSystemItemListController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    protected String getPreferenceTitle(String item) {
        return RegionalPreferencesDataUtils.measurementSystemConverter(mContext, item);
    }

    @Override
    protected String getPreferenceCategoryKey() {
        return KEY_PREFERENCE_CATEGORY_MEASUREMENT_SYSTEM_ITEM;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return KEY_PREFERENCE_MEASUREMENT_SYSTEM_ITEM;
    }

    @Override
    protected String getExtensionTypes() {
        return ExtensionTypes.MEASUREMENT_SYSTEM;
    }

    @Override
    protected String[] getUnitValues() {
        return mContext.getResources().getStringArray(R.array.measurement_system);
    }

    @Override
    protected int getMetricsActionKey() {
        return SettingsEnums.ACTION_SET_MEASUREMENT_SYSTEM;
    }
}
