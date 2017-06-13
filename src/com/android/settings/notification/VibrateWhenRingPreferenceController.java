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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import static android.provider.Settings.System.VIBRATE_WHEN_RINGING;

public class VibrateWhenRingPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {

    private static final String KEY_VIBRATE_WHEN_RINGING = "vibrate_when_ringing";
    private SettingObserver mSettingObserver;

    public VibrateWhenRingPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(KEY_VIBRATE_WHEN_RINGING);
        if (preference != null) {
            mSettingObserver = new SettingObserver(preference);
            preference.setPersistent(false);
        }
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.register(false /* register */);
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIBRATE_WHEN_RINGING;
    }

    @Override
    public boolean isAvailable() {
        return Utils.isVoiceCapable(mContext);
    }

    @Override
    public void updateState(Preference preference) {
        ((TwoStatePreference) preference).setChecked(
            Settings.System.getInt(mContext.getContentResolver(), VIBRATE_WHEN_RINGING, 0) != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean val = (Boolean) newValue;
        return Settings.System.putInt(mContext.getContentResolver(),
            VIBRATE_WHEN_RINGING, val ? 1 : 0);
    }

    private final class SettingObserver extends ContentObserver {

        private final Uri VIBRATE_WHEN_RINGING_URI =
            Settings.System.getUriFor(VIBRATE_WHEN_RINGING);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(boolean register) {
            final ContentResolver cr = mContext.getContentResolver();
            if (register) {
                cr.registerContentObserver(VIBRATE_WHEN_RINGING_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (VIBRATE_WHEN_RINGING_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }

}
