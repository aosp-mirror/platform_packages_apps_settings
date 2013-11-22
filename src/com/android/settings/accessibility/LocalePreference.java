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
import android.content.res.Resources;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.android.settings.R;

import java.text.Collator;
import java.util.Arrays;
import java.util.Locale;

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
        final String[] systemLocales = Resources.getSystem().getAssets().getLocales();
        Arrays.sort(systemLocales);

        final Resources resources = context.getResources();
        final String[] specialLocaleCodes = resources.getStringArray(
                com.android.internal.R.array.special_locale_codes);
        final String[] specialLocaleNames = resources.getStringArray(
                com.android.internal.R.array.special_locale_names);

        int finalSize = 0;

        final int origSize = systemLocales.length;
        final LocaleInfo[] localeInfos = new LocaleInfo[origSize];
        for (int i = 0; i < origSize; i++) {
            final String localeStr = systemLocales[i];
            final int len = localeStr.length();
            if (len != 5) {
                continue;
            }

            final String language = localeStr.substring(0, 2);
            final String country = localeStr.substring(3, 5);
            final Locale l = new Locale(language, country);

            if (finalSize == 0) {
                localeInfos[finalSize++] = new LocaleInfo(l.getDisplayLanguage(l), l);
            } else {
                // check previous entry:
                // same lang and a country -> upgrade to full name and
                // insert ours with full name
                // diff lang -> insert ours with lang-only name
                final LocaleInfo previous = localeInfos[finalSize - 1];
                if (previous.locale.getLanguage().equals(language)
                        && !previous.locale.getLanguage().equals("zz")) {
                    previous.label = getDisplayName(
                            localeInfos[finalSize - 1].locale, specialLocaleCodes,
                            specialLocaleNames);
                    localeInfos[finalSize++] = new LocaleInfo(getDisplayName(l,
                            specialLocaleCodes, specialLocaleNames), l);
                } else {
                    final String displayName;
                    if (localeStr.equals("zz_ZZ")) {
                        displayName = "[Developer] Accented English";
                    } else if (localeStr.equals("zz_ZY")) {
                        displayName = "[Developer] Fake Bi-Directional";
                    } else {
                        displayName = l.getDisplayLanguage(l);
                    }
                    localeInfos[finalSize++] = new LocaleInfo(displayName, l);
                }
            }
        }

        final CharSequence[] entries = new CharSequence[finalSize + 1];
        final CharSequence[] entryValues = new CharSequence[finalSize + 1];
        Arrays.sort(localeInfos, 0, finalSize);

        entries[0] = resources.getString(R.string.locale_default);
        entryValues[0] = "";

        for (int i = 0; i < finalSize; i++) {
            final LocaleInfo info = localeInfos[i];
            entries[i + 1] = info.toString();
            entryValues[i + 1] = info.locale.toString();
        }

        setEntries(entries);
        setEntryValues(entryValues);
    }

    private static String getDisplayName(
            Locale l, String[] specialLocaleCodes, String[] specialLocaleNames) {
        String code = l.toString();

        for (int i = 0; i < specialLocaleCodes.length; i++) {
            if (specialLocaleCodes[i].equals(code)) {
                return specialLocaleNames[i];
            }
        }

        return l.getDisplayName(l);
    }

    private static class LocaleInfo implements Comparable<LocaleInfo> {
        private static final Collator sCollator = Collator.getInstance();

        public String label;
        public Locale locale;

        public LocaleInfo(String label, Locale locale) {
            this.label = label;
            this.locale = locale;
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public int compareTo(LocaleInfo another) {
            return sCollator.compare(this.label, another.label);
        }
    }
}
