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
import android.os.LocaleList;

import androidx.annotation.NonNull;

import com.android.internal.app.LocaleStore;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settings.localepicker.LocaleFeatureProviderImpl;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** A controller for the entry of Numbering System's page */
public class NewNumberingSystemController extends BasePreferenceController {
    private static final String TAG = NewNumberingSystemController.class.getSimpleName();

    private LocaleList mLocaleList;
    public NewNumberingSystemController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
        // Initialize the supported languages to LocaleInfos
        LocaleStore.fillCache(context);
        mLocaleList = getNumberingSystemLocale();
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
        if (Flags.regionalPreferencesApiEnabled()) {
            return mLocaleList.isEmpty() ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    private static LocaleList getNumberingSystemLocale() {
        LocaleList localeList = LocaleList.getDefault();
        Set<Locale> localesHasNumberingSystems = new HashSet<>();
        for (int i = 0; i < localeList.size(); i++) {
            Locale locale = localeList.get(i);
            LocaleStore.LocaleInfo localeInfo = LocaleStore.getLocaleInfo(locale);
            if (localeInfo.hasNumberingSystems()) {
                localesHasNumberingSystems.add(locale);
            }
        }
        return convertToLocaleList(localesHasNumberingSystems);
    }

    private static LocaleList convertToLocaleList(Set<Locale> locales) {
        if (locales.isEmpty()) {
            return LocaleList.getEmptyLocaleList();
        }
        return new LocaleList(locales.stream().toArray(Locale[]::new));
    }

    @Override
    public CharSequence getSummary() {
        return new LocaleFeatureProviderImpl().getLocaleNames(getNumberingSystemLocale());
    }
}
