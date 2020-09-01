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

package com.android.settings.datetime.timezone;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.app.timezonedetector.ManualTimeZoneSuggestion;
import android.app.timezonedetector.TimeZoneDetector;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceCategory;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.datetime.timezone.model.FilteredCountryTimeZones;
import com.android.settings.datetime.timezone.model.TimeZoneData;
import com.android.settings.datetime.timezone.model.TimeZoneDataLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * The class displays a time zone picker either by regions or fixed offset time zones.
 */
@SearchIndexable
public class TimeZoneSettings extends DashboardFragment {

    private static final String TAG = "TimeZoneSettings";

    private static final int MENU_BY_REGION = Menu.FIRST;
    private static final int MENU_BY_OFFSET = Menu.FIRST + 1;

    private static final int REQUEST_CODE_REGION_PICKER = 1;
    private static final int REQUEST_CODE_ZONE_PICKER = 2;
    private static final int REQUEST_CODE_FIXED_OFFSET_ZONE_PICKER = 3;

    private static final String PREF_KEY_REGION = "time_zone_region";
    private static final String PREF_KEY_REGION_CATEGORY = "time_zone_region_preference_category";
    private static final String PREF_KEY_FIXED_OFFSET_CATEGORY =
            "time_zone_fixed_offset_preference_category";

    private Locale mLocale;
    private boolean mSelectByRegion;
    private TimeZoneData mTimeZoneData;
    private Intent mPendingZonePickerRequestResult;

    private String mSelectedTimeZoneId;
    private TimeZoneInfo.Formatter mTimeZoneInfoFormatter;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ZONE_PICKER;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.time_zone_prefs;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    /**
     * Called during onAttach
     */
    @VisibleForTesting
    @Override
    public List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        mLocale = context.getResources().getConfiguration().getLocales().get(0);
        mTimeZoneInfoFormatter = new TimeZoneInfo.Formatter(mLocale, new Date());
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        RegionPreferenceController regionPreferenceController =
                new RegionPreferenceController(context);
        regionPreferenceController.setOnClickListener(this::startRegionPicker);
        RegionZonePreferenceController regionZonePreferenceController =
                new RegionZonePreferenceController(context);
        regionZonePreferenceController.setOnClickListener(this::onRegionZonePreferenceClicked);
        FixedOffsetPreferenceController fixedOffsetPreferenceController =
                new FixedOffsetPreferenceController(context);
        fixedOffsetPreferenceController.setOnClickListener(this::startFixedOffsetPicker);

        controllers.add(regionPreferenceController);
        controllers.add(regionZonePreferenceController);
        controllers.add(fixedOffsetPreferenceController);
        return controllers;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Hide all interactive preferences
        setPreferenceCategoryVisible((PreferenceCategory) findPreference(
                PREF_KEY_REGION_CATEGORY), false);
        setPreferenceCategoryVisible((PreferenceCategory) findPreference(
                PREF_KEY_FIXED_OFFSET_CATEGORY), false);

