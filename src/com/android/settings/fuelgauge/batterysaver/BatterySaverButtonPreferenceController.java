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

package com.android.settings.fuelgauge.batterysaver;

import static com.android.settingslib.fuelgauge.BatterySaverLogging.SAVER_ENABLED_SETTINGS;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.SettingsSlicesContract;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.fuelgauge.BatterySaverReceiver;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.fuelgauge.BatterySaverUtils;
import com.android.settingslib.widget.MainSwitchPreference;

/** Controller to update the battery saver button */
public class BatterySaverButtonPreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnStart, OnStop, BatterySaverReceiver.BatterySaverListener {
    private static final long SWITCH_ANIMATION_DURATION = 350L;

    private final BatterySaverReceiver mBatterySaverReceiver;
    private final PowerManager mPowerManager;

    private Handler mHandler;
    private MainSwitchPreference mPreference;

    public BatterySaverButtonPreferenceController(Context context, String key) {
        super(context, key);
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        mBatterySaverReceiver = new BatterySaverReceiver(context);
        mBatterySaverReceiver.setBatterySaverListener(this);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public Uri getSliceUri() {
        return new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsSlicesContract.AUTHORITY)
                .appendPath(SettingsSlicesContract.PATH_SETTING_ACTION)
                .appendPath(SettingsSlicesContract.KEY_BATTERY_SAVER)
                .build();
    }

    @Override
    public void onStart() {
        mBatterySaverReceiver.setListening(true);
    }

    @Override
    public void onStop() {
        mBatterySaverReceiver.setListening(false);
        mHandler.removeCallbacksAndMessages(null /* token */);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference.updateStatus(isChecked());
    }

    @Override
    public boolean isChecked() {
        return mPowerManager.isPowerSaveMode();
    }

    @Override
    public boolean setChecked(boolean stateOn) {
        return BatterySaverUtils.setPowerSaveMode(
                mContext, stateOn, false /* needFirstTimeWarning */, SAVER_ENABLED_SETTINGS);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_battery;
    }

    @Override
    public void onPowerSaveModeChanged() {
        mHandler.postDelayed(() -> onPowerSaveModeChangedInternal(), SWITCH_ANIMATION_DURATION);
    }

    private void onPowerSaveModeChangedInternal() {
        final boolean isChecked = isChecked();
        if (mPreference != null && mPreference.isChecked() != isChecked) {
            mPreference.setChecked(isChecked);
        }
    }

    @Override
    public void onBatteryChanged(boolean pluggedIn) {
        if (mPreference != null) {
            mPreference.setEnabled(!pluggedIn);
        }
    }
}
