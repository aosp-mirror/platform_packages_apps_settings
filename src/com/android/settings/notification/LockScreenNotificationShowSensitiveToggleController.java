/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification;

import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS;
import static android.app.admin.DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS;

import android.app.KeyguardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;

import java.util.List;

/**
 * Controls the toggle that determines whether to show sensitive notifications on the lock screen
 * when locked.
 * Toggle for: Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS
 */
public class LockScreenNotificationShowSensitiveToggleController
        extends TogglePreferenceController implements LifecycleEventObserver {

    private static final int ON = 1;
    private static final int OFF = 0;
    @VisibleForTesting
    static final String KEY_SHOW_SENSITIVE = "lock_screen_notification_show_sensitive_toggle";
    @VisibleForTesting
    static final String KEY_SHOW_SENSITIVE_WORK_PROFILE =
            "work_profile_show_sensitive_notif_toggle";
    @Nullable private RestrictedSwitchPreference mPreference;
    private final ContentResolver mContentResolver;
    private UserManager mUserManager;
    private KeyguardManager mKeyguardManager;
    @VisibleForTesting
    int mWorkProfileUserId;

    final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            updateState(mPreference);
        }
    };

    public LockScreenNotificationShowSensitiveToggleController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();

        mUserManager = context.getSystemService(UserManager.class);
        mKeyguardManager = context.getSystemService(KeyguardManager.class);
        mWorkProfileUserId = UserHandle.myUserId();
        final List<UserInfo> profiles = mUserManager.getProfiles(UserHandle.myUserId());

        for (UserInfo profile: profiles) {
            if (profile.isManagedProfile()
                    && profile.getUserHandle().getIdentifier() != UserHandle.myUserId()) {
                mWorkProfileUserId = profile.getUserHandle().getIdentifier();
            }
        }
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            mContentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS),
                    /* notifyForDescendants= */ false, mContentObserver);
            mContentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(
                            Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS),
                    /* notifyForDescendants= */ false,
                    mContentObserver
            );
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        int userId = getUserId();

        if (mPreference != null && userId != UserHandle.USER_NULL) {
            mPreference.setDisabledByAdmin(getEnforcedAdmin(userId));
        }
    }

    private RestrictedLockUtils.EnforcedAdmin getEnforcedAdmin(int userId) {
        RestrictedLockUtils.EnforcedAdmin admin =
                RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                        mContext, KEYGUARD_DISABLE_SECURE_NOTIFICATIONS, userId);
        if (admin != null) {
            return admin;
        }
        admin = RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                mContext, KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS, userId);
        return admin;
    }

    private int getUserId() {
        return KEY_SHOW_SENSITIVE.equals(getPreferenceKey())
                ? UserHandle.myUserId() : mWorkProfileUserId;
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) return;
        setChecked(showSensitiveContentOnlyWhenUnlocked());
        preference.setVisible(isAvailable());
    }

    @Override
    public int getAvailabilityStatus() {
        // hide setting if no lock screen notification
        if (!lockScreenShowNotification()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        // hide setting if no screen lock
        if (!isLockScreenSecure()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        // For the work profile toggle
        if (KEY_SHOW_SENSITIVE_WORK_PROFILE.equals(getPreferenceKey())) {
            // hide work profile setting if no work profile
            if (mWorkProfileUserId == UserHandle.myUserId()) {
                return CONDITIONALLY_UNAVAILABLE;
            }

            // specifically the work profile setting requires the work profile to be unlocked
            if (mKeyguardManager.isDeviceLocked(mWorkProfileUserId)) {
                return CONDITIONALLY_UNAVAILABLE;
            }
        }

        return AVAILABLE;
    }

    /**
     * @return Whether showing notifications on the lockscreen is enabled.
     */
    private boolean lockScreenShowNotification() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, ON) != OFF;
    }

    @Override
    public boolean isChecked() {
        return showSensitiveContentOnlyWhenUnlocked();
    }

    private boolean showSensitiveContentOnlyWhenUnlocked() {
        int userId = getUserId();
        if (!isLockScreenSecure()) return false;
        if (getEnforcedAdmin(userId) != null) return true;
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, ON, userId) == OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putIntForUser(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                (isChecked ? OFF : ON), getUserId()
        );
    }

    private boolean isLockScreenSecure() {
        return FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(mContext)
                .isSecure(UserHandle.myUserId());
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }
}
