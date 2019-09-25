/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dream;

import static com.android.settingslib.dream.DreamBackend.EITHER;
import static com.android.settingslib.dream.DreamBackend.NEVER;
import static com.android.settingslib.dream.DreamBackend.WHILE_CHARGING;
import static com.android.settingslib.dream.DreamBackend.WHILE_DOCKED;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.WhenToDream;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class DreamSettings extends DashboardFragment {

    private static final String TAG = "DreamSettings";
    static final String WHILE_CHARGING_ONLY = "while_charging_only";
    static final String WHILE_DOCKED_ONLY = "while_docked_only";
    static final String EITHER_CHARGING_OR_DOCKED = "either_charging_or_docked";
    static final String NEVER_DREAM = "never";

    @WhenToDream
    static int getSettingFromPrefKey(String key) {
        switch (key) {
            case WHILE_CHARGING_ONLY:
                return WHILE_CHARGING;
            case WHILE_DOCKED_ONLY:
                return WHILE_DOCKED;
            case EITHER_CHARGING_OR_DOCKED:
                return EITHER;
            case NEVER_DREAM:
            default:
                return NEVER;
        }
    }

    static String getKeyFromSetting(@WhenToDream int dreamSetting) {
        switch (dreamSetting) {
            case WHILE_CHARGING:
                return WHILE_CHARGING_ONLY;
            case WHILE_DOCKED:
                return WHILE_DOCKED_ONLY;
            case EITHER:
                return EITHER_CHARGING_OR_DOCKED;
            case NEVER:
            default:
                return NEVER_DREAM;
        }
    }

    static int getDreamSettingDescriptionResId(@WhenToDream int dreamSetting) {
        switch (dreamSetting) {
            case WHILE_CHARGING:
                return R.string.screensaver_settings_summary_sleep;
            case WHILE_DOCKED:
                return R.string.screensaver_settings_summary_dock;
            case EITHER:
                return R.string.screensaver_settings_summary_either_long;
            case NEVER:
            default:
                return R.string.screensaver_settings_summary_never;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DREAM;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.dream_fragment_overview;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_screen_saver;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    public static CharSequence getSummaryTextWithDreamName(Context context) {
        DreamBackend backend = DreamBackend.getInstance(context);
        return getSummaryTextFromBackend(backend, context);
    }

    @VisibleForTesting
    static CharSequence getSummaryTextFromBackend(DreamBackend backend, Context context) {
        if (!backend.isEnabled()) {
            return context.getString(R.string.screensaver_settings_summary_off);
        } else {
            return backend.getActiveDreamName();
        }
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new WhenToDreamPreferenceController(context));
        controllers.add(new StartNowPreferenceController(context));
        return controllers;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER
            = new BaseSearchIndexProvider(R.xml.dream_fragment_overview) {

        @Override
        public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
            return buildPreferenceControllers(context);
        }
    };
}

