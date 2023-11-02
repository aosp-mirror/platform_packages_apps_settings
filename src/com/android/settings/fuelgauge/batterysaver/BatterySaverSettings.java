/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterysaver;

import android.app.settings.SettingsEnums;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

/** Battery saver settings page */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class BatterySaverSettings extends DashboardFragment {
    private static final String TAG = "BatterySaverSettings";
    private static final String KEY_FOOTER_PREFERENCE = "battery_saver_footer_preference";
    private String mHelpUri;

    @Override
    public void onStart() {
        super.onStart();
        setupFooter();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.OPEN_BATTERY_SAVER;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.battery_saver_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_battery_saver_settings;
    }

    /** For Search. */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.battery_saver_settings);

    // Updates the footer for this page.
    @VisibleForTesting
    void setupFooter() {
        mHelpUri = getString(R.string.help_url_battery_saver_settings);
        if (!TextUtils.isEmpty(mHelpUri)) {
            addHelpLink();
        }
    }

    // Changes the text to include a learn more link if possible.
    @VisibleForTesting
    void addHelpLink() {
        FooterPreference pref = getPreferenceScreen().findPreference(KEY_FOOTER_PREFERENCE);
        if (pref != null) {
            pref.setLearnMoreAction(
                    v -> {
                        mMetricsFeatureProvider.action(
                                getContext(), SettingsEnums.ACTION_APP_BATTERY_LEARN_MORE);
                        startActivityForResult(
                                HelpUtils.getHelpIntent(
                                        getContext(),
                                        getString(R.string.help_url_battery_saver_settings),
                                        /* backupContext= */ ""),
                                /* requestCode= */ 0);
                    });
            pref.setLearnMoreText(getString(R.string.battery_saver_link_a11y));
        }
    }
}
