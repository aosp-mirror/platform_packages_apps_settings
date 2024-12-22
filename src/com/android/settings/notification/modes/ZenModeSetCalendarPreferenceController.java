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
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.CalendarContract;
import android.service.notification.ZenModeConfig;

import androidx.annotation.NonNull;
import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

class ZenModeSetCalendarPreferenceController extends AbstractZenModePreferenceController {
    @VisibleForTesting
    protected static final String KEY_CALENDAR = "calendar";
    @VisibleForTesting
    protected static final String KEY_REPLY = "reply";

    private DropDownPreference mCalendar;
    private DropDownPreference mReply;

    private ZenModeConfig.EventInfo mEvent;

    public ZenModeSetCalendarPreferenceController(Context context, String key,
            ZenModesBackend backend) {
        super(context, key, backend);
    }

    @Override
    public void updateState(Preference preference, @NonNull ZenMode zenMode) {
        PreferenceCategory cat = (PreferenceCategory) preference;

        // Refresh our understanding of local preferences
        mCalendar = cat.findPreference(KEY_CALENDAR);
        mReply = cat.findPreference(KEY_REPLY);

        if (mCalendar == null || mReply == null) {
            return;
        }

        mCalendar.setOnPreferenceChangeListener(mCalendarChangeListener);

        mReply.setEntries(new CharSequence[] {
                mContext.getString(R.string.zen_mode_event_rule_reply_any_except_no),
                mContext.getString(R.string.zen_mode_event_rule_reply_yes_or_maybe),
                mContext.getString(R.string.zen_mode_event_rule_reply_yes),
        });
        mReply.setEntryValues(new CharSequence[] {
                Integer.toString(ZenModeConfig.EventInfo.REPLY_ANY_EXCEPT_NO),
                Integer.toString(ZenModeConfig.EventInfo.REPLY_YES_OR_MAYBE),
                Integer.toString(ZenModeConfig.EventInfo.REPLY_YES),
        });
        mReply.setOnPreferenceChangeListener(mReplyChangeListener);

        // Parse the zen mode's condition to update our EventInfo object.
        mEvent = ZenModeConfig.tryParseEventConditionId(zenMode.getRule().getConditionId());
        if (mEvent != null) {
            reloadCalendar();
            updatePrefValues();
        }
    }

    private void reloadCalendar() {
        List<CalendarInfo> calendars = getCalendars(mContext);
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        entries.add(mContext.getString(R.string.zen_mode_event_rule_calendar_any));
        values.add(key(0, null, ""));
        final String eventCalendar = mEvent != null ? mEvent.calName : null;
        for (CalendarInfo calendar : calendars) {
            entries.add(calendar.name);
            values.add(key(calendar));
            if (eventCalendar != null && (mEvent.calendarId == null
                    && eventCalendar.equals(calendar.name))) {
                mEvent.calendarId = calendar.calendarId;
            }
        }

        CharSequence[] entriesArr = entries.toArray(new CharSequence[entries.size()]);
        CharSequence[] valuesArr = values.toArray(new CharSequence[values.size()]);
        if (!Arrays.equals(mCalendar.getEntries(), entriesArr)) {
            mCalendar.setEntries(entriesArr);
        }

        if (!Arrays.equals(mCalendar.getEntryValues(), valuesArr)) {
            mCalendar.setEntryValues(valuesArr);
        }
    }

    @VisibleForTesting
    protected Function<ZenMode, ZenMode> updateEventMode(ZenModeConfig.EventInfo event) {
        return (zenMode) -> {
            zenMode.setCustomModeConditionId(mContext, ZenModeConfig.toEventConditionId(event));
            return zenMode;
        };
    }

    Preference.OnPreferenceChangeListener mCalendarChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String calendarKey = (String) newValue;
                    if (calendarKey.equals(key(mEvent))) return false;
                    String[] key = calendarKey.split(":", 3);
                    mEvent.userId = Integer.parseInt(key[0]);
                    mEvent.calendarId = key[1].equals("") ? null : Long.parseLong(key[1]);
                    mEvent.calName = key[2].equals("") ? null : key[2];
                    saveMode(updateEventMode(mEvent));
                    return true;
                }
            };

    Preference.OnPreferenceChangeListener mReplyChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final int reply = Integer.parseInt((String) newValue);
                    if (reply == mEvent.reply) return false;
                    mEvent.reply = reply;
                    saveMode(updateEventMode(mEvent));
                    return true;
                }
            };

    private void updatePrefValues() {
        if (!Objects.equals(mCalendar.getValue(), key(mEvent))) {
            mCalendar.setValue(key(mEvent));
        }
        if (!Objects.equals(mReply.getValue(), Integer.toString(mEvent.reply))) {
            mReply.setValue(Integer.toString(mEvent.reply));
        }
    }

    private List<CalendarInfo> getCalendars(Context context) {
        final List<CalendarInfo> calendars = new ArrayList<>();
        for (UserHandle user : UserManager.get(context).getUserProfiles()) {
            final Context userContext = getContextForUser(context, user);
            if (userContext != null) {
                addCalendars(userContext, calendars);
            }
        }
        Collections.sort(calendars, CALENDAR_NAME);
        return calendars;
    }

    private static Context getContextForUser(Context context, UserHandle user) {
        try {
            return context.createPackageContextAsUser(context.getPackageName(), 0, user);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private void addCalendars(Context context, List<CalendarInfo> outCalendars) {
        final String[] projection =
                {CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME};
        final String selection = CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + " >= "
                + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                + " AND " + CalendarContract.Calendars.SYNC_EVENTS + " = 1";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI,
                    projection, selection, null, null);
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext()) {
                addCalendar(cursor.getLong(0), cursor.getString(1),
                        context.getUserId(), outCalendars);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    @VisibleForTesting
    protected static void addCalendar(long calendarId, String calName, int userId,
            List<CalendarInfo> outCalendars) {
        final CalendarInfo ci = new CalendarInfo();
        ci.calendarId = calendarId;
        ci.name = calName;
        ci.userId = userId;
        if (!outCalendars.contains(ci)) {
            outCalendars.add(ci);
        }
    }

    private static String key(CalendarInfo calendar) {
        return key(calendar.userId, calendar.calendarId, calendar.name);
    }

    private static String key(ZenModeConfig.EventInfo event) {
        return key(event.userId, event.calendarId, event.calName);
    }

    @VisibleForTesting
    protected static String key(int userId, Long calendarId, String displayName) {
        return ZenModeConfig.EventInfo.resolveUserId(userId) + ":"
                + (calendarId == null ? "" : calendarId)
                + ":" + (displayName == null ? "" : displayName);
    }

    @VisibleForTesting
    protected static final Comparator<CalendarInfo> CALENDAR_NAME = Comparator.comparing(
            lhs -> lhs.name);

    public static class CalendarInfo {
        public String name;
        public int userId;
        public Long calendarId;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CalendarInfo)) return false;
            if (o == this) return true;
            final CalendarInfo other = (CalendarInfo) o;
            return Objects.equals(other.name, name)
                    && Objects.equals(other.calendarId, calendarId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, calendarId);
        }
    }
}
