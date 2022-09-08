/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.uwb;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.provider.Settings;
import android.uwb.UwbManager;
import android.uwb.UwbManager.AdapterStateCallback;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Controller for "UWB" toggle. */
public class UwbPreferenceController extends TogglePreferenceController implements
        AdapterStateCallback, LifecycleObserver {
    @VisibleForTesting
    static final String KEY_UWB_SETTINGS = "uwb_settings";
    @VisibleForTesting
    UwbManager mUwbManager;
    @VisibleForTesting
    boolean mAirplaneModeOn;
    @VisibleForTesting
    private final BroadcastReceiver mAirplaneModeChangedReceiver;
    private final Executor mExecutor;
    private final Handler mHandler;
    private Preference mPreference;

    public UwbPreferenceController(Context context, String key) {
        super(context, key);
        mExecutor = Executors.newSingleThreadExecutor();
        mHandler = new Handler(context.getMainLooper());
        if (isUwbSupportedOnDevice()) {
            mUwbManager = context.getSystemService(UwbManager.class);
        }
        mAirplaneModeOn = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        mAirplaneModeChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mAirplaneModeOn = Settings.Global.getInt(mContext.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
                updateState(mPreference);
            }
        };
    }

    public boolean isUwbSupportedOnDevice() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_UWB);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isUwbSupportedOnDevice()) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (mAirplaneModeOn) {
            return DISABLED_DEPENDENT_SETTING;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean isChecked() {
        if (!isUwbSupportedOnDevice()) {
            return false;
        }
        int state = mUwbManager.getAdapterState();
        return state == STATE_ENABLED_ACTIVE || state == STATE_ENABLED_INACTIVE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mAirplaneModeOn = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        if (isUwbSupportedOnDevice()) {
            if (mAirplaneModeOn) {
                mUwbManager.setUwbEnabled(false);
            } else {
                mUwbManager.setUwbEnabled(isChecked);
            }
        }
        return true;
    }

    @Override
    public void onStateChanged(int state, int reason) {
        Runnable runnable = () -> updateState(mPreference);
        mHandler.post(runnable);
    }

    /** Called when activity starts being displayed to user. */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        if (isUwbSupportedOnDevice()) {
            mUwbManager.registerAdapterStateCallback(mExecutor, this);
        }
        if (mAirplaneModeChangedReceiver != null) {
            mContext.registerReceiver(mAirplaneModeChangedReceiver,
                    new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED));
        }
        refreshSummary(mPreference);
    }

    /** Called when activity stops being displayed to user. */
    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        if (isUwbSupportedOnDevice()) {
            mUwbManager.unregisterAdapterStateCallback(this);
        }
        if (mAirplaneModeChangedReceiver != null) {
            mContext.unregisterReceiver(mAirplaneModeChangedReceiver);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!mAirplaneModeOn);
        refreshSummary(preference);
    }

    @Override
    public CharSequence getSummary() {
        if (mAirplaneModeOn) {
            return mContext.getResources().getString(R.string.uwb_settings_summary_airplane_mode);
        } else {
            return mContext.getResources().getString(R.string.uwb_settings_summary);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_connected_devices;
    }
}

