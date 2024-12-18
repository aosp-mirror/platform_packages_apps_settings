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

package com.android.settings.notification;

import static com.android.settings.notification.PoliteNotificationGlobalPreferenceController.ON;
import static com.android.settings.notification.PoliteNotificationGlobalPreferenceController.OFF;

import android.content.ContentResolver;
import android.content.Context;
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

import com.android.server.notification.Flags;
import com.android.settings.core.TogglePreferenceController;

/**
 * Controls the toggle that determines whether notification cooldown
 * should apply to work profiles.
 */
public class PoliteNotifWorkProfileToggleController extends TogglePreferenceController implements
        LifecycleEventObserver {

    private final int mManagedProfileId;
    private Preference mPreference;
    private final ContentResolver mContentResolver;

    final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            updateState(mPreference);
        }
    };

    public PoliteNotifWorkProfileToggleController(@NonNull Context context,
            @NonNull String preferenceKey) {
        this(context, preferenceKey, new AudioHelper(context));
    }

    @VisibleForTesting
    PoliteNotifWorkProfileToggleController(Context context, String preferenceKey,
                AudioHelper helper) {
        super(context, preferenceKey);
        mManagedProfileId = helper.getManagedProfileId(UserManager.get(mContext));
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
                    Settings.System.getUriFor(Settings.System.NOTIFICATION_COOLDOWN_ENABLED),
                    /* notifyForDescendants= */ false, mContentObserver);
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        // TODO: b/291897570 - remove this when the feature flag is removed!
        if (!Flags.politeNotifications()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        if (!isCoolDownEnabledForPrimary()) {
            return CONDITIONALLY_UNAVAILABLE;
        }

        return (mManagedProfileId != UserHandle.USER_NULL) ? AVAILABLE : DISABLED_FOR_USER;
    }

    @Override
    public boolean isChecked() {
        if (!isCoolDownEnabledForPrimary()) {
            return false;
        }
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON, mManagedProfileId) != OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putIntForUser(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, (isChecked ? ON : OFF),
                mManagedProfileId);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference == null) return;
        preference.setVisible(isAvailable());
    }

    private boolean isCoolDownEnabledForPrimary() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.NOTIFICATION_COOLDOWN_ENABLED, ON) == ON;
    }
}
