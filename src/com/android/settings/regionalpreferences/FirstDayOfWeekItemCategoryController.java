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

import android.content.Context;
import android.util.Log;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.widget.PreferenceCategoryController;

/** Category preference controller for first day of week preferences. */
public class FirstDayOfWeekItemCategoryController extends PreferenceCategoryController {

    private static final String LOG_TAG = "FirstDayOfWeekItemCategoryController";
    private static final String KEY_PREFERENCE_CATEGORY_FIRST_DAY_OF_WEEK_ITEM =
            "first_day_of_week_item_category";
    private static final String KEY_PREFERENCE_FIRST_DAY_OF_WEEK_ITEM =
            "first_day_of_week_item_list";

    private PreferenceCategory mPreferenceCategory;
    private FirstDayOfWeekItemListController mFirstDayOfWeekItemListController;

    public FirstDayOfWeekItemCategoryController(Context context, String key) {
        super(context, key);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceCategory = screen.findPreference(KEY_PREFERENCE_CATEGORY_FIRST_DAY_OF_WEEK_ITEM);
        if (mPreferenceCategory == null) {
            Log.d(LOG_TAG, "displayPreference(), Can not find the category.");
            return;
        }
        mPreferenceCategory.setVisible(isAvailable());
        mFirstDayOfWeekItemListController = new FirstDayOfWeekItemListController(mContext,
                KEY_PREFERENCE_FIRST_DAY_OF_WEEK_ITEM);
        mFirstDayOfWeekItemListController.displayPreference(screen);
    }
}
