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

package com.android.settings.accessibility;


import static com.android.settings.accessibility.FlashNotificationsUtil.ACTION_FLASH_NOTIFICATION_START_PREVIEW;
import static com.android.settings.accessibility.FlashNotificationsUtil.EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE;
import static com.android.settings.accessibility.FlashNotificationsUtil.TYPE_SHORT_PREVIEW;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

/**
 * Controller for flash notifications preview.
 */
public class FlashNotificationsPreviewPreferenceController extends
        BasePreferenceController implements LifecycleEventObserver {

    private Preference mPreference;
    private final ContentResolver mContentResolver;

    @VisibleForTesting
    final ContentObserver mContentObserver = new ContentObserver(
            new Handler(Looper.getMainLooper())) {
        @Override
        public void onChange(boolean selfChange, @Nullable Uri uri) {
            updateState(mPreference);
        }
    };

    public FlashNotificationsPreviewPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        updateState(mPreference);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (getPreferenceKey().equals(preference.getKey())) {
            Intent intent = new Intent(ACTION_FLASH_NOTIFICATION_START_PREVIEW);
            intent.putExtra(EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE, TYPE_SHORT_PREVIEW);
            mContext.sendBroadcastAsUser(intent, UserHandle.SYSTEM);
            return true;
        }

        return super.handlePreferenceTreeClick(preference);
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_RESUME) {
            mContentResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.CAMERA_FLASH_NOTIFICATION),
                    /* notifyForDescendants= */ false, mContentObserver);
            mContentResolver.registerContentObserver(
                    Settings.System.getUriFor(Settings.System.SCREEN_FLASH_NOTIFICATION),
                    /* notifyForDescendants= */ false, mContentObserver);
        } else if (event == Lifecycle.Event.ON_PAUSE) {
            mContentResolver.unregisterContentObserver(mContentObserver);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference == null) {
            return;
        }
        preference.setEnabled(FlashNotificationsUtil.getFlashNotificationsState(mContext)
                != FlashNotificationsUtil.State.OFF);
    }
}
