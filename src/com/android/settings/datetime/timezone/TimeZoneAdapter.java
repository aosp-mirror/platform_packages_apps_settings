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

import android.content.Context;
import android.graphics.Typeface;
import android.icu.impl.OlsonTimeZone;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.icu.util.TimeZoneTransition;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.settings.R;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for showing {@link TimeZoneInfo} objects in a recycler view.
 */
class TimeZoneAdapter extends RecyclerView.Adapter {

    static final int VIEW_TYPE_NORMAL = 1;
    static final int VIEW_TYPE_SELECTED = 2;

    private final DateFormat mTimeFormat;
    private final DateFormat mDateFormat;
    private final View.OnClickListener mOnClickListener;
    private final Context mContext;
    private final String mCurrentTimeZone;

    private List<TimeZoneInfo> mTimeZoneInfos;

    TimeZoneAdapter(View.OnClickListener onClickListener, Context context) {
        mOnClickListener = onClickListener;
        mContext = context;
        // Use android.text.format.DateFormat to observe 24-hour settings and find the best pattern
        // using ICU with skeleton.
        mTimeFormat = new SimpleDateFormat(
                android.text.format.DateFormat.getTimeFormatString(context),
                Locale.getDefault());
        mDateFormat = DateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
        mDateFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
        mCurrentTimeZone = TimeZone.getDefault().getID();
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getItemId();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.time_zone_list_item, parent, false);
        view.setOnClickListener(mOnClickListener);
        final ViewHolder viewHolder = new ViewHolder(view);
        if (viewType == VIEW_TYPE_SELECTED) {
            viewHolder.mNameView.setTypeface(
                    viewHolder.mNameView.getTypeface(), Typeface.BOLD);
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final TimeZoneInfo item = getItem(position);
        final ViewHolder tzHolder = (ViewHolder) holder;
        tzHolder.mNameView.setText(formatName(item));
        tzHolder.mDetailsView.setText(formatDetails(item));
        tzHolder.mTimeView.setText(formatTime(item));
        String dstText = formatDstText(item);
        tzHolder.mDstView.setText(dstText);
        // Hide DST TextView when it has no content.
        tzHolder.mDstView.setVisibility(dstText != null ? View.VISIBLE : View.GONE);

    }

    @Override
    public int getItemCount() {
        return getTimeZones().size();
    }

    @Override
    public int getItemViewType(int position) {
        final TimeZoneInfo tz = getItem(position);
        if (tz.getId().equals(mCurrentTimeZone)) {
            return VIEW_TYPE_SELECTED;
        } else {
            return VIEW_TYPE_NORMAL;
        }
    }

    public TimeZoneInfo getItem(int position) {
        return getTimeZones().get(position);
    }

    private CharSequence formatName(TimeZoneInfo item) {
        CharSequence name = item.getExemplarLocation();
        if (name == null) {
            name = item.getGenericName();
        }
        if (name == null && item.getTimeZone().inDaylightTime(new Date())) {
            name = item.getDaylightName();
        }
        if (name == null) {
            name = item.getStandardName();
        }
        if (name == null) {
            name = item.getGmtOffset();
        }
        return name;
    }

    private CharSequence formatDetails(TimeZoneInfo item) {
        String name = item.getGenericName();
        if (name == null) {
            if (item.getTimeZone().inDaylightTime(new Date())) {
                name = item.getDaylightName();
            } else {
                name = item.getStandardName();
            }
        }
        if (name == null) {
            return item.getGmtOffset();
        } else {
            return TextUtils.concat(item.getGmtOffset(), " ", name);
        }
    }

    private String formatDstText(TimeZoneInfo item) {
        final TimeZone timeZone = item.getTimeZone();
        if (!timeZone.observesDaylightTime()) {
            return null;
        }

        final TimeZoneTransition nextDstTransition = findNextDstTransition(timeZone);
        if (nextDstTransition == null) {
            return null;
        }
        final boolean toDst = nextDstTransition.getTo().getDSTSavings() != 0;
        String timeType = toDst ? item.getDaylightName() : item.getStandardName();
        if (timeType == null) {
            // Fall back to generic "summer time" and "standard time" if the time zone has no
            // specific names.
            timeType = toDst ?
                    mContext.getString(R.string.zone_time_type_dst) :
                    mContext.getString(R.string.zone_time_type_standard);

        }
        final Calendar transitionTime = Calendar.getInstance(timeZone);
        transitionTime.setTimeInMillis(nextDstTransition.getTime());
        final String date = mDateFormat.format(transitionTime);
        return mContext.getString(R.string.zone_change_to_from_dst, timeType, date);
    }

    private TimeZoneTransition findNextDstTransition(TimeZone timeZone) {
        if (!(timeZone instanceof OlsonTimeZone)) {
            return null;
        }
        final OlsonTimeZone olsonTimeZone = (OlsonTimeZone) timeZone;
        TimeZoneTransition transition = olsonTimeZone.getNextTransition(
                System.currentTimeMillis(), /* inclusive */ false);
        do {
            if (transition.getTo().getDSTSavings() != transition.getFrom().getDSTSavings()) {
                break;
            }
            transition = olsonTimeZone.getNextTransition(
                    transition.getTime(), /*inclusive */ false);
        } while (transition != null);
        return transition;
    }

    private String formatTime(TimeZoneInfo item) {
        return mTimeFormat.format(Calendar.getInstance(item.getTimeZone()));
    }

    private List<TimeZoneInfo> getTimeZones() {
        if (mTimeZoneInfos == null) {
            return Collections.emptyList();
        }
        return mTimeZoneInfos;
    }

    void setTimeZoneInfos(List<TimeZoneInfo> timeZoneInfos) {
        mTimeZoneInfos = timeZoneInfos;
        notifyDataSetChanged();
    }
}
