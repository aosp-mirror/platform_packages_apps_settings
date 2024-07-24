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

import static android.app.tare.EconomyManager.CAKE_IN_ARC;
import static android.app.tare.EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_BASE_PRICE_CAKES;
import static android.app.tare.EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE_CAKES;
import static android.app.tare.EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_CTP_CAKES;
import static android.app.tare.EconomyManager.DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_BASE_PRICE_CAKES;
import static android.app.tare.EconomyManager.parseCreditValue;
import static android.provider.Settings.Global.TARE_ALARM_MANAGER_CONSTANTS;
import static android.provider.Settings.Global.TARE_JOB_SCHEDULER_CONSTANTS;

import android.app.tare.EconomyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.KeyValueListParser;
import android.util.Slog;

import androidx.annotation.NonNull;

import com.android.settings.R;

/**
 * Takes an AlarmManager or JobScheduler csv string and parses it to get key:value pairs.
 * This allows us to populate a dialog with the correct information.
 */
public class TareFactorController {
    private static final String TAG = "TareFactorController";

    private static TareFactorController sInstance;

    private static final int POLICY_ALARM_MANAGER = 0;
    private static final int POLICY_JOB_SCHEDULER = 1;

    private final ContentResolver mContentResolver;
    private final KeyValueListParser mParser = new KeyValueListParser(',');
    private final Resources mResources;
    private final ArrayMap<String, TareFactorData> mAlarmManagerMap = new ArrayMap<>();
    private final ArrayMap<String, TareFactorData> mJobSchedulerMap = new ArrayMap<>();
    private String mAlarmManagerConstants;
    private String mJobSchedulerConstants;

    private final ArraySet<DataChangeListener> mDataChangeListeners = new ArraySet<>();

    private TareFactorController(Context context) {
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        ConfigObserver configObserver = new ConfigObserver(new Handler(Looper.getMainLooper()));
        configObserver.start();

        mAlarmManagerConstants =
                Settings.Global.getString(mContentResolver, TARE_ALARM_MANAGER_CONSTANTS);
        mJobSchedulerConstants =
                Settings.Global.getString(mContentResolver, TARE_JOB_SCHEDULER_CONSTANTS);

        initAlarmManagerMap();
        parseAlarmManagerGlobalSettings();

        initJobSchedulerMap();
        parseJobSchedulerGlobalSettings();
    }

    static TareFactorController getInstance(Context context) {
        synchronized (TareFactorController.class) {
            if (sInstance == null) {
                sInstance = new TareFactorController(context.getApplicationContext());
            }
        }
        return sInstance;
    }

