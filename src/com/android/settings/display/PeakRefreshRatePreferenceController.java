/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE;
import static com.android.internal.display.RefreshRateSettingsUtils.findHighestRefreshRateForDefaultDisplay;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

import java.util.concurrent.Executor;

public class PeakRefreshRatePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    @VisibleForTesting float mPeakRefreshRate;

    private static final String TAG = "RefreshRatePrefCtr";
    private static final float INVALIDATE_REFRESH_RATE = -1f;

    private final Handler mHandler;
    private final IDeviceConfigChange mOnDeviceConfigChange;
    private DeviceConfigDisplaySettings mDeviceConfigDisplaySettings;
    private Preference mPreference;

    private interface IDeviceConfigChange {
        void onDefaultRefreshRateChanged();
    }

    public PeakRefreshRatePreferenceController(Context context, String key) {
        super(context, key);
        mHandler = new Handler(context.getMainLooper());
        mDeviceConfigDisplaySettings = new DeviceConfigDisplaySettings();
        mOnDeviceConfigChange =
                new IDeviceConfigChange() {
                    public void onDefaultRefreshRateChanged() {
                        updateState(mPreference);
                    }
                };
        mPeakRefreshRate = Math.round(findHighestRefreshRateForDefaultDisplay(context));
        Log.d(
                TAG,
                "DEFAULT_REFRESH_RATE : "
                        + DEFAULT_REFRESH_RATE
                        + " mPeakRefreshRate : "
                        + mPeakRefreshRate);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        int defaultPeakRefreshRate = Math.round(mPeakRefreshRate);

        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.setSummary(mContext.getString(
                R.string.peak_refresh_rate_summary, defaultPeakRefreshRate));
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getResources().getBoolean(R.bool.config_show_smooth_display)) {
            return mPeakRefreshRate > DEFAULT_REFRESH_RATE ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
        } else {
            return UNSUPPORTED_ON_DEVICE;
        }
    }

    @Override
    public boolean isChecked() {
        final float peakRefreshRate =
                Settings.System.getFloat(
                        mContext.getContentResolver(),
                        Settings.System.PEAK_REFRESH_RATE,
                        getDefaultPeakRefreshRate());
        return Math.round(peakRefreshRate) == Math.round(mPeakRefreshRate)
                || Float.isInfinite(peakRefreshRate);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final float peakRefreshRate = isChecked ? Float.POSITIVE_INFINITY : DEFAULT_REFRESH_RATE;
        Log.d(TAG, "setChecked to : " + peakRefreshRate);

        return Settings.System.putFloat(
                mContext.getContentResolver(), Settings.System.PEAK_REFRESH_RATE, peakRefreshRate);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public void onStart() {
        mDeviceConfigDisplaySettings.startListening();
    }

    @Override
    public void onStop() {
        mDeviceConfigDisplaySettings.stopListening();
    }

    @VisibleForTesting
    void injectDeviceConfigDisplaySettings(
            DeviceConfigDisplaySettings deviceConfigDisplaySettings) {
        mDeviceConfigDisplaySettings = deviceConfigDisplaySettings;
    }

    @VisibleForTesting
    class DeviceConfigDisplaySettings
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
                updateState(mPreference);
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

        Log.d(TAG, "DeviceConfig getDefaultPeakRefreshRate : " + defaultPeakRefreshRate);
        return defaultPeakRefreshRate;
    }
}
