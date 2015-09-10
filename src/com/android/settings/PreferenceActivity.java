/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings;

import android.annotation.Nullable;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

public class PreferenceActivity extends SettingsActivity {

    private PreferenceActivityFragment mFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getIntent().putExtra(EXTRA_SHOW_FRAGMENT, PreferenceActivityFragment.class.getName());
        super.onCreate(savedInstanceState);
    }

    public void addPreferencesFromResource(int resource) {
        mFragment.addPreferencesFromResource(resource);
    }

    public Preference findPreference(String preference) {
        return mFragment.findPreference(preference);
    }

    public PreferenceScreen getPreferenceScreen() {
        return mFragment.getPreferenceScreen();
    }

    public void setPreferenceScreen(PreferenceScreen screen) {
        mFragment.setPreferenceScreen(screen);
    }

    public boolean onPreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return super.isValidFragment(fragmentName)
                || PreferenceActivityFragment.class.getName().equals(fragmentName);
    }

    public static class PreferenceActivityFragment extends PreferenceFragment {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            ((PreferenceActivity) getActivity()).mFragment = this;
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (((PreferenceActivity) getActivity()).onPreferenceTreeClick(preference)) {
                return true;
            }
            return super.onPreferenceTreeClick(preference);
        }
    }

}
