/**
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.localepicker;

import android.content.Context;
import android.os.LocaleList;

import com.android.internal.app.LocaleCollectorBase;
import com.android.internal.app.LocaleStore;
import com.android.internal.app.SystemLocaleCollector;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SystemLocaleSuggestedListPreferenceController extends
        LocalePickerBaseListPreferenceController {
    private static final String KEY_PREFERENCE_CATEGORY_ADD_A_LANGUAGE_SUGGESTED =
            "system_language_suggested_category";
    private static final String KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST =
            "system_locale_suggested_list";

    @Nullable private LocaleStore.LocaleInfo mLocaleInfo;
    private boolean mIsNumberingSystemMode;

    public SystemLocaleSuggestedListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    public SystemLocaleSuggestedListPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey,
            @NonNull LocaleStore.LocaleInfo parentLocale, boolean isNumberingSystemMode) {
        super(context, preferenceKey);
        mLocaleInfo = parentLocale;
        mIsNumberingSystemMode = isNumberingSystemMode;
    }

    @Override
    protected @NonNull String getPreferenceCategoryKey() {
        return KEY_PREFERENCE_CATEGORY_ADD_A_LANGUAGE_SUGGESTED;
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return KEY_PREFERENCE_SYSTEM_LOCALE_SUGGESTED_LIST;
    }

    @Override
    protected LocaleCollectorBase getLocaleCollectorController(Context context) {
        return new SystemLocaleCollector(context, getExplicitLocaleList());
    }

    @Override
    protected @Nullable LocaleStore.LocaleInfo getParentLocale() {
        return mLocaleInfo;
    }

    @Override
    protected boolean isNumberingMode() {
        return mIsNumberingSystemMode;
    }

    @Override
    protected @Nullable LocaleList getExplicitLocaleList() {
        return null;
    }
}
