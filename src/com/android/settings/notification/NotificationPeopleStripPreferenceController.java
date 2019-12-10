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


import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import com.google.common.annotations.VisibleForTesting;

/** Controls toggle setting for people strip in system ui. */
public class NotificationPeopleStripPreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;

    private final Uri mPeopleStripUri =
            Settings.Secure.getUriFor(Settings.Secure.PEOPLE_STRIP);

    private Preference mPreference;
    private Runnable mUnregisterOnPropertiesChangedListener;

    public NotificationPeopleStripPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference("notification_people_strip");
    }

    @Override
    public void onResume() {
        if (mPreference == null) {
            return;
        }
        ContentObserver observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);
                updateState(mPreference);
            }
        };
        ContentResolver contentResolver = mContext.getContentResolver();
        mUnregisterOnPropertiesChangedListener =
                () -> contentResolver.unregisterContentObserver(observer);
        contentResolver.registerContentObserver(mPeopleStripUri, false, observer);
    }

    @Override
    public void onPause() {
        if (mUnregisterOnPropertiesChangedListener != null) {
            mUnregisterOnPropertiesChangedListener.run();
            mUnregisterOnPropertiesChangedListener = null;
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public boolean isChecked() {
        int value = Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.PEOPLE_STRIP,
                OFF);
        return value != OFF;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.PEOPLE_STRIP,
                isChecked ? ON : OFF);
    }
}
