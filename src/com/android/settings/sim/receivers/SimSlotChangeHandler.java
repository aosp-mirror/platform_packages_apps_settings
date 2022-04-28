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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccSlotInfo;
import android.util.Log;

import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.UiccSlotUtil;
import com.android.settings.network.UiccSlotsException;
import com.android.settings.network.telephony.ToggleSubscriptionDialogActivity;
import com.android.settings.sim.ChooseSimActivity;
import com.android.settings.sim.DsdsDialogActivity;
import com.android.settings.sim.SimActivationNotifier;
import com.android.settings.sim.SimNotificationService;
import com.android.settings.sim.SwitchToEsimConfirmDialogActivity;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

/** Perform actions after a slot change event is triggered. */
public class SimSlotChangeHandler {
    private static final String TAG = "SimSlotChangeHandler";

    private static final String EUICC_PREFS = "euicc_prefs";
    // Shared preference keys
    private static final String KEY_REMOVABLE_SLOT_STATE = "removable_slot_state";
    private static final String KEY_SUW_PSIM_ACTION = "suw_psim_action";
    // User's last removable SIM insertion / removal action during SUW.
    private static final int LAST_USER_ACTION_IN_SUW_NONE = 0;
    private static final int LAST_USER_ACTION_IN_SUW_INSERT = 1;
    private static final int LAST_USER_ACTION_IN_SUW_REMOVE = 2;

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

    void onSlotsStatusChange(Context context) {
        init(context);

        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw new IllegalStateException("Cannot be called from main thread.");
        }

        if (mTelMgr.getActiveModemCount() > 1 && !isMultipleEnabledProfilesSupported()) {
            Log.i(TAG, "The device is already in DSDS mode and no MEP. Do nothing.");
            return;
        }

        UiccSlotInfo removableSlotInfo = getRemovableUiccSlotInfo();
        if (removableSlotInfo == null) {
            Log.e(TAG, "Unable to find the removable slot. Do nothing.");
            return;
        }

        int lastRemovableSlotState = getLastRemovableSimSlotState(mContext);
        int currentRemovableSlotState = removableSlotInfo.getCardStateInfo();
        boolean isRemovableSimInserted =
                lastRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_ABSENT
                        && currentRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_PRESENT;
        boolean isRemovableSimRemoved =
                lastRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_PRESENT
                        && currentRemovableSlotState == UiccSlotInfo.CARD_STATE_INFO_ABSENT;

        // Sets the current removable slot state.
        setRemovableSimSlotState(mContext, currentRemovableSlotState);

        if (mTelMgr.getActiveModemCount() > 1 && isMultipleEnabledProfilesSupported()) {
            if(!isRemovableSimInserted) {
                Log.i(TAG, "Removable Sim is not inserted in DSDS mode and MEP. Do nothing.");
                return;
            }
            handleRemovableSimInsertUnderDsdsMep(removableSlotInfo);
            return;
        }

