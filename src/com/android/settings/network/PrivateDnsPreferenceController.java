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

package com.android.settings.network;

import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OFF;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_OPPORTUNISTIC;
import static android.net.ConnectivityManager.PRIVATE_DNS_MODE_PROVIDER_HOSTNAME;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.core.lifecycle.LifecycleObserver;


public class PrivateDnsPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnStart, OnStop {
    private static final String KEY_PRIVATE_DNS_SETTINGS = "private_dns_settings";

    private static final Uri[] SETTINGS_URIS = new Uri[]{
        Settings.Global.getUriFor(Settings.Global.PRIVATE_DNS_MODE),
        Settings.Global.getUriFor(Settings.Global.PRIVATE_DNS_SPECIFIER),
    };

    private final Handler mHandler;
    private final ContentObserver mSettingsObserver;
    private Preference mPreference;

    public PrivateDnsPreferenceController(Context context) {
        super(context, KEY_PRIVATE_DNS_SETTINGS);
        mHandler = new Handler(Looper.getMainLooper());
        mSettingsObserver = new PrivateDnsSettingsObserver(mHandler);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_PRIVATE_DNS_SETTINGS;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        for (Uri uri : SETTINGS_URIS) {
            mContext.getContentResolver().registerContentObserver(uri, false, mSettingsObserver);
        }
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public CharSequence getSummary() {
        final Resources res = mContext.getResources();
        final ContentResolver cr = mContext.getContentResolver();
        final String mode = PrivateDnsModeDialogPreference.getModeFromSettings(cr);
        switch (mode) {
            case PRIVATE_DNS_MODE_OFF:
                return res.getString(R.string.private_dns_mode_off);
            case PRIVATE_DNS_MODE_OPPORTUNISTIC:
                return res.getString(R.string.private_dns_mode_opportunistic);
            case PRIVATE_DNS_MODE_PROVIDER_HOSTNAME:
                return PrivateDnsModeDialogPreference.getHostnameFromSettings(cr);
        }
        return "";
    }

    private class PrivateDnsSettingsObserver extends ContentObserver {
        public PrivateDnsSettingsObserver(Handler h) {
            super(h);
        }

        @Override
        public void onChange(boolean selfChange) {
            if (mPreference != null) {
                PrivateDnsPreferenceController.this.updateState(mPreference);
            }
        }
    }
}
