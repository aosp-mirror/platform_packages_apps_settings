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

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;

import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.google.common.primitives.Floats;

import java.util.ArrayList;
import java.util.List;

/** Settings fragment containing captioning properties. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class CaptionPropertiesFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {
    private static final String PREF_SWITCH = "captioning_preference_switch";
    private static final String PREF_TEXT = "captioning_caption_appearance";
    private static final String PREF_MORE = "captioning_more_options";

    private CaptioningManager mCaptioningManager;

    private SwitchPreference mSwitch;
    private Preference mTextAppearance;
    private Preference mMoreOptions;

    private final List<Preference> mPreferenceList = new ArrayList<>();
    private float[] mFontSizeValuesArray;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_CAPTION_PROPERTIES;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);

        addPreferencesFromResource(R.xml.captioning_settings);
        initializeAllPreferences();
        installUpdateListeners();
        initFontSizeValuesArray();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllPreferences();
    }

    private void initializeAllPreferences() {
        mSwitch = (SwitchPreference) findPreference(PREF_SWITCH);
        mTextAppearance = (Preference) findPreference(PREF_TEXT);
        mMoreOptions = (Preference) findPreference(PREF_MORE);

        mPreferenceList.add(mTextAppearance);
        mPreferenceList.add(mMoreOptions);
    }

    private void installUpdateListeners() {
        mSwitch.setOnPreferenceChangeListener(this);
    }

    private void initFontSizeValuesArray() {
        final String[] fontSizeValuesStrArray = getPrefContext().getResources().getStringArray(
                R.array.captioning_font_size_selector_values);
        final int length = fontSizeValuesStrArray.length;
        mFontSizeValuesArray = new float[length];
        for (int i = 0; i < length; ++i) {
            mFontSizeValuesArray[i] = Float.parseFloat(fontSizeValuesStrArray[i]);
        }
    }

    private void updateAllPreferences() {
        mSwitch.setChecked(mCaptioningManager.isEnabled());
        mTextAppearance.setSummary(geTextAppearanceSummary(getPrefContext()));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final ContentResolver cr = getActivity().getContentResolver();
        if (mSwitch == preference) {
            Settings.Secure.putInt(
                    cr, Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED, (boolean) value ? 1 : 0);
        }

        return true;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_caption;
    }

    private CharSequence geTextAppearanceSummary(Context context) {
        final String[] fontSizeSummaries = context.getResources().getStringArray(
                R.array.captioning_font_size_selector_summaries);

        final float fontSize = mCaptioningManager.getFontScale();
        final int idx = Floats.indexOf(mFontSizeValuesArray, fontSize);

        return fontSizeSummaries[idx == /* not exist */ -1 ? 0 : idx];
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.captioning_settings);
}
