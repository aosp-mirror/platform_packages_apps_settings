/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.notification.zen;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class ZenModePreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    private SettingObserver mSettingObserver;
    private ZenModeSettings.SummaryBuilder mSummaryBuilder;

    public ZenModePreferenceController(Context context, String key) {
        super(context, key);
        mSummaryBuilder = new ZenModeSettings.SummaryBuilder(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSettingObserver = new SettingObserver(screen.findPreference(getPreferenceKey()));
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver());
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.unregister(mContext.getContentResolver());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference.isEnabled()) {
            preference.setSummary(mSummaryBuilder.getSoundSummary());
        }
    }

    class SettingObserver extends ContentObserver {
        private final Uri ZEN_MODE_URI = Settings.Global.getUriFor(Settings.Global.ZEN_MODE);
        private final Uri ZEN_MODE_CONFIG_ETAG_URI = Settings.Global.getUriFor(
                Settings.Global.ZEN_MODE_CONFIG_ETAG);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr) {
            cr.registerContentObserver(ZEN_MODE_URI, false, this, UserHandle.USER_ALL);
            cr.registerContentObserver(ZEN_MODE_CONFIG_ETAG_URI, false, this, UserHandle.USER_ALL);
        }

        public void unregister(ContentResolver cr) {
            cr.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (ZEN_MODE_URI.equals(uri)) {
                updateState(mPreference);
            }

            if (ZEN_MODE_CONFIG_ETAG_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
