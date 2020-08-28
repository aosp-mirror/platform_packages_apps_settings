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

package com.android.settings.notification.zen;

import android.app.AutomaticZenRule;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.CalendarContract.Calendars;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.EventInfo;

import androidx.preference.DropDownPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ZenModeEventRuleSettings extends ZenModeRuleSettingsBase {
    private static final String KEY_CALENDAR = "calendar";
    private static final String KEY_REPLY = "reply";

    public static final String ACTION = Settings.ACTION_ZEN_MODE_EVENT_RULE_SETTINGS;

    private DropDownPreference mCalendar;
    private DropDownPreference mReply;

    private EventInfo mEvent;

    private boolean mCreate;

    @Override
    protected boolean setRule(AutomaticZenRule rule) {
        mEvent = rule != null ? ZenModeConfig.tryParseEventConditionId(rule.getConditionId())
                : null;
        return mEvent != null;
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

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.zen_mode_event_rule_settings;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        mHeader = new ZenAutomaticRuleHeaderPreferenceController(context, this,
                getSettingsLifecycle());
        mActionButtons = new ZenRuleButtonsPreferenceController(context, this,
                getSettingsLifecycle());
        mSwitch = new ZenAutomaticRuleSwitchPreferenceController(context, this,
                getSettingsLifecycle());
        controllers.add(mHeader);
        controllers.add(mActionButtons);
        controllers.add(mSwitch);
        return controllers;
    }

    private void reloadCalendar() {
        List<CalendarInfo> calendars = getCalendars(mContext);
        ArrayList<CharSequence> entries = new ArrayList<>();
        ArrayList<CharSequence> values = new ArrayList<>();
        entries.add(getString(R.string.zen_mode_event_rule_calendar_any));
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
        if (!Objects.equals(mCalendar.getEntries(), entriesArr)) {
            mCalendar.setEntries(entriesArr);
        }

        if (!Objects.equals(mCalendar.getEntryValues(), valuesArr)) {
            mCalendar.setEntryValues(valuesArr);
        }
    }

    @Override
    protected void onCreateInternal() {
        mCreate = true;
        final PreferenceScreen root = getPreferenceScreen();

        mCalendar = (DropDownPreference) root.findPreference(KEY_CALENDAR);
        mCalendar.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String calendarKey = (String) newValue;
                if (calendarKey.equals(key(mEvent))) return false;
                String[] key = calendarKey.split(":", 3);
                mEvent.userId = Integer.parseInt(key[0]);
                mEvent.calendarId = key[1].equals("") ? null : Long.parseLong(key[1]);
                mEvent.calName = key[2].equals("") ? null : key[2];
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
        if (!Objects.equals(mCalendar.getValue(), key(mEvent))) {
            mCalendar.setValue(key(mEvent));
        }
        if (!Objects.equals(mReply.getValue(), Integer.toString(mEvent.reply))) {
            mReply.setValue(Integer.toString(mEvent.reply));
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.NOTIFICATION_ZEN_MODE_EVENT_RULE;
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
        } catch (NameNotFoundException e) {
            return null;
        }
    }

    private void addCalendars(Context context, List<CalendarInfo> outCalendars) {
        final String[] projection = { Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME };
        final String selection = Calendars.CALENDAR_ACCESS_LEVEL + " >= "
                + Calendars.CAL_ACCESS_CONTRIBUTOR
                + " AND " + Calendars.SYNC_EVENTS + " = 1";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Calendars.CONTENT_URI, projection,
                    selection, null, null);
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
    void addCalendar(long calendarId, String calName, int userId, List<CalendarInfo>
            outCalendars) {
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

    private static String key(EventInfo event) {
        return key(event.userId, event.calendarId, event.calName);
    }

    private static String key(int userId, Long calendarId, String displayName) {
        return EventInfo.resolveUserId(userId) + ":" + (calendarId == null ? "" : calendarId)
                + ":" + (displayName == null ? "" : displayName);
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
            return Objects.hash(name,  calendarId);
        }
    }
}
