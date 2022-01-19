/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.nearby;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.MainSwitchPreference;

import java.util.Objects;

/**
 * Fragment with the top level fast pair settings.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class FastPairSettingsFragment extends SettingsPreferenceFragment {

    private static final String SCAN_SWITCH_KEY = "fast_pair_scan_switch";

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        MainSwitchPreference mainSwitchPreference = Objects.requireNonNull(
                findPreference(SCAN_SWITCH_KEY));
        mainSwitchPreference.addOnSwitchChangeListener(
                (switchView, isChecked) ->
                        Settings.Secure.putInt(getContentResolver(),
                                Settings.Secure.FAST_PAIR_SCAN_ENABLED, isChecked ? 1 : 0));
        mainSwitchPreference.setChecked(isFastPairScanAvailable());
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.CONNECTION_DEVICE_ADVANCED_FAST_PAIR;
    }

    @Override
    public int getHelpResource() {
        return 0;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.fast_pair_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.fast_pair_settings);

    private boolean isFastPairScanAvailable() {
        return Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.FAST_PAIR_SCAN_ENABLED, 1) != 0;
    }
}
