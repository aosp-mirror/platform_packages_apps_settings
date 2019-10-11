/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.annotation.UserIdInt;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManager.EnforcingUser;

import com.google.android.collect.Maps;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.annotation.Resetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Implements(value = UserManager.class)
public class ShadowUserManager extends org.robolectric.shadows.ShadowUserManager {

    private static boolean sIsSupportsMultipleUsers;

    private final List<String> mRestrictions = new ArrayList<>();
    private final Map<String, List<EnforcingUser>> mRestrictionSources = new HashMap<>();
    private final List<UserInfo> mUserProfileInfos = new ArrayList<>();
    private final Set<Integer> mManagedProfiles = new HashSet<>();
    private boolean mIsQuietModeEnabled = false;
    private int[] profileIdsForUser = new int[0];
    private boolean mUserSwitchEnabled;
    private final Map<Integer, Integer> mSameProfileGroupIds = Maps.newHashMap();

    public void addProfile(UserInfo userInfo) {
        mUserProfileInfos.add(userInfo);
    }

    @Resetter
    public static void reset() {
        sIsSupportsMultipleUsers = false;
    }

    @Implementation
    protected List<UserInfo> getProfiles(@UserIdInt int userHandle) {
        return mUserProfileInfos;
    }

    @Implementation
    protected int[] getProfileIds(@UserIdInt int userHandle, boolean enabledOnly) {
        int[] ids = new int[mUserProfileInfos.size()];
        for (int i = 0; i < mUserProfileInfos.size(); i++) {
            ids[i] = mUserProfileInfos.get(i).id;
        }
        return ids;
    }

    @Implementation
    protected int getCredentialOwnerProfile(@UserIdInt int userHandle) {
        return userHandle;
    }

    @Implementation
    protected boolean hasBaseUserRestriction(String restrictionKey, UserHandle userHandle) {
        return mRestrictions.contains(restrictionKey);
    }

    public void addBaseUserRestriction(String restriction) {
        mRestrictions.add(restriction);
    }

    public static ShadowUserManager getShadow() {
        return (ShadowUserManager) Shadow.extract(
                RuntimeEnvironment.application.getSystemService(UserManager.class));
    }

    @Implementation
    protected List<EnforcingUser> getUserRestrictionSources(
            String restrictionKey, UserHandle userHandle) {
        // Return empty list when there is no enforcing user, otherwise might trigger
        // NullPointer Exception in RestrictedLockUtils.checkIfRestrictionEnforced.
        List<EnforcingUser> enforcingUsers =
                mRestrictionSources.get(restrictionKey + userHandle.getIdentifier());
        return enforcingUsers == null ? Collections.emptyList() : enforcingUsers;
    }

    public void setUserRestrictionSources(
            String restrictionKey, UserHandle userHandle, List<EnforcingUser> enforcers) {
        mRestrictionSources.put(restrictionKey + userHandle.getIdentifier(), enforcers);
    }

    @Implementation
    protected boolean isQuietModeEnabled(UserHandle userHandle) {
        return mIsQuietModeEnabled;
    }

    public void setQuietModeEnabled(boolean enabled) {
        mIsQuietModeEnabled = enabled;
    }

    @Implementation
    protected int[] getProfileIdsWithDisabled(@UserIdInt int userId) {
        return profileIdsForUser;
    }

    public void setProfileIdsWithDisabled(int[] profileIds) {
        profileIdsForUser = profileIds;
    }

    @Implementation
    protected boolean isUserSwitcherEnabled() {
        return mUserSwitchEnabled;
    }

    public void setUserSwitcherEnabled(boolean userSwitchEnabled) {
        mUserSwitchEnabled = userSwitchEnabled;
    }

    @Implementation
    protected static boolean supportsMultipleUsers() {
        return sIsSupportsMultipleUsers;
    }

    @Implementation
    protected boolean isSameProfileGroup(@UserIdInt int userId, int otherUserId) {
        return mSameProfileGroupIds.containsKey(userId)
                && mSameProfileGroupIds.get(userId) == otherUserId
                || mSameProfileGroupIds.containsKey(otherUserId)
                && mSameProfileGroupIds.get(otherUserId) == userId;
    }

    public Map<Integer, Integer> getSameProfileGroupIds() {
        return mSameProfileGroupIds;
    }

    public void setSupportsMultipleUsers(boolean supports) {
        sIsSupportsMultipleUsers = supports;
    }
}
