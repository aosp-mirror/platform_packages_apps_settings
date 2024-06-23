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

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.UserManager.USER_TYPE_PROFILE_PRIVATE;

import android.annotation.UserIdInt;
import android.content.pm.UserInfo;
import android.content.pm.UserProperties;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.UserManager.EnforcingUser;

import com.google.android.collect.Maps;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Implements(value = UserManager.class)
public class ShadowUserManager extends org.robolectric.shadows.ShadowUserManager {

    private static boolean sIsSupportsMultipleUsers;
    private static boolean sIsMultipleAdminEnabled = false;

    private static final int PRIMARY_USER_ID = 0;

    private final List<String> mBaseRestrictions = new ArrayList<>();
    private final Map<String, List<EnforcingUser>> mRestrictionSources = new HashMap<>();
    private final List<UserInfo> mUserProfileInfos = new ArrayList<>();
    private final Set<Integer> mManagedProfiles = new HashSet<>();
    private final Map<Integer, Integer> mProfileToParent = new HashMap<>();
    private final Map<Integer, UserInfo> mUserInfoMap = new HashMap<>();
    private final Set<String> mEnabledTypes = new HashSet<>();
    private BiMap<UserHandle, Long> mUserProfiles = HashBiMap.create();
    private boolean mIsQuietModeEnabled = false;
    private int[] mProfileIdsForUser = new int[0];
    private boolean mUserSwitchEnabled;
    private Bundle mDefaultGuestUserRestriction = new Bundle();
    private boolean mIsGuestUser = false;
    private long mNextUserSerial = 0;

    private @UserManager.UserSwitchabilityResult int mSwitchabilityStatus =
            UserManager.SWITCHABILITY_STATUS_OK;
    private final Map<Integer, Integer> mSameProfileGroupIds = Maps.newHashMap();

    public void addProfile(UserInfo userInfo) {
        mUserProfileInfos.add(userInfo);
    }

    @Resetter
    public static void reset() {
        sIsSupportsMultipleUsers = false;
    }

    /**
     * Creates a user with the specified name, userId and flags.
     *
     * @param id the unique id of user
     * @param name name of the user
     * @param flags 16 bits for user type. See {@link UserInfo#flags}
     */
    @Override public UserHandle addUser(int id, String name, int flags) {
        UserHandle userHandle = super.addUser(id, name, flags);
        mUserInfoMap.put(id, new UserInfo(id, name, flags));
        return userHandle;
    }

    /** Add a profile to be returned by {@link #getProfiles(int)}. */
    public void addProfile(
            int userHandle, int profileUserHandle, String profileName, int profileFlags) {
        UserInfo profileUserInfo = new UserInfo(profileUserHandle, profileName, profileFlags);
        mUserProfileInfos.add(profileUserInfo);
        mUserInfoMap.put(profileUserHandle, profileUserInfo);
        mProfileToParent.put(profileUserHandle, userHandle);
        if (profileFlags == UserInfo.FLAG_MANAGED_PROFILE) {
            setManagedProfiles(new HashSet<>(Arrays.asList(profileUserHandle)));
        }
    }

    @Implementation
    protected List<UserInfo> getProfiles(@UserIdInt int userHandle) {
        return mUserProfileInfos;
    }

    /**
     * If this profile has been added using {@link #addProfile}, return its parent.
     */
    @Implementation(minSdk = LOLLIPOP)
    protected UserInfo getProfileParent(int userHandle) {
        if (!mProfileToParent.containsKey(userHandle)) {
            return null;
        }
        return mUserInfoMap.get(mProfileToParent.get(userHandle));
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
        return mBaseRestrictions.contains(restrictionKey);
    }

    public void addBaseUserRestriction(String restriction) {
        mBaseRestrictions.add(restriction);
    }

    @Implementation
    protected Bundle getDefaultGuestRestrictions() {
        return mDefaultGuestUserRestriction;
    }

    @Implementation
    protected void setDefaultGuestRestrictions(Bundle restrictions) {
        mDefaultGuestUserRestriction = restrictions;
    }

    @Implementation
    protected UserProperties getUserProperties(UserHandle userHandle) {
        return new UserProperties.Builder().build();
    }


    public void addGuestUserRestriction(String restriction) {
        mDefaultGuestUserRestriction.putBoolean(restriction, true);
    }

    public boolean hasGuestUserRestriction(String restriction, boolean expectedValue) {
        return mDefaultGuestUserRestriction.containsKey(restriction)
                && mDefaultGuestUserRestriction.getBoolean(restriction) == expectedValue;
    }


    @Implementation
    protected boolean hasUserRestriction(String restrictionKey) {
        return hasUserRestriction(restrictionKey, UserHandle.of(UserHandle.myUserId()));
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
        return mProfileIdsForUser;
    }

