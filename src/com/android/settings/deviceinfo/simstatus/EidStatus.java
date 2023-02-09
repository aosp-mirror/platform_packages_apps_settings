/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.deviceinfo.simstatus;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.euicc.EuiccManager;
import android.text.TextUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

/**
 * A class for query EID.
 */
public class EidStatus {

    private static final String TAG = "EidStatus";
    private final SlotSimStatus mSlotSimStatus;
    private final Phaser mBlocker = new Phaser(1);
    private final AtomicReference<String> mEid = new AtomicReference<String>();

    /**
     * Construct of class.
     * @param slotSimStatus SlotSimStatus
     * @param context Context
     */
    public EidStatus(SlotSimStatus slotSimStatus, Context context) {
        this(slotSimStatus, context, null);
    }

    /**
     * Construct of class.
     * @param slotSimStatus SlotSimStatus
     * @param context Context
     * @param executor executor for offload to thread
     */
    public EidStatus(SlotSimStatus slotSimStatus, Context context, Executor executor) {
        mSlotSimStatus = slotSimStatus;

        if (executor == null) {
            getEidOperation(context);
        } else {
            executor.execute(() -> getEidOperation(context));
        }
    }

    /**
     * Get the EID
     * @return EID string
     */
    public String getEid() {
        mBlocker.awaitAdvance(0);
        return mEid.get();
    }

    protected TelephonyManager getTelephonyManager(Context context) {
        return context.getSystemService(TelephonyManager.class);
    }

    protected EuiccManager getEuiccManager(Context context) {
        return context.getSystemService(EuiccManager.class);
    }

    protected String getDefaultEid(EuiccManager euiccMgr) {
        if ((euiccMgr == null) || (!euiccMgr.isEnabled())) {
            return null;
        }
        return euiccMgr.getEid();
    }

    protected void getEidOperation(Context context) {
        EuiccManager euiccMgr = getEuiccManager(context);
        String eid = getEidPerSlot(context, euiccMgr);
        if (eid == null) {
            eid = getDefaultEid(euiccMgr);
        }
        mEid.set(eid);
        mBlocker.arrive();
    }

    protected String getEidPerSlot(Context context, EuiccManager euiccMgr) {
        if (mSlotSimStatus.size() <= SimStatusDialogController.MAX_PHONE_COUNT_SINGLE_SIM) {
            return null;
        }

        TelephonyManager telMgr = getTelephonyManager(context);
        if (telMgr == null) {
            return null;
        }

        List<UiccCardInfo> uiccCardInfoList = telMgr.getUiccCardsInfo();
        if (uiccCardInfoList == null) {
            return null;
        }

        // Collects all card ID from all eSIM(s) reported from SubscsriptionManager
        final int [] cardIdList = IntStream.range(0, mSlotSimStatus.size())
                .mapToObj(slotIdx -> mSlotSimStatus.getSubscriptionInfo(slotIdx))
                .filter(Objects::nonNull)
                .filter(SubscriptionInfo::isEmbedded)
                .mapToInt(SubscriptionInfo::getCardId)
                .sorted()
                .distinct()
                .toArray();
        if (cardIdList.length == 0) {
            return null;
        }

        /**
         * Find EID from first slot which contains an eSIM and with card ID listed within
         * the eSIM card ID provided by SubscsriptionManager.
         */
        return uiccCardInfoList.stream()
                .filter(UiccCardInfo::isEuicc)
                .filter(cardInfo -> {
                    int cardId = cardInfo.getCardId();
                    return Arrays.binarySearch(cardIdList, cardId) >= 0;
                })
                .map(cardInfo -> {
                    String eid = cardInfo.getEid();
                    if (TextUtils.isEmpty(eid)) {
                        eid = euiccMgr.createForCardId(cardInfo.getCardId()).getEid();
                    }
                    return eid;
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

}
