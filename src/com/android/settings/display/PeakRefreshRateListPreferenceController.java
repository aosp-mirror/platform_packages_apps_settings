/*
 * Copyright (C) 2020 The Android Open Source Project
 * Copyright (C) 2021 The LineageOS Project
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

package com.android.settings.display;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.List;
import java.util.Locale;

public class PeakRefreshRateListPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop, Preference.OnPreferenceChangeListener {

    private static float DEFAULT_REFRESH_RATE = 60f;

    private static final String TAG = "PeakRefreshRatePrefCtr";
    private static final float INVALIDATE_REFRESH_RATE = -1f;

    private final Handler mHandler;
    private final IDeviceConfigChange mOnDeviceConfigChange;
    private final DeviceConfigDisplaySettings mDeviceConfigDisplaySettings;
    private ListPreference mListPreference;

    private List<String> mEntries = new ArrayList<>();
    private List<String> mValues = new ArrayList<>();

    private interface IDeviceConfigChange {
        void onDefaultRefreshRateChanged();
    }

    public PeakRefreshRateListPreferenceController(Context context, String key) {
        super(context, key);
        mHandler = new Handler(context.getMainLooper());
        mDeviceConfigDisplaySettings = new DeviceConfigDisplaySettings();
        mOnDeviceConfigChange =
                new IDeviceConfigChange() {
                    public void onDefaultRefreshRateChanged() {
                        updateState(mListPreference);
                    }
                };

        final DisplayManager dm = mContext.getSystemService(DisplayManager.class);
        final Display display = dm.getDisplay(Display.DEFAULT_DISPLAY);

        if (display == null) {
            Log.w(TAG, "No valid default display device");
        } else {
            Display.Mode mode = display.getMode();
            Display.Mode[] modes = display.getSupportedModes();
            for (Display.Mode m : modes) {
                if (m.getPhysicalWidth() == mode.getPhysicalWidth() &&
                        m.getPhysicalHeight() == mode.getPhysicalHeight()) {
                    mEntries.add(String.format("%.02fHz", m.getRefreshRate())
                            .replaceAll("[\\.,]00", ""));
                    mValues.add(String.format(Locale.US, "%.02f", m.getRefreshRate()));
                }
            }
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mListPreference = screen.findPreference(getPreferenceKey());
        mListPreference.setEntries(mEntries.toArray(new String[mEntries.size()]));
        mListPreference.setEntryValues(mValues.toArray(new String[mValues.size()]));
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getResources().getBoolean(R.bool.config_show_peak_refresh_rate_switch)) {
            return AVAILABLE;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public void updateState(Preference preference) {
        final float currentValue = Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, getDefaultPeakRefreshRate());
        int index = mListPreference.findIndexOfValue(
                String.format(Locale.US, "%.02f", currentValue));
        if (index < 0) index = 0;
        mListPreference.setValueIndex(index);
        mListPreference.setSummary(mListPreference.getEntries()[index]);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Settings.System.putFloat(mContext.getContentResolver(), Settings.System.PEAK_REFRESH_RATE,
                Float.valueOf((String) newValue));
        updateState(preference);
        return true;
    }

    @Override
    public void onStart() {
        mDeviceConfigDisplaySettings.startListening();
    }

    @Override
    public void onStop() {
        mDeviceConfigDisplaySettings.stopListening();
    }

    private float findPeakRefreshRate(Display.Mode[] modes) {
        float peakRefreshRate = DEFAULT_REFRESH_RATE;
        for (Display.Mode mode : modes) {
            if (Math.round(mode.getRefreshRate()) > DEFAULT_REFRESH_RATE) {
                peakRefreshRate = mode.getRefreshRate();
            }
        }
        return peakRefreshRate;
    }

    private class DeviceConfigDisplaySettings
            implements DeviceConfig.OnPropertiesChangedListener, Executor {
        public void startListening() {
            DeviceConfig.addOnPropertiesChangedListener(
                    DeviceConfig.NAMESPACE_DISPLAY_MANAGER,
                    this /* Executor */,
                    this /* Listener */);
        }

        public void stopListening() {
            DeviceConfig.removeOnPropertiesChangedListener(this);
        }

        public float getDefaultPeakRefreshRate() {
            float defaultPeakRefreshRate =
                    DeviceConfig.getFloat(
                            DeviceConfig.NAMESPACE_DISPLAY_MANAGER,
                            DisplayManager.DeviceConfig.KEY_PEAK_REFRESH_RATE_DEFAULT,
                            INVALIDATE_REFRESH_RATE);
            Log.d(TAG, "DeviceConfig getDefaultPeakRefreshRate : " + defaultPeakRefreshRate);

            return defaultPeakRefreshRate;
        }

        @Override
        public void onPropertiesChanged(DeviceConfig.Properties properties) {
            // Got notified if any property has been changed in NAMESPACE_DISPLAY_MANAGER. The
            // KEY_PEAK_REFRESH_RATE_DEFAULT value could be added, changed, removed or unchanged.
            // Just force a UI update for any case.
            if (mOnDeviceConfigChange != null) {
                mOnDeviceConfigChange.onDefaultRefreshRateChanged();
                updateState(mListPreference);
            }
        }

        @Override
        public void execute(Runnable runnable) {
            if (mHandler != null) {
                mHandler.post(runnable);
            }
        }
    }

    private float getDefaultPeakRefreshRate() {
        float defaultPeakRefreshRate = mDeviceConfigDisplaySettings.getDefaultPeakRefreshRate();
        if (defaultPeakRefreshRate == INVALIDATE_REFRESH_RATE) {
            defaultPeakRefreshRate = (float) mContext.getResources().getInteger(
                    com.android.internal.R.integer.config_defaultPeakRefreshRate);
        }

        return defaultPeakRefreshRate;
    }
}
