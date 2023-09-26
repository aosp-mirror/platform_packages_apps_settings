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

package com.android.settings.display;

import static android.hardware.SensorPrivacyManager.Sensors.CAMERA;
import static android.provider.Settings.Secure.CAMERA_AUTOROTATE;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.display.SmartAutoRotateController.hasSufficientPermission;
import static com.android.settings.display.SmartAutoRotateController.isRotationResolverServiceAvailable;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.SensorPrivacyManager;
import android.os.PowerManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * SmartAutoRotatePreferenceController provides auto rotate summary in display settings
 */
public class SmartAutoRotatePreferenceController extends TogglePreferenceController
        implements LifecycleObserver {

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final SensorPrivacyManager mPrivacyManager;
    private final PowerManager mPowerManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSummary(mPreference);
        }
    };

    private final SensorPrivacyManager.OnSensorPrivacyChangedListener mPrivacyChangedListener =
            new SensorPrivacyManager.OnSensorPrivacyChangedListener() {
        @Override
        public void onSensorPrivacyChanged(int sensor, boolean enabled) {
            refreshSummary(mPreference);
        }
    };

    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;
    private Preference mPreference;

    public SmartAutoRotatePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPrivacyManager = SensorPrivacyManager.getInstance(context);
        mPowerManager = context.getSystemService(PowerManager.class);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return RotationPolicy.isRotationLockToggleVisible(mContext)
                && !DeviceStateAutoRotationHelper.isDeviceStateRotationEnabled(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "auto_rotate");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        refreshSummary(mPreference);
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mContext.registerReceiver(mReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    if (mPreference != null) {
                        updateState(mPreference);
                    }
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(mContext,
                mRotationPolicyListener);
        mPrivacyManager.addSensorPrivacyListener(CAMERA, mPrivacyChangedListener);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
        if (mRotationPolicyListener != null) {
            RotationPolicy.unregisterRotationPolicyListener(mContext,
                    mRotationPolicyListener);
        }
        mPrivacyManager.removeSensorPrivacyListener(CAMERA, mPrivacyChangedListener);
    }

    /**
     * Need this because all controller tests use Roboelectric. No easy way to mock this service,
     * so we mock the call we need
     */
    @VisibleForTesting
    boolean isCameraLocked() {
        return mPrivacyManager.isSensorPrivacyEnabled(SensorPrivacyManager.Sensors.CAMERA);
    }

    @VisibleForTesting
    boolean isPowerSaveMode() {
        return mPowerManager.isPowerSaveMode();
    }

    @Override
    public boolean isChecked() {
        return !RotationPolicy.isRotationLocked(mContext);
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final boolean isLocked = !isChecked;
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ROTATION_LOCK,
                isLocked);
        RotationPolicy.setRotationLock(mContext, isLocked,
                /* caller= */ "SmartAutoRotatePreferenceController#setChecked");
        return true;
    }

    @Override
    public CharSequence getSummary() {
        int activeStringId = R.string.auto_rotate_option_off;
        if (!RotationPolicy.isRotationLocked(mContext)) {
            final int cameraRotate = Settings.Secure.getIntForUser(
                    mContext.getContentResolver(),
                    CAMERA_AUTOROTATE,
                    0, UserHandle.USER_CURRENT);
            activeStringId = cameraRotate == 1 && isRotationResolverServiceAvailable(mContext)
                    && hasSufficientPermission(mContext)
                    && !isCameraLocked()
                    && !isPowerSaveMode()
                    ? R.string.auto_rotate_option_face_based
                    : R.string.auto_rotate_option_on;
        }
        return mContext.getString(activeStringId);
    }
}