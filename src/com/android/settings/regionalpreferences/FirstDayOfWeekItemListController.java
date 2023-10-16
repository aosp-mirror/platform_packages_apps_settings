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

import com.android.settings.R;

/** A controller for handling all first day of week preferences. */
public class FirstDayOfWeekItemListController extends
        RegionalPreferenceListBasePreferenceController {

    private static final String KEY_PREFERENCE_CATEGORY_FIRST_DAY_OF_WEEK_ITEM =
            "first_day_of_week_item_category";
    private static final String KEY_PREFERENCE_FIRST_DAY_OF_WEEK_ITEM =
            "first_day_of_week_item_list";

    public FirstDayOfWeekItemListController(Context context, String key) {
        super(context, key);
    }

    @Override
    protected String getPreferenceTitle(String item) {
        return RegionalPreferencesDataUtils.dayConverter(mContext, item);
    }

    @Override
    protected String getPreferenceCategoryKey() {
        return KEY_PREFERENCE_CATEGORY_FIRST_DAY_OF_WEEK_ITEM;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PREFERENCE_FIRST_DAY_OF_WEEK_ITEM;
    }

    @Override
    protected String getExtensionTypes() {
        return ExtensionTypes.FIRST_DAY_OF_WEEK;
    }

    @Override
    protected String[] getUnitValues() {
        return mContext.getResources().getStringArray(R.array.first_day_of_week);
    }

    @Override
    protected int getMetricsActionKey() {
        return SettingsEnums.ACTION_SET_FIRST_DAY_OF_WEEK;
    }
}
