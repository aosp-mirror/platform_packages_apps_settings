/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.R;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import static android.provider.Settings.Secure.NOTIFICATION_BADGING;

public class BadgingNotificationPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {

    private static final String TAG = "BadgeNotifPrefContr";
    private static final String KEY_NOTIFICATION_BADGING = "notification_badging";
    private static final int ON = 1;
    private static final int OFF = 0;

    private SettingObserver mSettingObserver;

    public BadgingNotificationPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(NOTIFICATION_BADGING);
        if (preference != null) {
            mSettingObserver = new SettingObserver(preference);
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
    public String getPreferenceKey() {
        return KEY_NOTIFICATION_BADGING;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources()
                .getBoolean(com.android.internal.R.bool.config_notificationBadging);
    }

    @Override
    public void updateState(Preference preference) {
        final boolean checked = Settings.Secure.getInt(mContext.getContentResolver(),
                NOTIFICATION_BADGING, ON) == ON;
        ((TwoStatePreference) preference).setChecked(checked);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean val = (Boolean) newValue;
        return Settings.Secure.putInt(mContext.getContentResolver(),
                NOTIFICATION_BADGING, val ? ON : OFF);
    }

    class SettingObserver extends ContentObserver {

        private final Uri NOTIFICATION_BADGING_URI =
                Settings.Secure.getUriFor(NOTIFICATION_BADGING);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(NOTIFICATION_BADGING_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NOTIFICATION_BADGING_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                ConfigureNotificationSettings.class.getName(), KEY_NOTIFICATION_BADGING,
                mContext.getString(R.string.configure_notification_settings));

        return new InlineSwitchPayload(Settings.Secure.NOTIFICATION_BADGING,
                ResultPayload.SettingsSource.SECURE, ON /* onValue */, intent, isAvailable(),
                ON /* defaultValue */);
    }
}
