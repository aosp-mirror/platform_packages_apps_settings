/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import com.android.internal.app.LocalePicker;
import com.android.settings.R;

import java.util.List;

/**
 * List preference that allows the user to pick a locale from the list of
 * supported device locales.
 */
public class LocalePreference extends ListPreference {
    public LocalePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LocalePreference(Context context) {
        super(context);
        init(context);
    }

    public void init(Context context) {
        List<LocalePicker.LocaleInfo> locales = LocalePicker.getAllAssetLocales(context,
                false /* in developer mode */);

        final int finalSize = locales.size();
        final CharSequence[] entries = new CharSequence[finalSize + 1];
        final CharSequence[] entryValues = new CharSequence[finalSize + 1];
        entries[0] = context.getResources().getString(R.string.locale_default);
        entryValues[0] = "";

        for (int i = 0; i < finalSize; i++) {
            final LocalePicker.LocaleInfo info = locales.get(i);
            entries[i + 1] = info.toString();
            entryValues[i + 1] = info.getLocale().toString();
        }

        setEntries(entries);
        setEntryValues(entryValues);
    }
}
