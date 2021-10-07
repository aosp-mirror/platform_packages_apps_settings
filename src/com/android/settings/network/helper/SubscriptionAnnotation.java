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
package com.android.settings.network.helper;

import android.content.Context;
import android.os.ParcelUuid;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;

import com.android.settings.network.SubscriptionUtil;

import java.util.List;

/**
 * This is a class helps providing additional info required by UI
 * based on SubscriptionInfo.
 */
public class SubscriptionAnnotation {
    private static final String TAG = "SubscriptionAnnotation";

    private SubscriptionInfo mSubInfo;
    private int mOrderWithinList;
    private int mType = TYPE_UNKNOWN;
    private boolean mIsExisted;
    private boolean mIsActive;
    private boolean mIsAllowToDisplay;

    public static final ParcelUuid EMPTY_UUID = ParcelUuid.fromString("0-0-0-0-0");

    public static final int TYPE_UNKNOWN = 0x0;
    public static final int TYPE_PSIM = 0x1;
    public static final int TYPE_ESIM = 0x2;

    /**
     * Builder class for SubscriptionAnnotation
     */
    public static class Builder {

        private List<SubscriptionInfo> mSubInfoList;
        private int mIndexWithinList;

        /**
         * Constructor of builder
         * @param subInfoList list of subscription info
         * @param indexWithinList target index within list provided
         */
        public Builder(List<SubscriptionInfo> subInfoList, int indexWithinList) {
            mSubInfoList = subInfoList;
            mIndexWithinList = indexWithinList;
        }

        public SubscriptionAnnotation build(Context context, List<Integer> eSimCardId,
                List<Integer> simSlotIndex, List<Integer> activeSimSlotIndex) {
            return new SubscriptionAnnotation(mSubInfoList, mIndexWithinList, context,
                    eSimCardId, simSlotIndex, activeSimSlotIndex);
        }
    }

    /**
     * Constructor of class
     */
    @Keep
    @VisibleForTesting
    protected SubscriptionAnnotation(List<SubscriptionInfo> subInfoList, int subInfoIndex,
            Context context, List<Integer> eSimCardId,
            List<Integer> simSlotIndex, List<Integer> activeSimSlotIndexList) {
        if ((subInfoIndex < 0) || (subInfoIndex >= subInfoList.size())) {
            return;
        }
        mSubInfo = subInfoList.get(subInfoIndex);
        if (mSubInfo == null) {
            return;
        }

        mOrderWithinList = subInfoIndex;
        mType = mSubInfo.isEmbedded() ? TYPE_ESIM : TYPE_PSIM;
        mIsExisted = true;
        if (mType == TYPE_ESIM) {
            int cardId = mSubInfo.getCardId();
            mIsActive = activeSimSlotIndexList.contains(mSubInfo.getSimSlotIndex());
            mIsAllowToDisplay = (cardId < 0)    // always allow when eSIM not in slot
                  || isDisplayAllowed(context);
            return;
        }

        mIsActive = (mSubInfo.getSimSlotIndex() > SubscriptionManager.INVALID_SIM_SLOT_INDEX)
            && activeSimSlotIndexList.contains(mSubInfo.getSimSlotIndex());
        mIsAllowToDisplay = isDisplayAllowed(context);
    }

    // the index provided during construction of Builder
    @Keep
    public int getOrderingInList() {
        return mOrderWithinList;
    }

    // type of subscription
    @Keep
    public int getType() {
        return mType;
    }

    // if a subscription is existed within device
    @Keep
    public boolean isExisted() {
        return mIsExisted;
    }

    // if a subscription is currently ON
    @Keep
    public boolean isActive() {
        return mIsActive;
    }

    // if display of subscription is allowed
    @Keep
    public boolean isDisplayAllowed() {
        return mIsAllowToDisplay;
    }

    // the subscription ID
    @Keep
    public int getSubscriptionId() {
        return (mSubInfo == null) ? SubscriptionManager.INVALID_SUBSCRIPTION_ID :
                mSubInfo.getSubscriptionId();
    }

    // the grouping UUID
    @Keep
    public ParcelUuid getGroupUuid() {
        return (mSubInfo == null) ? null : mSubInfo.getGroupUuid();
    }

    // the SubscriptionInfo
    @Keep
    public SubscriptionInfo getSubInfo() {
        return mSubInfo;
    }

    private boolean isDisplayAllowed(Context context) {
        return SubscriptionUtil.isSubscriptionVisible(
                context.getSystemService(SubscriptionManager.class), context, mSubInfo);
    }

    public String toString() {
        return TAG + "{" + "subId=" + getSubscriptionId()
                + ",type=" + getType() + ",exist=" + isExisted()
                + ",active=" + isActive() + ",displayAllow=" + isDisplayAllowed()
                + "}";
    }
}