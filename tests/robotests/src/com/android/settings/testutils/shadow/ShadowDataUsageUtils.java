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
    protected static boolean hasMobileData(Context context) {
        return IS_MOBILE_DATA_SUPPORTED;
    }

    @Implementation
    protected static boolean hasWifiRadio(Context context) {
        return IS_WIFI_SUPPORTED;
    }

    @Implementation
    protected static int getDefaultSubscriptionId(Context context) {
        return DEFAULT_SUBSCRIPTION_ID;
    }

    @Implementation
    protected static boolean hasSim(Context context) {
        return HAS_SIM;
    }

    @Implementation
    protected static boolean hasEthernet(Context context) { return false; }
}
