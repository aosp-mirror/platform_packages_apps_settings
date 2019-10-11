/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.Activity;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.provider.SearchIndexableResource;

import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.datetime.AutoTimeFormatPreferenceController;
import com.android.settings.datetime.AutoTimePreferenceController;
import com.android.settings.datetime.AutoTimeZonePreferenceController;
import com.android.settings.datetime.DatePreferenceController;
import com.android.settings.datetime.TimeChangeListenerMixin;
import com.android.settings.datetime.TimeFormatPreferenceController;
import com.android.settings.datetime.TimePreferenceController;
import com.android.settings.datetime.TimeZonePreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.datetime.ZoneGetter;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
        getSettingsLifecycle().addObserver(new TimeChangeListenerMixin(context, this));
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Activity activity = getActivity();
        final Intent intent = activity.getIntent();
        final boolean isFromSUW = intent.getBooleanExtra(EXTRA_IS_FROM_SUW, false);

        final AutoTimeZonePreferenceController autoTimeZonePreferenceController =
                new AutoTimeZonePreferenceController(
                        activity, this /* UpdateTimeAndDateCallback */, isFromSUW);
        final AutoTimePreferenceController autoTimePreferenceController =
                new AutoTimePreferenceController(
                        activity, this /* UpdateTimeAndDateCallback */);
        final AutoTimeFormatPreferenceController autoTimeFormatPreferenceController =
                new AutoTimeFormatPreferenceController(
                        activity, this /* UpdateTimeAndDateCallback */);
        controllers.add(autoTimeZonePreferenceController);
        controllers.add(autoTimePreferenceController);
        controllers.add(autoTimeFormatPreferenceController);

        controllers.add(new TimeFormatPreferenceController(
                activity, this /* UpdateTimeAndDateCallback */, isFromSUW));
        controllers.add(new TimeZonePreferenceController(
                activity, autoTimeZonePreferenceController));
        controllers.add(new TimePreferenceController(
                activity, this /* UpdateTimeAndDateCallback */, autoTimePreferenceController));
        controllers.add(new DatePreferenceController(
                activity, this /* UpdateTimeAndDateCallback */, autoTimePreferenceController));
        return controllers;
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
                        .buildDatePicker(getActivity());
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

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                final Calendar now = Calendar.getInstance();
                mSummaryLoader.setSummary(this, ZoneGetter.getTimeZoneOffsetAndName(mContext,
                        now.getTimeZone(), now.getTime()));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };


    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new DateTimeSearchIndexProvider();

    private static class DateTimeSearchIndexProvider extends BaseSearchIndexProvider {

        @Override
        public List<SearchIndexableResource> getXmlResourcesToIndex(
                Context context, boolean enabled) {
            List<SearchIndexableResource> result = new ArrayList<>();

            SearchIndexableResource sir = new SearchIndexableResource(context);
            sir.xmlResId = R.xml.date_time_prefs;
            result.add(sir);

            return result;
        }
    }
}
