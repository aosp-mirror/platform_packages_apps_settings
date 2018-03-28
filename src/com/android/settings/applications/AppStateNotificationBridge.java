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
package com.android.settings.applications;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;
import com.android.settingslib.applications.ApplicationsState.AppFilter;
import com.android.settingslib.utils.StringUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

/**
 * Connects the info provided by ApplicationsState and UsageStatsManager.
 * Also provides app filters that can use the notification data.
 */
public class AppStateNotificationBridge extends AppStateBaseBridge {

    private final Context mContext;
    private UsageStatsManager mUsageStatsManager;
    private NotificationBackend mBackend;
    private static final int DAYS_TO_CHECK = 7;

    public AppStateNotificationBridge(Context context, ApplicationsState appState,
            Callback callback, UsageStatsManager usageStatsManager,
            NotificationBackend backend) {
        super(appState, callback);
        mContext = context;
        mUsageStatsManager = usageStatsManager;
        mBackend = backend;
    }

    @Override
    protected void loadAllExtraInfo() {
        ArrayList<AppEntry> apps = mAppSession.getAllApps();
        if (apps == null) return;

        final Map<String, NotificationsSentState> map = getAggregatedUsageEvents();
        for (AppEntry entry : apps) {
            NotificationsSentState stats = map.get(entry.info.packageName);
            calculateAvgSentCounts(stats);
            addBlockStatus(entry, stats);
            entry.extraInfo = stats;
        }
    }

    @Override
    protected void updateExtraInfo(AppEntry entry, String pkg, int uid) {
        Map<String, NotificationsSentState> map = getAggregatedUsageEvents();
        NotificationsSentState stats = map.get(entry.info.packageName);
        calculateAvgSentCounts(stats);
        addBlockStatus(entry, stats);
        entry.extraInfo = stats;
    }

    public static CharSequence getSummary(Context context, NotificationsSentState state,
            boolean sortByRecency) {
        if (sortByRecency) {
            if (state.lastSent == 0) {
                return context.getString(R.string.notifications_sent_never);
            }
            return StringUtil.formatRelativeTime(
                    context, System.currentTimeMillis() - state.lastSent, true);
        } else {
            if (state.avgSentWeekly > 0) {
                return context.getString(R.string.notifications_sent_weekly, state.avgSentWeekly);
            }
            return context.getString(R.string.notifications_sent_daily, state.avgSentDaily);
        }
    }

    private void addBlockStatus(AppEntry entry, NotificationsSentState stats) {
        if (stats != null) {
            stats.blocked = mBackend.getNotificationsBanned(entry.info.packageName, entry.info.uid);
            stats.systemApp = mBackend.isSystemApp(mContext, entry.info);
            stats.blockable = !stats.systemApp || (stats.systemApp && stats.blocked);
        }
    }

    private void calculateAvgSentCounts(NotificationsSentState stats) {
        if (stats != null) {
            stats.avgSentDaily = Math.round((float) stats.sentCount / DAYS_TO_CHECK);
            if (stats.sentCount < DAYS_TO_CHECK) {
                stats.avgSentWeekly = stats.sentCount;
            }
        }
    }

    protected Map<String, NotificationsSentState> getAggregatedUsageEvents() {
        ArrayMap<String, NotificationsSentState> aggregatedStats = new ArrayMap<>();

        long now = System.currentTimeMillis();
        long startTime = now - (DateUtils.DAY_IN_MILLIS * DAYS_TO_CHECK);
        UsageEvents events = mUsageStatsManager.queryEvents(startTime, now);
        if (events != null) {
            UsageEvents.Event event = new UsageEvents.Event();
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                NotificationsSentState stats = aggregatedStats.get(event.getPackageName());
                if (stats == null) {
                    stats = new NotificationsSentState();
                    aggregatedStats.put(event.getPackageName(), stats);
                }

                if (event.getEventType() == UsageEvents.Event.NOTIFICATION_INTERRUPTION) {
                    if (event.getTimeStamp() > stats.lastSent) {
                        stats.lastSent = event.getTimeStamp();
                    }
                    stats.sentCount++;
                }

            }
        }
        return aggregatedStats;
    }

    private static NotificationsSentState getNotificationsSentState(AppEntry entry) {
        if (entry == null || entry.extraInfo == null) {
            return null;
        }
        if (entry.extraInfo instanceof NotificationsSentState) {
            return (NotificationsSentState) entry.extraInfo;
        }
        return null;
    }

    public View.OnClickListener getSwitchOnClickListener(final AppEntry entry) {
        if (entry != null) {
            return v -> {
                ViewGroup view = (ViewGroup) v;
                Switch toggle = view.findViewById(R.id.switchWidget);
                if (toggle != null) {
                    if (!toggle.isEnabled()) {
                        return;
                    }
                    toggle.toggle();
                    mBackend.setNotificationsEnabledForPackage(
                            entry.info.packageName, entry.info.uid, toggle.isChecked());
                    NotificationsSentState stats = getNotificationsSentState(entry);
                    if (stats != null) {
                        stats.blocked = !toggle.isChecked();
                    }
                }
            };
        }
        return null;
    }

    public static final AppFilter FILTER_APP_NOTIFICATION_RECENCY = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            NotificationsSentState state = getNotificationsSentState(info);
            if (state != null) {
                return state.lastSent != 0;
            }
            return false;
        }
    };

    public static final AppFilter FILTER_APP_NOTIFICATION_FREQUENCY = new AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(AppEntry info) {
            NotificationsSentState state = getNotificationsSentState(info);
            if (state != null) {
                return state.sentCount != 0;
            }
            return false;
        }
    };

    public static final Comparator<AppEntry> RECENT_NOTIFICATION_COMPARATOR
            = new Comparator<AppEntry>() {
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            NotificationsSentState state1 = getNotificationsSentState(object1);
            NotificationsSentState state2 = getNotificationsSentState(object2);
            if (state1 == null && state2 != null) return -1;
            if (state1 != null && state2 == null) return 1;
            if (state1 != null && state2 != null) {
                if (state1.lastSent < state2.lastSent) return 1;
                if (state1.lastSent > state2.lastSent) return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(object1, object2);
        }
    };

    public static final Comparator<AppEntry> FREQUENCY_NOTIFICATION_COMPARATOR
            = new Comparator<AppEntry>() {
        @Override
        public int compare(AppEntry object1, AppEntry object2) {
            NotificationsSentState state1 = getNotificationsSentState(object1);
            NotificationsSentState state2 = getNotificationsSentState(object2);
            if (state1 == null && state2 != null) return -1;
            if (state1 != null && state2 == null) return 1;
            if (state1 != null && state2 != null) {
                if (state1.sentCount < state2.sentCount) return 1;
                if (state1.sentCount > state2.sentCount) return -1;
            }
            return ApplicationsState.ALPHA_COMPARATOR.compare(object1, object2);
        }
    };

    public static final boolean enableSwitch(AppEntry entry) {
        NotificationsSentState stats = getNotificationsSentState(entry);
        if (stats == null) {
            return false;
        }

        return stats.blockable;
    }

    public static final boolean checkSwitch(AppEntry entry) {
        NotificationsSentState stats = getNotificationsSentState(entry);
        if (stats == null) {
            return false;
        }

        return !stats.blocked;
    }

    /**
     * NotificationsSentState contains how often an app sends notifications and how recently it sent
     * one.
     */
    public static class NotificationsSentState {
        public int avgSentDaily = 0;
        public int avgSentWeekly = 0;
        public long lastSent = 0;
        public int sentCount = 0;
        public boolean blockable;
        public boolean blocked;
        public boolean systemApp;
    }
}
