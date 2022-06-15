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

package com.android.settings.wifi;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * {@link TogglePreferenceController} that controls whether we should notify user when open
 * network is available.
 */
public class NotifyOpenNetworksPreferenceController extends TogglePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume, OnPause {

    private static final String KEY_NOTIFY_OPEN_NETWORKS = "notify_open_networks";
    private SettingObserver mSettingObserver;

    public NotifyOpenNetworksPreferenceController(Context context) {
        super(context, KEY_NOTIFY_OPEN_NETWORKS);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSettingObserver = new SettingObserver(screen.findPreference(getPreferenceKey()));
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
        return AVAILABLE;
    }

    @Override
    public boolean isChecked() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, 0) == 1;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON,
                isChecked ? 1 : 0);
        return true;
    }

    class SettingObserver extends ContentObserver {
        private final Uri NETWORKS_AVAILABLE_URI = Settings.Global.getUriFor(
                Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(NETWORKS_AVAILABLE_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NETWORKS_AVAILABLE_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
