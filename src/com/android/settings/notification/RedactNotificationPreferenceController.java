/*
 * Copyright (C) 2019 The Android Open Source Project
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
import static android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.UserInfo;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.List;

/**
 * The controller of the sensitive notifications.
 */
public class RedactNotificationPreferenceController extends TogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "LockScreenNotifPref";

    static final String KEY_LOCKSCREEN_REDACT = "lock_screen_redact";
    static final String KEY_LOCKSCREEN_WORK_PROFILE_REDACT = "lock_screen_work_redact";

    private UserManager mUm;
    private KeyguardManager mKm;
    int mProfileUserId;
    private RestrictedSwitchPreference mPreference;
    private ContentObserver mContentObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    if (mPreference != null) {
                        mPreference.setEnabled(
                                getAvailabilityStatus() != DISABLED_DEPENDENT_SETTING);
                    }
                }
            };

    public RedactNotificationPreferenceController(Context context, String settingKey) {
        super(context, settingKey);

        mUm = context.getSystemService(UserManager.class);
        mKm = context.getSystemService(KeyguardManager.class);

        mProfileUserId = UserHandle.myUserId();
        final List<UserInfo> profiles = mUm.getProfiles(UserHandle.myUserId());
        final int count = profiles.size();
        for (int i = 0; i < count; i++) {
            final UserInfo profile = profiles.get(i);
            if (profile.isManagedProfile()
                    && profile.getUserHandle().getIdentifier() !=  UserHandle.myUserId()) {
                mProfileUserId = profile.getUserHandle().getIdentifier();
            }
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());

        int userId = KEY_LOCKSCREEN_REDACT.equals(getPreferenceKey())
                ? UserHandle.myUserId() : mProfileUserId;
        if (userId != UserHandle.USER_NULL) {
            mPreference.setDisabledByAdmin(getEnforcedAdmin(userId));
        }
    }

    @Override
    public boolean isChecked() {
        int userId = KEY_LOCKSCREEN_REDACT.equals(getPreferenceKey())
                ? UserHandle.myUserId() : mProfileUserId;

        return getAllowPrivateNotifications(userId);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        int userId = KEY_LOCKSCREEN_REDACT.equals(getPreferenceKey())
                ? UserHandle.myUserId() : mProfileUserId;

        Settings.Secure.putIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, isChecked ? 1 : 0, userId);
        return true;
    }

    @Override
    public int getAvailabilityStatus() {
        // hide work profile setting if no work profile
        if (KEY_LOCKSCREEN_WORK_PROFILE_REDACT.equals(getPreferenceKey())
                && mProfileUserId == UserHandle.myUserId()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        int userId = KEY_LOCKSCREEN_REDACT.equals(getPreferenceKey())
                ? UserHandle.myUserId() : mProfileUserId;

        // hide if lockscreen isn't secure for this user
        final LockPatternUtils utils = FeatureFactory.getFeatureFactory()
                .getSecurityFeatureProvider()
                .getLockPatternUtils(mContext);
        if (!utils.isSecure(userId)) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        // all notifs hidden? disabled
        if (!getLockscreenNotificationsEnabled(userId)) {
            return DISABLED_DEPENDENT_SETTING;
        }

        // specifically the work profile setting requires the work profile to be unlocked
        if (KEY_LOCKSCREEN_WORK_PROFILE_REDACT.equals(getPreferenceKey())) {
            if (mKm.isDeviceLocked(mProfileUserId)) {
                return DISABLED_DEPENDENT_SETTING;
            }
        }

        return AVAILABLE;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_notifications;
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS),
                false /* notifyForDescendants */, mContentObserver);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
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

    private boolean getAllowPrivateNotifications(int userId) {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 1, userId) != 0
                && getEnforcedAdmin(userId) == null;
    }

    private boolean getLockscreenNotificationsEnabled(int userId) {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1, userId) != 0;
    }
}
