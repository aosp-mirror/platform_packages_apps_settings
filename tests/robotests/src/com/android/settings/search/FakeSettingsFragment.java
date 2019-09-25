/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.search;

import android.content.Context;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;

/**
 * Test class for Settings Search Indexing.
 * If you change this class, please run robotests to make sure they still pass.
 */
public class FakeSettingsFragment extends DashboardFragment {

    public static final String TITLE = "raw title";
    public static final String SUMMARY_ON = "raw summary on";
    public static final String SUMMARY_OFF = "raw summary off";
    public static final String ENTRIES = "rawentries";
    public static final String KEYWORDS = "keywords, keywordss, keywordsss";
    public static final String SPACE_KEYWORDS = "keywords keywordss keywordsss";
    public static final String SCREEN_TITLE = "raw screen title";
    public static final String CLASS_NAME = FakeSettingsFragment.class.getName();
    public static final int ICON = 0xff;
    public static final String INTENT_ACTION = "raw action";
    public static final String PACKAGE_NAME = "raw target package";
    public static final String TARGET_CLASS = "raw target class";
    public static final String TARGET_PACKAGE = "raw package name";
    public static final String KEY = "raw key";
    public static final boolean ENABLED = true;


    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DISPLAY;
    }

    @Override
    protected String getLogTag() {
        return "";
    }

    @Override
    protected int getPreferenceScreenResId() {
        return com.android.settings.R.xml.display_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return null;
    }

    /** Index provider used to expose this fragment in search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = TITLE;
                    data.summaryOn = SUMMARY_ON;
                    data.summaryOff = SUMMARY_OFF;
                    data.entries = ENTRIES;
                    data.keywords = KEYWORDS;
                    data.screenTitle = SCREEN_TITLE;
                    data.packageName = PACKAGE_NAME;
                    data.intentAction = INTENT_ACTION;
                    data.intentTargetClass = TARGET_CLASS;
                    data.intentTargetPackage = TARGET_PACKAGE;
                    data.key = KEY;
                    data.iconResId = ICON;
                    data.enabled = ENABLED;

                    final List<SearchIndexableRaw> result = new ArrayList<>(1);
                    result.add(data);
                    return result;
                }

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();

                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = com.android.settings.R.xml.display_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add("pref_key_1");
                    keys.add("pref_key_3");
                    return keys;
                }
            };
}