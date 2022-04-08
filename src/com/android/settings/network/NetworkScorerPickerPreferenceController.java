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
package com.android.settings.network;

import android.content.Context;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.List;

/**
 * {@link AbstractPreferenceController} that shows the active network scorer and toggles the
 * preference based on whether or not there are valid scorers installed.
 */
public class NetworkScorerPickerPreferenceController extends BasePreferenceController {

    private final NetworkScoreManager mNetworkScoreManager;

    public NetworkScorerPickerPreferenceController(Context context, String key) {
        super(context, key);
        mNetworkScoreManager =
                (NetworkScoreManager) mContext.getSystemService(Context.NETWORK_SCORE_SERVICE);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        final List<NetworkScorerAppData> allValidScorers =
                mNetworkScoreManager.getAllValidScorers();
        boolean enabled = !allValidScorers.isEmpty();
        preference.setEnabled(enabled);
        if (!enabled) {
            preference.setSummary(null);
            return;
        }

        NetworkScorerAppData scorer = mNetworkScoreManager.getActiveScorer();
        if (scorer == null) {
            preference.setSummary(mContext.getString(
                    R.string.network_scorer_picker_none_preference));
        } else {
            preference.setSummary(scorer.getRecommendationServiceLabel());
        }
    }
}
