/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.network;

import static android.telephony.SubscriptionManager.INVALID_SIM_SLOT_INDEX;
import static android.telephony.UiccSlotInfo.CARD_STATE_INFO_PRESENT;

import static com.android.internal.util.CollectionUtils.emptyIfNull;

import android.content.Context;
import android.os.ParcelUuid;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccSlotInfo;
import android.text.TextUtils;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionUtil {
    private static final String TAG = "SubscriptionUtil";
    private static List<SubscriptionInfo> sAvailableResultsForTesting;
    private static List<SubscriptionInfo> sActiveResultsForTesting;

    @VisibleForTesting
    public static void setAvailableSubscriptionsForTesting(List<SubscriptionInfo> results) {
        sAvailableResultsForTesting = results;
    }

    @VisibleForTesting
    public static void setActiveSubscriptionsForTesting(List<SubscriptionInfo> results) {
        sActiveResultsForTesting = results;
    }

    public static List<SubscriptionInfo> getActiveSubscriptions(SubscriptionManager manager) {
        if (sActiveResultsForTesting != null) {
            return sActiveResultsForTesting;
        }
        final List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList();
        if (subscriptions == null) {
            return new ArrayList<>();
        }
        return subscriptions;
    }

    @VisibleForTesting
    static boolean isInactiveInsertedPSim(UiccSlotInfo slotInfo) {
        if (slotInfo == null)  {
            return false;
        }
        return !slotInfo.getIsEuicc() && !slotInfo.getIsActive() &&
                slotInfo.getCardStateInfo() == CARD_STATE_INFO_PRESENT;
    }

    /**
     * Get all of the subscriptions which is available to display to the user.
     *
     * @param context {@code Context}
     * @return list of {@code SubscriptionInfo}
     */
    public static List<SubscriptionInfo> getAvailableSubscriptions(Context context) {
        if (sAvailableResultsForTesting != null) {
            return sAvailableResultsForTesting;
        }
        final SubscriptionManager subMgr = context.getSystemService(SubscriptionManager.class);

        final List<SubscriptionInfo> subscriptions =
                new ArrayList<>(emptyIfNull(subMgr.getSelectableSubscriptionInfoList()));

        return subscriptions;
    }

    /**
     * Get subscription which is available to be displayed to the user
     * per subscription id.
     *
     * @param context {@code Context}
     * @param subscriptionManager The ProxySubscriptionManager for accessing subcription
     *         information
     * @param subId The id of subscription to be retrieved
     * @return {@code SubscriptionInfo} based on the given subscription id. Null of subscription
     *         is invalid or not allowed to be displayed to the user.
     */
    public static SubscriptionInfo getAvailableSubscription(Context context,
            ProxySubscriptionManager subscriptionManager, int subId) {
        final SubscriptionInfo subInfo = subscriptionManager.getAccessibleSubscriptionInfo(subId);
        if (subInfo == null) {
            return null;
        }

        final ParcelUuid groupUuid = subInfo.getGroupUuid();

        if (groupUuid != null) {
            if (isPrimarySubscriptionWithinSameUuid(getUiccSlotsInfo(context), groupUuid,
                    subscriptionManager.getAccessibleSubscriptionsInfo(), subId)) {
                return subInfo;
            }
            return null;
        }

        return subInfo;
    }

    private static UiccSlotInfo [] getUiccSlotsInfo(Context context) {
        final TelephonyManager telMgr = context.getSystemService(TelephonyManager.class);
        return telMgr.getUiccSlotsInfo();
    }

    private static boolean isPrimarySubscriptionWithinSameUuid(UiccSlotInfo[] slotsInfo,
            ParcelUuid groupUuid, List<SubscriptionInfo> subscriptions, int subId) {
        // only interested in subscriptions with this group UUID
        final ArrayList<SubscriptionInfo> physicalSubInfoList =
                new ArrayList<SubscriptionInfo>();
        final ArrayList<SubscriptionInfo> nonOpportunisticSubInfoList =
                new ArrayList<SubscriptionInfo>();
        final ArrayList<SubscriptionInfo> activeSlotSubInfoList =
                new ArrayList<SubscriptionInfo>();
        final ArrayList<SubscriptionInfo> inactiveSlotSubInfoList =
                new ArrayList<SubscriptionInfo>();
        for (SubscriptionInfo subInfo : subscriptions) {
            if (groupUuid.equals(subInfo.getGroupUuid())) {
                if (!subInfo.isEmbedded()) {
                    physicalSubInfoList.add(subInfo);
                } else  {
                    if (!subInfo.isOpportunistic()) {
                        nonOpportunisticSubInfoList.add(subInfo);
                    }
                    if (subInfo.getSimSlotIndex()
                            != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                        activeSlotSubInfoList.add(subInfo);
                    } else {
                        inactiveSlotSubInfoList.add(subInfo);
                    }
                }
            }
        }

        // find any physical SIM which is currently inserted within logical slot
        // and which is our target subscription
        if ((slotsInfo != null) && (physicalSubInfoList.size() > 0)) {
            final SubscriptionInfo subInfo = searchForSubscriptionId(physicalSubInfoList, subId);
            if (subInfo == null) {
                return false;
            }
            // verify if subscription is inserted within slot
            for (UiccSlotInfo slotInfo : slotsInfo) {
                if ((slotInfo != null) && (!slotInfo.getIsEuicc())
                        && (slotInfo.getCardStateInfo() == CARD_STATE_INFO_PRESENT)
                        && (slotInfo.getLogicalSlotIdx() == subInfo.getSimSlotIndex())
                        && TextUtils.equals(slotInfo.getCardId(), subInfo.getCardString())) {
                    return true;
                }
            }
            return false;
        }

        // When all of the eSIM profiles are opprtunistic and no physical SIM,
        // first opportunistic subscriptions with same group UUID can be primary.
        if (nonOpportunisticSubInfoList.size() <= 0) {
            if (physicalSubInfoList.size() > 0) {
                return false;
            }
            if (activeSlotSubInfoList.size() > 0) {
                return (activeSlotSubInfoList.get(0).getSubscriptionId() == subId);
            }
            return (inactiveSlotSubInfoList.get(0).getSubscriptionId() == subId);
        }

        // Allow non-opportunistic + active eSIM subscription as primary
        int numberOfActiveNonOpportunisticSubs = 0;
        boolean isTargetNonOpportunistic = false;
        for (SubscriptionInfo subInfo : nonOpportunisticSubInfoList) {
            final boolean isTargetSubInfo = (subInfo.getSubscriptionId() == subId);
            if (subInfo.getSimSlotIndex() != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                if (isTargetSubInfo) {
                    return true;
                }
                numberOfActiveNonOpportunisticSubs++;
            } else {
                isTargetNonOpportunistic |= isTargetSubInfo;
            }
        }
        if (numberOfActiveNonOpportunisticSubs > 0) {
            return false;
        }
        return isTargetNonOpportunistic;
    }

    private static SubscriptionInfo searchForSubscriptionId(List<SubscriptionInfo> subInfoList,
            int subscriptionId) {
        for (SubscriptionInfo subInfo : subInfoList) {
            if (subInfo.getSubscriptionId() == subscriptionId) {
                return subInfo;
            }
        }
        return null;
    }

    public static String getDisplayName(SubscriptionInfo info) {
        final CharSequence name = info.getDisplayName();
        if (name != null) {
            return name.toString();
        }
        return "";
    }

    /**
     * Whether Settings should show a "Use SIM" toggle in pSIM detailed page.
     */
    public static boolean showToggleForPhysicalSim(SubscriptionManager subMgr) {
        return subMgr.canDisablePhysicalSubscription();
    }

    /**
     * Get phoneId or logical slot index for a subId if active, or INVALID_PHONE_INDEX if inactive.
     */
    public static int getPhoneId(Context context, int subId) {
        final SubscriptionManager subManager = context.getSystemService(SubscriptionManager.class);
        if (subManager == null) {
            return INVALID_SIM_SLOT_INDEX;
        }
        final SubscriptionInfo info = subManager.getActiveSubscriptionInfo(subId);
        if (info == null) {
            return INVALID_SIM_SLOT_INDEX;
        }
        return info.getSimSlotIndex();
    }
}
