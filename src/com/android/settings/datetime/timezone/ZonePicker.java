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
package com.android.settings.datetime.timezone;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.icu.util.TimeZone;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The class displaying a region list and a list of time zones for the selected region.
 * Choosing an item from the list will set the time zone. Pressing Back without choosing from the
 * list will not result in a change in the time zone setting.
 */
public class ZonePicker extends InstrumentedFragment
    implements AdapterView.OnItemSelectedListener, View.OnClickListener {

    private static final int MENU_BY_REGION = Menu.FIRST;
    private static final int MENU_BY_OFFSET = Menu.FIRST + 1;

    private Locale mLocale;
    private List<RegionInfo> mRegions;
    private Map<String, List<TimeZoneInfo>> mZoneInfos;
    private List<TimeZoneInfo> mFixedOffsetTimeZones;
    private TimeZoneAdapter mTimeZoneAdapter;
    private String mSelectedTimeZone;
    private boolean mSelectByRegion;
    private DataLoader mDataLoader;
    private RecyclerView mRecyclerView;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.ZONE_PICKER;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.time_zone_list, container, false);

        mLocale = getContext().getResources().getConfiguration().locale;
        mDataLoader = new DataLoader(mLocale);
        // TOOD: move this off the UI thread.
        mRegions = mDataLoader.loadRegionInfos();
        mZoneInfos = new HashMap<>();
        mSelectByRegion = true;
        mSelectedTimeZone = TimeZone.getDefault().getID();

        mTimeZoneAdapter = new TimeZoneAdapter(this, getContext());
        mRecyclerView = view.findViewById(R.id.tz_list);
        mRecyclerView.setAdapter(mTimeZoneAdapter);
        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, /* reverseLayout */ false));

        final ArrayAdapter<RegionInfo> regionAdapter = new ArrayAdapter<>(getContext(),
                R.layout.filter_spinner_item, mRegions);
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        final Spinner spinner = view.findViewById(R.id.tz_region_spinner);
        spinner.setAdapter(regionAdapter);
        spinner.setOnItemSelectedListener(this);
        setupForCurrentTimeZone(spinner);
        setHasOptionsMenu(true);
        return view;
    }

    private void setupForCurrentTimeZone(Spinner spinner) {
        final String localeRegionId = mLocale.getCountry().toUpperCase(Locale.ROOT);
        final String currentTimeZone = TimeZone.getDefault().getID();
        boolean fixedOffset = currentTimeZone.startsWith("Etc/GMT") ||
            currentTimeZone.equals("Etc/UTC");

        for (int regionIndex = 0; regionIndex < mRegions.size(); regionIndex++) {
            final RegionInfo region = mRegions.get(regionIndex);
            if (localeRegionId.equals(region.getId())) {
                spinner.setSelection(regionIndex);
            }
            if (!fixedOffset) {
                for (String timeZoneId: region.getTimeZoneIds()) {
                    if (TextUtils.equals(timeZoneId, mSelectedTimeZone)) {
                        spinner.setSelection(regionIndex);
                        return;
                    }
                }
            }
        }

        if (fixedOffset) {
            setSelectByRegion(false);
        }
    }

    @Override
    public void onClick(View view) {
        // Ignore extra clicks
        if (!isResumed()) {
            return;
        }
        final int position = mRecyclerView.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) {
            return;
        }
        final TimeZoneInfo timeZoneInfo = mTimeZoneAdapter.getItem(position);

        // Update the system timezone value
        final Activity activity = getActivity();
        final AlarmManager alarm = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
        alarm.setTimeZone(timeZoneInfo.getId());

        activity.onBackPressed();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_BY_REGION, 0, R.string.zone_menu_by_region);
        menu.add(0, MENU_BY_OFFSET, 0, R.string.zone_menu_by_offset);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (mSelectByRegion) {
            menu.findItem(MENU_BY_REGION).setVisible(false);
            menu.findItem(MENU_BY_OFFSET).setVisible(true);
        } else {
            menu.findItem(MENU_BY_REGION).setVisible(true);
            menu.findItem(MENU_BY_OFFSET).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case MENU_BY_REGION:
                setSelectByRegion(true);
                return true;

            case MENU_BY_OFFSET:
                setSelectByRegion(false);
                return true;

            default:
                return false;
        }
    }

    private void setSelectByRegion(boolean selectByRegion) {
        mSelectByRegion = selectByRegion;
        getView().findViewById(R.id.tz_region_spinner_layout).setVisibility(
            mSelectByRegion ? View.VISIBLE : View.GONE);
        List<TimeZoneInfo> tzInfos;
        if (selectByRegion) {
            Spinner regionSpinner = getView().findViewById(R.id.tz_region_spinner);
            int selectedRegion = regionSpinner.getSelectedItemPosition();
            if (selectedRegion == -1) {
                // Arbitrarily pick the first item if no region was selected above.
                selectedRegion = 0;
                regionSpinner.setSelection(selectedRegion);
            }
            tzInfos = getTimeZoneInfos(mRegions.get(selectedRegion));
        } else {
            if (mFixedOffsetTimeZones == null) {
                mFixedOffsetTimeZones = mDataLoader.loadFixedOffsets();
            }
            tzInfos = mFixedOffsetTimeZones;
        }
        mTimeZoneAdapter.setTimeZoneInfos(tzInfos);
    }

    private List<TimeZoneInfo> getTimeZoneInfos(RegionInfo regionInfo) {
        List<TimeZoneInfo> tzInfos = mZoneInfos.get(regionInfo.getId());
        if (tzInfos == null) {
            // TODO: move this off the UI thread.
            Collection<String> tzIds = regionInfo.getTimeZoneIds();
            tzInfos = mDataLoader.loadTimeZoneInfos(tzIds);
            mZoneInfos.put(regionInfo.getId(), tzInfos);
        }
        return tzInfos;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mTimeZoneAdapter.setTimeZoneInfos(getTimeZoneInfos(mRegions.get(position)));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        mTimeZoneAdapter.setTimeZoneInfos(null);
    }

}
