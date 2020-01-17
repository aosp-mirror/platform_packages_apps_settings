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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settingslib.widget.RadioButtonPreference;

import java.util.List;

/**
 * Fragment for choosing default network scorer.
 */
public class NetworkScorerPicker extends InstrumentedPreferenceFragment implements
        RadioButtonPreference.OnClickListener {

    private NetworkScoreManager mNetworkScoreManager;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_NETWORK_SCORER;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        updateCandidates();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mNetworkScoreManager = createNetworkScorerManager(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        // this is needed so the back button goes back to previous fragment
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.network_scorer_picker_prefs;
    }

    @VisibleForTesting
    public void updateCandidates() {
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();

        final List<NetworkScorerAppData> scorers = mNetworkScoreManager.getAllValidScorers();
        final String defaultAppKey = getActiveScorerPackage();

        final RadioButtonPreference nonePref = new RadioButtonPreference(getPrefContext());
        nonePref.setTitle(R.string.network_scorer_picker_none_preference);
        if (scorers.isEmpty()) {
            nonePref.setChecked(true);
        } else {
            nonePref.setKey(null);
            nonePref.setChecked(TextUtils.isEmpty(defaultAppKey));
            nonePref.setOnClickListener(this);
        }
        screen.addPreference(nonePref);

        final int numScorers = scorers.size();
        for (int i = 0; i < numScorers; i++) {
            final RadioButtonPreference pref = new RadioButtonPreference(getPrefContext());
            final NetworkScorerAppData appData = scorers.get(i);
            final String appKey = appData.getRecommendationServicePackageName();
            pref.setTitle(appData.getRecommendationServiceLabel());
            pref.setKey(appKey);
            pref.setChecked(TextUtils.equals(defaultAppKey, appKey));
            pref.setOnClickListener(this);
            screen.addPreference(pref);
        }
    }

    private String getActiveScorerPackage() {
        return mNetworkScoreManager.getActiveScorerPackage();
    }

    private boolean setActiveScorer(String key) {
        if (!TextUtils.equals(key, getActiveScorerPackage())) {
            return mNetworkScoreManager.setActiveScorer(key);
        }
        return false;
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference selected) {
        final String selectedKey = selected.getKey();
        final boolean success = setActiveScorer(selectedKey);
        if (success) {
            updateCheckedState(selectedKey);
        }
    }

    private void updateCheckedState(String selectedKey) {
        final PreferenceScreen screen = getPreferenceScreen();
        final int count = screen.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            final Preference pref = screen.getPreference(i);
            if (pref instanceof RadioButtonPreference) {
                final RadioButtonPreference radioPref = (RadioButtonPreference) pref;
                radioPref.setChecked(TextUtils.equals(pref.getKey(), selectedKey));
            }
        }
    }

    @VisibleForTesting
    NetworkScoreManager createNetworkScorerManager(Context context) {
        return (NetworkScoreManager) context.getSystemService(Context.NETWORK_SCORE_SERVICE);
    }
}
