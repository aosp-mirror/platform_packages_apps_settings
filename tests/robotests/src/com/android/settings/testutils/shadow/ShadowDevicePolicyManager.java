package com.android.settings.testutils.shadow;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.robolectric.shadow.api.Shadow;

/**
 * This shadow if using {@link ShadowDevicePolicyManagerWrapper} is not possible.
 */
@Implements(DevicePolicyManager.class)
public class ShadowDevicePolicyManager extends org.robolectric.shadows.ShadowDevicePolicyManager {
    private Map<Integer, CharSequence> mSupportMessagesMap = new HashMap<>();
    private boolean mIsAdminActiveAsUser = false;
    ComponentName mDeviceOwnerComponentName;

    public void setShortSupportMessageForUser(ComponentName admin, int userHandle, String message) {
        mSupportMessagesMap.put(Objects.hash(admin, userHandle), message);
    }

    @Implementation
    public @Nullable CharSequence getShortSupportMessageForUser(@NonNull ComponentName admin,
            int userHandle) {
        return mSupportMessagesMap.get(Objects.hash(admin, userHandle));
    }

    @Implementation
    public boolean isAdminActiveAsUser(@NonNull ComponentName admin, int userId) {
        return mIsAdminActiveAsUser;
    }

    public void setIsAdminActiveAsUser(boolean active) {
        mIsAdminActiveAsUser = active;
    }

    public static ShadowDevicePolicyManager getShadow() {
        return (ShadowDevicePolicyManager) Shadow.extract(
            RuntimeEnvironment.application.getSystemService(DevicePolicyManager.class));
    }

    public ComponentName getDeviceOwnerComponentOnAnyUser() {
        return mDeviceOwnerComponentName;
    }

    public void setDeviceOwnerComponentOnAnyUser(ComponentName admin) {
        mDeviceOwnerComponentName = admin;
    }
}
