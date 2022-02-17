/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.development.tare;

import android.app.tare.EconomyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.KeyValueListParser;
import android.util.Slog;

import com.android.settings.R;

/**
 * Takes an AlarmManager or JobScheduler csv string and parses it to get key:value pairs.
 * This allows us to populate a dialog with the correct information.
 */
public class TareFactorController {
    private static final String TAG = "TareFactorController";

    private static final int POLICY_ALARM_MANAGER = 0;
    private static final int POLICY_JOB_SCHEDULER = 1;

    private final ContentResolver mContentResolver;
    private final KeyValueListParser mParser = new KeyValueListParser(',');
    private final Resources mResources;
    private final ArrayMap<String, TareFactorData> mAlarmManagerMap = new ArrayMap<>();
    private final ArrayMap<String, TareFactorData> mJobSchedulerMap = new ArrayMap<>();
    private String mAlarmManagerConstants;
    private String mJobSchedulerConstants;

    public TareFactorController(Context context) {
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        mAlarmManagerConstants = Settings.Global
                .getString(mContentResolver, Settings.Global.TARE_ALARM_MANAGER_CONSTANTS);

        mJobSchedulerConstants = Settings.Global
                .getString(mContentResolver, Settings.Global.TARE_JOB_SCHEDULER_CONSTANTS);

        initAlarmManagerMap();
        parseAlarmManagerGlobalSettings();

        initJobSchedulerMap();
        parseJobSchedulerGlobalSettings();
    }

