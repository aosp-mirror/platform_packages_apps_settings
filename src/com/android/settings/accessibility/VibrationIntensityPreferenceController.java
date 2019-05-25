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

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

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
    private final String mEnabledKey;
    private final boolean mSupportRampingRinger;

    private Preference mPreference;

    public VibrationIntensityPreferenceController(Context context, String prefkey,
            String settingKey, String enabledKey, boolean supportRampingRinger) {
        super(context, prefkey);
        mVibrator = mContext.getSystemService(Vibrator.class);
        mSettingKey = settingKey;
        mEnabledKey = enabledKey;
        mSupportRampingRinger= supportRampingRinger;
        mSettingsContentObserver = new SettingObserver(settingKey) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateState(mPreference);
            }
        };
    }

    public VibrationIntensityPreferenceController(Context context, String prefkey,
            String settingKey, String enabledKey) {
        this(context, prefkey, settingKey, enabledKey, /* supportRampingRinger= */ false);
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
    public CharSequence getSummary() {
        final int intensity = Settings.System.getInt(mContext.getContentResolver(),
                mSettingKey, getDefaultIntensity());
        final boolean enabled = (Settings.System.getInt(mContext.getContentResolver(),
                mEnabledKey, 1) == 1) ||
                (mSupportRampingRinger && AccessibilitySettings.isRampingRingerEnabled(mContext));
        return getIntensityString(mContext, enabled ? intensity : Vibrator.VIBRATION_INTENSITY_OFF);
    }

    public static CharSequence getIntensityString(Context context, int intensity) {
        final boolean supportsMultipleIntensities = context.getResources().getBoolean(
                R.bool.config_vibration_supports_multiple_intensities);
        if (supportsMultipleIntensities) {
            switch (intensity) {
                case Vibrator.VIBRATION_INTENSITY_OFF:
                    return context.getString(R.string.accessibility_vibration_intensity_off);
                case Vibrator.VIBRATION_INTENSITY_LOW:
                    return context.getString(R.string.accessibility_vibration_intensity_low);
                case Vibrator.VIBRATION_INTENSITY_MEDIUM:
                    return context.getString(R.string.accessibility_vibration_intensity_medium);
                case Vibrator.VIBRATION_INTENSITY_HIGH:
                    return context.getString(R.string.accessibility_vibration_intensity_high);
                default:
                    return "";
            }
        } else {
            if (intensity == Vibrator.VIBRATION_INTENSITY_OFF) {
                return context.getString(R.string.switch_off_text);
            } else {
                return context.getString(R.string.switch_on_text);
            }
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
