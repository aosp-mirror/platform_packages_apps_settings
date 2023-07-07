/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.sim.smartForwarding;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.CallForwardingInfo;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

public class SmartForwardingUtils {
    public static final String TAG = "SmartForwarding";
    public static final String SMART_FORWARDING_PREF = "smart_forwarding_pref_";

    public static final String CALL_WAITING_KEY = "call_waiting_key";
    public static final String CALL_FORWARDING_ENABLED_KEY = "call_forwarding_enabled_key";
    public static final String CALL_FORWARDING_REASON_KEY = "call_forwarding_reason_key";
    public static final String CALL_FORWARDING_NUMBER_KEY = "call_forwarding_number_key";
    public static final String CALL_FORWARDING_TIME_KEY = "call_forwarding_timekey";

    public static boolean getBackupCallWaitingStatus(Context context, int subId) {
        SharedPreferences preferences = context.getSharedPreferences(
                SMART_FORWARDING_PREF + subId, Context.MODE_PRIVATE);
        return preferences.getBoolean(CALL_WAITING_KEY, false);
    }

    public static CallForwardingInfo getBackupCallForwardingStatus(Context context, int subId) {
        SharedPreferences preferences = context.getSharedPreferences(
                SMART_FORWARDING_PREF + subId, Context.MODE_PRIVATE);
        if (preferences.contains(CALL_FORWARDING_ENABLED_KEY)) {
            boolean enabled = preferences.getBoolean(CALL_FORWARDING_ENABLED_KEY, false);
            int reason = preferences.getInt(CALL_FORWARDING_REASON_KEY,
                    CallForwardingInfo.REASON_UNCONDITIONAL);
            String number = preferences.getString(CALL_FORWARDING_NUMBER_KEY, "");
            int time = preferences.getInt(CALL_FORWARDING_TIME_KEY, 1);

            return new CallForwardingInfo(enabled, reason, number, time);
        } else {
            return null;
        }
    }

    public static void saveCallWaitingStatus(Context context, int subId, boolean value) {
        SharedPreferences.Editor preferences = context.getSharedPreferences(
                SMART_FORWARDING_PREF + subId, Context.MODE_PRIVATE).edit();
        preferences.putBoolean(CALL_WAITING_KEY, value).commit();
    }

    public static void saveCallForwardingStatus(Context context, int subId,
            CallForwardingInfo callForwardingInfo) {
        SharedPreferences.Editor preferences = context.getSharedPreferences(
                SMART_FORWARDING_PREF + subId, Context.MODE_PRIVATE).edit();

        preferences.putBoolean(CALL_FORWARDING_ENABLED_KEY, callForwardingInfo.isEnabled())
                .commit();
        preferences.putInt(CALL_FORWARDING_REASON_KEY, callForwardingInfo.getReason()).commit();
        preferences.putString(CALL_FORWARDING_NUMBER_KEY, callForwardingInfo.getNumber()).commit();
        preferences.putInt(CALL_FORWARDING_TIME_KEY, callForwardingInfo.getTimeoutSeconds())
                .commit();
    }

    public static void clearBackupData(Context context, int subId) {
        SharedPreferences.Editor preferences = context.getSharedPreferences(
                SMART_FORWARDING_PREF + subId, Context.MODE_PRIVATE).edit();
        preferences.clear().commit();
    }

    public static boolean[] getAllSlotCallWaitingStatus(Context context, TelephonyManager tm) {
        int phoneCount = tm.getActiveModemCount();
        boolean[] allStatus = new boolean[phoneCount];

        for (int i = 0; i < phoneCount; i++) {
            int subId = SubscriptionManager.getSubscriptionId(i);
            boolean callWaitingStatus = getBackupCallWaitingStatus(context, subId);
            allStatus[i] = callWaitingStatus;
        }
        return allStatus;
    }

    public static CallForwardingInfo[] getAllSlotCallForwardingStatus(
            Context context, SubscriptionManager sm, TelephonyManager tm) {
        int phoneCount = tm.getActiveModemCount();
        CallForwardingInfo[] allStatus = new CallForwardingInfo[phoneCount];

        for (int i = 0; i < phoneCount; i++) {
            int subId = SubscriptionManager.getSubscriptionId(i);
            CallForwardingInfo callWaitingStatus = getBackupCallForwardingStatus(context, subId);
            allStatus[i] = callWaitingStatus;
        }
        return allStatus;
    }

    public static void clearAllBackupData(Context context, SubscriptionManager sm,
            TelephonyManager tm) {
        int phoneCount = tm.getActiveModemCount();
        for (int i = 0; i < phoneCount; i++) {
            int subId = SubscriptionManager.getSubscriptionId(i);
            clearBackupData(context, subId);
        }
    }

    public static void backupPrevStatus(Context context,
            EnableSmartForwardingTask.SlotUTData[] slotUTData) {
        for (int i = 0; i < slotUTData.length; i++) {
            int callWaiting = slotUTData[i].mQueryCallWaiting.result;
            saveCallWaitingStatus(
                    context,
                    slotUTData[i].subId,
                    callWaiting == TelephonyManager.CALL_WAITING_STATUS_ENABLED);

            saveCallForwardingStatus(
                    context,
                    slotUTData[i].subId,
                    slotUTData[i].mQueryCallForwarding.result);
        }
    }

    public static String getPhoneNumber(Context context, int slotId) {
        SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);
        SubscriptionInfo subInfo = subscriptionManager.getActiveSubscriptionInfo(
                SubscriptionManager.getSubscriptionId(slotId));
        return (subInfo != null) ? subInfo.getNumber() : "";
    }
}