    /**
     * Initialization for AlarmManager Map that sets a AM factor key to a title, default value, and
     * policy type in a data object.
     */
    private void initAlarmManagerMap() {
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_EXEMPTED,
                new TareFactorData(mResources.getString(R.string.tare_min_satiated_balance),
                        EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_EXEMPTED,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP,
                new TareFactorData(mResources.getString(R.string.tare_headless_app),
                        EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_OTHER_APP,
                new TareFactorData(mResources.getString(R.string.tare_other_app),
                        EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_OTHER_APP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MAX_SATIATED_BALANCE,
                new TareFactorData(mResources.getString(R.string.tare_max_satiated_balance),
                        EconomyManager.DEFAULT_AM_MAX_SATIATED_BALANCE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MAX_CIRCULATION,
                new TareFactorData(mResources.getString(R.string.tare_max_circulation),
                        EconomyManager.DEFAULT_AM_MAX_CIRCULATION,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_INSTANT,
                        POLICY_ALARM_MANAGER));
        // TODO: Add support to handle floats
        //  mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_ONGOING,
        //          new TareFactorData(mResources.getString(R.string.tare_top_activity),
        //                  EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_ONGOING));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_MAX,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_MAX, POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_INSTANT,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_ONGOING,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_MAX,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen_15_min),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_INSTANT,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen_15_min),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_ONGOING,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen_15_min),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_MAX,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_INSTANT,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_ONGOING,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_MAX,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_WIDGET_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_INSTANT,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_WIDGET_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_ONGOING,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_WIDGET_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_MAX,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_OTHER_USER_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_INSTANT,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_OTHER_USER_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_ONGOING,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_OTHER_USER_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_MAX,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_wakeup_exact_idle),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_wakeup_inexact_idle),
                        EconomyManager
                                .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_EXACT_WAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_wakeup_exact),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_WAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_wakeup_inexact),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_WAKEUP_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_nonwakeup_exact_idle),
                        EconomyManager
                                .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_EXACT_NONWAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_nonwakeup_exact),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_NONWAKEUP_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_nonwakeup_inexact_idle),
                        EconomyManager
                                .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_NONWAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_nonwakeup_inexact),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_NONWAKEUP_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_ALARMCLOCK_CTP,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_ALARMCLOCK_CTP,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager
                                .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager
                                .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_WAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_EXACT_NONWAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager
                            .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_NONWAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_NONWAKEUP_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_ALARMCLOCK_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_ALARMCLOCK_BASE_PRICE,
                        POLICY_ALARM_MANAGER));
    }

    /**
     * Initialization for JobScheduler Map that sets a JS factor key to a title, default value, and
     * policy type in a data object.
     */
    private void initJobSchedulerMap() {
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MIN_SATIATED_BALANCE_EXEMPTED,
                new TareFactorData(mResources.getString(R.string.tare_min_satiated_balance),
                        EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_EXEMPTED,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP,
                new TareFactorData(mResources.getString(R.string.tare_headless_app),
                        EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MIN_SATIATED_BALANCE_OTHER_APP,
                new TareFactorData(mResources.getString(R.string.tare_other_app),
                        EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_OTHER_APP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MAX_SATIATED_BALANCE,
                new TareFactorData(mResources.getString(R.string.tare_max_satiated_balance),
                        EconomyManager.DEFAULT_JS_MAX_SATIATED_BALANCE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MAX_CIRCULATION,
                new TareFactorData(mResources.getString(R.string.tare_max_circulation),
                        EconomyManager.DEFAULT_JS_MAX_CIRCULATION,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_TOP_ACTIVITY_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_INSTANT,
                        POLICY_JOB_SCHEDULER));
        // TODO: Add support to handle floats
        //  mAlarmManagerMap.put(EconomyManager.KEY_JS_REWARD_TOP_ACTIVITY_ONGOING,
        //          new TareFactorData(mResources.getString(R.string.tare_top_activity),
        //                  EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_ONGOING));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_TOP_ACTIVITY_MAX,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_MAX, POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_SEEN_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_INSTANT,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_SEEN_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_ONGOING,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_SEEN_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_MAX,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_INSTANT,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_ONGOING,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_MAX,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_WIDGET_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_INSTANT,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_WIDGET_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_ONGOING,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_WIDGET_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_MAX,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_OTHER_USER_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_INSTANT,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_OTHER_USER_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_ONGOING,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_OTHER_USER_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_MAX,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MAX_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_max_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_START_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MAX_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_max_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_RUNNING_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_HIGH_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_high_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_START_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_HIGH_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_high_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_RUNNING_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_DEFAULT_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_default_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_START_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_DEFAULT_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_default_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_RUNNING_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_LOW_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_low_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_START_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_LOW_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_low_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_RUNNING_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MIN_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_min_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_START_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_MIN_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_min_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_RUNNING_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_TIMEOUT_PENALTY_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_timeout_penalty),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_TIMEOUT_PENALTY_CTP,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MAX_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_max_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_START_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_MAX_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_max_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_RUNNING_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_HIGH_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_high_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_START_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_HIGH_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_high_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_RUNNING_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_DEFAULT_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_default_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_START_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_DEFAULT_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_default_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_RUNNING_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_LOW_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_low_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_START_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_LOW_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_low_running),
                        EconomyManager
                                .DEFAULT_JS_ACTION_JOB_LOW_RUNNING_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MIN_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_min_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_START_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MIN_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_min_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_RUNNING_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_TIMEOUT_PENALTY_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_timeout_penalty),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_TIMEOUT_PENALTY_BASE_PRICE,
                        POLICY_JOB_SCHEDULER));
    }


    /**
     * Takes a key and factor policy as input and grabs the default value linked to it.
     *
     * @param key the key of the factor you want to get the default value of
     * @param factorPolicy the policy you want the default value of
     */
    private int getDefaultValue(String key, int factorPolicy) {
        ArrayMap<String, TareFactorData> currentMap;
        switch (factorPolicy) {
            case POLICY_ALARM_MANAGER:
                currentMap = mAlarmManagerMap;
                break;
            case POLICY_JOB_SCHEDULER:
                currentMap = mJobSchedulerMap;
                break;
            default:
                throw new IllegalArgumentException("Invalid factor policy given");
        }
        return currentMap.get(key).defaultValue;
    }

    /**
     * Parses the AM constant from Settings.Global to get to the current value.
     */
    private void parseAlarmManagerGlobalSettings() {
        try {
            mParser.setString(mAlarmManagerConstants);
        } catch (Exception e) {
            Slog.e(TAG, "Bad value string constants", e);
        }
        int size = mParser.size();

        for (int i = 0; i < size - 1; i++) {
            String key = mParser.keyAt(i);
            TareFactorData data = mAlarmManagerMap.get(key);
            data.currentValue = mParser.getInt(key, getDefaultValue(key, getFactorType(key)));
        }
    }

    /**
     * Parses the JS constant from Settings.Global to get to the current value.
     */
    private void parseJobSchedulerGlobalSettings() {
        try {
            mParser.setString(mJobSchedulerConstants);
        } catch (Exception e) {
            Slog.e(TAG, "Bad value string constants", e);
        }
        int size = mParser.size();

        for (int i = 0; i < size - 1; i++) {
            String key = mParser.keyAt(i);
            TareFactorData data = mJobSchedulerMap.get(key);
            data.currentValue = mParser.getInt(key, getDefaultValue(key, getFactorType(key)));
        }
    }

    /**
     * Takes a key and factor policy as input and grabs the title linked to it.
     *
     * @param key the key of the factor you want to get the title of
     * @param factorPolicy the policy you want the title of
     */
    private String getTitle(String key, int factorPolicy) {
        ArrayMap<String, TareFactorData> currentMap;
        switch (factorPolicy) {
            case POLICY_ALARM_MANAGER:
                currentMap = mAlarmManagerMap;
                break;
            case POLICY_JOB_SCHEDULER:
                currentMap = mJobSchedulerMap;
                break;
            default:
                throw new IllegalArgumentException("Invalid factor policy given");
        }
        return currentMap.get(key).title;
    }

    /**
     * Takes a key and factor policy as input and grabs the current value linked to it.
     *
     * @param key the key of the factor you want to get the default value of
     * @param factorPolicy the policy you want the current value of
     */
    private int getCurrentValue(String key, int factorPolicy) {
        ArrayMap<String, TareFactorData> currentMap;
        switch (factorPolicy) {
            case POLICY_ALARM_MANAGER:
                currentMap = mAlarmManagerMap;
                break;
            case POLICY_JOB_SCHEDULER:
                currentMap = mJobSchedulerMap;
                break;
            default:
                throw new IllegalArgumentException("Invalid factor policy given");
        }
        return currentMap.get(key).currentValue;
    }

    /**
     * Takes a key as input and grabs the factor type linked to it.
     *
     * @param key the key of the factor you want to get the factor type of
     */
    private int getFactorType(String key) {
        ArrayMap<String, TareFactorData> currentMap;
        if (mAlarmManagerMap.containsKey(key)) {
            currentMap = mAlarmManagerMap;
        } else if (mJobSchedulerMap.containsKey(key)) {
            currentMap = mJobSchedulerMap;
        } else {
            throw new IllegalArgumentException("Couldn't link key to policy map");
        }
        return currentMap.get(key).factorPolicy;
    }

    /**
     * Takes a key,edited value, and factor policy as input and assigns the new edited value to
     * be the new current value for that factors key.
     *
     * @param key          the key of the factor you want to get the default value of
     * @param editedValue  the value entered by the user in the dialog
     * @param factorPolicy policy being updated
     */
    public void updateValue(String key, int editedValue, int factorPolicy) {
        switch (factorPolicy) {
            case POLICY_ALARM_MANAGER:
                mAlarmManagerMap.get(key).currentValue = editedValue;
                rebuildPolicyConstants(factorPolicy);
                break;
            case POLICY_JOB_SCHEDULER:
                mJobSchedulerMap.get(key).currentValue = editedValue;
                rebuildPolicyConstants(factorPolicy);
                break;
            default:
                throw new IllegalArgumentException("Invalid factor policy given");
        }
    }


    /**
     * Iterates through the factor policy map for keys and current values to
     * rebuild a current string that is then assigned to be the new global settings string.
     *
     * @param factorPolicy policy being updated
     */
    private void rebuildPolicyConstants(int factorPolicy) {
        StringBuilder newConstantsStringBuilder = new StringBuilder();

        switch (factorPolicy) {
            case POLICY_ALARM_MANAGER:
                int sizeAM = mAlarmManagerMap.size();

                for (int i = 0; i < sizeAM; i++) {
                    if (i > 0) {
                        newConstantsStringBuilder.append(",");
                    }

                    String key = mAlarmManagerMap.keyAt(i);
                    newConstantsStringBuilder.append(key + "=" + mAlarmManagerMap.get(key)
                            .currentValue);
                }

                String newAMConstantsString = newConstantsStringBuilder.toString();

                Settings.Global.putString(mContentResolver, Settings.Global
                                .TARE_ALARM_MANAGER_CONSTANTS,
                        newAMConstantsString);

                mAlarmManagerConstants = Settings.Global
                        .getString(mContentResolver, Settings.Global
                                .TARE_ALARM_MANAGER_CONSTANTS);
                break;
            case POLICY_JOB_SCHEDULER:
                int sizeJS = mJobSchedulerMap.size();

                for (int i = 0; i < sizeJS; i++) {
                    if (i > 0) {
                        newConstantsStringBuilder.append(",");
                    }

                    String key = mJobSchedulerMap.keyAt(i);
                    newConstantsStringBuilder.append(key + "=" + mJobSchedulerMap.get(key)
                            .currentValue);
                }

                String newJSConstantsString = newConstantsStringBuilder.toString();

                Settings.Global.putString(mContentResolver, Settings.Global
                                .TARE_JOB_SCHEDULER_CONSTANTS,
                        newJSConstantsString);

                mJobSchedulerConstants = Settings.Global
                        .getString(mContentResolver, Settings.Global
                                .TARE_JOB_SCHEDULER_CONSTANTS);
                break;
        }
    }

    /**
     * Creates a dialog with the values linked to the key.
     *
     * @param key the key of the factor you want to get the default value of
     */
    public TareFactorDialogFragment createDialog(String key) {
        int policy = getFactorType(key);
        return new TareFactorDialogFragment(getTitle(key, policy), key,
                getCurrentValue(key, policy), policy , this);
    }

    /**
     * Data object that holds a title,default value,and current value for a key.
     */
    private static class TareFactorData {
        public final String title;
        public final int defaultValue;
        public final int factorPolicy;
        public int currentValue;

        TareFactorData(String title, int defaultValue, int factorPolicy) {
            this.title = title;
            this.defaultValue = defaultValue;
            this.factorPolicy = factorPolicy;
            this.currentValue = defaultValue;
        }
    }
}