    /**
     * Initialization for AlarmManager Map that sets a AM factor key to a title, default value, and
     * policy type in a data object.
     */
    private void initAlarmManagerMap() {
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_EXEMPTED,
                new TareFactorData(mResources.getString(R.string.tare_min_balance_exempted),
                        EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_EXEMPTED_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP,
                new TareFactorData(mResources.getString(R.string.tare_min_balance_headless_app),
                        EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MIN_SATIATED_BALANCE_OTHER_APP,
                new TareFactorData(mResources.getString(R.string.tare_min_balance_other_app),
                        EconomyManager.DEFAULT_AM_MIN_SATIATED_BALANCE_OTHER_APP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MAX_SATIATED_BALANCE,
                new TareFactorData(mResources.getString(R.string.tare_max_satiated_balance),
                        EconomyManager.DEFAULT_AM_MAX_SATIATED_BALANCE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_INITIAL_CONSUMPTION_LIMIT,
                new TareFactorData(mResources.getString(R.string.tare_initial_consumption_limit),
                        EconomyManager.DEFAULT_AM_INITIAL_CONSUMPTION_LIMIT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MIN_CONSUMPTION_LIMIT,
                new TareFactorData(mResources.getString(R.string.tare_min_consumption_limit),
                        EconomyManager.DEFAULT_AM_MIN_CONSUMPTION_LIMIT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_MAX_CONSUMPTION_LIMIT,
                new TareFactorData(mResources.getString(R.string.tare_max_consumption_limit),
                        EconomyManager.DEFAULT_AM_MAX_CONSUMPTION_LIMIT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_INSTANT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_ONGOING_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_TOP_ACTIVITY_MAX,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_AM_REWARD_TOP_ACTIVITY_MAX_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_INSTANT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_ONGOING_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_MAX_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen_15_min),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_INSTANT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen_15_min),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_ONGOING_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen_15_min),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_SEEN_WITHIN_15_MAX_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_INSTANT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_ONGOING_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_NOTIFICATION_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_NOTIFICATION_INTERACTION_MAX_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_WIDGET_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_INSTANT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_WIDGET_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_ONGOING_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_WIDGET_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_WIDGET_INTERACTION_MAX_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_OTHER_USER_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_INSTANT_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_OTHER_USER_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_ONGOING_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_REWARD_OTHER_USER_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_AM_REWARD_OTHER_USER_INTERACTION_MAX_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_wakeup_exact_idle),
                        EconomyManager
                                .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_wakeup_inexact_idle),
                        EconomyManager
                                .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_EXACT_WAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_wakeup_exact),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_WAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_wakeup_inexact),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_WAKEUP_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_nonwakeup_exact_idle),
                        EconomyManager
                                .DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_EXACT_NONWAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_nonwakeup_exact),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_NONWAKEUP_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_nonwakeup_inexact_idle),
                        DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_NONWAKEUP_CTP,
                new TareFactorData(mResources.getString(R.string.tare_nonwakeup_inexact),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_NONWAKEUP_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_ALARMCLOCK_CTP,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_ALARMCLOCK_CTP_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_WAKEUP_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_WAKEUP_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_WAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_EXACT_NONWAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_EXACT_NONWAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_EXACT_WAKEUP_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(
                EconomyManager.KEY_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        DEFAULT_AM_ACTION_ALARM_ALLOW_WHILE_IDLE_INEXACT_NONWAKEUP_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_INEXACT_NONWAKEUP_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_INEXACT_NONWAKEUP_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
        mAlarmManagerMap.put(EconomyManager.KEY_AM_ACTION_ALARM_ALARMCLOCK_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_alarm_clock),
                        EconomyManager.DEFAULT_AM_ACTION_ALARM_ALARMCLOCK_BASE_PRICE_CAKES,
                        POLICY_ALARM_MANAGER));
    }

    /**
     * Initialization for JobScheduler Map that sets a JS factor key to a title, default value, and
     * policy type in a data object.
     */
    private void initJobSchedulerMap() {
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MIN_SATIATED_BALANCE_EXEMPTED,
                new TareFactorData(mResources.getString(R.string.tare_min_balance_exempted),
                        EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_EXEMPTED_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP,
                new TareFactorData(mResources.getString(R.string.tare_min_balance_headless_app),
                        EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_HEADLESS_SYSTEM_APP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MIN_SATIATED_BALANCE_OTHER_APP,
                new TareFactorData(mResources.getString(R.string.tare_min_balance_other_app),
                        EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_OTHER_APP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MIN_SATIATED_BALANCE_INCREMENT_APP_UPDATER,
                new TareFactorData(
                        mResources.getString(R.string.tare_min_balance_addition_app_updater),
                        EconomyManager.DEFAULT_JS_MIN_SATIATED_BALANCE_INCREMENT_APP_UPDATER_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MAX_SATIATED_BALANCE,
                new TareFactorData(mResources.getString(R.string.tare_max_satiated_balance),
                        EconomyManager.DEFAULT_JS_MAX_SATIATED_BALANCE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_INITIAL_CONSUMPTION_LIMIT,
                new TareFactorData(mResources.getString(R.string.tare_initial_consumption_limit),
                        EconomyManager.DEFAULT_JS_INITIAL_CONSUMPTION_LIMIT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MIN_CONSUMPTION_LIMIT,
                new TareFactorData(mResources.getString(R.string.tare_min_consumption_limit),
                        EconomyManager.DEFAULT_JS_MIN_CONSUMPTION_LIMIT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_MAX_CONSUMPTION_LIMIT,
                new TareFactorData(mResources.getString(R.string.tare_max_consumption_limit),
                        EconomyManager.DEFAULT_JS_MAX_CONSUMPTION_LIMIT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_APP_INSTALL_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_app_install),
                        EconomyManager.DEFAULT_JS_REWARD_APP_INSTALL_INSTANT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_APP_INSTALL_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_app_install),
                        EconomyManager.DEFAULT_JS_REWARD_APP_INSTALL_ONGOING_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_APP_INSTALL_MAX,
                new TareFactorData(mResources.getString(R.string.tare_app_install),
                        EconomyManager.DEFAULT_JS_REWARD_APP_INSTALL_MAX_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_TOP_ACTIVITY_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_INSTANT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_TOP_ACTIVITY_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_ONGOING_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_TOP_ACTIVITY_MAX,
                new TareFactorData(mResources.getString(R.string.tare_top_activity),
                        EconomyManager.DEFAULT_JS_REWARD_TOP_ACTIVITY_MAX_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_SEEN_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_INSTANT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_SEEN_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_ONGOING_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_SEEN_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_seen),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_SEEN_MAX_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_INSTANT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_ONGOING_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_NOTIFICATION_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_notification_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_NOTIFICATION_INTERACTION_MAX_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_WIDGET_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_INSTANT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_WIDGET_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_ONGOING_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_WIDGET_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_widget_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_WIDGET_INTERACTION_MAX_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_OTHER_USER_INTERACTION_INSTANT,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_INSTANT_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_OTHER_USER_INTERACTION_ONGOING,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_ONGOING_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_REWARD_OTHER_USER_INTERACTION_MAX,
                new TareFactorData(mResources.getString(R.string.tare_other_interaction),
                        EconomyManager.DEFAULT_JS_REWARD_OTHER_USER_INTERACTION_MAX_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MAX_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_max_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_START_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MAX_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_max_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_RUNNING_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_HIGH_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_high_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_START_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_HIGH_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_high_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_RUNNING_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_DEFAULT_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_default_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_START_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_DEFAULT_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_default_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_RUNNING_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_LOW_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_low_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_START_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_LOW_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_low_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_RUNNING_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MIN_START_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_min_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_START_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_MIN_RUNNING_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_min_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_RUNNING_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_TIMEOUT_PENALTY_CTP,
                new TareFactorData(mResources.getString(R.string.tare_job_timeout_penalty),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_TIMEOUT_PENALTY_CTP_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MAX_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_max_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_START_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_MAX_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_max_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MAX_RUNNING_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_HIGH_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_high_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_START_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_HIGH_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_high_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_HIGH_RUNNING_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_DEFAULT_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_default_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_START_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_DEFAULT_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_default_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_DEFAULT_RUNNING_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_LOW_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_low_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_START_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(
                EconomyManager.KEY_JS_ACTION_JOB_LOW_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_low_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_LOW_RUNNING_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MIN_START_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_min_start),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_START_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_MIN_RUNNING_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_min_running),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_MIN_RUNNING_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
        mJobSchedulerMap.put(EconomyManager.KEY_JS_ACTION_JOB_TIMEOUT_PENALTY_BASE_PRICE,
                new TareFactorData(mResources.getString(R.string.tare_job_timeout_penalty),
                        EconomyManager.DEFAULT_JS_ACTION_JOB_TIMEOUT_PENALTY_BASE_PRICE_CAKES,
                        POLICY_JOB_SCHEDULER));
    }

    /**
     * Parses the AM constant from Settings.Global to get to the current value.
     */
    private void parseAlarmManagerGlobalSettings() {
        parseSettingsIntoMap(mAlarmManagerConstants, mAlarmManagerMap);
    }

    /**
     * Parses the JS constant from Settings.Global to get to the current value.
     */
    private void parseJobSchedulerGlobalSettings() {
        parseSettingsIntoMap(mJobSchedulerConstants, mJobSchedulerMap);
    }

    private void parseSettingsIntoMap(String constants, ArrayMap<String, TareFactorData> map) {
        try {
            mParser.setString(constants);
        } catch (Exception e) {
            Slog.e(TAG, "Bad string constants value", e);
        }

        for (int i = map.size() - 1; i >= 0; --i) {
            final String key = map.keyAt(i);
            final TareFactorData data = map.valueAt(i);
            data.currentValue = parseCreditValue(mParser.getString(key, null), data.defaultValue);
        }
    }

    @NonNull
    private ArrayMap<String, TareFactorData> getMap(int factorPolicy) {
        switch (factorPolicy) {
            case POLICY_ALARM_MANAGER:
                return mAlarmManagerMap;
            case POLICY_JOB_SCHEDULER:
                return mJobSchedulerMap;
            default:
                throw new IllegalArgumentException("Invalid factor policy given");
        }
    }

    /**
     * Takes a key and factor policy as input and grabs the title linked to it.
     *
     * @param key          the key of the factor you want to get the title of
     * @param factorPolicy the policy you want the title of
     */
    private String getTitle(String key, int factorPolicy) {
        final ArrayMap<String, TareFactorData> currentMap = getMap(factorPolicy);
        return currentMap.get(key).title;
    }

    /**
     * Takes a key and factor policy as input and grabs the current value linked to it.
     *
     * @param key          the key of the factor you want to get the default value of
     * @param factorPolicy the policy you want the current value of
     */
    private long getCurrentValue(String key, int factorPolicy) {
        final ArrayMap<String, TareFactorData> currentMap = getMap(factorPolicy);
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
            throw new IllegalArgumentException("Couldn't link key '" + key + "' to a policy");
        }
        return currentMap.get(key).factorPolicy;
    }

    long getValue(String key) {
        final int policy = getFactorType(key);
        return getCurrentValue(key, policy);
    }

    /**
     * Takes a key,edited value, and factor policy as input and assigns the new edited value to
     * be the new current value for that factors key.
     *
     * @param key          the key of the factor you want to get the default value of
     * @param editedValue  the value entered by the user in the dialog
     * @param factorPolicy policy being updated
     */
    public void updateValue(String key, long editedValue, int factorPolicy) {
        final ArrayMap<String, TareFactorData> map = getMap(factorPolicy);

        final TareFactorData data = map.get(key);
        if (data.currentValue == editedValue) {
            return;
        }
        data.currentValue = editedValue;
        rebuildPolicyConstants(factorPolicy);
    }

    /**
     * Iterates through the factor policy map for keys and current values to
     * rebuild a current string that is then assigned to be the new global settings string.
     *
     * @param factorPolicy policy being updated
     */
    private void rebuildPolicyConstants(int factorPolicy) {
        switch (factorPolicy) {
            case POLICY_ALARM_MANAGER:
                writeConstantsToSettings(mAlarmManagerMap, TARE_ALARM_MANAGER_CONSTANTS);
                break;
            case POLICY_JOB_SCHEDULER:
                writeConstantsToSettings(mJobSchedulerMap, TARE_JOB_SCHEDULER_CONSTANTS);
                break;
        }
    }

    private void writeConstantsToSettings(ArrayMap<String, TareFactorData> factorMap,
            String settingsKey) {
        final StringBuilder constantsStringBuilder = new StringBuilder();

        for (int i = 0, size = factorMap.size(); i < size; ++i) {
            final TareFactorData factor = factorMap.valueAt(i);
            if (factor.currentValue == factor.defaultValue) {
                continue;
            }

            if (constantsStringBuilder.length() > 0) {
                constantsStringBuilder.append(",");
            }

            constantsStringBuilder
                    .append(factorMap.keyAt(i))
                    .append("=");
            if (factor.currentValue % CAKE_IN_ARC == 0) {
                constantsStringBuilder
                        .append(factor.currentValue / CAKE_IN_ARC)
                        .append("A");
            } else {
                constantsStringBuilder
                        .append(factor.currentValue)
                        .append("ck");
            }
        }

        Settings.Global.putString(mContentResolver, settingsKey, constantsStringBuilder.toString());
    }

    /**
     * Creates a dialog with the values linked to the key.
     *
     * @param key the key of the factor you want to get the default value of
     */
    public TareFactorDialogFragment createDialog(String key) {
        int policy = getFactorType(key);
        return new TareFactorDialogFragment(getTitle(key, policy), key,
                getCurrentValue(key, policy), policy, this);
    }

    /**
     * Data object that holds a title,default value,and current value for a key.
     */
    private static class TareFactorData {
        public final String title;
        public final long defaultValue;
        public final int factorPolicy;
        public long currentValue;

        TareFactorData(String title, long defaultValue, int factorPolicy) {
            this.title = title;
            this.defaultValue = defaultValue;
            this.factorPolicy = factorPolicy;
            this.currentValue = defaultValue;
        }
    }

    interface DataChangeListener {
        void onDataChanged();
    }

    void registerListener(DataChangeListener listener) {
        mDataChangeListeners.add(listener);
    }

    void unregisterListener(DataChangeListener listener) {
        mDataChangeListeners.remove(listener);
    }

    void notifyListeners() {
        for (int i = mDataChangeListeners.size() - 1; i >= 0; --i) {
            mDataChangeListeners.valueAt(i).onDataChanged();
        }
    }

    private class ConfigObserver extends ContentObserver {

        ConfigObserver(Handler handler) {
            super(handler);
        }

        public void start() {
            mContentResolver.registerContentObserver(
                    Settings.Global.getUriFor(TARE_ALARM_MANAGER_CONSTANTS), false, this);
            mContentResolver.registerContentObserver(
                    Settings.Global.getUriFor(TARE_JOB_SCHEDULER_CONSTANTS), false, this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.Global.getUriFor(TARE_ALARM_MANAGER_CONSTANTS))) {
                mAlarmManagerConstants =
                        Settings.Global.getString(mContentResolver, TARE_ALARM_MANAGER_CONSTANTS);
                parseAlarmManagerGlobalSettings();
                notifyListeners();
            } else if (uri.equals(Settings.Global.getUriFor(TARE_JOB_SCHEDULER_CONSTANTS))) {
                mJobSchedulerConstants =
                        Settings.Global.getString(mContentResolver, TARE_JOB_SCHEDULER_CONSTANTS);
                parseJobSchedulerGlobalSettings();
                notifyListeners();
            }
        }
    }
}
