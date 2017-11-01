package com.android.settings.testutils.shadow;

import android.content.Context;
import com.android.settings.network.MobileNetworkPreferenceController;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(MobileNetworkPreferenceController.class)
public class ShadowMobileNetworkPreferenceController {
    private static boolean mIsRestricted = false;

    public void __constructor__(Context context) {
    }

    @Implementation
    public boolean isAvailable() {
        return mIsRestricted ? false : true;
    }

    @Implementation
    public boolean isUserRestricted() {
        return mIsRestricted;
    }

    public static void setRestricted(boolean restricted) {
        mIsRestricted = restricted;
    }
}
