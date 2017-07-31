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
import android.net.NetworkTemplate;
import android.os.SystemProperties;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import java.util.List;

/**
 * Utility methods for data usage classes.
 */
public final class DataUsageUtils {
    static final boolean TEST_RADIOS = false;
    static final String TEST_RADIOS_PROP = "test.radios";

    private DataUsageUtils() {
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

        ConnectivityManager connectivityManager = ConnectivityManager.from(context);
        return connectivityManager != null && connectivityManager.isNetworkSupported(TYPE_WIFI);
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
