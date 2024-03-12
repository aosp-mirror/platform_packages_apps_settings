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

import static android.uwb.UwbManager.AdapterStateCallback.STATE_CHANGED_REASON_SYSTEM_REGULATION;
import static android.uwb.UwbManager.AdapterStateCallback.STATE_DISABLED;
import static android.uwb.UwbManager.AdapterStateCallback.STATE_ENABLED_ACTIVE;
import static android.uwb.UwbManager.AdapterStateCallback.STATE_ENABLED_INACTIVE;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerExecutor;
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

/** Controller for "UWB" toggle. */
public class UwbPreferenceController extends TogglePreferenceController implements
        LifecycleObserver {
    private final UwbManager mUwbManager;
    private final UwbUtils mUwbUtils;
    private boolean mAirplaneModeOn;
    private /* @AdapterStateCallback.State */ int mState;
    private /* @AdapterStateCallback.StateChangedReason */ int mStateReason;
    private final BroadcastReceiver mAirplaneModeChangedReceiver;
    private final AdapterStateCallback mAdapterStateCallback;
    private final Executor mExecutor;
    private final Handler mHandler;
    private Preference mPreference;

    @VisibleForTesting
    public UwbPreferenceController(Context context, String key, UwbUtils uwbUtils) {
        super(context, key);
        mHandler = new Handler(context.getMainLooper());
        mExecutor = new HandlerExecutor(mHandler);
        mUwbUtils = uwbUtils;
        if (isUwbSupportedOnDevice()) {
            mUwbManager = context.getSystemService(UwbManager.class);
            mAirplaneModeChangedReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mAirplaneModeOn = mUwbUtils.isAirplaneModeOn(mContext);
                    updateState(mPreference);
                }
            };
            mAdapterStateCallback = (state, reason) -> {
                mState = state;
                mStateReason = reason;
                updateState(mPreference);
            };
            mState = mUwbManager.getAdapterState();
        } else {
            mUwbManager = null;
            mAirplaneModeChangedReceiver = null;
            mAdapterStateCallback = null;
        }
    }

    public UwbPreferenceController(Context context, String key) {
        this(context, key, new UwbUtils());
    }

    public boolean isUwbSupportedOnDevice() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_UWB);
    }

    private boolean isUwbDisabledDueToRegulatory() {
        return mState == STATE_DISABLED && mStateReason == STATE_CHANGED_REASON_SYSTEM_REGULATION;
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isUwbSupportedOnDevice()) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (mAirplaneModeOn) {
            return DISABLED_DEPENDENT_SETTING;
        } else if (isUwbDisabledDueToRegulatory()) {
            return CONDITIONALLY_UNAVAILABLE;
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
        return mState == STATE_ENABLED_ACTIVE || mState == STATE_ENABLED_INACTIVE;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isUwbSupportedOnDevice()) {
            if (mAirplaneModeOn) {
                mUwbManager.setUwbEnabled(false);
            } else {
                mUwbManager.setUwbEnabled(isChecked);
            }
        }
        return true;
    }

    /** Called when activity starts being displayed to user. */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        if (isUwbSupportedOnDevice()) {
            mState = mUwbManager.getAdapterState();
            mStateReason = AdapterStateCallback.STATE_CHANGED_REASON_ERROR_UNKNOWN;
            mAirplaneModeOn = mUwbUtils.isAirplaneModeOn(mContext);
            mUwbManager.registerAdapterStateCallback(mExecutor, mAdapterStateCallback);
            mContext.registerReceiver(mAirplaneModeChangedReceiver,
                    new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED), null, mHandler);
            refreshSummary(mPreference);
        }
    }

    /** Called when activity stops being displayed to user. */
    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        if (isUwbSupportedOnDevice()) {
            mUwbManager.unregisterAdapterStateCallback(mAdapterStateCallback);
            mContext.unregisterReceiver(mAirplaneModeChangedReceiver);
        }
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setEnabled(!mAirplaneModeOn);
        refreshSummary(mPreference);
    }

    @Override
    public CharSequence getSummary() {
        if (mAirplaneModeOn) {
            return mContext.getResources().getString(R.string.uwb_settings_summary_airplane_mode);
        } else if (isUwbDisabledDueToRegulatory()) {
            return mContext.getResources().getString(
                    R.string.uwb_settings_summary_no_uwb_regulatory);
        } else {
            return mContext.getResources().getString(R.string.uwb_settings_summary);
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_connected_devices;
    }

    @Override
    public boolean hasAsyncUpdate() {
        return true;
    }
}

