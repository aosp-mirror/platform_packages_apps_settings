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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.widget.PreferenceCategoryController;

/** Category preference controller for measurement system preferences. */
public class MeasurementSystemItemCategoryController extends PreferenceCategoryController {

    private static final String LOG_TAG = "MeasurementSystemItemCategoryController";
    private static final String KEY_PREFERENCE_CATEGORY_MEASUREMENT_SYSTEM_ITEM =
            "measurement_system_item_category";
    private static final String KEY_PREFERENCE_MEASUREMENT_SYSTEM_ITEM =
            "measurement_system_item_list";

    public MeasurementSystemItemCategoryController(@NonNull Context context, @NonNull String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        PreferenceCategory preferenceCategory =
                screen.findPreference(KEY_PREFERENCE_CATEGORY_MEASUREMENT_SYSTEM_ITEM);
        if (preferenceCategory == null) {
            Log.d(LOG_TAG, "displayPreference(), Can not find the category.");
            return;
        }
        preferenceCategory.setVisible(isAvailable());
        MeasurementSystemItemListController measurementSystemItemListController =
                new MeasurementSystemItemListController(
                    mContext,
                    KEY_PREFERENCE_MEASUREMENT_SYSTEM_ITEM);
        measurementSystemItemListController.displayPreference(screen);
    }
}
