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
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.accessibility.CaptioningManager;
import android.view.accessibility.CaptioningManager.CaptionStyle;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.List;

/** Settings fragment containing font style of captioning properties. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class CaptionAppearanceFragment extends DashboardFragment {

    private static final String TAG = "CaptionAppearanceFragment";
    @VisibleForTesting
    static final String PREF_CUSTOM = "custom";
    @VisibleForTesting
    static final List<String> CAPTIONING_FEATURE_KEYS = Arrays.asList(
            Settings.Secure.ACCESSIBILITY_CAPTIONING_PRESET
    );

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    @VisibleForTesting
    AccessibilitySettingsContentObserver mSettingsContentObserver;
    private CaptioningManager mCaptioningManager;
    private PreferenceCategory mCustom;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_CAPTION_APPEARANCE;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        mCaptioningManager = getContext().getSystemService(CaptioningManager.class);
        mSettingsContentObserver = new AccessibilitySettingsContentObserver(mHandler);
        mSettingsContentObserver.registerKeysToObserverCallback(CAPTIONING_FEATURE_KEYS,
                key -> refreshShowingCustom());
        mCustom = findPreference(PREF_CUSTOM);
        refreshShowingCustom();
    }

    @Override
    public void onStart() {
        super.onStart();
        mSettingsContentObserver.register(getContext().getContentResolver());
    }

    @Override
    public void onStop() {
        super.onStop();
        getContext().getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.captioning_appearance;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_caption;
    }

    private void refreshShowingCustom() {
        final boolean isCustomPreset =
                mCaptioningManager.getRawUserStyle() == CaptionStyle.PRESET_CUSTOM;
        mCustom.setVisible(isCustomPreset);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.captioning_appearance);
}

