package com.android.settings.testutils.shadow;

import android.content.Context;

import com.android.settings.bluetooth.RestrictionUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(RestrictionUtils.class)
public class ShadowRestrictionUtils {
    private static boolean isRestricted = false;

    @Implementation
    protected EnforcedAdmin checkIfRestrictionEnforced(Context context, String restriction) {
        if (isRestricted) {
            return new EnforcedAdmin();
        }
        return null;
    }

    public static void setRestricted(boolean restricted) {
        isRestricted = restricted;
    }
}
