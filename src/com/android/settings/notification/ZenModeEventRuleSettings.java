/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.notification;

import android.app.AutomaticZenRule;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.CalendarContract.Calendars;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.EventInfo;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ZenModeEventRuleSettings extends ZenModeRuleSettingsBase {
    private static final String KEY_CALENDAR = "calendar";
    private static final String KEY_REPLY = "reply";

    public static final String ACTION = Settings.ACTION_ZEN_MODE_EVENT_RULE_SETTINGS;

    private DropDownPreference mCalendar;
    private DropDownPreference mReply;

    private EventInfo mEvent;
    private List<CalendarInfo> mCalendars;
    private boolean mCreate;

    @Override
    protected boolean setRule(AutomaticZenRule rule) {
        mEvent = rule != null ? ZenModeConfig.tryParseEventConditionId(rule.getConditionId())
                : null;
        return mEvent != null;
    }

    @Override
    protected String getZenModeDependency() {
        return null;
    }

    @Override
    protected int getEnabledToastText() {
        return R.string.zen_event_rule_enabled_toast;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isUiRestricted()) {
            return;
        }
        if (!mCreate) {
            reloadCalendar();
        }
        mCreate = false;
    }

    private void reloadCalendar() {
        mCalendars = getCalendars(mContext);
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        entries.add(getString(R.string.zen_mode_event_rule_calendar_any));
        values.add(key(0, null));
        final String eventCalendar = mEvent != null ? mEvent.calendar : null;
        boolean found = false;
        for (CalendarInfo calendar : mCalendars) {
            entries.add(calendar.name);
            values.add(key(calendar));
            if (eventCalendar != null && eventCalendar.equals(calendar.name)) {
                found = true;
            }
        }
        if (eventCalendar != null && !found) {
            entries.add(eventCalendar);
            values.add(key(mEvent.userId, eventCalendar));
        }
        mCalendar.setEntries(entries.toArray(new CharSequence[entries.size()]));
        mCalendar.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }

    @Override
    protected void onCreateInternal() {
        mCreate = true;
        addPreferencesFromResource(R.xml.zen_mode_event_rule_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mCalendar = (DropDownPreference) root.findPreference(KEY_CALENDAR);
        mCalendar.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String calendarKey = (String) newValue;
                if (calendarKey.equals(key(mEvent))) return false;
                final int i = calendarKey.indexOf(':');
                mEvent.userId = Integer.parseInt(calendarKey.substring(0, i));
                mEvent.calendar = calendarKey.substring(i + 1);
                if (mEvent.calendar.isEmpty()) {
                    mEvent.calendar = null;
                }
                updateRule(ZenModeConfig.toEventConditionId(mEvent));
                return true;
            }
        });

        mReply = (DropDownPreference) root.findPreference(KEY_REPLY);
        mReply.setEntries(new CharSequence[] {
                getString(R.string.zen_mode_event_rule_reply_any_except_no),
                getString(R.string.zen_mode_event_rule_reply_yes_or_maybe),
                getString(R.string.zen_mode_event_rule_reply_yes),
        });
        mReply.setEntryValues(new CharSequence[] {
                Integer.toString(EventInfo.REPLY_ANY_EXCEPT_NO),
                Integer.toString(EventInfo.REPLY_YES_OR_MAYBE),
                Integer.toString(EventInfo.REPLY_YES),
        });
        mReply.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final int reply = Integer.parseInt((String) newValue);
                if (reply == mEvent.reply) return false;
                mEvent.reply = reply;
                updateRule(ZenModeConfig.toEventConditionId(mEvent));
                return true;
            }
        });

        reloadCalendar();
        updateControlsInternal();
    }

    @Override
    protected void updateControlsInternal() {
        mCalendar.setValue(key(mEvent));
        mReply.setValue(Integer.toString(mEvent.reply));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NOTIFICATION_ZEN_MODE_EVENT_RULE;
    }

    public static CalendarInfo findCalendar(Context context, EventInfo event) {
        if (context == null || event == null) return null;
        final String eventKey = key(event);
        for (CalendarInfo calendar : getCalendars(context)) {
            if (eventKey.equals(key(calendar))) {
                return calendar;
            }
        }
        return null;
    }

    private static List<CalendarInfo> getCalendars(Context context) {
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
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    public static void addCalendars(Context context, List<CalendarInfo> outCalendars) {
        final String primary = "\"primary\"";
        final String[] projection = { Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME,
                "(" + Calendars.ACCOUNT_NAME + "=" + Calendars.OWNER_ACCOUNT + ") AS " + primary };
        final String selection = primary + " = 1";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Calendars.CONTENT_URI, projection,
                    selection, null, null);
            if (cursor == null) {
                return;
            }
            while (cursor.moveToNext()) {
                final CalendarInfo ci = new CalendarInfo();
                ci.name = cursor.getString(1);
                ci.userId = context.getUserId();
                outCalendars.add(ci);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static String key(CalendarInfo calendar) {
        return key(calendar.userId, calendar.name);
    }

    private static String key(EventInfo event) {
        return key(event.userId, event.calendar);
    }

    private static String key(int userId, String calendar) {
        return EventInfo.resolveUserId(userId) + ":" + (calendar == null ? "" : calendar);
    }

    private static final Comparator<CalendarInfo> CALENDAR_NAME = new Comparator<CalendarInfo>() {
        @Override
        public int compare(CalendarInfo lhs, CalendarInfo rhs) {
            return lhs.name.compareTo(rhs.name);
        }
    };

    public static class CalendarInfo {
        public String name;
        public int userId;
    }

}
