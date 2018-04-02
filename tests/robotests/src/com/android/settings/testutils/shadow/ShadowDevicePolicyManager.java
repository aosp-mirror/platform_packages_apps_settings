package com.android.settings.testutils.shadow;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Implements(DevicePolicyManager.class)
public class ShadowDevicePolicyManager extends org.robolectric.shadows.ShadowDevicePolicyManager {

    private final Map<Integer, Long> mProfileTimeouts = new HashMap<>();
    private Map<Integer, CharSequence> mSupportMessagesMap = new HashMap<>();
    private boolean mIsAdminActiveAsUser = false;
    ComponentName mDeviceOwnerComponentName;
    private int mDeviceOwnerUserId = -1;

    public void setShortSupportMessageForUser(ComponentName admin, int userHandle, String message) {
        mSupportMessagesMap.put(Objects.hash(admin, userHandle), message);
    }

    @Implementation
    public @Nullable
    CharSequence getShortSupportMessageForUser(@NonNull ComponentName admin,
            int userHandle) {
        return mSupportMessagesMap.get(Objects.hash(admin, userHandle));
    }

    @Implementation
    public boolean isAdminActiveAsUser(@NonNull ComponentName admin, int userId) {
        return mIsAdminActiveAsUser;
    }

    @Implementation
    public int getDeviceOwnerUserId() {
        return mDeviceOwnerUserId;
    }

    @Implementation
    public long getMaximumTimeToLock(ComponentName admin, @UserIdInt int userHandle) {
        return mProfileTimeouts.getOrDefault(userHandle, 0L);
    }

    @Implementation
    public ComponentName getDeviceOwnerComponentOnAnyUser() {
        return mDeviceOwnerComponentName;
    }

    public void setIsAdminActiveAsUser(boolean active) {
        mIsAdminActiveAsUser = active;
    }

    public void setDeviceOwnerUserId(int id) {
        mDeviceOwnerUserId = id;
    }

    public void setMaximumTimeToLock(@UserIdInt int userHandle, Long timeout) {
        mProfileTimeouts.put(userHandle, timeout);
    }

    public void setDeviceOwnerComponentOnAnyUser(ComponentName admin) {
        mDeviceOwnerComponentName = admin;
    }

    public static ShadowDevicePolicyManager getShadow() {
        return (ShadowDevicePolicyManager) Shadow.extract(
                RuntimeEnvironment.application.getSystemService(DevicePolicyManager.class));
    }
}
