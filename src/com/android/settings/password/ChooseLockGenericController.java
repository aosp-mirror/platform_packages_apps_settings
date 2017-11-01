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

import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.os.UserHandle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A controller for ChooseLockGeneric, and other similar classes which shows a list of possible
 * screen locks for the user to choose from.
 */
public class ChooseLockGenericController {

    private final Context mContext;
    private final int mUserId;
    private ManagedLockPasswordProvider mManagedPasswordProvider;
    private DevicePolicyManager mDpm;

    public ChooseLockGenericController(Context context, int userId) {
        this(
                context,
                userId,
                context.getSystemService(DevicePolicyManager.class),
                ManagedLockPasswordProvider.get(context, userId));
    }

    @VisibleForTesting
    ChooseLockGenericController(
            Context context,
            int userId,
            DevicePolicyManager dpm,
            ManagedLockPasswordProvider managedLockPasswordProvider) {
        mContext = context;
        mUserId = userId;
        mManagedPasswordProvider = managedLockPasswordProvider;
        mDpm = dpm;
    }

    /**
     * @return The higher quality of either the specified {@code quality} or the quality required
     *         by {@link DevicePolicyManager#getPasswordQuality}.
     */
    public int upgradeQuality(int quality) {
        // Compare min allowed password quality
        return Math.max(quality, mDpm.getPasswordQuality(null, mUserId));
    }

    /**
     * Whether the given screen lock type should be visible in the given context.
     */
    public boolean isScreenLockVisible(ScreenLockType type) {
        switch (type) {
            case NONE:
                return !mContext.getResources().getBoolean(R.bool.config_hide_none_security_option);
            case SWIPE:
                return !mContext.getResources().getBoolean(R.bool.config_hide_swipe_security_option)
                    // Swipe doesn't make sense for profiles.
                    && mUserId == UserHandle.myUserId();
            case MANAGED:
                return mManagedPasswordProvider.isManagedPasswordChoosable();
        }
        return true;
    }

    /**
     * Whether screen lock with {@code type} should be enabled.
     *
     * @param type The screen lock type.
     * @param quality The minimum required quality. This can either be requirement by device policy
     *                manager or because some flow only makes sense with secure lock screens.
     */
    public boolean isScreenLockEnabled(ScreenLockType type, int quality) {
        return type.maxQuality >= quality;
    }

    /**
     * Whether screen lock with {@code type} is disabled by device policy admin.
     *
     * @param type The screen lock type.
     * @param adminEnforcedQuality The minimum quality that the admin enforces.
     */
    public boolean isScreenLockDisabledByAdmin(ScreenLockType type, int adminEnforcedQuality) {
        boolean disabledByAdmin = type.maxQuality < adminEnforcedQuality;
        if (type == ScreenLockType.MANAGED) {
            disabledByAdmin = disabledByAdmin
                    || !mManagedPasswordProvider.isManagedPasswordChoosable();
        }
        return disabledByAdmin;
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
     * Gets a list of screen locks that should be visible for the given quality. The returned list
     * is ordered in the natural order of the enum (the order those enums were defined).
     *
     * @param quality The minimum quality required in the context of the current flow. This should
     *                be one of the constants defined in
     *                {@code DevicePolicyManager#PASSWORD_QUALITY_*}.
     * @param includeDisabled Whether to include screen locks disabled by {@code quality}
     *                        requirements in the returned list.
     */
    @NonNull
    public List<ScreenLockType> getVisibleScreenLockTypes(int quality, boolean includeDisabled) {
        int upgradedQuality = upgradeQuality(quality);
        List<ScreenLockType> locks = new ArrayList<>();
        // EnumSet's iterator guarantees the natural order of the enums
        for (ScreenLockType lock : ScreenLockType.values()) {
            if (isScreenLockVisible(lock)) {
                if (includeDisabled || isScreenLockEnabled(lock, upgradedQuality)) {
                    locks.add(lock);
                }
            }
        }
        return locks;
    }
}
