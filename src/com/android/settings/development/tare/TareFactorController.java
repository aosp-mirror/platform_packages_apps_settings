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
    private String mAlarmManagerConstants;

    public TareFactorController(Context context) {
        mContentResolver = context.getContentResolver();
        mResources = context.getResources();

        mAlarmManagerConstants = Settings.Global
                .getString(mContentResolver, Settings.Global.TARE_ALARM_MANAGER_CONSTANTS);

        initAlarmManagerMap();
        parseAlarmManagerGlobalSettings();
    }

    /**
     * Initialization for AlarmManager Map that sets a AM factor key to a default value and title
     * in the form of a string.
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
     * Takes a key as input and grabs the default value linked to it.
     *
     * @param key the key of the factor you want to get the default value of
     */
    private int getDefaultValue(String key) {
        return mAlarmManagerMap.get(key).defaultValue;
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
            data.currentValue = mParser.getInt(key, getDefaultValue(key));
        }
    }

    /**
     * Takes a key as input and grabs the title linked to it.
     *
     * @param key the key of the factor you want to get the title of
     */
    private String getTitle(String key) {
        return mAlarmManagerMap.get(key).title;
    }

    /**
     * Takes a key as input and grabs the current value linked to it.
     *
     * @param key the key of the factor you want to get the default value of
     */
    private int getCurrentValue(String key) {
        return mAlarmManagerMap.get(key).currentValue;
    }

    /**
     * Takes a key as input and grabs the factor type linked to it.
     *
     * @param key the key of the factor you want to get the factor type of
     */
    private int getFactorType(String key) {
        return mAlarmManagerMap.get(key).factorPolicy;
    }

    /**
     * Takes a key and edited value as input and assigns the new edited value to be the new current
     * value for that factors key.
     *
     * @param key         the key of the factor you want to get the default value of
     * @param editedValue the value entered by the user in the dialog
     */
    public void updateValue(String key, int editedValue, int factorPolicy) {
        switch (factorPolicy) {
            case POLICY_ALARM_MANAGER:
                mAlarmManagerMap.get(key).currentValue = editedValue;
                rebuildAlarmManagerConstants();
                break;
            case POLICY_JOB_SCHEDULER:
                // TODO: Add JobScheduler
                break;
            default:
                Slog.e(TAG, "Invalid factor policy given");
                break;
        }
    }

    /**
     * Iterates through the AM map for keys and current values to rebuild a current string that is
     * then assigned to be the new global settings string.
     */
    private void rebuildAlarmManagerConstants() {
        StringBuilder newConstantsStringBuilder = new StringBuilder();

        int size = mAlarmManagerMap.size();

        for (int i = 0; i < size; i++) {
            if (i > 0) {
                newConstantsStringBuilder.append(",");
            }

            String key = mAlarmManagerMap.keyAt(i);
            newConstantsStringBuilder.append(key + "=" + mAlarmManagerMap.get(key).currentValue);
        }

        String newConstantsString = newConstantsStringBuilder.toString();

        Settings.Global.putString(mContentResolver, Settings.Global.TARE_ALARM_MANAGER_CONSTANTS,
                newConstantsString);

        mAlarmManagerConstants = Settings.Global
                .getString(mContentResolver, Settings.Global.TARE_ALARM_MANAGER_CONSTANTS);
    }

    /**
     * Creates a dialog with the values linked to the key.
     *
     * @param key the key of the factor you want to get the default value of
     */
    public TareFactorDialogFragment createDialog(String key) {
        return new TareFactorDialogFragment(getTitle(key), key, getCurrentValue(key),
                getFactorType(key), this);
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