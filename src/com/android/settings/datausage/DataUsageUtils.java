/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.settings.datausage;

import static android.net.ConnectivityManager.TYPE_WIFI;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkTemplate;
import android.net.TrafficStats;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;

import java.util.List;

/**
 * Utility methods for data usage classes.
 */
public final class DataUsageUtils {
    static final boolean TEST_RADIOS = false;
    static final String TEST_RADIOS_PROP = "test.radios";
    private static final String ETHERNET = "ethernet";

    private DataUsageUtils() {
    }

    /**
     * Format byte value to readable string using IEC units.
     */
    public static CharSequence formatDataUsage(Context context, long byteValue) {
        final BytesResult res = Formatter.formatBytes(context.getResources(), byteValue,
                Formatter.FLAG_IEC_UNITS);
        return BidiFormatter.getInstance().unicodeWrap(context.getString(
                com.android.internal.R.string.fileSizeSuffix, res.value, res.units));
    }

    /**
     * Test if device has an ethernet network connection.
     */
    public static boolean hasEthernet(Context context) {
        if (DataUsageUtils.TEST_RADIOS) {
            return SystemProperties.get(DataUsageUtils.TEST_RADIOS_PROP).contains(ETHERNET);
        }

        final ConnectivityManager conn = ConnectivityManager.from(context);
        final boolean hasEthernet = conn.isNetworkSupported(ConnectivityManager.TYPE_ETHERNET);

        final long ethernetBytes;
        try {
            INetworkStatsService statsService = INetworkStatsService.Stub.asInterface(
                    ServiceManager.getService(Context.NETWORK_STATS_SERVICE));

            INetworkStatsSession statsSession = statsService.openSession();
            if (statsSession != null) {
                ethernetBytes = statsSession.getSummaryForNetwork(
                        NetworkTemplate.buildTemplateEthernet(), Long.MIN_VALUE, Long.MAX_VALUE)
                        .getTotalBytes();
                TrafficStats.closeQuietly(statsSession);
            } else {
                ethernetBytes = 0;
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

        // only show ethernet when both hardware present and traffic has occurred
        return hasEthernet && ethernetBytes > 0;
    }

    /**
     * Returns whether device has mobile data.
     * TODO: This is the opposite to Utils.isWifiOnly(), it should be refactored into 1 method.
     */
    public static boolean hasMobileData(Context context) {
        ConnectivityManager connectivityManager = ConnectivityManager.from(context);
        return connectivityManager != null && connectivityManager
                .isNetworkSupported(ConnectivityManager.TYPE_MOBILE);
    }

    /**
     * Whether device has a Wi-Fi data radio.
     */
    public static boolean hasWifiRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("wifi");
        }

        ConnectivityManager connectivityManager =
                context.getSystemService(ConnectivityManager.class);
        return connectivityManager != null && connectivityManager.isNetworkSupported(TYPE_WIFI);
    }

    public static boolean hasSim(Context context) {
        TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        final int simState = telephonyManager.getSimState();
        // Note that pulling the SIM card returns UNKNOWN, not ABSENT.
        return simState != TelephonyManager.SIM_STATE_ABSENT
                && simState != TelephonyManager.SIM_STATE_UNKNOWN;
    }

    /**
     * Returns the default subscription if available else returns
     * SubscriptionManager#INVALID_SUBSCRIPTION_ID
     */
    public static int getDefaultSubscriptionId(Context context) {
        SubscriptionManager subManager = SubscriptionManager.from(context);
        if (subManager == null) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        SubscriptionInfo subscriptionInfo = subManager.getDefaultDataSubscriptionInfo();
        if (subscriptionInfo == null) {
            List<SubscriptionInfo> list = subManager.getAllSubscriptionInfoList();
            if (list.size() == 0) {
                return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            }
            subscriptionInfo = list.get(0);
        }
        return subscriptionInfo.getSubscriptionId();
    }

    /**
     * Returns the default network template based on the availability of mobile data, Wifi. Returns
     * ethernet template if both mobile data and Wifi are not available.
     */
    static NetworkTemplate getDefaultTemplate(Context context, int defaultSubId) {
        if (hasMobileData(context) && defaultSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            TelephonyManager telephonyManager = TelephonyManager.from(context);
            NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                    telephonyManager.getSubscriberId(defaultSubId));
            return NetworkTemplate.normalize(mobileAll,
                    telephonyManager.getMergedSubscriberIds());
        } else if (hasWifiRadio(context)) {
            return NetworkTemplate.buildTemplateWifiWildcard();
        } else {
            return NetworkTemplate.buildTemplateEthernet();
        }
    }
}
