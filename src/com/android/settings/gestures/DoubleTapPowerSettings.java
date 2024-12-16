/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.gestures;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.SearchIndexableResource;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProvider;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.List;

@SearchIndexable
public class DoubleTapPowerSettings extends DashboardFragment {

    private static final String TAG = "DoubleTapPower";

    public static final String PREF_KEY_SUGGESTION_COMPLETE =
            "pref_double_tap_power_suggestion_complete";
    private Context mContext;

    @Override
    public void onAttach(Context context) {
        mContext = context;
        super.onAttach(context);
        SuggestionFeatureProvider suggestionFeatureProvider =
                FeatureFactory.getFeatureFactory().getSuggestionFeatureProvider();
        SharedPreferences prefs = suggestionFeatureProvider.getSharedPrefs(context);
        prefs.edit().putBoolean(PREF_KEY_SUGGESTION_COMPLETE, true).apply();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_GESTURE_DOUBLE_TAP_POWER;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return getDoubleTapPowerSettingsResId(mContext);
    }

    private static int getDoubleTapPowerSettingsResId(Context context) {
        if (!android.service.quickaccesswallet.Flags.launchWalletOptionOnPowerDoubleTap()) {
            return R.xml.double_tap_power_to_open_camera_settings;
        }
        return DoubleTapPowerSettingsUtils
                .isMultiTargetDoubleTapPowerButtonGestureAvailable(context)
                ? R.xml.double_tap_power_settings
                : R.xml.double_tap_power_to_open_camera_settings;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                @NonNull
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        @NonNull Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = getDoubleTapPowerSettingsResId(context);
                    return List.of(sir);
                }
            };
}
