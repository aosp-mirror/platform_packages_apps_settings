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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settingslib.utils.ThreadUtils;

import java.util.List;

/** The receiver when the slot status changes. */
public class SimSlotChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "SlotChangeReceiver";

    private final SimSlotChangeHandler mSlotChangeHandler = SimSlotChangeHandler.get();
    private final Object mLock = new Object();

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if (!TelephonyManager.ACTION_SIM_SLOT_STATUS_CHANGED.equals(action)) {
            Log.e(TAG, "Ignore slot changes due to unexpected action: " + action);
            return;
        }

        final PendingResult pendingResult = goAsync();
        ThreadUtils.postOnBackgroundThread(
                () -> {
                    synchronized (mLock) {
                        if (shouldHandleSlotChange(context)) {
                            mSlotChangeHandler.onSlotsStatusChange(context.getApplicationContext());
                        }
                    }
                    ThreadUtils.postOnMainThread(pendingResult::finish);
                });
    }

    // Checks whether the slot event should be handled.
    private boolean shouldHandleSlotChange(Context context) {
        if (!context.getResources().getBoolean(R.bool.config_handle_sim_slot_change)) {
            Log.i(TAG, "The flag is off. Ignore slot changes.");
            return false;
        }

        final EuiccManager euiccManager = context.getSystemService(EuiccManager.class);
        if (euiccManager == null || !euiccManager.isEnabled()) {
            Log.i(TAG, "Ignore slot changes because EuiccManager is disabled.");
            return false;
        }

        if (euiccManager.getOtaStatus() == EuiccManager.EUICC_OTA_IN_PROGRESS) {
            Log.i(TAG, "Ignore slot changes because eSIM OTA is in progress.");
            return false;
        }

        if (!isSimSlotStateValid(context)) {
            Log.i(TAG, "Ignore slot changes because SIM states are not valid.");
            return false;
        }

        return true;
    }

    // Checks whether the SIM slot state is valid for slot change event.
    private boolean isSimSlotStateValid(Context context) {
        final TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        UiccSlotInfo[] slotInfos = telMgr.getUiccSlotsInfo();
        if (slotInfos == null) {
            Log.e(TAG, "slotInfos is null. Unable to get slot infos.");
            return false;
        }

        boolean isAllCardStringsEmpty = true;
        for (int i = 0; i < slotInfos.length; i++) {
            UiccSlotInfo slotInfo = slotInfos[i];

            if (slotInfo == null) {
                return false;
            }

            // After pSIM is inserted, there might be a short period that the status of both slots
            // are not accurate. We drop the event if any of sim presence state is ERROR or
            // RESTRICTED.
            if (slotInfo.getCardStateInfo() == UiccSlotInfo.CARD_STATE_INFO_ERROR
                    || slotInfo.getCardStateInfo() == UiccSlotInfo.CARD_STATE_INFO_RESTRICTED) {
                Log.i(TAG, "The SIM state is in an error. Drop the event. SIM info: " + slotInfo);
                return false;
            }

            UiccCardInfo cardInfo = findUiccCardInfoBySlot(telMgr, i);
            if (cardInfo == null) {
                continue;
            }
            for (UiccPortInfo portInfo : cardInfo.getPorts()) {
                if (!TextUtils.isEmpty(slotInfo.getCardId())
                        || !TextUtils.isEmpty(portInfo.getIccId())) {
                    isAllCardStringsEmpty = false;
                }
            }
        }

        // We also drop the event if both the card strings are empty, which usually means it's
        // between SIM slots switch the slot status is not stable at this moment.
        if (isAllCardStringsEmpty) {
            Log.i(TAG, "All UICC card strings are empty. Drop this event.");
            return false;
        }

        return true;
    }

    @Nullable
    private UiccCardInfo findUiccCardInfoBySlot(TelephonyManager telMgr, int physicalSlotIndex) {
        List<UiccCardInfo> cardInfos = telMgr.getUiccCardsInfo();
        if (cardInfos == null) {
            return null;
        }
        return cardInfos.stream()
                .filter(info -> info.getPhysicalSlotIndex() == physicalSlotIndex)
                .findFirst()
                .orElse(null);
    }
}
