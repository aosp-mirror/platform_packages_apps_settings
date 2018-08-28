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
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.datetime.timezone.model.TimeZoneData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Render a list of {@class TimeZoneInfo} into the list view in {@class BaseTimeZonePicker}
 */
public abstract class BaseTimeZoneInfoPicker extends BaseTimeZonePicker {
    protected static final String TAG = "RegionZoneSearchPicker";
    protected ZoneAdapter mAdapter;

    protected BaseTimeZoneInfoPicker(int titleResId, int searchHintResId,
            boolean searchEnabled, boolean defaultExpandSearch) {
        super(titleResId, searchHintResId, searchEnabled, defaultExpandSearch);
    }

    @Override
    protected BaseTimeZoneAdapter createAdapter(TimeZoneData timeZoneData) {
        mAdapter = new ZoneAdapter(getContext(), getAllTimeZoneInfos(timeZoneData),
                this::onListItemClick, getLocale(), getHeaderText());
        return mAdapter;
    }

    /**
     * @return the text shown in the header, or null to show no header.
     */
    protected @Nullable CharSequence getHeaderText() {
        return null;
    }

    private void onListItemClick(TimeZoneInfoItem item) {
        final TimeZoneInfo timeZoneInfo = item.mTimeZoneInfo;
        getActivity().setResult(Activity.RESULT_OK, prepareResultData(timeZoneInfo));
        getActivity().finish();
    }

    protected Intent prepareResultData(TimeZoneInfo selectedTimeZoneInfo) {
        return new Intent().putExtra(EXTRA_RESULT_TIME_ZONE_ID, selectedTimeZoneInfo.getId());
    }

    public abstract List<TimeZoneInfo> getAllTimeZoneInfos(TimeZoneData timeZoneData);

    protected static class ZoneAdapter extends BaseTimeZoneAdapter<TimeZoneInfoItem> {

        public ZoneAdapter(Context context, List<TimeZoneInfo> timeZones,
                OnListItemClickListener<TimeZoneInfoItem> onListItemClickListener, Locale locale,
                CharSequence headerText) {
            super(createTimeZoneInfoItems(context, timeZones, locale),
                    onListItemClickListener, locale,  true /* showItemSummary */,
                    headerText /* headerText */);
        }

        private static List<TimeZoneInfoItem> createTimeZoneInfoItems(Context context,
                List<TimeZoneInfo> timeZones, Locale locale) {
            final DateFormat currentTimeFormat = new SimpleDateFormat(
                    android.text.format.DateFormat.getTimeFormatString(context), locale);
            final ArrayList<TimeZoneInfoItem> results = new ArrayList<>(timeZones.size());
            final Resources resources = context.getResources();
            long i = 0;
            for (TimeZoneInfo timeZone : timeZones) {
                results.add(new TimeZoneInfoItem(i++, timeZone, resources, currentTimeFormat));
            }
            return results;
        }
    }

    private static class TimeZoneInfoItem implements BaseTimeZoneAdapter.AdapterItem {
        private final long mItemId;
        private final TimeZoneInfo mTimeZoneInfo;
        private final Resources mResources;
        private final DateFormat mTimeFormat;
        private final String mTitle;
        private final String[] mSearchKeys;

        private TimeZoneInfoItem(long itemId, TimeZoneInfo timeZoneInfo, Resources resources,
                DateFormat timeFormat) {
            mItemId = itemId;
            mTimeZoneInfo = timeZoneInfo;
            mResources = resources;
            mTimeFormat = timeFormat;
            mTitle = createTitle(timeZoneInfo);
            mSearchKeys = new String[] { mTitle };
        }

        private static String createTitle(TimeZoneInfo timeZoneInfo) {
            String name = timeZoneInfo.getExemplarLocation();
            if (name == null) {
                name = timeZoneInfo.getGenericName();
            }
            if (name == null && timeZoneInfo.getTimeZone().inDaylightTime(new Date())) {
                name = timeZoneInfo.getDaylightName();
            }
            if (name == null) {
                name = timeZoneInfo.getStandardName();
            }
            if (name == null) {
                name = String.valueOf(timeZoneInfo.getGmtOffset());
            }
            return name;
        }

        @Override
        public CharSequence getTitle() {
            return mTitle;
        }

        @Override
        public CharSequence getSummary() {
            String name = mTimeZoneInfo.getGenericName();
            if (name == null) {
                if (mTimeZoneInfo.getTimeZone().inDaylightTime(new Date())) {
                    name = mTimeZoneInfo.getDaylightName();
                } else {
                    name = mTimeZoneInfo.getStandardName();
                }
            }

            // Ignore name / GMT offset if the title shows the same information
            if (name == null || name.equals(mTitle)) {
                CharSequence gmtOffset = mTimeZoneInfo.getGmtOffset();
                return gmtOffset == null || gmtOffset.toString().equals(mTitle) ? "" : gmtOffset;
            } else {
                return SpannableUtil.getResourcesText(mResources,
                        R.string.zone_info_offset_and_name, mTimeZoneInfo.getGmtOffset(), name);
            }
        }

        @Override
        public String getIconText() {
            return null;
        }

        @Override
        public String getCurrentTime() {
            return mTimeFormat.format(Calendar.getInstance(mTimeZoneInfo.getTimeZone()));
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
}
