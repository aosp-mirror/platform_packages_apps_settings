/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import android.content.Context;
import android.service.notification.ZenModeConfig;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.LayoutPreference;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Calendar;
import java.util.function.Function;

/**
 * Preference controller for setting the start and end time and days of the week associated with
 * an automatic zen mode.
 */
class ZenModeSetSchedulePreferenceController extends AbstractZenModePreferenceController {
    // per-instance to ensure we're always using the current locale
    // E = day of the week; "EEEEE" is the shortest version; "EEEE" is the full name
    private final SimpleDateFormat mShortDayFormat = new SimpleDateFormat("EEEEE");
    private final SimpleDateFormat mLongDayFormat = new SimpleDateFormat("EEEE");

    private static final String TAG = "ZenModeSetSchedulePreferenceController";
    private Fragment mParent;
    private ZenModeConfig.ScheduleInfo mSchedule;

    ZenModeSetSchedulePreferenceController(Context context, Fragment parent, String key,
            ZenModesBackend backend) {
        super(context, key, backend);
        mParent = parent;
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        mSchedule = ZenModeConfig.tryParseScheduleConditionId(zenMode.getRule().getConditionId());
        LayoutPreference layoutPref = (LayoutPreference) preference;

        TextView start = layoutPref.findViewById(R.id.start_time);
        String startTimeString = timeString(mSchedule.startHour, mSchedule.startMinute);
        start.setText(startTimeString);
        start.setContentDescription(
                mContext.getString(R.string.zen_mode_start_time) + "\n" + startTimeString);
        start.setOnClickListener(
                timePickerLauncher(mSchedule.startHour, mSchedule.startMinute, mStartSetter));

        TextView end = layoutPref.findViewById(R.id.end_time);
        String endTimeString = timeString(mSchedule.endHour, mSchedule.endMinute);
        end.setText(endTimeString);
        end.setContentDescription(
                mContext.getString(R.string.zen_mode_end_time) + "\n" + endTimeString);
        end.setOnClickListener(
                timePickerLauncher(mSchedule.endHour, mSchedule.endMinute, mEndSetter));

        TextView durationView = layoutPref.findViewById(R.id.schedule_duration);
        durationView.setText(getScheduleDurationDescription(mSchedule));

        ViewGroup daysContainer = layoutPref.findViewById(R.id.days_of_week_container);
        setupDayToggles(daysContainer, mSchedule, Calendar.getInstance());
    }

    private String timeString(int hour, int minute) {
        final Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        return DateFormat.getTimeFormat(mContext).format(c.getTime());
    }

    private boolean isValidTime(int hour, int minute) {
        return ZenModeConfig.isValidHour(hour) && ZenModeConfig.isValidMinute(minute);
    }

    private String getScheduleDurationDescription(ZenModeConfig.ScheduleInfo schedule) {
        final int startMin = 60 * schedule.startHour + schedule.startMinute;
        final int endMin = 60 * schedule.endHour + schedule.endMinute;
        final boolean nextDay = startMin >= endMin;

        Duration scheduleDuration;
        if (nextDay) {
            // add one day's worth of minutes (24h x 60min) to end minute for end time calculation
            int endMinNextDay = endMin + (24 * 60);
            scheduleDuration = Duration.ofMinutes(endMinNextDay - startMin);
        } else {
            scheduleDuration = Duration.ofMinutes(endMin - startMin);
        }

        int hours = scheduleDuration.toHoursPart();
        int minutes = scheduleDuration.minusHours(hours).toMinutesPart();
        return mContext.getString(R.string.zen_mode_schedule_duration, hours, minutes);
    }

    @VisibleForTesting
    protected Function<ZenMode, ZenMode> updateScheduleMode(ZenModeConfig.ScheduleInfo schedule) {
        return (zenMode) -> {
            zenMode.setCustomModeConditionId(mContext,
                    ZenModeConfig.toScheduleConditionId(schedule));
            return zenMode;
        };
    }

    private final ZenModeTimePickerFragment.TimeSetter mStartSetter = (hour, minute) -> {
        if (!isValidTime(hour, minute)) {
            return;
        }
        if (hour == mSchedule.startHour && minute == mSchedule.startMinute) {
            return;
        }
        mSchedule.startHour = hour;
        mSchedule.startMinute = minute;
        saveMode(updateScheduleMode(mSchedule));
    };

