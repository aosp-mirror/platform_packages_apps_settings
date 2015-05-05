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

import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceScreen;
import android.provider.CalendarContract.Calendars;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.service.notification.ZenModeConfig.EventInfo;
import android.service.notification.ZenModeConfig.ZenRule;

import com.android.internal.logging.MetricsLogger;
import com.android.settings.DropDownPreference;
import com.android.settings.R;

public class ZenModeEventRuleSettings extends ZenModeRuleSettingsBase {
    private static final String KEY_CALENDAR = "calendar";
    private static final String KEY_REPLY = "reply";

    public static final String ACTION = Settings.ACTION_ZEN_MODE_EVENT_RULE_SETTINGS;

    private DropDownPreference mCalendar;
    private DropDownPreference mReply;

    private EventInfo mEvent;
    private CalendarInfo[] mCalendars;

    @Override
    protected boolean setRule(ZenRule rule) {
        mEvent = rule != null ? ZenModeConfig.tryParseEventConditionId(rule.conditionId)
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
        reloadCalendar();
    }

    private void reloadCalendar() {
        mCalendars = getCalendars(mContext);
        mCalendar.clearItems();
        mCalendar.addItem(R.string.zen_mode_event_rule_calendar_any, 0L);
        for (int i = 0; i < mCalendars.length; i++) {
            mCalendar.addItem(mCalendars[i].name, mCalendars[i].id);
        }
    }

    @Override
    protected void onCreateInternal() {
        addPreferencesFromResource(R.xml.zen_mode_event_rule_settings);
        final PreferenceScreen root = getPreferenceScreen();

        mCalendar = (DropDownPreference) root.findPreference(KEY_CALENDAR);
        mCalendar.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final long calendar = (Long) value;
                if (calendar == mEvent.calendar) return true;
                mEvent.calendar = calendar;
                updateRule(ZenModeConfig.toEventConditionId(mEvent));
                return true;
            }
        });

        mReply = (DropDownPreference) root.findPreference(KEY_REPLY);
        mReply.addItem(R.string.zen_mode_event_rule_reply_any_except_no,
                EventInfo.REPLY_ANY_EXCEPT_NO);
        mReply.addItem(R.string.zen_mode_event_rule_reply_yes_or_maybe,
                EventInfo.REPLY_YES_OR_MAYBE);
        mReply.addItem(R.string.zen_mode_event_rule_reply_yes,
                EventInfo.REPLY_YES);
        mReply.setCallback(new DropDownPreference.Callback() {
            @Override
            public boolean onItemSelected(int pos, Object value) {
                final int reply = (Integer) value;
                if (reply == mEvent.reply) return true;
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
        mCalendar.setSelectedValue(mEvent.calendar);
        mReply.setSelectedValue(mEvent.reply);
    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.NOTIFICATION_ZEN_MODE_EVENT_RULE;
    }

    public static CalendarInfo[] getCalendars(Context context) {
        final String primary = "\"primary\"";
        final String[] projection = { Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME,
                "(" + Calendars.ACCOUNT_NAME + "=" + Calendars.OWNER_ACCOUNT + ") AS " + primary };
        final String selection = primary + " = 1";
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(Calendars.CONTENT_URI, projection,
                    selection, null, null);
            if (cursor == null) {
                return new CalendarInfo[0];
            }
            final CalendarInfo[] rt = new CalendarInfo[cursor.getCount()];
            int i = 0;
            while (cursor.moveToNext()) {
                final CalendarInfo ci = new CalendarInfo();
                ci.id = cursor.getLong(0);
                ci.name = cursor.getString(1);
                rt[i++] = ci;
            }
            return rt;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static class CalendarInfo {
        public long id;
        public String name;
    }

}
