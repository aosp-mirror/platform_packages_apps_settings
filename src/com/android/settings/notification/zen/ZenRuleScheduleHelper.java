/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.content.Context;
import android.service.notification.ZenModeConfig.ScheduleInfo;
import android.text.format.DateFormat;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Helper class for shared functionality regarding descriptions of custom zen rule schedules.
 */
public class ZenRuleScheduleHelper {
    // per-instance to ensure we're always using the current locale
    private SimpleDateFormat mDayFormat;

    // Default constructor, which will use the current locale.
    public ZenRuleScheduleHelper() {
        mDayFormat = new SimpleDateFormat("EEE");
    }

    // Constructor for tests to provide an explicit locale
    @VisibleForTesting
    public ZenRuleScheduleHelper(Locale locale) {
        mDayFormat = new SimpleDateFormat("EEE", locale);
    }

    /**
     * Returns an ordered, comma-separated list of the days that a schedule applies, or null if no
     * days.
     */
    public String getDaysDescription(Context context, ScheduleInfo schedule) {
        // Compute an ordered, delimited list of day names based on the persisted user config.
        final int[] days = schedule.days;
        if (days != null && days.length > 0) {
            final StringBuilder sb = new StringBuilder();
            final Calendar c = Calendar.getInstance();
            int[] daysOfWeek = ZenModeScheduleDaysSelection.getDaysOfWeekForLocale(c);
            for (int i = 0; i < daysOfWeek.length; i++) {
                final int day = daysOfWeek[i];
                for (int j = 0; j < days.length; j++) {
                    if (day == days[j]) {
                        c.set(Calendar.DAY_OF_WEEK, day);
                        if (sb.length() > 0) {
                            sb.append(context.getString(R.string.summary_divider_text));
                        }
                        sb.append(mDayFormat.format(c.getTime()));
                        break;
                    }
                }
            }

            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * Returns an ordered summarized list of the days on which this schedule applies, with
     * adjacent days grouped together ("Sun-Wed" instead of "Sun,Mon,Tue,Wed").
     */
    public String getShortDaysSummary(Context context, ScheduleInfo schedule) {
        // Compute a list of days with contiguous days grouped together, for example: "Sun-Thu" or
        // "Sun-Mon,Wed,Fri"
        final int[] days = schedule.days;
        if (days != null && days.length > 0) {
            final StringBuilder sb = new StringBuilder();
            final Calendar cStart = Calendar.getInstance();
            final Calendar cEnd = Calendar.getInstance();
            int[] daysOfWeek = ZenModeScheduleDaysSelection.getDaysOfWeekForLocale(cStart);
            // the i for loop goes through days in order as determined by locale. as we walk through
            // the days of the week, keep track of "start" and "last seen"  as indicators for
            // what's contiguous, and initialize them to something not near actual indices
            int startDay = Integer.MIN_VALUE;
            int lastSeenDay = Integer.MIN_VALUE;
            for (int i = 0; i < daysOfWeek.length; i++) {
                final int day = daysOfWeek[i];

                // by default, output if this day is *not* included in the schedule, and thus
                // ends a previously existing block. if this day is included in the schedule
                // after all (as will be determined in the inner for loop), then output will be set
                // to false.
                boolean output = (i == lastSeenDay + 1);
                for (int j = 0; j < days.length; j++) {
                    if (day == days[j]) {
                        // match for this day in the schedule (indicated by counter i)
                        if (i == lastSeenDay + 1) {
                            // contiguous to the block we're walking through right now, record it
                            // (specifically, i, the day index) and move on to the next day
                            lastSeenDay = i;
                            output = false;
                        } else {
                            // it's a match, but not 1 past the last match, we are starting a new
                            // block
                            startDay = i;
                            lastSeenDay = i;
                        }

                        // if there is a match on the last day, also make sure to output at the end
                        // of this loop, and mark the day as the last day we'll have seen in the
                        // scheduled days.
                        if (i == daysOfWeek.length - 1) {
                            output = true;
                        }
                        break;
                    }
                }

                // output in either of 2 cases: this day is not a match, so has ended any previous
                // block, or this day *is* a match but is the last day of the week, so we need to
                // summarize
                if (output) {
                    // either describe just the single day if startDay == lastSeenDay, or
                    // output "startDay - lastSeenDay" as a group
                    if (sb.length() > 0) {
                        sb.append(context.getString(R.string.summary_divider_text));
                    }

                    if (startDay == lastSeenDay) {
                        // last group was only one day
                        cStart.set(Calendar.DAY_OF_WEEK, daysOfWeek[startDay]);
                        sb.append(mDayFormat.format(cStart.getTime()));
                    } else {
                        // last group was a contiguous group of days, so group them together
                        cStart.set(Calendar.DAY_OF_WEEK, daysOfWeek[startDay]);
                        cEnd.set(Calendar.DAY_OF_WEEK, daysOfWeek[lastSeenDay]);
                        sb.append(context.getString(R.string.summary_range_symbol_combination,
                                mDayFormat.format(cStart.getTime()),
                                mDayFormat.format(cEnd.getTime())));
                    }
                }
            }

            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * Convenience method for representing the specified time in string format.
     */
    private String timeString(Context context, int hour, int minute) {
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        return DateFormat.getTimeFormat(context).format(c.getTime());
    }

    /**
     * Combination description for a zen rule schedule including both day summary and time bounds.
     */
    public String getDaysAndTimeSummary(Context context, ScheduleInfo schedule) {
        final StringBuilder sb = new StringBuilder();
        String daysSummary = getShortDaysSummary(context, schedule);
        if (daysSummary == null) {
            // no use outputting times without dates
            return null;
        }
        sb.append(daysSummary);
        sb.append(context.getString(R.string.summary_divider_text));
        sb.append(context.getString(R.string.summary_range_symbol_combination,
                timeString(context, schedule.startHour, schedule.startMinute),
                timeString(context, schedule.endHour, schedule.endMinute)));

        return sb.toString();
    }
}
