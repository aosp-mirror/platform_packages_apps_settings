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

import static com.android.settingslib.fuelgauge.BatterySaverUtils.KEY_NO_SCHEDULE;
import static com.android.settingslib.fuelgauge.BatterySaverUtils.KEY_PERCENTAGE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.widget.RadioButtonPickerFragment;
import com.android.settingslib.fuelgauge.BatterySaverUtils;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Fragment that allows users to customize their automatic battery saver mode settings. <br>
 * <br>
 * Location: Settings > Battery > Battery Saver > Set a Schedule <br>
 * See {@link BatterySaverSchedulePreferenceController} for the controller that manages navigation
 * to this screen from "Settings > Battery > Battery Saver" and the summary. <br>
 * See {@link BatterySaverScheduleRadioButtonsController} & {@link
 * BatterySaverScheduleSeekBarController} for the controller that manages user interactions in this
 * screen.
 */
public class BatterySaverScheduleSettings extends RadioButtonPickerFragment {

    public BatterySaverScheduleRadioButtonsController mRadioButtonController;
    @VisibleForTesting Context mContext;
    private int mSaverPercentage;
    private String mSaverScheduleKey;
    private BatterySaverScheduleSeekBarController mSeekBarController;

    @VisibleForTesting
    final ContentObserver mSettingsObserver =
            new ContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    getPreferenceScreen().removeAll();
                    updateCandidates();
                }
            };

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.battery_saver_schedule_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSeekBarController = new BatterySaverScheduleSeekBarController(context);
        mRadioButtonController =
                new BatterySaverScheduleRadioButtonsController(context, mSeekBarController);
        mContext = context;
    }

    @Override
    public void onResume() {
        super.onResume();
        mContext.getContentResolver()
                .registerContentObserver(
                        Settings.Secure.getUriFor(Settings.Secure.LOW_POWER_WARNING_ACKNOWLEDGED),
                        false,
                        mSettingsObserver);
        mSaverScheduleKey = BatterySaverUtils.getBatterySaverScheduleKey(mContext);
        mSaverPercentage = getSaverPercentage();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
        AsyncTask.execute(() -> logPowerSaver());
        super.onPause();
    }

    @Override
    protected List<? extends CandidateInfo> getCandidates() {
        Context context = getContext();
        List<CandidateInfo> candidates = Lists.newArrayList();
        candidates.add(
                new BatterySaverScheduleCandidateInfo(
                        context.getText(R.string.battery_saver_auto_no_schedule),
                        /* summary */ null,
                        KEY_NO_SCHEDULE,
                        /* enabled */ true));
        BatterySaverUtils.revertScheduleToNoneIfNeeded(context);
        candidates.add(
                new BatterySaverScheduleCandidateInfo(
                        context.getText(R.string.battery_saver_auto_percentage),
                        /* summary */ null,
                        KEY_PERCENTAGE,
                        /* enabled */ true));

        return candidates;
    }

    @Override
    public void bindPreferenceExtra(
            SelectorWithWidgetPreference pref,
            String key,
            CandidateInfo info,
            String defaultKey,
            String systemDefaultKey) {
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
        mSeekBarController.updateSeekBar();
        mSeekBarController.addToScreen(screen);
    }

    @Override
    protected String getDefaultKey() {
        return BatterySaverUtils.getBatterySaverScheduleKey(mContext);
    }

    @Override
    protected boolean setDefaultKey(String key) {
        return mRadioButtonController.setDefaultKey(key);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FUELGAUGE_BATTERY_SAVER_SCHEDULE;
    }

    private void logPowerSaver() {
        final int currentSaverPercentage = getSaverPercentage();
        final String currentSaverScheduleKey =
                BatterySaverUtils.getBatterySaverScheduleKey(mContext);
        if (mSaverScheduleKey.equals(currentSaverScheduleKey)
                && mSaverPercentage == currentSaverPercentage) {
            return;
        }
        FeatureFactory.getFeatureFactory()
                .getMetricsFeatureProvider()
                .action(
                        SettingsEnums.FUELGAUGE_BATTERY_SAVER,
                        SettingsEnums.FIELD_BATTERY_SAVER_SCHEDULE_TYPE,
                        SettingsEnums.FIELD_BATTERY_SAVER_PERCENTAGE_VALUE,
                        currentSaverScheduleKey,
                        currentSaverPercentage);
    }

    private int getSaverPercentage() {
        return Settings.Global.getInt(
                mContext.getContentResolver(), Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, -1);
    }

    static class BatterySaverScheduleCandidateInfo extends CandidateInfo {

        private final CharSequence mLabel;
        private final CharSequence mSummary;
        private final String mKey;

        BatterySaverScheduleCandidateInfo(
                CharSequence label, CharSequence summary, String key, boolean enabled) {
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
