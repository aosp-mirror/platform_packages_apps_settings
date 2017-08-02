package com.android.settings.testutils.shadow;

import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.gestures.DoubleTwistPreferenceController;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

@Implements(DoubleTwistPreferenceController.class)
public class ShadowDoubleTwistPreferenceController {
    private static int sManagedProfileId = UserHandle.USER_NULL;
    @Implementation
    public static boolean isGestureAvailable(Context context) {
        return true;
    }

    @Implementation
    public static int getManagedProfileId(UserManager userManager) {
        return sManagedProfileId;
    }

    public static void setManagedProfileId(int managedProfileId) {
        sManagedProfileId = managedProfileId;
    }
}
