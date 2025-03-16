/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.os.UserManager.DISALLOW_CONFIG_BLUETOOTH;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.FeatureFlagUtils;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.dashboard.RestrictedDashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

/** Settings fragment containing bluetooth audio routing. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AccessibilityAudioRoutingFragment extends RestrictedDashboardFragment {
    private static final String TAG = "AccessibilityAudioRoutingFragment";

    public AccessibilityAudioRoutingFragment() {
        super(DISALLOW_CONFIG_BLUETOOTH);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.HEARING_AID_AUDIO_ROUTING;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_audio_routing_fragment;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @VisibleForTesting
    static boolean isPageSearchEnabled(Context context) {
        if (!FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_AUDIO_ROUTING)) {
            return false;
        }

        final HearingAidHelper mHelper = new HearingAidHelper(context);
        return mHelper.isHearingAidSupported();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_audio_routing_fragment) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    if (Flags.fixA11ySettingsSearch()) {
                        return AccessibilityAudioRoutingFragment.isPageSearchEnabled(context);
                    } else {
                        return super.isPageSearchEnabled(context);
                    }
                }
            };
}
