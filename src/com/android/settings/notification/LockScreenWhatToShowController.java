/*
 * Copyright (C) 2025 The Android Open Source Project
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

import com.android.settings.widget.PreferenceCategoryController;

public class LockScreenWhatToShowController extends PreferenceCategoryController implements
        LifecycleEventObserver {

    @Nullable
    private Preference mPreference;
    private final ContentResolver mContentResolver;

    final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            if (mPreference == null) return;
            updateState(mPreference);
        }
    };

    @Override
    public void updateState(@Nullable Preference preference) {
        super.updateState(preference);
        if (preference == null) return;
        preference.setVisible(isAvailable());
    }

    public LockScreenWhatToShowController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        if (!lockScreenShowNotification()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    /**
     * @return Whether showing notifications on the lockscreen is enabled.
     */
    private boolean lockScreenShowNotification() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS,
                LockScreenNotificationsGlobalPreferenceController.OFF
        ) == LockScreenNotificationsGlobalPreferenceController.ON;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            mContentResolver.registerContentObserver(
                    Settings.Secure.getUriFor(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS),
                    /* notifyForDescendants= */ false, mContentObserver);
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }
}
