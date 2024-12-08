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
import com.android.settings.widget.SettingsMainSwitchPreferenceController;

public class LockScreenNotificationsGlobalPreferenceController
        extends SettingsMainSwitchPreferenceController
        implements LifecycleEventObserver {

    public static final int ON = 1;
    public static final int OFF = 0;
    private final ContentResolver mContentResolver;
    private final ContentObserver mContentObserver;
    @Nullable private Preference mPreference;


    public LockScreenNotificationsGlobalPreferenceController(
            @NonNull Context context,
            @NonNull String preferenceKey
    ) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
        mContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, @Nullable Uri uri) {
                if (mPreference == null) return;
                updateState(mPreference);
            }
        };
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(@NonNull Preference preference) {
        super.updateState(preference);
        setChecked(lockScreenShowNotifications());
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO: b/367455695 - remove this when the feature flag is removed!
        return Flags.notificationLockScreenSettings() ? AVAILABLE : CONDITIONALLY_UNAVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return lockScreenShowNotifications();
    }

    private boolean lockScreenShowNotifications() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, (isChecked ? ON : OFF));
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            mContentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS),
                    /* notifyForDescendants= */ false,
                    mContentObserver
            );
        } else {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }
}
