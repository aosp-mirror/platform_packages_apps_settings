/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Settings fragment containing more options of captioning properties. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class CaptionMoreOptionsFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {
    private static final String PREF_LOCALE = "captioning_locale";

    private CaptioningManager mCaptioningManager;

    private LocalePreference mLocale;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_CAPTION_MORE_OPTIONS;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);

        addPreferencesFromResource(R.xml.captioning_more_options);
        initializeAllPreferences();
        updateAllPreferences();
        installUpdateListeners();
    }

    private void initializeAllPreferences() {
        mLocale = (LocalePreference) findPreference(PREF_LOCALE);
    }

    private void installUpdateListeners() {
        mLocale.setOnPreferenceChangeListener(this);
    }

    private void updateAllPreferences() {
        final String rawLocale = mCaptioningManager.getRawLocale();
        mLocale.setValue(rawLocale == null ? "" : rawLocale);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final ContentResolver cr = getActivity().getContentResolver();
        if (mLocale == preference) {
            Settings.Secure.putString(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_LOCALE, (String) value);
        }

        return true;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_caption;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.captioning_more_options);
}