        if (isRemovableSimInserted) {
            handleSimInsert(removableSlotInfo);
            return;
        }
        if (isRemovableSimRemoved) {
            handleSimRemove(removableSlotInfo);
            return;
        }
        Log.i(TAG, "Do nothing on slot status changes.");
    }

    void onSuwFinish(Context context) {
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

        boolean embeddedSimExist = getGroupedEmbeddedSubscriptions().size() != 0;
        int removableSlotAction = getSuwRemovableSlotAction(mContext);
        setSuwRemovableSlotAction(mContext, LAST_USER_ACTION_IN_SUW_NONE);

        if (embeddedSimExist
                && removableSlotInfo.getCardStateInfo() == UiccSlotInfo.CARD_STATE_INFO_PRESENT) {
            if (mTelMgr.isMultiSimSupported() == TelephonyManager.MULTISIM_ALLOWED) {
                Log.i(TAG, "DSDS condition satisfied. Show notification.");
                SimNotificationService.scheduleSimNotification(
                        mContext, SimActivationNotifier.NotificationType.ENABLE_DSDS);
            } else if (removableSlotAction == LAST_USER_ACTION_IN_SUW_INSERT) {
                Log.i(
                        TAG,
                        "Both removable SIM and eSIM are present. DSDS condition doesn't"
                            + " satisfied. User inserted pSIM during SUW. Show choose SIM"
                            + " screen.");
                startChooseSimActivity(true);
            }
        } else if (removableSlotAction == LAST_USER_ACTION_IN_SUW_REMOVE) {
            handleSimRemove(removableSlotInfo);
        }
    }

    private void init(Context context) {
        mSubMgr =
                (SubscriptionManager)
                        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        mTelMgr = context.getSystemService(TelephonyManager.class);
        mContext = context;
    }

    private void handleSimInsert(UiccSlotInfo removableSlotInfo) {
        Log.i(TAG, "Handle SIM inserted.");
        if (!isSuwFinished(mContext)) {
            Log.i(TAG, "Still in SUW. Handle SIM insertion after SUW is finished");
            setSuwRemovableSlotAction(mContext, LAST_USER_ACTION_IN_SUW_INSERT);
            return;
        }
        if (removableSlotInfo.getPorts().stream().findFirst().get().isActive()) {
            Log.i(TAG, "The removable slot is already active. Do nothing.");
            return;
        }

        if (hasActiveEsimSubscription()) {
            if (mTelMgr.isMultiSimSupported() == TelephonyManager.MULTISIM_ALLOWED) {
                Log.i(TAG, "Enabled profile exists. DSDS condition satisfied.");
                startDsdsDialogActivity();
            } else {
                Log.i(TAG, "Enabled profile exists. DSDS condition not satisfied.");
                startChooseSimActivity(true);
            }
            return;
        }

        Log.i(
                TAG,
                "No enabled eSIM profile. Ready to switch to removable slot and show"
                        + " notification.");
        try {
            UiccSlotUtil.switchToRemovableSlot(
                    UiccSlotUtil.INVALID_PHYSICAL_SLOT_ID, mContext.getApplicationContext());
        } catch (UiccSlotsException e) {
            Log.e(TAG, "Failed to switch to removable slot.");
            return;
        }
        SimNotificationService.scheduleSimNotification(
                mContext, SimActivationNotifier.NotificationType.SWITCH_TO_REMOVABLE_SLOT);
    }

    private void handleSimRemove(UiccSlotInfo removableSlotInfo) {
        Log.i(TAG, "Handle SIM removed.");

        if (!isSuwFinished(mContext)) {
            Log.i(TAG, "Still in SUW. Handle SIM removal after SUW is finished");
            setSuwRemovableSlotAction(mContext, LAST_USER_ACTION_IN_SUW_REMOVE);
            return;
        }

        List<SubscriptionInfo> groupedEmbeddedSubscriptions = getGroupedEmbeddedSubscriptions();
        if (groupedEmbeddedSubscriptions.size() == 0 || !removableSlotInfo.getPorts().stream()
                .findFirst().get().isActive()) {
            Log.i(TAG, "eSIM slot is active or no subscriptions exist. Do nothing."
                            + " The removableSlotInfo: " + removableSlotInfo
                            + ", groupedEmbeddedSubscriptions: " + groupedEmbeddedSubscriptions);
            return;
        }

        // If there is only 1 eSIM profile exists, we ask the user if they want to switch to that
        // profile.
        if (groupedEmbeddedSubscriptions.size() == 1) {
            Log.i(TAG, "Only 1 eSIM profile found. Ask user's consent to switch.");
            startSwitchSlotConfirmDialogActivity(groupedEmbeddedSubscriptions.get(0));
            return;
        }

        // If there are more than 1 eSIM profiles installed, we show a screen to let users to choose
        // the number they want to use.
        Log.i(TAG, "Multiple eSIM profiles found. Ask user which subscription to use.");
        startChooseSimActivity(false);
    }

    private void handleRemovableSimInsertUnderDsdsMep(UiccSlotInfo removableSlotInfo) {
        Log.i(TAG, "Handle Removable SIM inserted under DSDS+Mep.");

        if (removableSlotInfo.getPorts().stream().findFirst().get().isActive()) {
            Log.i(TAG, "The removable slot is already active. Do nothing. removableSlotInfo: "
                    + removableSlotInfo);
            return;
        }

        List<SubscriptionInfo> subscriptionInfos = getAvailableRemovableSubscription();
        if (subscriptionInfos == null || subscriptionInfos.get(0) == null) {
            Log.e(TAG, "Unable to find the removable subscriptionInfo. Do nothing.");
            return;
        }
        Log.d(TAG, "getAvailableRemovableSubscription:" + subscriptionInfos);
        startSimConfirmDialogActivity(subscriptionInfos.get(0).getSubscriptionId());
    }

    private int getLastRemovableSimSlotState(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(EUICC_PREFS, MODE_PRIVATE);
        return prefs.getInt(KEY_REMOVABLE_SLOT_STATE, UiccSlotInfo.CARD_STATE_INFO_ABSENT);
    }

    private void setRemovableSimSlotState(Context context, int state) {
        final SharedPreferences prefs = context.getSharedPreferences(EUICC_PREFS, MODE_PRIVATE);
        prefs.edit().putInt(KEY_REMOVABLE_SLOT_STATE, state).apply();
    }

    private int getSuwRemovableSlotAction(Context context) {
        final SharedPreferences prefs = context.getSharedPreferences(EUICC_PREFS, MODE_PRIVATE);
        return prefs.getInt(KEY_SUW_PSIM_ACTION, LAST_USER_ACTION_IN_SUW_NONE);
    }

    private void setSuwRemovableSlotAction(Context context, int action) {
        final SharedPreferences prefs = context.getSharedPreferences(EUICC_PREFS, MODE_PRIVATE);
        prefs.edit().putInt(KEY_SUW_PSIM_ACTION, action).apply();
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

    private static boolean isSuwFinished(Context context) {
        try {
            // DEVICE_PROVISIONED is 0 if still in setup wizard. 1 if setup completed.
            return Settings.Global.getInt(
                            context.getContentResolver(), Settings.Global.DEVICE_PROVISIONED)
                    == 1;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Cannot get DEVICE_PROVISIONED from the device.", e);
            return false;
        }
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

    protected List<SubscriptionInfo> getAvailableRemovableSubscription() {
        List<SubscriptionInfo> subList = new ArrayList<>();
        for (SubscriptionInfo info : SubscriptionUtil.getAvailableSubscriptions(mContext)) {
            if (!info.isEmbedded()) {
                subList.add(info);
            }
        }
        return subList;
    }

    private void startChooseSimActivity(boolean psimInserted) {
        Intent intent = ChooseSimActivity.getIntent(mContext);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(ChooseSimActivity.KEY_HAS_PSIM, psimInserted);
        mContext.startActivity(intent);
    }

    private void startSwitchSlotConfirmDialogActivity(SubscriptionInfo subscriptionInfo) {
        Intent intent = new Intent(mContext, SwitchToEsimConfirmDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SwitchToEsimConfirmDialogActivity.KEY_SUB_TO_ENABLE, subscriptionInfo);
        mContext.startActivity(intent);
    }

    private void startDsdsDialogActivity() {
        Intent intent = new Intent(mContext, DsdsDialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private void startSimConfirmDialogActivity(int subId) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            Log.i(TAG, "Unable to enable subscription due to invalid subscription ID.");
            return;
        }
        Log.d(TAG, "Start ToggleSubscriptionDialogActivity with " + subId + " under DSDS+Mep.");
        Intent intent = ToggleSubscriptionDialogActivity.getIntent(mContext, subId, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    private boolean isMultipleEnabledProfilesSupported() {
        List<UiccCardInfo> cardInfos = mTelMgr.getUiccCardsInfo();
        if (cardInfos == null) {
            Log.d(TAG, "UICC cards info list is empty.");
            return false;
        }
        return cardInfos.stream().anyMatch(
                cardInfo -> cardInfo.isMultipleEnabledProfilesSupported());
    }

    private SimSlotChangeHandler() {}
}
