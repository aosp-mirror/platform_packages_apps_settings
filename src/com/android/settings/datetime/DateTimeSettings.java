/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.datetime;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.app.timedetector.TimeDetectorHelper;
import android.content.Context;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.google.android.setupcompat.util.WizardManagerHelper;

@SearchIndexable
public class DateTimeSettings extends DashboardFragment implements
        TimePreferenceController.TimePreferenceHost, DatePreferenceController.DatePreferenceHost {

    private static final String TAG = "DateTimeSettings";

    // have we been launched from the setup wizard?
    protected static final String EXTRA_IS_FROM_SUW = "firstRun";

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DATE_TIME;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.date_time_prefs;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        boolean isFromSUW = WizardManagerHelper.isAnySetupWizard(getIntent());
        getSettingsLifecycle().addObserver(new TimeChangeListenerMixin(context, this));
        use(LocationTimeZoneDetectionPreferenceController.class).setFragment(this);
        use(AutoTimePreferenceController.class).setDateAndTimeCallback(this);
        use(DatePreferenceController.class).setHost(this);
        use(TimePreferenceController.class).setHost(this);
        use(AutoTimeZonePreferenceController.class)
                .setTimeAndDateCallback(this)
                .setFromSUW(isFromSUW);
        use(TimeFormatPreferenceController.class)
                .setTimeAndDateCallback(this)
                .setFromSUW(isFromSUW);

    }

    @Override
    public void updateTimeAndDateDisplay(Context context) {
        updatePreferenceStates();
    }

    @Override
    public Dialog onCreateDialog(int id) {
        switch (id) {
            case DatePreferenceController.DIALOG_DATEPICKER:
                return use(DatePreferenceController.class)
                        .buildDatePicker(getActivity(), TimeDetectorHelper.INSTANCE);
            case TimePreferenceController.DIALOG_TIMEPICKER:
                return use(TimePreferenceController.class)
                        .buildTimePicker(getActivity());
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DatePreferenceController.DIALOG_DATEPICKER:
                return SettingsEnums.DIALOG_DATE_PICKER;
            case TimePreferenceController.DIALOG_TIMEPICKER:
                return SettingsEnums.DIALOG_TIME_PICKER;
            default:
                return 0;
        }
    }

    @Override
    public void showTimePicker() {
        removeDialog(TimePreferenceController.DIALOG_TIMEPICKER);
        showDialog(TimePreferenceController.DIALOG_TIMEPICKER);
    }

    @Override
    public void showDatePicker() {
        showDialog(DatePreferenceController.DIALOG_DATEPICKER);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.date_time_prefs);
}
