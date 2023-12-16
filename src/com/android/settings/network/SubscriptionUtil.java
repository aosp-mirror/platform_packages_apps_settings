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
import static android.telephony.SubscriptionManager.PROFILE_CLASS_PROVISIONING;

import static com.android.internal.util.CollectionUtils.emptyIfNull;

import android.annotation.Nullable;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccSlotInfo;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.internal.telephony.MccTable;
import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.network.helper.SelectableSubscriptions;
import com.android.settings.network.helper.SubscriptionAnnotation;
import com.android.settings.network.telephony.DeleteEuiccSubscriptionDialogActivity;
import com.android.settings.network.telephony.ToggleSubscriptionDialogActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SubscriptionUtil {
    private static final String TAG = "SubscriptionUtil";
    private static final String PROFILE_GENERIC_DISPLAY_NAME = "CARD";
    @VisibleForTesting
    static final String SUB_ID = "sub_id";
    @VisibleForTesting
    static final String KEY_UNIQUE_SUBSCRIPTION_DISPLAYNAME = "unique_subscription_displayName";
    private static final String REGEX_DISPLAY_NAME_PREFIXES = "^";
    private static final String REGEX_DISPLAY_NAME_SUFFIXES = "\\s[0-9]+";

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
        //TODO (b/315499317) : Refactor the subscription utils.

        if (sActiveResultsForTesting != null) {
            return sActiveResultsForTesting;
        }
        if (manager == null) {
            return Collections.emptyList();
        }
        final List<SubscriptionInfo> subscriptions = manager.getActiveSubscriptionInfoList();
        if (subscriptions == null) {
            return new ArrayList<>();
        }
        // Since the SubscriptionManager.getActiveSubscriptionInfoList() has checked whether the
        // sim visible by the SubscriptionManager.isSubscriptionVisible(), here only checks whether
        // the esim visible here.
        return subscriptions.stream()
                .filter(subInfo -> subInfo != null && isEmbeddedSubscriptionVisible(subInfo))
                .collect(Collectors.toList());
    }

    /**
     * Check if SIM hardware is visible to the end user.
     */
    public static boolean isSimHardwareVisible(Context context) {
        return context.getResources()
            .getBoolean(R.bool.config_show_sim_info);
    }

    @VisibleForTesting
    static boolean isInactiveInsertedPSim(UiccSlotInfo slotInfo) {
        if (slotInfo == null)  {
            return false;
        }
        return !slotInfo.getIsEuicc() && !slotInfo.getPorts().stream().findFirst().get()
                .isActive() && slotInfo.getCardStateInfo() == CARD_STATE_INFO_PRESENT;
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
        return new ArrayList<>(emptyIfNull(getSelectableSubscriptionInfoList(context)));
    }

    /**
     * Get subscriptionInfo which is available to be displayed to the user
     * per subscription id.
     *
     * @param context {@code Context}
     * @param subscriptionManager The ProxySubscriptionManager for accessing subcription
     *         information
     * @param subId The id of subscription to be retrieved
     * @return {@code SubscriptionInfo} based on the given subscription id. Null of subscription
     *         is invalid or not allowed to be displayed to the user.
     */
    public static SubscriptionInfo getAvailableSubscriptionBySubIdAndShowingForUser(Context context,
            ProxySubscriptionManager subscriptionManager, int subId) {
        //TODO (b/315499317) : Refactor the subscription utils.
        final SubscriptionInfo subInfo = subscriptionManager.getAccessibleSubscriptionInfo(subId);
        if (subInfo == null) {
            return null;
        }

        // hide provisioning/bootstrap and satellite profiles for user
        if (!isEmbeddedSubscriptionVisible(subInfo)) {
            Log.d(TAG, "Do not insert the provision eSIM or NTN eSim");
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
                        && (slotInfo.getPorts().stream().findFirst().get().getLogicalSlotIndex()
                        == subInfo.getSimSlotIndex())) {
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

    /**
     * Return a mapping of active subscription ids to display names. Each display name is
     * guaranteed to be unique in the following manner:
     * 1) If the original display name is not unique, the last four digits of the phone number
     *    will be appended.
     * 2) If the phone number is not visible or the last four digits are shared with another
     *    subscription, the subscription id will be appended to the original display name.
     * More details can be found at go/unique-sub-display-names.
     *
     * @return map of active subscription ids to display names.
     */
    @VisibleForTesting
    public static Map<Integer, CharSequence> getUniqueSubscriptionDisplayNames(Context context) {
        class DisplayInfo {
            public SubscriptionInfo subscriptionInfo;
            public CharSequence originalName;
            public CharSequence uniqueName;
        }

        // Map of SubscriptionId to DisplayName
        final Supplier<Stream<DisplayInfo>> originalInfos =
                () -> getAvailableSubscriptions(context)
                        .stream()
                        .filter(i -> {
                            // Filter out null values.
                            return (i != null && i.getDisplayName() != null);
                        })
                        .map(i -> {
                            DisplayInfo info = new DisplayInfo();
                            info.subscriptionInfo = i;
                            String displayName = i.getDisplayName().toString();
                            info.originalName =
                                    TextUtils.equals(displayName, PROFILE_GENERIC_DISPLAY_NAME)
                                            ? context.getResources().getString(R.string.sim_card)
                                            : displayName.trim();
                            return info;
                        });

        // TODO(goldmanj) consider using a map of DisplayName to SubscriptionInfos.
        // A Unique set of display names
        Set<CharSequence> uniqueNames = new HashSet<>();
        // Return the set of duplicate names
        final Set<CharSequence> duplicateOriginalNames = originalInfos.get()
                .filter(info -> !uniqueNames.add(info.originalName))
                .map(info -> info.originalName)
                .collect(Collectors.toSet());

        // If a display name is duplicate, append the final 4 digits of the phone number.
        // Creates a mapping of Subscription id to original display name + phone number display name
        final Supplier<Stream<DisplayInfo>> uniqueInfos = () -> originalInfos.get().map(info -> {
            int infoSubId = info.subscriptionInfo.getSubscriptionId();
            String cachedDisplayName = getDisplayNameFromSharedPreference(
                    context, infoSubId);
            if (isValidCachedDisplayName(cachedDisplayName, info.originalName.toString())) {
                Log.d(TAG, "use cached display name : for subId : " + infoSubId
                        + "cached display name : " + cachedDisplayName);
                info.uniqueName = cachedDisplayName;
                return info;
            } else {
                Log.d(TAG, "remove cached display name : " + infoSubId);
                removeItemFromDisplayNameSharedPreference(context, infoSubId);
            }

            if (duplicateOriginalNames.contains(info.originalName)) {
                // This may return null, if the user cannot view the phone number itself.
                final String phoneNumber = getBidiFormattedPhoneNumber(context,
                        info.subscriptionInfo);
                String lastFourDigits = "";
                if (phoneNumber != null) {
                    lastFourDigits = (phoneNumber.length() > 4)
                            ? phoneNumber.substring(phoneNumber.length() - 4) : phoneNumber;
                }
                if (TextUtils.isEmpty(lastFourDigits)) {
                    info.uniqueName = info.originalName;
                } else {
                    info.uniqueName = info.originalName + " " + lastFourDigits;
                    Log.d(TAG, "Cache display name [" + info.uniqueName + "] for sub id "
                            + infoSubId);
                    saveDisplayNameToSharedPreference(context, infoSubId, info.uniqueName);
                }
            } else {
                info.uniqueName = info.originalName;
            }
            return info;
        });

        // Check uniqueness a second time.
        // We might not have had permission to view the phone numbers.
        // There might also be multiple phone numbers whose last 4 digits the same.
        uniqueNames.clear();
        final Set<CharSequence> duplicatePhoneNames = uniqueInfos.get()
                .filter(info -> !uniqueNames.add(info.uniqueName))
                .map(info -> info.uniqueName)
                .collect(Collectors.toSet());

        return uniqueInfos.get().map(info -> {
            if (duplicatePhoneNames.contains(info.uniqueName)) {
                info.uniqueName = info.originalName + " "
                        + info.subscriptionInfo.getSubscriptionId();
            }
            return info;
        }).collect(Collectors.toMap(
                info -> info.subscriptionInfo.getSubscriptionId(),
                info -> info.uniqueName));
    }

    /**
     * Return the display name for a subscription id, which is guaranteed to be unique.
     * The logic to create this name has the following order of operations:
     * 1) If the original display name is not unique, the last four digits of the phone number
     *    will be appended.
     * 2) If the phone number is not visible or the last four digits are shared with another
     *    subscription, the subscription id will be appended to the original display name.
     * More details can be found at go/unique-sub-display-names.
     *
     * @return map of active subscription ids to display names.
     */
    @VisibleForTesting
    public static CharSequence getUniqueSubscriptionDisplayName(
            Integer subscriptionId, Context context) {
        final Map<Integer, CharSequence> displayNames = getUniqueSubscriptionDisplayNames(context);
        return displayNames.getOrDefault(subscriptionId, "");
    }

    /**
     * Return the display name for a subscription, which is guaranteed to be unique.
     * The logic to create this name has the following order of operations:
     * 1) If the original display name is not unique, the last four digits of the phone number
     *    will be appended.
     * 2) If the phone number is not visible or the last four digits are shared with another
     *    subscription, the subscription id will be appended to the original display name.
     * More details can be found at go/unique-sub-display-names.
     *
     * @return map of active subscription ids to display names.
     */
    public static CharSequence getUniqueSubscriptionDisplayName(
            SubscriptionInfo info, Context context) {
        if (info == null) {
            return "";
        }
        return getUniqueSubscriptionDisplayName(info.getSubscriptionId(), context);
    }


    private static SharedPreferences getDisplayNameSharedPreferences(Context context) {
        return context.getSharedPreferences(
                KEY_UNIQUE_SUBSCRIPTION_DISPLAYNAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences.Editor getDisplayNameSharedPreferenceEditor(Context context) {
        return getDisplayNameSharedPreferences(context).edit();
    }

    private static void saveDisplayNameToSharedPreference(
            Context context, int subId, CharSequence displayName) {
        getDisplayNameSharedPreferenceEditor(context)
                .putString(SUB_ID + subId, String.valueOf(displayName))
                .apply();
    }

    private static void removeItemFromDisplayNameSharedPreference(Context context, int subId) {
        getDisplayNameSharedPreferenceEditor(context)
                .remove(SUB_ID + subId)
                .commit();
    }

    private static String getDisplayNameFromSharedPreference(Context context, int subid) {
        return getDisplayNameSharedPreferences(context).getString(SUB_ID + subid, "");
    }

    @VisibleForTesting
    static boolean isValidCachedDisplayName(String cachedDisplayName, String originalName) {
        if (TextUtils.isEmpty(cachedDisplayName) || TextUtils.isEmpty(originalName)) {
            return false;
        }
        String regex = REGEX_DISPLAY_NAME_PREFIXES + originalName + REGEX_DISPLAY_NAME_SUFFIXES;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(cachedDisplayName);
        return matcher.matches();
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

    /**
     * Return a list of subscriptions that are available and visible to the user.
     *
     * @return list of user selectable subscriptions.
     */
    public static List<SubscriptionInfo> getSelectableSubscriptionInfoList(Context context) {
        SubscriptionManager subManager = context.getSystemService(SubscriptionManager.class);
        List<SubscriptionInfo> availableList = subManager.getAvailableSubscriptionInfoList();
        if (availableList == null) {
            return null;
        } else {
            // Multiple subscriptions in a group should only have one representative.
            // It should be the current active primary subscription if any, or any
            // primary subscription.
            List<SubscriptionInfo> selectableList = new ArrayList<>();
            Map<ParcelUuid, SubscriptionInfo> groupMap = new HashMap<>();

            for (SubscriptionInfo info : availableList) {
                // Opportunistic subscriptions are considered invisible
                // to users so they should never be returned.
                if (!isSubscriptionVisible(subManager, context, info)) continue;

                ParcelUuid groupUuid = info.getGroupUuid();
                if (groupUuid == null) {
                    // Doesn't belong to any group. Add in the list.
                    selectableList.add(info);
                } else if (!groupMap.containsKey(groupUuid)
                        || (groupMap.get(groupUuid).getSimSlotIndex() == INVALID_SIM_SLOT_INDEX
                        && info.getSimSlotIndex() != INVALID_SIM_SLOT_INDEX)) {
                    // If it belongs to a group that has never been recorded or it's the current
                    // active subscription, add it in the list.
                    selectableList.remove(groupMap.get(groupUuid));
                    selectableList.add(info);
                    groupMap.put(groupUuid, info);
                }

            }
            Log.d(TAG, "getSelectableSubscriptionInfoList: " + selectableList);
            return selectableList;
        }
    }

    /**
     * Starts a dialog activity to handle SIM enabling/disabling.
     * @param context {@code Context}
     * @param subId The id of subscription need to be enabled or disabled.
     * @param enable Whether the subscription with {@code subId} should be enabled or disabled.
     */
    public static void startToggleSubscriptionDialogActivity(
            Context context, int subId, boolean enable) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            Log.i(TAG, "Unable to toggle subscription due to invalid subscription ID.");
            return;
        }
        context.startActivity(ToggleSubscriptionDialogActivity.getIntent(context, subId, enable));
    }

    /**
     * Starts a dialog activity to handle eSIM deletion.
     * @param context {@code Context}
     * @param subId The id of subscription need to be deleted.
     */
    public static void startDeleteEuiccSubscriptionDialogActivity(Context context, int subId) {
        if (!SubscriptionManager.isUsableSubscriptionId(subId)) {
            Log.i(TAG, "Unable to delete subscription due to invalid subscription ID.");
            return;
        }
        context.startActivity(DeleteEuiccSubscriptionDialogActivity.getIntent(context, subId));
    }

    /**
     * Finds and returns a subscription with a specific subscription ID.
     * @param subscriptionManager The ProxySubscriptionManager for accessing subscription
     *                            information
     * @param subId The id of subscription to be returned
     * @return the {@code SubscriptionInfo} whose ID is {@code subId}. It returns null if the
     * {@code subId} is {@code SubscriptionManager.INVALID_SUBSCRIPTION_ID} or no such
     * {@code SubscriptionInfo} is found.
     */
    @Nullable
    public static SubscriptionInfo getSubById(SubscriptionManager subscriptionManager, int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return null;
        }
        return subscriptionManager
                .getAllSubscriptionInfoList()
                .stream()
                .filter(subInfo -> subInfo.getSubscriptionId() == subId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Whether a subscription is visible to API caller. If it's a bundled opportunistic
     * subscription, it should be hidden anywhere in Settings, dialer, status bar etc.
     * Exception is if caller owns carrier privilege, in which case they will
     * want to see their own hidden subscriptions.
     *
     * @param info the subscriptionInfo to check against.
     * @return true if this subscription should be visible to the API caller.
     */
    public static boolean isSubscriptionVisible(
            SubscriptionManager subscriptionManager, Context context, SubscriptionInfo info) {
        if (info == null) return false;

        // hide provisioning/bootstrap and satellite profiles for user
        if (!isEmbeddedSubscriptionVisible(info)) {
            return false;
        }

        // If subscription is NOT grouped opportunistic subscription, it's visible.
        if (info.getGroupUuid() == null || !info.isOpportunistic()) return true;

        // If the caller is the carrier app and owns the subscription, it should be visible
        // to the caller.
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class)
                .createForSubscriptionId(info.getSubscriptionId());
        boolean hasCarrierPrivilegePermission = telephonyManager.hasCarrierPrivileges()
                || subscriptionManager.canManageSubscription(info);
        return hasCarrierPrivilegePermission;
    }

    /**
     * Finds all the available subscriptions having the same group uuid as {@code subscriptionInfo}.
     * @param subscriptionManager The SubscriptionManager for accessing subscription information
     * @param subId The id of subscription
     * @return a list of {@code SubscriptionInfo} which have the same group UUID.
     */
    public static List<SubscriptionInfo> findAllSubscriptionsInGroup(
            SubscriptionManager subscriptionManager, int subId) {

        SubscriptionInfo subscription = getSubById(subscriptionManager, subId);
        if (subscription == null) {
            return Collections.emptyList();
        }
        ParcelUuid groupUuid = subscription.getGroupUuid();
        List<SubscriptionInfo> availableSubscriptions =
                subscriptionManager.getAvailableSubscriptionInfoList();

        if (availableSubscriptions == null
                || availableSubscriptions.isEmpty()
                || groupUuid == null) {
            return Collections.singletonList(subscription);
        }

        return availableSubscriptions
                .stream()
                .filter(sub -> sub.isEmbedded() && groupUuid.equals(sub.getGroupUuid()))
                .collect(Collectors.toList());
    }

    /** Returns the formatted phone number of a subscription. */
    @Nullable
    public static String getFormattedPhoneNumber(
            Context context, SubscriptionInfo subscriptionInfo) {
        if (subscriptionInfo == null) {
            Log.e(TAG, "Invalid subscription.");
            return null;
        }

        final SubscriptionManager subscriptionManager = context.getSystemService(
                SubscriptionManager.class);
        String rawPhoneNumber = subscriptionManager.getPhoneNumber(
                subscriptionInfo.getSubscriptionId());
        if (TextUtils.isEmpty(rawPhoneNumber)) {
            return null;
        }
        String countryIso = MccTable.countryCodeForMcc(subscriptionInfo.getMccString())
                .toUpperCase(Locale.ROOT);
        return PhoneNumberUtils.formatNumber(rawPhoneNumber, countryIso);
    }

    /**
     * To get the formatting text for display in a potentially opposite-directionality context
     * without garbling.
     * @param subscriptionInfo {@link SubscriptionInfo} subscription information.
     * @return Returns phone number with Bidi format.
     */
    @Nullable
    public static String getBidiFormattedPhoneNumber(Context context,
            SubscriptionInfo subscriptionInfo) {
        String phoneNumber = getFormattedPhoneNumber(context, subscriptionInfo);
        return (phoneNumber == null) ? phoneNumber :
                BidiFormatter.getInstance().unicodeWrap(phoneNumber, TextDirectionHeuristics.LTR);
    }

    /**
     * Returns the subscription on a removable sim card. The device does not need to be on removable
     * slot.
     */
    @Nullable
    public static SubscriptionInfo getFirstRemovableSubscription(Context context) {
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        SubscriptionManager subscriptionManager =
                context.getSystemService(SubscriptionManager.class);
        List<UiccCardInfo> cardInfos = telephonyManager.getUiccCardsInfo();
        if (cardInfos == null) {
            Log.w(TAG, "UICC cards info list is empty.");
            return null;
        }
        List<SubscriptionInfo> allSubscriptions = subscriptionManager.getAllSubscriptionInfoList();
        if (allSubscriptions == null) {
            Log.w(TAG, "All subscription info list is empty.");
            return null;
        }
        for (UiccCardInfo cardInfo : cardInfos) {
            if (cardInfo == null) {
                Log.w(TAG, "Got null card.");
                continue;
            }
            if (!cardInfo.isRemovable()
                    || cardInfo.getCardId() == TelephonyManager.UNSUPPORTED_CARD_ID) {
                Log.i(TAG, "Skip embedded card or invalid cardId on slot: "
                        + cardInfo.getPhysicalSlotIndex());
                continue;
            }
            Log.i(TAG, "Target removable cardId :" + cardInfo.getCardId());
            for (SubscriptionInfo subInfo : allSubscriptions) {
                // Match the removable card id with subscription card id.
                if (cardInfo.getCardId() == subInfo.getCardId()) {
                    return subInfo;
                }
            }
        }
        return null;
    }

    public static CharSequence getDefaultSimConfig(Context context, int subId) {
        boolean isDefaultCall = subId == getDefaultVoiceSubscriptionId();
        boolean isDefaultSms = subId == getDefaultSmsSubscriptionId();
        boolean isDefaultData = subId == getDefaultDataSubscriptionId();

        if (!isDefaultData && !isDefaultCall && !isDefaultSms) {
            return "";
        }

        final StringBuilder defaultConfig = new StringBuilder();
        if (isDefaultData) {
            defaultConfig.append(
                    getResForDefaultConfig(context, R.string.default_active_sim_mobile_data))
                    .append(", ");
        }

        if (isDefaultCall) {
            defaultConfig.append(getResForDefaultConfig(context, R.string.default_active_sim_calls))
                    .append(", ");
        }

        if (isDefaultSms) {
            defaultConfig.append(getResForDefaultConfig(context, R.string.default_active_sim_sms))
                    .append(", ");
        }

        // Do not add ", " for the last config.
        defaultConfig.setLength(defaultConfig.length() - 2);

        final String summary = context.getResources().getString(
                R.string.sim_category_default_active_sim,
                defaultConfig);

        return summary;
    }

    private static String getResForDefaultConfig(Context context, int resId) {
        return context.getResources().getString(resId);
    }

    private static int getDefaultVoiceSubscriptionId() {
        return SubscriptionManager.getDefaultVoiceSubscriptionId();
    }

    private static int getDefaultSmsSubscriptionId() {
        return SubscriptionManager.getDefaultSmsSubscriptionId();
    }

    private static int getDefaultDataSubscriptionId() {
        return SubscriptionManager.getDefaultDataSubscriptionId();
    }


    /**
     * Select one of the subscription as the default subscription.
     * @param subAnnoList a list of {@link SubscriptionAnnotation}
     * @return ideally the {@link SubscriptionAnnotation} as expected
     */
    private static SubscriptionAnnotation getDefaultSubscriptionSelection(
            List<SubscriptionAnnotation> subAnnoList) {
        return (subAnnoList == null) ? null :
                subAnnoList.stream()
                        .filter(SubscriptionAnnotation::isDisplayAllowed)
                        .filter(SubscriptionAnnotation::isActive)
                        .findFirst().orElse(null);
    }

    public static boolean isDefaultSubscription(Context context, int subId) {
        SubscriptionAnnotation subInfo = getDefaultSubscriptionSelection(
                new SelectableSubscriptions(context, true).call());
        return subInfo != null && subInfo.getSubscriptionId() == subId;
    }

    public static SubscriptionInfo getSubscriptionOrDefault(Context context, int subscriptionId) {
        return getSubscription(context, subscriptionId,
                (subscriptionId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) ? null : (
                        subAnnoList -> getDefaultSubscriptionSelection(subAnnoList)
                ));
    }

    /**
     * Get the current subscription to display. First check whether intent has {@link
     * Settings#EXTRA_SUB_ID} and if so find the subscription with that id.
     * If not, select default one based on {@link Function} provided.
     *
     * @param preferredSubscriptionId preferred subscription id
     * @param selectionOfDefault when true current subscription is absent
     */
    private static SubscriptionInfo getSubscription(Context context, int preferredSubscriptionId,
            Function<List<SubscriptionAnnotation>, SubscriptionAnnotation> selectionOfDefault) {
        List<SubscriptionAnnotation> subList =
                (new SelectableSubscriptions(context, true)).call();
        Log.d(TAG, "get subId=" + preferredSubscriptionId + " from " + subList);
        SubscriptionAnnotation currentSubInfo = subList.stream()
                .filter(SubscriptionAnnotation::isDisplayAllowed)
                .filter(subAnno -> (subAnno.getSubscriptionId() == preferredSubscriptionId))
                .findFirst().orElse(null);
        if ((currentSubInfo == null) && (selectionOfDefault != null)) {
            currentSubInfo = selectionOfDefault.apply(subList);
        }
        return (currentSubInfo == null) ? null : currentSubInfo.getSubInfo();
    }

    private static boolean isEmbeddedSubscriptionVisible(@NonNull SubscriptionInfo subInfo) {
        if (subInfo.isEmbedded()
                && (subInfo.getProfileClass() == PROFILE_CLASS_PROVISIONING
                || (Flags.oemEnabledSatelliteFlag()
                && subInfo.isOnlyNonTerrestrialNetwork()))) {
            return false;
        }
        return true;
    }
}
