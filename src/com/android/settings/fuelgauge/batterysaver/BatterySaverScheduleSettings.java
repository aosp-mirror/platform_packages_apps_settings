/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.preference.PreferenceScreen;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settings.R;
import com.android.settings.widget.RadioButtonPreference;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.widget.CandidateInfo;
import com.google.common.collect.Lists;
import java.util.List;

public class BatterySaverScheduleSettings extends RadioButtonPickerFragment {

    private static final String KEY_NO_SCHEDULE = "key_battery_saver_no_schedule";
    private static final String KEY_ROUTINE = "key_battery_saver_routine";
    private static final String KEY_PERCENTAGE = "key_battery_saver_percentage";
    public static final int MAX_SEEKBAR_VALUE = 15;
    public static final int MIN_SEEKBAR_VALUE = 1;
    public static final String KEY_BATTERY_SAVER_SEEK_BAR = "battery_saver_seek_bar";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.battery_saver_schedule_settings;
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        Context context = getContext();
        List<CandidateInfo> candidates = Lists.newArrayList();
        candidates.add(new BatterySaverScheduleCandidateInfo(
                context.getText(R.string.battery_saver_auto_no_schedule),
                /* summary */ null,
                KEY_NO_SCHEDULE,
                /* enabled */ true));
        candidates.add(new BatterySaverScheduleCandidateInfo(
                context.getText(R.string.battery_saver_auto_routine),
                context.getText(R.string.battery_saver_auto_routine_summary),
                KEY_ROUTINE,
                /* enabled */ true));
        candidates.add(new BatterySaverScheduleCandidateInfo(
                context.getText(R.string.battery_saver_auto_percentage),
                /* summary */ null,
                KEY_PERCENTAGE,
                /* enabled */ true));

        return candidates;
    }

    @Override
    public void bindPreferenceExtra(RadioButtonPreference pref, String key, CandidateInfo info,
            String defaultKey, String systemDefaultKey) {
        final BatterySaverScheduleCandidateInfo candidateInfo =
                (BatterySaverScheduleCandidateInfo) info;
        final CharSequence summary = candidateInfo.getSummary();
        if (summary != null) {
            pref.setSummary(summary);
            pref.setAppendixVisibility(View.GONE);
        }
    }

    @Override
    protected void addStaticPreferences(PreferenceScreen screen) {
        SeekBarPreference seekbar = new SeekBarPreference(getContext());
        seekbar.setMax(MAX_SEEKBAR_VALUE);
        seekbar.setMin(MIN_SEEKBAR_VALUE);
        seekbar.setTitle(R.string.battery_saver_seekbar_title_placeholder);
        seekbar.setKey(KEY_BATTERY_SAVER_SEEK_BAR);
        screen.addPreference(seekbar);
    }

    @Override
    protected String getDefaultKey() {
        return null;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return 0;
    }

    static class BatterySaverScheduleCandidateInfo extends CandidateInfo {

        private final CharSequence mLabel;
        private final CharSequence mSummary;
        private final String mKey;

        BatterySaverScheduleCandidateInfo(CharSequence label, CharSequence summary, String key,
                boolean enabled) {
            super(enabled);
            mLabel = label;
            mKey = key;
            mSummary = summary;
        }

        @Override
        public CharSequence loadLabel() {
            return mLabel;
        }

        @Override
        public Drawable loadIcon() {
            return null;
        }

        @Override
        public String getKey() {
            return mKey;
        }

        public CharSequence getSummary() {
            return mSummary;
        }
    }
}