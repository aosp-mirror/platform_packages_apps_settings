package com.android.settings.testutils.shadow;

import android.content.Context;
import com.android.settings.network.RestrictedLockUtilsWrapper;
import org.robolectric.annotation.Implements;

/**
 * Shadow for the wrapper around RestrictedLockUtils. Should be removed/updated once robolectric is
 * updated to allow usage of new UserManager API's. see
 * {@link com.android.settingslib.RestrictedLockUtils} and
 * {@link com.android.settings.network.RestrictedLockUtilsWrapper}
 */
@Implements(RestrictedLockUtilsWrapper.class)
public class ShadowRestrictedLockUtilsWrapper {

    private boolean isRestricted;

    public boolean hasBaseUserRestriction(Context context, String userRestriction, int userId) {
        return isRestricted;
    }

    public void setRestricted(boolean restricted) {
        isRestricted = restricted;
    }
}
