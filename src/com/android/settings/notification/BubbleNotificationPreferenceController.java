/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.provider.Settings.Secure.NOTIFICATION_BUBBLES;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.widget.SettingsMainSwitchPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * Feature level screen for bubbles, available through notification menu.
 * Allows user to turn bubbles on or off for the device.
 */
public class BubbleNotificationPreferenceController extends
        SettingsMainSwitchPreferenceController implements LifecycleObserver, OnResume, OnPause {

    private static final String TAG = "BubbleNotifPrefContr";

    private SettingObserver mSettingObserver;

    public BubbleNotificationPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (mSwitchPreference != null) {
            mSettingObserver = new SettingObserver(mSwitchPreference);
        }
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return BubbleHelper.isSupportedByDevice(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return false;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        // not needed since it's not sliceable
        return NO_RES;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                BubbleHelper.SYSTEM_WIDE_ON) == BubbleHelper.SYSTEM_WIDE_ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(mContext.getContentResolver(), NOTIFICATION_BUBBLES,
                isChecked ? BubbleHelper.SYSTEM_WIDE_ON : BubbleHelper.SYSTEM_WIDE_OFF);
        return true;
    }

    class SettingObserver extends ContentObserver {

        private final Uri NOTIFICATION_BUBBLES_URI =
                Settings.Secure.getUriFor(NOTIFICATION_BUBBLES);

        private final Preference mPreference;

        SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(NOTIFICATION_BUBBLES_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NOTIFICATION_BUBBLES_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
