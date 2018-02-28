/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

public abstract class VibrationIntensityPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    protected final Vibrator mVibrator;
    private final SettingObserver mSettingsContentObserver;
    private final String mSettingKey;

    private Preference mPreference;

    public VibrationIntensityPreferenceController(Context context, String prefkey,
            String settingKey) {
        super(context, prefkey);
        mVibrator = mContext.getSystemService(Vibrator.class);
        mSettingKey = settingKey;
        mSettingsContentObserver = new SettingObserver(settingKey) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateState(null);
            }
        };
    }

    @Override
    public void onStart() {
        mContext.getContentResolver().registerContentObserver(
                mSettingsContentObserver.uri,
                false /* notifyForDescendants */,
                mSettingsContentObserver);
    }

    @Override
    public void onStop() {
        mContext.getContentResolver().unregisterContentObserver(mSettingsContentObserver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        if (mPreference == null) {
            return;
        }
        mPreference.setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        final int intensity = Settings.System.getInt(mContext.getContentResolver(),
                mSettingKey, getDefaultIntensity());

        switch (intensity) {
            case Vibrator.VIBRATION_INTENSITY_OFF:
                return mContext.getText(R.string.accessibility_vibration_intensity_off);
            case Vibrator.VIBRATION_INTENSITY_LOW:
                return mContext.getText(R.string.accessibility_vibration_intensity_low);
            case Vibrator.VIBRATION_INTENSITY_MEDIUM:
                return mContext.getText(R.string.accessibility_vibration_intensity_medium);
            case Vibrator.VIBRATION_INTENSITY_HIGH:
                return mContext.getText(R.string.accessibility_vibration_intensity_high);
            default:
                return "";
        }
    }

    protected abstract int getDefaultIntensity();

    private static class SettingObserver extends ContentObserver {

        public final Uri uri;

        public SettingObserver(String settingKey) {
            super(new Handler(Looper.getMainLooper()));
            uri = Settings.System.getUriFor(settingKey);
        }
    }
}
