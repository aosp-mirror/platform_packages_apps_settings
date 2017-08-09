package com.android.settings.network;

import android.content.Context;
import com.android.settingslib.RestrictedLockUtils;

/**
 * Wrapper class needed to be able to test classes which use RestrictedLockUtils methods.
 * Unfortunately there is no way to deal with this until robolectric is updated due to the fact
 * that it is a static method and it uses new API's.
 */
public class RestrictedLockUtilsWrapper {
    public boolean hasBaseUserRestriction(Context context, String userRestriction, int userId) {
        return RestrictedLockUtils.hasBaseUserRestriction(context, userRestriction, userId);
    }
}