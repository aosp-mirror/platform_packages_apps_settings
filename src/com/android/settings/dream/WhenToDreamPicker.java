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

package com.android.settings.dream;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.android.settings.R;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.widget.CandidateInfo;

import java.util.ArrayList;
import java.util.List;

public class WhenToDreamPicker extends RadioButtonPickerFragment {

    private static final String TAG = "WhenToDreamPicker";
    private DreamBackend mBackend;
    private boolean mDreamsSupportedOnBattery;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mBackend = DreamBackend.getInstance(context);
        mDreamsSupportedOnBattery = getResources().getBoolean(
                com.android.internal.R.bool.config_dreamsEnabledOnBattery);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.when_to_dream_settings;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DREAM;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        final String[] entries = entries();
        final String[] values = keys();
        final List<WhenToDreamCandidateInfo> candidates = new ArrayList<>();

        if (entries == null || entries.length <= 0) return null;
        if (values == null || values.length != entries.length) {
            throw new IllegalArgumentException("Entries and values must be of the same length.");
        }

        for (int i = 0; i < entries.length; i++) {
            candidates.add(new WhenToDreamCandidateInfo(entries[i], values[i]));
        }

        return candidates;
    }

    private String[] entries() {
        if (mDreamsSupportedOnBattery) {
            return getResources().getStringArray(R.array.when_to_start_screensaver_entries);
        }
        return getResources().getStringArray(R.array.when_to_start_screensaver_entries_no_battery);
    }

    private String[] keys() {
        if (mDreamsSupportedOnBattery) {
            return getResources().getStringArray(R.array.when_to_start_screensaver_values);
        }
        return getResources().getStringArray(R.array.when_to_start_screensaver_values_no_battery);
    }

    @Override
    protected String getDefaultKey() {
        return DreamSettings.getKeyFromSetting(mBackend.getWhenToDreamSetting());
    }

    @Override
    protected boolean setDefaultKey(String key) {
        mBackend.setWhenToDream(DreamSettings.getSettingFromPrefKey(key));
        return true;
    }

    @Override
    protected void onSelectionPerformed(boolean success) {
        super.onSelectionPerformed(success);

        getActivity().finish();
    }

    private final class WhenToDreamCandidateInfo extends CandidateInfo {
        private final String name;
        private final String key;

        WhenToDreamCandidateInfo(String title, String value) {
            super(true);

            name = title;
            key = value;
        }

        @Override
        public CharSequence loadLabel() {
            return name;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return key;
        }
    }
}