    private final ZenModeTimePickerFragment.TimeSetter mEndSetter = (hour, minute) -> {
        if (!isValidTime(hour, minute)) {
            return;
        }
        if (hour == mSchedule.endHour && minute == mSchedule.endMinute) {
            return;
        }
        mSchedule.endHour = hour;
        mSchedule.endMinute = minute;
        saveMode(updateScheduleMode(mSchedule));
    };

    private View.OnClickListener timePickerLauncher(int hour, int minute,
            ZenModeTimePickerFragment.TimeSetter timeSetter) {
        return v -> {
            final ZenModeTimePickerFragment frag = new ZenModeTimePickerFragment(mContext, hour,
                    minute, timeSetter);
            frag.show(mParent.getParentFragmentManager(), TAG);
        };
    }

    protected static int[] getDaysOfWeekForLocale(Calendar c) {
        int[] daysOfWeek = new int[7];
        int currentDay = c.getFirstDayOfWeek();
        for (int i = 0; i < daysOfWeek.length; i++) {
            if (currentDay > 7) currentDay = 1;
            daysOfWeek[i] = currentDay;
            currentDay++;
        }
        return daysOfWeek;
    }

    @VisibleForTesting
    protected void setupDayToggles(ViewGroup dayContainer, ZenModeConfig.ScheduleInfo schedule,
            Calendar c) {
        int[] daysOfWeek = getDaysOfWeekForLocale(c);

        // Index in daysOfWeek is associated with the [idx]'th object in the list of days in the
        // layout. Note that because the order of the days of the week may differ per locale, this
        // is not necessarily the same as the actual value of the day number at that index.
        for (int i = 0; i < daysOfWeek.length; i++) {
            ToggleButton dayToggle = dayContainer.findViewById(resIdForDayIndex(i));
            if (dayToggle == null) {
                continue;
            }

            final int day = daysOfWeek[i];
            c.set(Calendar.DAY_OF_WEEK, day);

            // find current setting for this day
            boolean dayEnabled = false;
            if (schedule.days != null) {
                for (int idx = 0; idx < schedule.days.length; idx++) {
                    if (schedule.days[idx] == day) {
                        dayEnabled = true;
                        break;
                    }
                }
            }

            // On/off is indicated by visuals, and both states share the shortest (one-character)
            // day label.
            dayToggle.setTextOn(mShortDayFormat.format(c.getTime()));
            dayToggle.setTextOff(mShortDayFormat.format(c.getTime()));
            String state = dayEnabled
                    ? mContext.getString(com.android.internal.R.string.capital_on)
                    : mContext.getString(com.android.internal.R.string.capital_off);
            dayToggle.setStateDescription(mLongDayFormat.format(c.getTime()) + ", " + state);

            dayToggle.setChecked(dayEnabled);
            dayToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (updateScheduleDays(schedule, day, isChecked)) {
                    saveMode(updateScheduleMode(schedule));
                }
            });

            // If display and text settings cause the text to be larger than its containing box,
            // don't show scrollbars.
            dayToggle.setVerticalScrollBarEnabled(false);
            dayToggle.setHorizontalScrollBarEnabled(false);
        }
    }

    // Updates the set of enabled days in provided schedule to either turn on or off the given day.
    // The format of days in ZenModeConfig.ScheduleInfo is an array of days, where inclusion means
    // the schedule is set to run on that day. Returns whether anything was changed.
    @VisibleForTesting
    protected static boolean updateScheduleDays(ZenModeConfig.ScheduleInfo schedule, int day,
            boolean set) {
        // Build a set representing the days that are currently set in mSchedule.
        ArraySet<Integer> daySet = new ArraySet();
        if (schedule.days != null) {
            for (int i = 0; i < schedule.days.length; i++) {
                daySet.add(schedule.days[i]);
            }
        }

        if (daySet.contains(day) != set) {
            if (set) {
                daySet.add(day);
            } else {
                daySet.remove(day);
            }

            // rebuild days array for mSchedule
            final int[] out = new int[daySet.size()];
            for (int i = 0; i < daySet.size(); i++) {
                out[i] = daySet.valueAt(i);
            }
            Arrays.sort(out);
            schedule.days = out;
            return true;
        }
        // If the setting is the same as it was before, no need to update anything.
        return false;
    }

    protected static int resIdForDayIndex(int idx) {
        switch (idx) {
            case 0:
                return R.id.day0;
            case 1:
                return R.id.day1;
            case 2:
                return R.id.day2;
            case 3:
                return R.id.day3;
            case 4:
                return R.id.day4;
            case 5:
                return R.id.day5;
            case 6:
                return R.id.day6;
            default:
                return 0;  // unknown
        }
    }
}
