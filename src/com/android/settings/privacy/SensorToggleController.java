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

package com.android.settings.privacy;

import android.content.Context;
import android.provider.DeviceConfig;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.utils.SensorPrivacyManagerHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.concurrent.Executor;

/**
 * Base class for sensor toggle controllers
 */
public abstract class SensorToggleController extends TogglePreferenceController implements
        SensorPrivacyManagerHelper.Callback, LifecycleObserver {

    protected final SensorPrivacyManagerHelper mSensorPrivacyManagerHelper;
    private final Executor mCallbackExecutor;

    private PreferenceScreen mScreen;

    /** For testing since DeviceConfig uses static method calls */
    private boolean mIgnoreDeviceConfig;

    public SensorToggleController(Context context, String preferenceKey) {
        this(context, preferenceKey, SensorPrivacyManagerHelper.getInstance(context), false);
    }

    @VisibleForTesting
    SensorToggleController(Context context, String preferenceKey,
            SensorPrivacyManagerHelper sensorPrivacyManagerHelper, boolean ignoreDeviceConfig) {
        super(context, preferenceKey);

        mIgnoreDeviceConfig = ignoreDeviceConfig;
        mSensorPrivacyManagerHelper = sensorPrivacyManagerHelper;
        mCallbackExecutor = context.getMainExecutor();
    }

    /**
     * The sensor id, defined in SensorPrivacyManagerHelper, which an implementing class controls
     */
    public abstract int getSensor();

    /**
     * The key for the device config setting for whether the feature is enabled.
     */
    public abstract String getDeviceConfigKey();

    protected String getRestriction() {
        return null;
    }

    @Override
    public int getAvailabilityStatus() {
        return mSensorPrivacyManagerHelper.supportsSensorToggle(getSensor())
                && (mIgnoreDeviceConfig || DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_PRIVACY,
                getDeviceConfigKey(), true)) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        return !mSensorPrivacyManagerHelper.isSensorBlocked(getSensor());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mSensorPrivacyManagerHelper.setSensorBlocked(getSensor(), !isChecked);
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mScreen = screen;

        RestrictedSwitchPreference preference = mScreen.findPreference(getPreferenceKey());
        if (preference != null) {
            preference.setDisabledByAdmin(RestrictedLockUtilsInternal
                    .checkIfRestrictionEnforced(mContext, getRestriction(), mContext.getUserId()));
        }
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_privacy;
    }

    @Override
    public void onSensorPrivacyChanged(int toggleType, int sensor, boolean blocked) {
        updateState(mScreen.findPreference(mPreferenceKey));
    }

    /**
     * onStart lifecycle event
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        mSensorPrivacyManagerHelper.addSensorBlockedListener(getSensor(), mCallbackExecutor, this);
    }

    /**
     * onStop lifecycle event
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        mSensorPrivacyManagerHelper.removeSensorBlockedListener(this);
    }
}