    public void setProfileIdsWithDisabled(int[] profileIds) {
        mProfileIdsForUser = profileIds;
    }

    @Implementation
    protected boolean isUserSwitcherEnabled() {
        return mUserSwitchEnabled;
    }

    @Implementation
    protected boolean isManagedProfile(int userId) {
        return mManagedProfiles.contains(userId);
    }

    public void setManagedProfiles(Set<Integer> profileIds) {
        mManagedProfiles.clear();
        mManagedProfiles.addAll(profileIds);
    }

    public void setPrivateProfile(int id, String name, int flags) {
        mUserProfileInfos.add(new UserInfo(id, name, null, flags, USER_TYPE_PROFILE_PRIVATE));
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

    @Implementation
    protected UserInfo getUserInfo(@UserIdInt int userId) {
        return mUserProfileInfos.stream()
                .filter(userInfo -> userInfo.id == userId)
                .findFirst()
                .orElse(super.getUserInfo(userId));
    }

    @Implementation
    protected @UserManager.UserSwitchabilityResult int getUserSwitchability() {
        return mSwitchabilityStatus;
    }

    public void setSwitchabilityStatus(@UserManager.UserSwitchabilityResult int newStatus) {
        mSwitchabilityStatus = newStatus;
    }

    @Implementation
    protected boolean isUserTypeEnabled(String userType) {
        return mEnabledTypes.contains(userType);
    }

    public void setUserTypeEnabled(String type, boolean enabled) {
        if (enabled) {
            mEnabledTypes.add(type);
        } else {
            mEnabledTypes.remove(type);
        }
    }

    @Implementation
    protected UserInfo getPrimaryUser() {
        return new UserInfo(PRIMARY_USER_ID, null, null,
                UserInfo.FLAG_INITIALIZED | UserInfo.FLAG_ADMIN | UserInfo.FLAG_PRIMARY);
    }

    protected boolean setUserEphemeral(@UserIdInt int userId, boolean enableEphemeral) {
        UserInfo userInfo = mUserProfileInfos.stream()
                .filter(user -> user.id == userId)
                .findFirst()
                .orElse(super.getUserInfo(userId));

        boolean isSuccess = false;
        boolean isEphemeralUser =
                        (userInfo.flags & UserInfo.FLAG_EPHEMERAL) != 0;
        boolean isEphemeralOnCreateUser =
                (userInfo.flags & UserInfo.FLAG_EPHEMERAL_ON_CREATE)
                    != 0;
        // when user is created in ephemeral mode via FLAG_EPHEMERAL
        // its state cannot be changed.
        // FLAG_EPHEMERAL_ON_CREATE is used to keep track of this state
        if (!isEphemeralOnCreateUser) {
            isSuccess = true;
            if (isEphemeralUser != enableEphemeral) {
                if (enableEphemeral) {
                    userInfo.flags |= UserInfo.FLAG_EPHEMERAL;
                } else {
                    userInfo.flags &= ~UserInfo.FLAG_EPHEMERAL;
                }
            }
        }
        return isSuccess;
    }

    @Implementation
    protected void setUserAdmin(@UserIdInt int userId) {
        for (int i = 0; i < mUserProfileInfos.size(); i++) {
            mUserProfileInfos.get(i).flags |= UserInfo.FLAG_ADMIN;
        }
    }

    /**
     * Sets that the current user is an admin user; controls the return value of
     * {@link UserManager#isAdminUser}.
     */
    public void setIsAdminUser(boolean isAdminUser) {
        UserInfo userInfo = getUserInfo(UserHandle.myUserId());
        if (isAdminUser) {
            userInfo.flags |= UserInfo.FLAG_ADMIN;
        } else {
            userInfo.flags &= ~UserInfo.FLAG_ADMIN;
        }
    }

    @Implementation
    protected boolean isGuestUser() {
        return mIsGuestUser;
    }

    public void setGuestUser(boolean isGuestUser) {
        mIsGuestUser = isGuestUser;
    }

    public static void setIsMultipleAdminEnabled(boolean enableMultipleAdmin) {
        sIsMultipleAdminEnabled = enableMultipleAdmin;
    }

    /**
     * Adds a profile associated for the user that the calling process is running on.
     *
     * <p>The user is assigned an arbitrary unique serial number.
     *
     * @return the user's serial number
     * @deprecated use either addUser() or addProfile()
     */
    @Deprecated
    public long addUserProfile(UserHandle userHandle) {
        long serialNumber = mNextUserSerial++;
        mUserProfiles.put(userHandle, serialNumber);
        return serialNumber;
    }

    protected List<UserHandle> getUserProfiles() {
        return ImmutableList.copyOf(mUserProfiles.keySet());
    }
}
