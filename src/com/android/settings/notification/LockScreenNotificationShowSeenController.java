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

import static android.provider.Settings.Secure.LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.server.notification.Flags;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controls the toggle that determines whether to hide seen notifications from the lock screen.
 * Toggle for setting: Settings.Secure.LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS
 */
public class LockScreenNotificationShowSeenController extends TogglePreferenceController
        implements LifecycleEventObserver {

    // 0 is the default value for phones, we treat 0 as off as usage
    private static final int UNSET_OFF = 0;
    static final int ON = 1;
    static final int OFF = 2;
    @Nullable private Preference mPreference;
    private final ContentResolver mContentResolver;

    final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            if (mPreference == null) return;
            updateState(mPreference);
        }
    };

    public LockScreenNotificationShowSeenController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
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
                            Settings.Secure.LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS),
                    /* notifyForDescendants= */ false,
                    mContentObserver
            );
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        setChecked(lockScreenShowSeenNotifications());
        preference.setVisible(isAvailable());
    }

    @Override
    public int getAvailabilityStatus() {
        if (Flags.notificationMinimalism()) {
            if (!lockScreenShowNotification()) {
                return CONDITIONALLY_UNAVAILABLE;
            }
            // We want to show the switch when the lock screen notification minimalism flag is on.
            return AVAILABLE;
        }

        int setting = Settings.Secure.getInt(mContext.getContentResolver(),
                LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, UNSET_OFF);
        if (setting == UNSET_OFF) {
            // hide the setting if the minimalism flag is off, and the device is phone
            // UNSET_OFF is the default value for phones
            return CONDITIONALLY_UNAVAILABLE;
        } else {
            return AVAILABLE;
        }
    }

    /**
     * @return Whether showing notifications on the lockscreen is enabled.
     */
    private boolean lockScreenShowNotification() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, OFF) == ON;
    }

    @Override
    public boolean isChecked() {
        return lockScreenShowSeenNotifications();
    }

    /**
     * @return whether to show seen notifications on lockscreen
     */
    private boolean lockScreenShowSeenNotifications() {
        // UNSET_OFF is the default value for phone, which is equivalent to off in effect
        // (show seen notification)
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, UNSET_OFF) != ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_ONLY_UNSEEN_NOTIFICATIONS, (isChecked ? OFF : ON));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed because Sliceable is deprecated
        return NO_RES;
    }
}