        // Start loading TimeZoneData
        getLoaderManager().initLoader(0, null, new TimeZoneDataLoader.LoaderCreator(
                getContext(), this::onTimeZoneDataReady));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_REGION_PICKER:
            case REQUEST_CODE_ZONE_PICKER: {
                if (mTimeZoneData == null) {
                    mPendingZonePickerRequestResult = data;
                } else {
                    onZonePickerRequestResult(mTimeZoneData, data);
                }
                break;
            }
            case REQUEST_CODE_FIXED_OFFSET_ZONE_PICKER: {
                String tzId = data.getStringExtra(FixedOffsetPicker.EXTRA_RESULT_TIME_ZONE_ID);
                // Ignore the result if user didn't change the time zone.
                if (tzId != null && !tzId.equals(mSelectedTimeZoneId)) {
                    onFixedOffsetZoneChanged(tzId);
                }
                break;
            }
        }
    }

    @VisibleForTesting
    void setTimeZoneData(TimeZoneData timeZoneData) {
        mTimeZoneData = timeZoneData;
    }

    private void onTimeZoneDataReady(TimeZoneData timeZoneData) {
        if (mTimeZoneData == null && timeZoneData != null) {
            mTimeZoneData = timeZoneData;
            setupForCurrentTimeZone();
            getActivity().invalidateOptionsMenu();
            if (mPendingZonePickerRequestResult != null) {
                onZonePickerRequestResult(timeZoneData, mPendingZonePickerRequestResult);
                mPendingZonePickerRequestResult = null;
            }
        }
    }

    private void startRegionPicker() {
        startPickerFragment(RegionSearchPicker.class, new Bundle(), REQUEST_CODE_REGION_PICKER);
    }

    private void onRegionZonePreferenceClicked() {
        final Bundle args = new Bundle();
        args.putString(RegionZonePicker.EXTRA_REGION_ID,
                use(RegionPreferenceController.class).getRegionId());
        startPickerFragment(RegionZonePicker.class, args, REQUEST_CODE_ZONE_PICKER);
    }

    private void startFixedOffsetPicker() {
        startPickerFragment(FixedOffsetPicker.class, new Bundle(),
                REQUEST_CODE_FIXED_OFFSET_ZONE_PICKER);
    }

    private void startPickerFragment(Class<? extends BaseTimeZonePicker> fragmentClass, Bundle args,
            int resultRequestCode) {
        new SubSettingLauncher(getContext())
                .setDestination(fragmentClass.getCanonicalName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory())
                .setResultListener(this, resultRequestCode)
                .launch();
    }

    private void setDisplayedRegion(String regionId) {
        use(RegionPreferenceController.class).setRegionId(regionId);
        updatePreferenceStates();
    }

    private void setDisplayedTimeZoneInfo(String regionId, String tzId) {
        final TimeZoneInfo tzInfo = tzId == null ? null : mTimeZoneInfoFormatter.format(tzId);
        final FilteredCountryTimeZones countryTimeZones =
                mTimeZoneData.lookupCountryTimeZones(regionId);

        use(RegionZonePreferenceController.class).setTimeZoneInfo(tzInfo);
        // Only clickable when the region has more than 1 time zones or no time zone is selected.

        use(RegionZonePreferenceController.class).setClickable(tzInfo == null ||
                (countryTimeZones != null && countryTimeZones.getTimeZoneIds().size() > 1));
        use(TimeZoneInfoPreferenceController.class).setTimeZoneInfo(tzInfo);

        updatePreferenceStates();
    }

    private void setDisplayedFixedOffsetTimeZoneInfo(String tzId) {
        if (isFixedOffset(tzId)) {
            use(FixedOffsetPreferenceController.class).setTimeZoneInfo(
                    mTimeZoneInfoFormatter.format(tzId));
        } else {
            use(FixedOffsetPreferenceController.class).setTimeZoneInfo(null);
        }
        updatePreferenceStates();
    }

    private void onZonePickerRequestResult(TimeZoneData timeZoneData, Intent data) {
        String regionId = data.getStringExtra(RegionSearchPicker.EXTRA_RESULT_REGION_ID);
        String tzId = data.getStringExtra(RegionZonePicker.EXTRA_RESULT_TIME_ZONE_ID);
        // Ignore the result if user didn't change the region or time zone.
        if (Objects.equals(regionId, use(RegionPreferenceController.class).getRegionId())
                && Objects.equals(tzId, mSelectedTimeZoneId)) {
            return;
        }

        FilteredCountryTimeZones countryTimeZones =
                timeZoneData.lookupCountryTimeZones(regionId);
        if (countryTimeZones == null || !countryTimeZones.getTimeZoneIds().contains(tzId)) {
            Log.e(TAG, "Unknown time zone id is selected: " + tzId);
            return;
        }

        mSelectedTimeZoneId = tzId;
        setDisplayedRegion(regionId);
        setDisplayedTimeZoneInfo(regionId, mSelectedTimeZoneId);
        saveTimeZone(regionId, mSelectedTimeZoneId);

        // Switch to the region mode if the user switching from the fixed offset
        setSelectByRegion(true);
    }

    private void onFixedOffsetZoneChanged(String tzId) {
        mSelectedTimeZoneId = tzId;
        setDisplayedFixedOffsetTimeZoneInfo(tzId);
        saveTimeZone(null, mSelectedTimeZoneId);

        // Switch to the fixed offset mode if the user switching from the region mode
        setSelectByRegion(false);
    }

    private void saveTimeZone(String regionId, String tzId) {
        SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
        if (regionId == null) {
            editor.remove(PREF_KEY_REGION);
        } else {
            editor.putString(PREF_KEY_REGION, regionId);
        }
        editor.apply();
        ManualTimeZoneSuggestion manualTimeZoneSuggestion =
                TimeZoneDetector.createManualTimeZoneSuggestion(tzId, "Settings: Set time zone");
        TimeZoneDetector timeZoneDetector = getActivity().getSystemService(TimeZoneDetector.class);
        timeZoneDetector.suggestManualTimeZone(manualTimeZoneSuggestion);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_BY_REGION, 0, R.string.zone_menu_by_region);
        menu.add(0, MENU_BY_OFFSET, 0, R.string.zone_menu_by_offset);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Do not show menu when data is not ready,
        menu.findItem(MENU_BY_REGION).setVisible(mTimeZoneData != null && !mSelectByRegion);
        menu.findItem(MENU_BY_OFFSET).setVisible(mTimeZoneData != null && mSelectByRegion);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_BY_REGION:
                startRegionPicker();
                return true;

            case MENU_BY_OFFSET:
                startFixedOffsetPicker();
                return true;

            default:
                return false;
        }
    }

    private void setupForCurrentTimeZone() {
        mSelectedTimeZoneId = TimeZone.getDefault().getID();
        setSelectByRegion(!isFixedOffset(mSelectedTimeZoneId));
    }

    private static boolean isFixedOffset(String tzId) {
        return tzId.startsWith("Etc/GMT") || tzId.equals("Etc/UTC");
    }

    /**
     * Switch the current view to select region or select fixed offset time zone.
     * When showing the selected region, it guess the selected region from time zone id.
     * See {@link #findRegionIdForTzId} for more info.
     */
    private void setSelectByRegion(boolean selectByRegion) {
        mSelectByRegion = selectByRegion;
        setPreferenceCategoryVisible((PreferenceCategory) findPreference(
                PREF_KEY_REGION_CATEGORY), selectByRegion);
        setPreferenceCategoryVisible((PreferenceCategory) findPreference(
                PREF_KEY_FIXED_OFFSET_CATEGORY), !selectByRegion);
        final String localeRegionId = getLocaleRegionId();
        final Set<String> allCountryIsoCodes = mTimeZoneData.getRegionIds();

        String displayRegion = allCountryIsoCodes.contains(localeRegionId) ? localeRegionId : null;
        setDisplayedRegion(displayRegion);
        setDisplayedTimeZoneInfo(displayRegion, null);

        if (!mSelectByRegion) {
            setDisplayedFixedOffsetTimeZoneInfo(mSelectedTimeZoneId);
            return;
        }

        String regionId = findRegionIdForTzId(mSelectedTimeZoneId);
        if (regionId != null) {
            setDisplayedRegion(regionId);
            setDisplayedTimeZoneInfo(regionId, mSelectedTimeZoneId);
        }
    }

    /**
     * Find the a region associated with the specified time zone, based on the time zone data.
     * If there are multiple regions associated with the given time zone, the priority will be given
     * to the region the user last picked and the country in user's locale.
     *
     * @return null if no region associated with the time zone
     */
    private String findRegionIdForTzId(String tzId) {
        return findRegionIdForTzId(tzId,
                getPreferenceManager().getSharedPreferences().getString(PREF_KEY_REGION, null),
                getLocaleRegionId());
    }

    @VisibleForTesting
    String findRegionIdForTzId(String tzId, String sharePrefRegionId, String localeRegionId) {
        final Set<String> matchedRegions = mTimeZoneData.lookupCountryCodesForZoneId(tzId);
        if (matchedRegions.size() == 0) {
            return null;
        }
        if (sharePrefRegionId != null && matchedRegions.contains(sharePrefRegionId)) {
            return sharePrefRegionId;
        }
        if (localeRegionId != null && matchedRegions.contains(localeRegionId)) {
            return localeRegionId;
        }

        return matchedRegions.toArray(new String[matchedRegions.size()])[0];
    }

    private void setPreferenceCategoryVisible(PreferenceCategory category,
            boolean isVisible) {
        // Hiding category doesn't hide all the children preference. Set visibility of its children.
        // Do not care grandchildren as time_zone_pref.xml has only 2 levels.
        category.setVisible(isVisible);
        for (int i = 0; i < category.getPreferenceCount(); i++) {
            category.getPreference(i).setVisible(isVisible);
        }
    }

    private String getLocaleRegionId() {
        return mLocale.getCountry().toUpperCase(Locale.US);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.time_zone_prefs) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    // We can't enter this page if the auto time zone is enabled.
                    final int autoTimeZone = Settings.Global.getInt(context.getContentResolver(),
                            Settings.Global.AUTO_TIME_ZONE, 1);
                    return autoTimeZone == 1 ? false : true;
                }
            };
}
