/*
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.LocaleStore.LocaleInfo;
import com.android.internal.app.SystemLocaleCollector;

public class SystemRegionAllListPreferenceController extends
        RegionPickerBaseListPreferenceController {

    private static final String KEY_PREFERENCE_CATEGORY = "system_region_all_supported_category";
    private static final String KEY_PREFERENCE_SYSTEM_REGION_LIST =
            "system_region_list";
    @Nullable private LocaleStore.LocaleInfo mLocaleInfo;

    public SystemRegionAllListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    public SystemRegionAllListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey, @NonNull LocaleStore.LocaleInfo parentLocale) {
        super(context, preferenceKey);
        mLocaleInfo = parentLocale;
    }

    @Override
    protected String getPreferenceCategoryKey() {
        return KEY_PREFERENCE_CATEGORY;
    }

    @Override
    @NonNull
    public String getPreferenceKey() {
        return KEY_PREFERENCE_SYSTEM_REGION_LIST;
    }

    @Override
    protected LocaleCollectorBase getLocaleCollectorController(Context context) {
        return new SystemLocaleCollector(context, null);
    }

    @Nullable
    @Override
    protected LocaleInfo getParentLocale() {
        return mLocaleInfo;
    }
}
