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

import static android.provider.Settings.System.VIBRATE_WHEN_RINGING;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class VibrateWhenRingPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume, OnPause {

    /** Flag for whether or not to apply ramping ringer on incoming phone calls. */
    private static final String RAMPING_RINGER_ENABLED = "ramping_ringer_enabled";
    private static final String KEY_VIBRATE_WHEN_RINGING = "vibrate_when_ringing";
    private final int DEFAULT_VALUE = 0;
    private final int NOTIFICATION_VIBRATE_WHEN_RINGING = 1;
    private SettingObserver mSettingObserver;

    public VibrateWhenRingPreferenceController(Context context, String key) {
        super(context, key);
    }

    @Override
    public boolean isChecked() {
        return Settings.System.getInt(mContext.getContentResolver(),
                VIBRATE_WHEN_RINGING, DEFAULT_VALUE) != DEFAULT_VALUE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.System.putInt(mContext.getContentResolver(), VIBRATE_WHEN_RINGING,
                isChecked ? NOTIFICATION_VIBRATE_WHEN_RINGING : DEFAULT_VALUE);
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        // If ramping ringer is enabled then this setting will be injected
        // with additional options.
        return Utils.isVoiceCapable(mContext) && !isRampingRingerEnabled()
            ? AVAILABLE
            : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "vibrate_when_ringing");
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

    private boolean isRampingRingerEnabled() {
        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_TELEPHONY, RAMPING_RINGER_ENABLED, false);
    }

}
