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
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * The controller of the sensitive notifications.
 */
public class RedactNotificationPreferenceController extends TogglePreferenceController implements
        LifecycleObserver, OnStart, OnStop {
    private static final String TAG = "LockScreenNotifPref";

    static final String KEY_LOCKSCREEN_REDACT = "lock_screen_redact";
    static final String KEY_LOCKSCREEN_WORK_PROFILE_REDACT = "lock_screen_work_redact";

    private DevicePolicyManager mDpm;
    private UserManager mUm;
    private KeyguardManager mKm;
    private final int mProfileUserId;
    private Preference mPreference;
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
        mDpm = context.getSystemService(DevicePolicyManager.class);
        mKm = context.getSystemService(KeyguardManager.class);

        mProfileUserId = Utils.getManagedProfileId(mUm, UserHandle.myUserId());
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
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
                && mProfileUserId == UserHandle.USER_NULL) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        int userId = KEY_LOCKSCREEN_REDACT.equals(getPreferenceKey())
                ? UserHandle.myUserId() : mProfileUserId;

        // hide if lockscreen isn't secure for this user
        final LockPatternUtils utils = FeatureFactory.getFactory(mContext)
                .getSecurityFeatureProvider()
                .getLockPatternUtils(mContext);
        if (!utils.isSecure(userId)) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        // all notifs hidden? admin doesn't allow notifs or redacted notifs? disabled
        if (!getLockscreenNotificationsEnabled(userId)
                || !adminAllowsNotifications(userId)
                || !adminAllowsUnredactedNotifications(userId)) {
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
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS),
                false /* notifyForDescendants */, mContentObserver);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    private boolean adminAllowsNotifications(int userId) {
        final int dpmFlags = mDpm.getKeyguardDisabledFeatures(null/* admin */, userId);
        return (dpmFlags & KEYGUARD_DISABLE_SECURE_NOTIFICATIONS) == 0;
    }

    private boolean adminAllowsUnredactedNotifications(int userId) {
        final int dpmFlags = mDpm.getKeyguardDisabledFeatures(null/* admin */, userId);
        return (dpmFlags & KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS) == 0;
    }

    private boolean getAllowPrivateNotifications(int userId) {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 1, userId) != 0;
    }

    private boolean getLockscreenNotificationsEnabled(int userId) {
        return Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1, userId) != 0;
    }
}
