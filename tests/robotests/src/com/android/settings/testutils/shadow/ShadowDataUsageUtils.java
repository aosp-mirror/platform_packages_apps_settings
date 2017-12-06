package com.android.settings.testutils.shadow;

import android.content.Context;

import com.android.settings.datausage.DataUsageUtils;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(DataUsageUtils.class)
public class ShadowDataUsageUtils {

    public static boolean IS_MOBILE_DATA_SUPPORTED = true;
    public static boolean IS_WIFI_SUPPORTED = true;

    @Implementation
    public static boolean hasMobileData(Context context) {
        return IS_MOBILE_DATA_SUPPORTED;
    }

    @Implementation
    public static boolean hasWifiRadio(Context context) {
        return IS_WIFI_SUPPORTED;
    }
}
