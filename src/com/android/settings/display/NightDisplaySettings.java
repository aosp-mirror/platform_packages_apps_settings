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

package com.android.settings.display;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import androidx.preference.Preference;

import com.android.internal.app.ColorDisplayController;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Settings screen for Night display.
 */
public class NightDisplaySettings extends DashboardFragment
        implements ColorDisplayController.Callback, Indexable {

    private static final String TAG = "NightDisplaySettings";

    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;

    private ColorDisplayController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();
        mController = new ColorDisplayController(context);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Listen for changes only while visible.
        mController.setListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop listening for state changes.
        mController.setListener(null);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if ("night_display_end_time".equals(preference.getKey())) {
            showDialog(DIALOG_END_TIME);
            return true;
        } else if ("night_display_start_time".equals(preference.getKey())) {
            showDialog(DIALOG_START_TIME);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public Dialog onCreateDialog(final int dialogId) {
        if (dialogId == DIALOG_START_TIME || dialogId == DIALOG_END_TIME) {
            final LocalTime initialTime;
            if (dialogId == DIALOG_START_TIME) {
                initialTime = mController.getCustomStartTime();
            } else {
                initialTime = mController.getCustomEndTime();
            }

            final Context context = getContext();
            final boolean use24HourFormat = android.text.format.DateFormat.is24HourFormat(context);
            return new TimePickerDialog(context, (view, hourOfDay, minute) -> {
                final LocalTime time = LocalTime.of(hourOfDay, minute);
                if (dialogId == DIALOG_START_TIME) {
                    mController.setCustomStartTime(time);
                } else {
                    mController.setCustomEndTime(time);
                }
            }, initialTime.getHour(), initialTime.getMinute(), use24HourFormat);
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_START_TIME:
                return MetricsEvent.DIALOG_NIGHT_DISPLAY_SET_START_TIME;
            case DIALOG_END_TIME:
                return MetricsEvent.DIALOG_NIGHT_DISPLAY_SET_END_TIME;
            default:
                return 0;
        }
    }

    @Override
    public void onActivated(boolean activated) {
        // Update activated and temperature preferences.
        updatePreferenceStates();
    }

    @Override
    public void onAutoModeChanged(int autoMode) {
        // Update auto mode, start time, and end time preferences.
        updatePreferenceStates();
    }

    @Override
    public void onColorTemperatureChanged(int colorTemperature) {
        // Update temperature preference.
        updatePreferenceStates();
    }

    @Override
    public void onCustomStartTimeChanged(LocalTime startTime) {
        // Update start time preference.
        updatePreferenceStates();
    }

    @Override
    public void onCustomEndTimeChanged(LocalTime endTime) {
        // Update end time preference.
        updatePreferenceStates();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.night_display_settings;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NIGHT_DISPLAY_SETTINGS;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_night_display;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private static List <AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>(1);
        controllers.add(new NightDisplayFooterPreferenceController(context));
        return controllers;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    final ArrayList<SearchIndexableResource> result = new ArrayList<>();
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.night_display_settings;
                    result.add(sir);
                    return result;
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return ColorDisplayController.isAvailable(context);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                    Context context) {
                    return buildPreferenceControllers(context);
                }
            };
}
