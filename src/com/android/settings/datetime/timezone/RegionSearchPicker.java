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
import android.content.Intent;
import android.graphics.Paint;
import android.icu.text.Collator;
import android.icu.text.LocaleDisplayNames;
import android.os.Bundle;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.datetime.timezone.model.FilteredCountryTimeZones;
import com.android.settings.datetime.timezone.model.TimeZoneData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Render a list of regions into a list view.
 */
public class RegionSearchPicker extends BaseTimeZonePicker {
    private static final int REQUEST_CODE_ZONE_PICKER = 1;
    private static final String TAG = "RegionSearchPicker";

    private BaseTimeZoneAdapter<RegionItem> mAdapter;
    private TimeZoneData mTimeZoneData;

    public RegionSearchPicker() {
        super(R.string.date_time_select_region, R.string.search_settings, true, true);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_ZONE_PICKER_REGION;
    }

    @Override
    protected BaseTimeZoneAdapter createAdapter(TimeZoneData timeZoneData) {
        mTimeZoneData = timeZoneData;
        mAdapter = new BaseTimeZoneAdapter<>(createAdapterItem(timeZoneData.getRegionIds()),
                this::onListItemClick, getLocale(), false);
        return mAdapter;
    }

    private void onListItemClick(int position) {
        final String regionId = mAdapter.getItem(position).getId();
        final FilteredCountryTimeZones countryTimeZones = mTimeZoneData.lookupCountryTimeZones(
                regionId);
        final Activity activity = getActivity();
        if (countryTimeZones == null || countryTimeZones.getTimeZoneIds().isEmpty()) {
            Log.e(TAG, "Region has no time zones: " + regionId);
            activity.setResult(Activity.RESULT_CANCELED);
            activity.finish();
            return;
        }

        List<String> timeZoneIds = countryTimeZones.getTimeZoneIds();
        // Choose the time zone associated the region if there is only one time zone in that region
        if (timeZoneIds.size() == 1) {
            final Intent resultData = new Intent()
                    .putExtra(EXTRA_RESULT_REGION_ID, regionId)
                    .putExtra(EXTRA_RESULT_TIME_ZONE_ID, timeZoneIds.get(0));
            getActivity().setResult(Activity.RESULT_OK, resultData);
            getActivity().finish();
        } else {
            // Launch the zone picker and let the user choose a time zone from the list of
            // time zones associated with the region.
            final Bundle args = new Bundle();
            args.putString(RegionZonePicker.EXTRA_REGION_ID, regionId);
            new SubSettingLauncher(getContext())
                    .setDestination(RegionZonePicker.class.getCanonicalName())
                    .setArguments(args)
                    .setSourceMetricsCategory(getMetricsCategory())
                    .setResultListener(this, REQUEST_CODE_ZONE_PICKER)
                    .launch();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ZONE_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                getActivity().setResult(Activity.RESULT_OK, data);
            }
            getActivity().finish();
        }
    }

    private List<RegionItem> createAdapterItem(Set<String> regionIds) {
        final Collator collator = Collator.getInstance(getLocale());
        final TreeSet<RegionItem> items = new TreeSet<>(new RegionInfoComparator(collator));
        final Paint paint = new Paint();
        final LocaleDisplayNames localeDisplayNames = LocaleDisplayNames.getInstance(getLocale());
        long i = 0;
        for (String regionId : regionIds) {
            String name = localeDisplayNames.regionDisplayName(regionId);
            String regionalIndicator = createRegionalIndicator(regionId, paint);
            items.add(new RegionItem(i++, regionId, name, regionalIndicator));
        }
        return new ArrayList<>(items);
    }

    /**
     * Create a Unicode Region Indicator Symbol for a given region id (a.k.a flag emoji). If the
     * system can't render a flag for this region or the input is not a region id, this returns
     * {@code null}.
     *
     * @param id the two-character region id.
     * @param paint Paint contains the glyph
     * @return a String representing the flag of the region or {@code null}.
     */
    private static String createRegionalIndicator(String id, Paint paint) {
        if (id.length() != 2) {
            return null;
        }
        final char c1 = id.charAt(0);
        final char c2 = id.charAt(1);
        if ('A' > c1 || c1 > 'Z' || 'A' > c2 || c2 > 'Z') {
            return null;
        }
        // Regional Indicator A is U+1F1E6 which is 0xD83C 0xDDE6 in UTF-16.
        final String regionalIndicator = new String(
            new char[]{0xd83c, (char) (0xdde6 - 'A' + c1), 0xd83c, (char) (0xdde6 - 'A' + c2)});
        if (!paint.hasGlyph(regionalIndicator)) {
            return null;
        }
        return regionalIndicator;
    }

    private static class RegionItem implements BaseTimeZoneAdapter.AdapterItem {

        private final String mId;
        private final String mName;
        private final String mRegionalIndicator;
        private final long mItemId;
        private final String[] mSearchKeys;

        RegionItem(long itemId, String id, String name, String regionalIndicator) {
            mId = id;
            mName = name;
            mRegionalIndicator = regionalIndicator;
            mItemId = itemId;
            // Allow to search with ISO_3166-1 alpha-2 code. It's handy for english users in some
            // countries, e.g. US for United States. It's not best search keys for users, but
            // ICU doesn't have the data for the alias names of a region.
            mSearchKeys = new String[] {mId, mName};
        }

        public String getId() {
            return mId;
        }

        @Override
        public CharSequence getTitle() {
            return mName;
        }

        @Override
        public CharSequence getSummary() {
            return null;
        }

        @Override
        public String getIconText() {
            return mRegionalIndicator;
        }

        @Override
        public String getCurrentTime() {
            return null;
        }

        @Override
        public long getItemId() {
            return mItemId;
        }

        @Override
        public String[] getSearchKeys() {
            return mSearchKeys;
        }
    }

    private static class RegionInfoComparator implements Comparator<RegionItem> {
        private final Collator mCollator;

        RegionInfoComparator(Collator collator) {
            mCollator = collator;
        }

        @Override
        public int compare(RegionItem r1, RegionItem r2) {
            return mCollator.compare(r1.getTitle(), r2.getTitle());
        }
    }
}
