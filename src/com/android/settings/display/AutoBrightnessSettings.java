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

package com.android.settings.display;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.display.AutoBrightnessObserver;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchPreference;
import com.android.settingslib.search.SearchIndexable;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AutoBrightnessSettings extends DashboardFragment {

    private static final String TAG = "AutoBrightnessSettings";

    private AutoBrightnessObserver mAutoBrightnessObserver;

    private final Runnable mCallback = () -> {
        final int value = Settings.System.getInt(
                getContext().getContentResolver(),
                SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        SettingsMainSwitchPreference pref = findPreference("auto_brightness");
        if (pref == null) return;
        pref.setChecked(value == SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mAutoBrightnessObserver = new AutoBrightnessObserver(getContext());
    }

    @Override
    public void onStart() {
        super.onStart();
        mAutoBrightnessObserver.subscribe(mCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAutoBrightnessObserver.unsubscribe();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.auto_brightness_detail;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_AUTO_BRIGHTNESS;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_auto_brightness;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.auto_brightness_detail);
}
