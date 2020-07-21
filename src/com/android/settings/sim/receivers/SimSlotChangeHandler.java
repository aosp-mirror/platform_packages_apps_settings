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

package com.android.settings.sim.receivers;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.util.Log;

import com.android.settings.network.SubscriptionUtil;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/** Perform actions after a slot change event is triggered. */
public class SimSlotChangeHandler {
    private static final String TAG = "SimSlotChangeHandler";

    private static final String SYSTEM_PROPERTY_DEVICE_PROVISIONED =
            "persist.sys.device_provisioned";

    private static final String EUICC_PREFS = "euicc_prefs";
    private static final String KEY_REMOVABLE_SLOT_STATE = "removable_slot_state";

    private static volatile SimSlotChangeHandler sSlotChangeHandler;

    /** Returns a SIM slot change handler singleton. */
    public static SimSlotChangeHandler get() {
        if (sSlotChangeHandler == null) {
            synchronized (SimSlotChangeHandler.class) {
                if (sSlotChangeHandler == null) {
                    sSlotChangeHandler = new SimSlotChangeHandler();
                }
            }
        }
        return sSlotChangeHandler;
    }

    private SubscriptionManager mSubMgr;
    private TelephonyManager mTelMgr;
    private Context mContext;
    private boolean mNotificationEnabled = true;

    void onSlotsStatusChange(Context context) {
        init(context);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Cannot be called from main thread.");
        }

        if (mTelMgr.getActiveModemCount() > 1) {
            Log.i(TAG, "The device is already in DSDS mode. Do nothing.");
            return;
        }

        UiccSlotInfo removableSlotInfo = getRemovableUiccSlotInfo();
        if (removableSlotInfo == null) {
            Log.e(TAG, "Unable to find the removable slot. Do nothing.");
            return;
        }

        int lastRemovableSlotState = getLastRemovableSimSlotState(mContext);
        int currentRemovableSlotState = removableSlotInfo.getCardStateInfo();

        // Sets the current removable slot state.
        setRemovableSimSlotState(mContext, currentRemovableSlotState);

        if (lastRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_ABSENT
                && currentRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_PRESENT) {
            handleSimInsert(removableSlotInfo);
            return;
        }
        if (lastRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_PRESENT
                && currentRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_ABSENT) {
            handleSimRemove(removableSlotInfo);
            return;
        }
        Log.i(TAG, "Do nothing on slot status changes.");
    }

    private void init(Context context) {
        mSubMgr =
                (SubscriptionManager)
                        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mTelMgr = context.getSystemService(TelephonyManager.class);
        mContext = context;
    }

    private void handleSimInsert(UiccSlotInfo removableSlotInfo) {
        Log.i(TAG, "Detect SIM inserted.");

        if (!isSuwFinished()) {
            // TODO: Store the action and handle it after SUW is finished.
            Log.i(TAG, "Still in SUW. Handle SIM insertion after SUW is finished");
            return;
        }

        if (removableSlotInfo.getIsActive()) {
            Log.i(TAG, "The removable slot is already active. Do nothing.");
            return;
        }

        if (!hasActiveEsimSubscription()) {
            if (mTelMgr.isMultiSimEnabled()) {
                Log.i(TAG, "Enabled profile exists. DSDS condition satisfied.");
                // TODO Display DSDS dialog to ask users whether to enable DSDS.
            } else {
                Log.i(TAG, "Enabled profile exists. DSDS condition not satisfied.");
                // TODO Display Choose a number to use screen for subscription selection.
            }
            return;
        }

        Log.i(
                TAG,
                "No enabled eSIM profile. Ready to switch to removable slot and show"
                        + " notification.");
        // TODO Switch the slot to the removebale slot and show the notification.
    }

    private void handleSimRemove(UiccSlotInfo removableSlotInfo) {
        Log.i(TAG, "Detect SIM removed.");

        if (!isSuwFinished()) {
            // TODO: Store the action and handle it after SUW is finished.
            Log.i(TAG, "Still in SUW. Handle SIM removal after SUW is finished");
            return;
        }

        List<SubscriptionInfo> groupedEmbeddedSubscriptions = getGroupedEmbeddedSubscriptions();

        if (groupedEmbeddedSubscriptions.size() == 0 || !removableSlotInfo.getIsActive()) {
            Log.i(TAG, "eSIM slot is active or no subscriptions exist. Do nothing.");
            return;
        }

        // If there is only 1 eSIM profile exists, we ask the user if they want to switch to that
        // profile.
        if (groupedEmbeddedSubscriptions.size() == 1) {
            Log.i(TAG, "Only 1 eSIM profile found. Ask user's consent to switch.");
            // TODO Display a dialog to ask users to switch.
            return;
        }

        // If there are more than 1 eSIM profiles installed, we show a screen to let users to choose
        // the number they want to use.
        Log.i(TAG, "Multiple eSIM profiles found. Ask user which subscription to use.");
        // TODO Display a dialog to ask user which SIM to switch.
    }

    private int getLastRemovableSimSlotState(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(EUICC_PREFS, MODE_PRIVATE);
        return prefs.getInt(KEY_REMOVABLE_SLOT_STATE, UiccSlotInfo.CARD_STATE_INFO_ABSENT);
    }

    private void setRemovableSimSlotState(Context context, int state) {
        final SharedPreferences prefs = context.getSharedPreferences(EUICC_PREFS, MODE_PRIVATE);
        prefs.edit().putInt(KEY_REMOVABLE_SLOT_STATE, state).apply();
    }

    @Nullable
    private UiccSlotInfo getRemovableUiccSlotInfo() {
        UiccSlotInfo[] slotInfos = mTelMgr.getUiccSlotsInfo();
        if (slotInfos == null) {
            Log.e(TAG, "slotInfos is null. Unable to get slot infos.");
            return null;
        }
        for (UiccSlotInfo slotInfo : slotInfos) {
            if (slotInfo != null && slotInfo.isRemovable()) {

                return slotInfo;
            }
        }
        return null;
    }

    private boolean isSuwFinished() {
        return "1".equals(SystemProperties.get(SYSTEM_PROPERTY_DEVICE_PROVISIONED, "0"));
    }

    private boolean hasActiveEsimSubscription() {
        List<SubscriptionInfo> activeSubs = SubscriptionUtil.getActiveSubscriptions(mSubMgr);
        return activeSubs.stream().anyMatch(SubscriptionInfo::isEmbedded);
    }

    private List<SubscriptionInfo> getGroupedEmbeddedSubscriptions() {
        List<SubscriptionInfo> groupedSubscriptions =
                SubscriptionUtil.getSelectableSubscriptionInfoList(mContext);
        if (groupedSubscriptions == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(
                groupedSubscriptions.stream()
                        .filter(sub -> sub.isEmbedded())
                        .collect(Collectors.toList()));
    }

    private SimSlotChangeHandler() {}
}
