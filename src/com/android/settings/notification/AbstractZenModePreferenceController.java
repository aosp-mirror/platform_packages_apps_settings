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

import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

abstract public class AbstractZenModePreferenceController extends
        AbstractPreferenceController implements PreferenceControllerMixin, LifecycleObserver,
        OnResume, OnPause {

    @VisibleForTesting
    protected SettingObserver mSettingObserver;
    private final String KEY;
    final private NotificationManager mNotificationManager;

    public AbstractZenModePreferenceController(Context context, String key,
            Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        KEY = key;
        mNotificationManager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSettingObserver = new SettingObserver(screen.findPreference(KEY));
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

    protected NotificationManager.Policy getPolicy() {
        return mNotificationManager.getNotificationPolicy();
    }

    protected int getZenMode() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.ZEN_MODE, 0);
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
