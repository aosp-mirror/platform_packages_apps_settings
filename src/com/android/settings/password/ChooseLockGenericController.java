/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.password;

import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_NONE;

import android.app.admin.DevicePolicyManager.PasswordComplexity;
import android.app.admin.PasswordMetrics;
import android.content.Context;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller for ChooseLockGeneric, and other similar classes which shows a list of possible
 * screen lock types for the user to choose from. This is the main place where different
 * restrictions on allowed screen lock types are aggregated in Settings.
 *
 * Each screen lock type has two states: whether it is visible and whether it is enabled.
 * Visibility is affected by things like resource configs, whether it's for a managed profile,
 * or whether the caller allows it or not. This is determined by
 * {@link #isScreenLockVisible(ScreenLockType)}. For visible screen lock types, they can be disabled
 * by a combination of admin policies and request from the calling app, which is determined by
 * {@link #isScreenLockEnabled(ScreenLockType}.
 */

public class ChooseLockGenericController {

    private final Context mContext;
    private final int mUserId;
    private final boolean mHideInsecureScreenLockTypes;
    @PasswordComplexity private final int mAppRequestedMinComplexity;
    private final boolean mDevicePasswordRequirementOnly;
    private final int mUnificationProfileId;
    private final ManagedLockPasswordProvider mManagedPasswordProvider;
    private final LockPatternUtils mLockPatternUtils;

    public ChooseLockGenericController(Context context, int userId,
            ManagedLockPasswordProvider managedPasswordProvider, LockPatternUtils lockPatternUtils,
            boolean hideInsecureScreenLockTypes, int appRequestedMinComplexity,
            boolean devicePasswordRequirementOnly, int unificationProfileId) {
        mContext = context;
        mUserId = userId;
        mManagedPasswordProvider = managedPasswordProvider;
        mLockPatternUtils = lockPatternUtils;
        mHideInsecureScreenLockTypes = hideInsecureScreenLockTypes;
        mAppRequestedMinComplexity = appRequestedMinComplexity;
        mDevicePasswordRequirementOnly = devicePasswordRequirementOnly;
        mUnificationProfileId = unificationProfileId;
    }

    /** Builder class for {@link ChooseLockGenericController} */
    public static class Builder {
        private final Context mContext;
        private final int mUserId;
        private final ManagedLockPasswordProvider mManagedPasswordProvider;
        private final LockPatternUtils mLockPatternUtils;

        private boolean mHideInsecureScreenLockTypes = false;
        @PasswordComplexity private int mAppRequestedMinComplexity = PASSWORD_COMPLEXITY_NONE;
        private boolean mDevicePasswordRequirementOnly = false;
        private int mUnificationProfileId = UserHandle.USER_NULL;

        public Builder(Context context, int userId) {
            this(context, userId, new LockPatternUtils(context));
        }

        public Builder(Context context, int userId,
                LockPatternUtils lockPatternUtils) {
            this(
                    context,
                    userId,
                    ManagedLockPasswordProvider.get(context, userId),
                    lockPatternUtils);
        }

        @VisibleForTesting
        Builder(
                Context context,
                int userId,
                ManagedLockPasswordProvider managedLockPasswordProvider,
                LockPatternUtils lockPatternUtils) {
            mContext = context;
            mUserId = userId;
            mManagedPasswordProvider = managedLockPasswordProvider;
            mLockPatternUtils = lockPatternUtils;
        }
        /**
         * Sets the password complexity requested by the calling app via
         * {@link android.app.admin.DevicePolicyManager#EXTRA_PASSWORD_COMPLEXITY}.
         */
        public Builder setAppRequestedMinComplexity(int complexity) {
            mAppRequestedMinComplexity = complexity;
            return this;
        }

        /**
         * Sets whether the enrolment flow should discard any password policies originating from the
         * work profile, even if the work profile currently has unified challenge. This can be
         * requested by the calling app via
         * {@link android.app.admin.DevicePolicyManager#EXTRA_DEVICE_PASSWORD_REQUIREMENT_ONLY}.
         */
        public Builder setEnforceDevicePasswordRequirementOnly(boolean deviceOnly) {
            mDevicePasswordRequirementOnly = deviceOnly;
            return this;
        }

        /**
         * Sets the user ID of any profile whose work challenge should be unified at the end of this
         * enrolment flow. This will lead to all password policies from that profile to be taken
         * into consideration by this class, so that we are enrolling a compliant password. This is
         * because once unified, the profile's password policy will be enforced on the new
         * credential.
         */
        public Builder setProfileToUnify(int profileId) {
            mUnificationProfileId = profileId;
            return this;
        }

        /**
         * Sets whether insecure screen lock types (NONE and SWIPE) should be hidden in the UI.
         */
        public Builder setHideInsecureScreenLockTypes(boolean hide) {
            mHideInsecureScreenLockTypes = hide;
            return this;
        }

        /** Creates {@link ChooseLockGenericController} instance. */
        public ChooseLockGenericController build() {
            return new ChooseLockGenericController(mContext, mUserId, mManagedPasswordProvider,
                    mLockPatternUtils, mHideInsecureScreenLockTypes, mAppRequestedMinComplexity,
                    mDevicePasswordRequirementOnly, mUnificationProfileId);
        }
    }

    /**
     * Returns whether the given screen lock type should be visible in the given context.
     */
    public boolean isScreenLockVisible(ScreenLockType type) {
        final boolean managedProfile = mContext.getSystemService(UserManager.class)
                .isManagedProfile(mUserId);
        switch (type) {
            case NONE:
                return !mHideInsecureScreenLockTypes
                    && !mContext.getResources().getBoolean(R.bool.config_hide_none_security_option)
                    && !managedProfile; // Profiles should use unified challenge instead.
            case SWIPE:
                return !mHideInsecureScreenLockTypes
                    && !mContext.getResources().getBoolean(R.bool.config_hide_swipe_security_option)
                    && !managedProfile; // Swipe doesn't make sense for profiles.
            case MANAGED:
                return mManagedPasswordProvider.isManagedPasswordChoosable();
            case PIN:
            case PATTERN:
            case PASSWORD:
                // Hide the secure lock screen options if the device doesn't support the secure lock
                // screen feature.
                return mLockPatternUtils.hasSecureLockScreen();
        }
        return true;
    }

    /**
     * Whether screen lock with {@code type} should be enabled assuming all relevant password
     * requirements. The lock's visibility ({@link #isScreenLockVisible}) is not considered here.
     */
    public boolean isScreenLockEnabled(ScreenLockType type) {
        return !mLockPatternUtils.isCredentialsDisabledForUser(mUserId)
                && type.maxQuality >= upgradeQuality(PASSWORD_QUALITY_UNSPECIFIED);
    }

    /**
     * Increases the given quality to be as high as the combined quality from all relevant
     * password requirements.
     */
    // TODO(b/142781408): convert from quality to credential type once PIN is supported.
    public int upgradeQuality(int quality) {
        return Math.max(quality,
                Math.max(
                        LockPatternUtils.credentialTypeToPasswordQuality(
                                getAggregatedPasswordMetrics().credType),
                        PasswordMetrics.complexityLevelToMinQuality(
                                getAggregatedPasswordComplexity())
                )
        );
    }

    /**
     * User friendly title for the given screen lock type.
     */
    public CharSequence getTitle(ScreenLockType type) {
        switch (type) {
            case NONE:
                return mContext.getText(R.string.unlock_set_unlock_off_title);
            case SWIPE:
                return mContext.getText(R.string.unlock_set_unlock_none_title);
            case PATTERN:
                return mContext.getText(R.string.unlock_set_unlock_pattern_title);
            case PIN:
                return mContext.getText(R.string.unlock_set_unlock_pin_title);
            case PASSWORD:
                return mContext.getText(R.string.unlock_set_unlock_password_title);
            case MANAGED:
                return mManagedPasswordProvider.getPickerOptionTitle(false);
        }
        return null;
    }

    /**
     * Gets a list of screen lock types that should be visible for the given quality. The returned
     * list is ordered in the natural order of the enum (the order those enums were defined). Screen
     * locks disabled by password policy will not be returned.
     */
    @NonNull
    public List<ScreenLockType> getVisibleAndEnabledScreenLockTypes() {
        List<ScreenLockType> locks = new ArrayList<>();
        // EnumSet's iterator guarantees the natural order of the enums
        for (ScreenLockType lock : ScreenLockType.values()) {
            if (isScreenLockVisible(lock) && isScreenLockEnabled(lock)) {
                locks.add(lock);
            }
        }
        return locks;
    }

    /**
     * Returns the combined password metrics from all relevant policies which affects the current
     * user. Normally password policies set on the current user's work profile instance will be
     * taken into consideration here iff the work profile doesn't have its own work challenge.
     * By setting {@link #mUnificationProfileId}, the work profile's password policy will always
     * be combined here. Alternatively, by setting {@link #mDevicePasswordRequirementOnly}, its
     * password policy will always be disregarded here.
     */
    public PasswordMetrics getAggregatedPasswordMetrics() {
        PasswordMetrics metrics = mLockPatternUtils.getRequestedPasswordMetrics(mUserId,
                mDevicePasswordRequirementOnly);
        if (mUnificationProfileId != UserHandle.USER_NULL) {
            metrics.maxWith(mLockPatternUtils.getRequestedPasswordMetrics(mUnificationProfileId));
        }
        return metrics;
    }

    /**
     * Returns the combined password complexity from all relevant policies which affects the current
     * user. The same logic of handling work profile password policies as
     * {@link #getAggregatedPasswordMetrics} applies here.
     */
    public int getAggregatedPasswordComplexity() {
        int complexity = Math.max(mAppRequestedMinComplexity,
                mLockPatternUtils.getRequestedPasswordComplexity(
                        mUserId, mDevicePasswordRequirementOnly));
        if (mUnificationProfileId != UserHandle.USER_NULL) {
            complexity = Math.max(complexity,
                    mLockPatternUtils.getRequestedPasswordComplexity(mUnificationProfileId));
        }
        return complexity;
    }

    /**
     * Returns whether any screen lock type has been disabled only due to password policy
     * from the admin. Will return {@code false} if the restriction is purely due to calling
     * app's request.
     */
    public boolean isScreenLockRestrictedByAdmin() {
        return getAggregatedPasswordMetrics().credType != CREDENTIAL_TYPE_NONE
                || isComplexityProvidedByAdmin();
    }

    /**
     * Returns whether the aggregated password complexity is non-zero and comes from
     * admin policy.
     */
    public boolean isComplexityProvidedByAdmin() {
        final int aggregatedComplexity = getAggregatedPasswordComplexity();
        return aggregatedComplexity > mAppRequestedMinComplexity
                && aggregatedComplexity > PASSWORD_COMPLEXITY_NONE;
    }
}
