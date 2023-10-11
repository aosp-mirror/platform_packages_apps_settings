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

import static android.content.pm.PackageManager.FEATURE_ETHERNET;
import static android.content.pm.PackageManager.FEATURE_USB_HOST;
import static android.content.pm.PackageManager.FEATURE_WIFI;

import android.app.usage.NetworkStats.Bucket;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkTemplate;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.Log;

import com.android.settings.datausage.lib.DataUsageLib;
import com.android.settings.network.ProxySubscriptionManager;

import java.util.List;
import java.util.Optional;

/**
 * Utility methods for data usage classes.
 */
public final class DataUsageUtils {
    static final boolean TEST_RADIOS = false;
    static final String TEST_RADIOS_PROP = "test.radios";
    private static final String ETHERNET = "ethernet";
    private static final String TAG = "DataUsageUtils";

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

        // See ConnectivityService#deviceSupportsEthernet.
        final PackageManager pm = context.getPackageManager();
        if (!pm.hasSystemFeature(FEATURE_ETHERNET) && !pm.hasSystemFeature(FEATURE_USB_HOST)) {
            return false;
        }

        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        final NetworkStatsManager networkStatsManager =
                context.getSystemService(NetworkStatsManager.class);
        boolean hasEthernetUsage = false;
        try {
            final Bucket bucket = networkStatsManager.querySummaryForUser(
                    ConnectivityManager.TYPE_ETHERNET, telephonyManager.getSubscriberId(),
                    0L /* startTime */, System.currentTimeMillis() /* endTime */);
            if (bucket != null) {
                hasEthernetUsage = bucket.getRxBytes() > 0 || bucket.getTxBytes() > 0;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Exception querying network detail.", e);
        }
        return hasEthernetUsage;
    }

    /**
     * Returns whether device has mobile data.
     * TODO: This is the opposite to Utils.isWifiOnly(), it should be refactored into 1 method.
     */
    public static boolean hasMobileData(Context context) {
        final TelephonyManager tele = context.getSystemService(TelephonyManager.class);
        return tele.isDataCapable();
    }

    /**
     * Whether device has a Wi-Fi data radio.
     */
    public static boolean hasWifiRadio(Context context) {
        if (TEST_RADIOS) {
            return SystemProperties.get(TEST_RADIOS_PROP).contains("wifi");
        }

        final PackageManager packageManager = context.getPackageManager();
        return packageManager != null && packageManager.hasSystemFeature(FEATURE_WIFI);
    }

    /**
     * Returns the default subscription if available else returns
     * SubscriptionManager#INVALID_SUBSCRIPTION_ID
     */
    public static int getDefaultSubscriptionId(Context context) {
        // default data subscription is first choice
        final int dataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (SubscriptionManager.isValidSubscriptionId(dataSubId)) {
            return dataSubId;
        }

        final ProxySubscriptionManager proxySubscriptionMgr =
                ProxySubscriptionManager.getInstance(context);

        // any active subscription is second choice
        List<SubscriptionInfo> subList = proxySubscriptionMgr.getActiveSubscriptionsInfo();
        if ((subList == null) || (subList.size() <= 0)) {
            // any subscription is third choice
            subList = proxySubscriptionMgr.getAccessibleSubscriptionsInfo();
        }
        if ((subList == null) || (subList.size() <= 0)) {
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        return subList.get(0).getSubscriptionId();
    }

    /**
     * Returns the default network template based on the availability of mobile data, Wifi. Returns
     * ethernet template if both mobile data and Wifi are not available.
     */
    public static NetworkTemplate getDefaultTemplate(Context context, int defaultSubId) {
        if (SubscriptionManager.isValidSubscriptionId(defaultSubId) && hasMobileData(context)) {
            return DataUsageLib.getMobileTemplate(context, defaultSubId);
        } else if (hasWifiRadio(context)) {
            return new NetworkTemplate.Builder(NetworkTemplate.MATCH_WIFI).build();
        } else {
            return new NetworkTemplate.Builder(NetworkTemplate.MATCH_ETHERNET).build();
        }
    }

    /**
     * Returns a mobile NetworkTemplate if EXTRA_SUB_ID of the Intent is available and the subId
     * is valid & hasMobileData. Otherwise, returns empty data.
     */
    public static Optional<NetworkTemplate> getMobileNetworkTemplateFromSubId(Context context,
            Intent intent) {
        if (intent == null || !intent.hasExtra(Settings.EXTRA_SUB_ID)) {
            return Optional.empty();
        }

        int subId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        if (SubscriptionManager.isValidSubscriptionId(subId) && hasMobileData(context)) {
            return Optional.of(DataUsageLib.getMobileTemplate(context, subId));
        }

        return  Optional.empty();
    }
}
