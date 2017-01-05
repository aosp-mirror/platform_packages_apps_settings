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
 * limitations under the License
 */

package com.android.settings.qstile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.instrumentation.Instrumentable;
import com.android.settings.widget.SwitchBar;

public class DevelopmentTileConfigActivity extends SettingsActivity {

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent())
                .putExtra(EXTRA_SHOW_FRAGMENT, DevelopmentTileConfigFragment.class.getName())
                .putExtra(EXTRA_HIDE_DRAWER, true);
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return (DevelopmentTileConfigFragment.class.getName().equals(fragmentName));
    }

    public static class DevelopmentTileConfigFragment extends SettingsPreferenceFragment implements
            SharedPreferences.OnSharedPreferenceChangeListener {

        private DevelopmentModeTile.DevModeProperties mProps =
                new DevelopmentModeTile.DevModeProperties();

        private SwitchBar mSwitchBar;
        private View mDisabledMessage;

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
            getPreferenceManager()
                    .setSharedPreferencesName(DevelopmentModeTile.SHARED_PREFERENCES_NAME);
            addPreferencesFromResource(R.xml.development_tile_prefs);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mDisabledMessage = setPinnedHeaderView(R.layout.development_tile_config_header);
            refreshHeader();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
            mSwitchBar.setEnabled(false);
        }

        @Override
        public int getMetricsCategory() {
            return Instrumentable.METRICS_CATEGORY_UNKNOWN;
        }

        @Override
        public void onResume() {
            super.onResume();
            refreshHeader();
            getPreferenceManager().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceManager().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            refreshHeader();
        }

        private void refreshHeader() {
            if (mSwitchBar != null && mDisabledMessage != null) {
                mProps.refreshState(getActivity());
                if (mProps.isSet) {
                    mSwitchBar.show();
                    mDisabledMessage.setVisibility(View.GONE);
                } else {
                    mSwitchBar.hide();
                    mDisabledMessage.setVisibility(View.VISIBLE);
                }
                mSwitchBar.setChecked(mProps.allMatch);
            }
        }
    }
}
