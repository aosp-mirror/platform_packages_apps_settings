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

package com.android.settings.notification;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

/**
 * Spatial audio settings located in the sound menu
 */
@SearchIndexable
public class SpatialAudioSettings extends DashboardFragment {

    private static final String TAG = "SpatialAudioSettings";
    private static final String KEY_FOOTER = "spatial_audio_footer";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        FooterPreference footerPreference = findPreference(KEY_FOOTER);
        if (footerPreference != null) {
            footerPreference.setLearnMoreText(
                    getString(R.string.spatial_audio_footer_learn_more_text));
            footerPreference.setLearnMoreAction(
                    view -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_SPATIAL_AUDIO;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.spatial_audio_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.spatial_audio_settings);
}
