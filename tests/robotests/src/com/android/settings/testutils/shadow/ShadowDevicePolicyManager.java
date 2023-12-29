/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.testutils.shadow;

import static android.app.admin.DevicePolicyManager.DEVICE_OWNER_TYPE_DEFAULT;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
import static android.os.Build.VERSION_CODES.O;
import static android.os.Build.VERSION_CODES.Q;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.UserIdInt;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManager.DeviceOwnerType;
import android.app.admin.IDevicePolicyManager;
import android.app.admin.ManagedSubscriptionsPolicy;
import android.app.admin.PasswordMetrics;
import android.app.admin.PasswordPolicy;
import android.content.ComponentName;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Implements(DevicePolicyManager.class)
public class ShadowDevicePolicyManager extends org.robolectric.shadows.ShadowDevicePolicyManager {

    private final Map<Integer, Long> mProfileTimeouts = new HashMap<>();
    private final Map<String, Integer> mDeviceOwnerTypes = new HashMap<>();
    private Map<Integer, CharSequence> mSupportMessagesMap = new HashMap<>();
    private boolean mIsAdminActiveAsUser = false;
    private ComponentName mProfileOwner;
    private ComponentName mDeviceOwnerComponentName;
    private ManagedSubscriptionsPolicy mManagedSubscriptionsPolicy;
    private int mDeviceOwnerUserId = -1;
    private int mPasswordMinQuality = PASSWORD_QUALITY_UNSPECIFIED;
    private int mPasswordMinLength = 0;
    private int mPasswordMinSymbols = 0;
    private Set<String> mCrossProfileCalendarPackages = Collections.emptySet();

    private List<String> mPermittedAccessibilityServices = null;

    @Implementation(minSdk = O)
    protected void __constructor__(Context context, IDevicePolicyManager service) {
        super.__constructor__(ApplicationProvider.getApplicationContext(), service);
    }

    public void setShortSupportMessageForUser(ComponentName admin, int userHandle, String message) {
        mSupportMessagesMap.put(Objects.hash(admin, userHandle), message);
    }

    @Implementation
    protected @Nullable CharSequence getShortSupportMessageForUser(@NonNull ComponentName admin,
            int userHandle) {
        return mSupportMessagesMap.get(Objects.hash(admin, userHandle));
    }

    @Implementation
    protected boolean isAdminActiveAsUser(@NonNull ComponentName admin, int userId) {
        return mIsAdminActiveAsUser;
    }

    @Implementation
    protected int getDeviceOwnerUserId() {
        return mDeviceOwnerUserId;
    }

    @Implementation
    protected long getMaximumTimeToLock(ComponentName admin, @UserIdInt int userHandle) {
        return mProfileTimeouts.getOrDefault(userHandle, 0L);
    }

    @Implementation
    protected ComponentName getDeviceOwnerComponentOnAnyUser() {
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

    public void setDeviceOwnerType(@NonNull ComponentName admin,
            @DeviceOwnerType int deviceOwnerType) {
        mDeviceOwnerTypes.put(admin.getPackageName(), deviceOwnerType);
    }

    public void setManagedSubscriptionsPolicy(ManagedSubscriptionsPolicy policy) {
        mManagedSubscriptionsPolicy = policy;
    }

    @DeviceOwnerType
    public int getDeviceOwnerType(@NonNull ComponentName admin) {
        return mDeviceOwnerTypes.getOrDefault(admin.getPackageName(), DEVICE_OWNER_TYPE_DEFAULT);
    }

    @Implementation
    public PasswordMetrics getPasswordMinimumMetrics(int userHandle) {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = mPasswordMinQuality;
        policy.length = mPasswordMinLength;
        policy.symbols = mPasswordMinSymbols;
        return policy.getMinMetrics();
    }

    @Implementation
    public ManagedSubscriptionsPolicy getManagedSubscriptionsPolicy() {
        return mManagedSubscriptionsPolicy;
    }

    public void setPasswordQuality(int quality) {
        mPasswordMinQuality = quality;
    }

    public void setPasswordMinimumLength(int length) {
        mPasswordMinLength = length;
    }

    public void setPasswordMinimumSymbols(int numOfSymbols) {
        mPasswordMinSymbols = numOfSymbols;
    }

    public void setPermittedAccessibilityServices(List<String> permittedAccessibilityServices) {
        mPermittedAccessibilityServices = permittedAccessibilityServices;
    }

    @Implementation
    @Nullable
    public List<String> getPermittedAccessibilityServices(int userId) {
        return mPermittedAccessibilityServices;
    }

    public static ShadowDevicePolicyManager getShadow() {
        return (ShadowDevicePolicyManager) Shadow.extract(
                ApplicationProvider.getApplicationContext()
                        .getSystemService(DevicePolicyManager.class));
    }

    // BEGIN-INTERNAL
    @Implementation(minSdk = Q)
    protected Set<String> getCrossProfileCalendarPackages() {
        return mCrossProfileCalendarPackages;
    }

    @Implementation(minSdk = Q)
    public void setCrossProfileCalendarPackages(ComponentName admin, Set<String> packageNames) {
        enforceProfileOwner(admin);
        mCrossProfileCalendarPackages = packageNames;
    }

    /**
     * Sets the admin as active admin and profile owner.
     *
     * @see DevicePolicyManager#getProfileOwner()
     */
    public void setProfileOwner(ComponentName admin) {
        setActiveAdmin(admin);
        mProfileOwner = admin;
    }

    private void enforceProfileOwner(ComponentName admin) {
        if (!admin.equals(mProfileOwner)) {
            throw new SecurityException("[" + admin + "] is not a profile owner");
        }
    }
}
