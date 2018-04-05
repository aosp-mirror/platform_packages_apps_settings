package com.android.settings.testutils.shadow;

import android.content.Context;
import android.telephony.SubscriptionManager;

import com.android.settings.datausage.DataUsageUtils;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(DataUsageUtils.class)
public class ShadowDataUsageUtils {

    public static boolean IS_MOBILE_DATA_SUPPORTED = true;
    public static boolean IS_WIFI_SUPPORTED = true;
    public static boolean HAS_SIM = true;
    public static int DEFAULT_SUBSCRIPTION_ID = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

    @Implementation
    public static boolean hasMobileData(Context context) {
        return IS_MOBILE_DATA_SUPPORTED;
    }

    @Implementation
    public static boolean hasWifiRadio(Context context) {
        return IS_WIFI_SUPPORTED;
    }

    @Implementation
    public static int getDefaultSubscriptionId(Context context) {
        return DEFAULT_SUBSCRIPTION_ID;
    }

    @Implementation
    public static boolean hasSim(Context context) {
        return HAS_SIM;
    }

    @Implementation
    public static boolean hasEthernet(Context context) { return false; }
}
