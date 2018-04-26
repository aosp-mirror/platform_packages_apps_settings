/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.fuelgauge.batterysaver;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Controller that update the battery saver seekbar
 */
public class AutoBatterySeekBarPreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, SeekBarPreference.OnPreferenceChangeListener {
    private static final String TAG = "AutoBatterySeekBarPreferenceController";
    @VisibleForTesting
    static final String KEY_AUTO_BATTERY_SEEK_BAR = "battery_saver_seek_bar";
    private SeekBarPreference mPreference;
    private AutoBatterySaverSettingObserver mContentObserver;

    public AutoBatterySeekBarPreferenceController(Context context, Lifecycle lifecycle) {
        super(context, KEY_AUTO_BATTERY_SEEK_BAR);
        mContentObserver = new AutoBatterySaverSettingObserver(new Handler(Looper.getMainLooper()));
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = (SeekBarPreference) screen.findPreference(
                KEY_AUTO_BATTERY_SEEK_BAR);
        mPreference.setContinuousUpdates(true);
        mPreference.setAccessibilityRangeInfoType(
                AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_PERCENT);
        updatePreference(mPreference);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        updatePreference(preference);
    }

    @Override
    public void onStart() {
        mContentObserver.registerContentObserver();
    }

    @Override
    public void onStop() {
        mContentObserver.unRegisterContentObserver();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final int progress = (int) newValue;
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL, progress);
        return true;
    }

    @VisibleForTesting
    void updatePreference(Preference preference) {
        final ContentResolver contentResolver = mContext.getContentResolver();

        // Override the max value with LOW_POWER_MODE_TRIGGER_LEVEL_MAX, if set.
        final int maxLevel = Settings.Global.getInt(contentResolver,
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL_MAX, 0);
        if (maxLevel > 0) {
            if (!(preference instanceof SeekBarPreference)) {
                Log.e(TAG, "Unexpected preference class: " + preference.getClass());
            } else {
                final SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
                if (maxLevel < seekBarPreference.getMin()) {
                    Log.e(TAG, "LOW_POWER_MODE_TRIGGER_LEVEL_MAX too low; ignored.");
                } else {
                    seekBarPreference.setMax(maxLevel);
                }
            }
        }

        // Set the current value.
        final int level = Settings.Global.getInt(contentResolver,
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL,
                AutoBatterySaverPreferenceController.DEFAULT_TRIGGER_LEVEL);
        if (level == 0) {
            preference.setVisible(false);
        } else {
            preference.setVisible(true);
            preference.setTitle(mContext.getString(R.string.battery_saver_seekbar_title,
                    Utils.formatPercentage(level)));
            SeekBarPreference seekBarPreference = (SeekBarPreference) preference;
            seekBarPreference.setProgress(level);
            seekBarPreference.setSeekBarContentDescription(
                    mContext.getString(R.string.battery_saver_turn_on_automatically_title));
        }
    }

    /**
     * Observer that listens to change from {@link Settings.Global#LOW_POWER_MODE_TRIGGER_LEVEL}
     */
    private final class AutoBatterySaverSettingObserver extends ContentObserver {
        private final Uri mUri = Settings.Global.getUriFor(
                Settings.Global.LOW_POWER_MODE_TRIGGER_LEVEL);
        private final ContentResolver mContentResolver;

        public AutoBatterySaverSettingObserver(Handler handler) {
            super(handler);
            mContentResolver = mContext.getContentResolver();
        }

        public void registerContentObserver() {
            mContentResolver.registerContentObserver(mUri, false, this);
        }

        public void unRegisterContentObserver() {
            mContentResolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri, int userId) {
            if (mUri.equals(uri)) {
                updatePreference(mPreference);
            }
        }
    }
}
