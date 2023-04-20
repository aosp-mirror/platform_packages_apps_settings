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

import android.Manifest;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.SensorPrivacyManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.service.rotationresolver.RotationResolverService;
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
import com.android.settingslib.devicestate.DeviceStateRotationLockSettingsManager;

/**
 * SmartAutoRotateController controls whether auto rotation is enabled
 */
public class SmartAutoRotateController extends TogglePreferenceController implements
        Preference.OnPreferenceChangeListener, LifecycleObserver {

    protected Preference mPreference;

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final SensorPrivacyManager mPrivacyManager;
    private final PowerManager mPowerManager;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateState(mPreference);
        }
    };
    private final DeviceStateRotationLockSettingsManager mDeviceStateAutoRotateSettingsManager;
    private final DeviceStateRotationLockSettingsManager.DeviceStateRotationLockSettingsListener
            mDeviceStateRotationLockSettingsListener = () -> updateState(mPreference);
    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;

    public SmartAutoRotateController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mPrivacyManager = SensorPrivacyManager.getInstance(context);
        mPrivacyManager
                .addSensorPrivacyListener(CAMERA, (sensor, enabled) -> updateState(mPreference));
        mPowerManager = context.getSystemService(PowerManager.class);
        mDeviceStateAutoRotateSettingsManager = DeviceStateRotationLockSettingsManager.getInstance(
                context);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isRotationResolverServiceAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return !isRotationLocked() && hasSufficientPermission(mContext)
                && !isCameraLocked() && !isPowerSaveMode() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
    }

    protected boolean isRotationLocked() {
        if (DeviceStateAutoRotationHelper.isDeviceStateRotationEnabled(mContext)) {
            return mDeviceStateAutoRotateSettingsManager.isRotationLockedForAllStates();
        }
        return RotationPolicy.isRotationLocked(mContext);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setEnabled(getAvailabilityStatus() == AVAILABLE);
        }
    }

    /**
     * Need this because all controller tests use RoboElectric. No easy way to mock this service,
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

    @OnLifecycleEvent(ON_START)
    public void onStart() {
        mContext.registerReceiver(mReceiver,
                new IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED));
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    updateState(mPreference);
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(mContext, mRotationPolicyListener);
        mDeviceStateAutoRotateSettingsManager.registerListener(
                mDeviceStateRotationLockSettingsListener);
    }

    @OnLifecycleEvent(ON_STOP)
    public void onStop() {
        mContext.unregisterReceiver(mReceiver);
        if (mRotationPolicyListener != null) {
            RotationPolicy.unregisterRotationPolicyListener(mContext, mRotationPolicyListener);
            mRotationPolicyListener = null;
        }
        mDeviceStateAutoRotateSettingsManager.unregisterListener(
                mDeviceStateRotationLockSettingsListener);
    }

    @Override
    public boolean isChecked() {
        return !isRotationLocked() && hasSufficientPermission(mContext)
                && !isCameraLocked() && !isPowerSaveMode() && Settings.Secure.getInt(
                mContext.getContentResolver(),
                CAMERA_AUTOROTATE, 0) == 1;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_CAMERA_ROTATE_TOGGLE,
                isChecked);
        Settings.Secure.putInt(mContext.getContentResolver(),
                CAMERA_AUTOROTATE,
                isChecked ? 1 : 0);
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    /**
     * Returns true if there is a {@link RotationResolverService} available
     */
    public static boolean isRotationResolverServiceAvailable(Context context) {
        if (!context.getResources().getBoolean(
                R.bool.config_auto_rotate_face_detection_available)) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        final String resolvePackage = packageManager.getRotationResolverPackageName();
        if (TextUtils.isEmpty(resolvePackage)) {
            return false;
        }
        final Intent intent = new Intent(RotationResolverService.SERVICE_INTERFACE).setPackage(
                resolvePackage);
        final ResolveInfo resolveInfo = packageManager.resolveService(intent,
                PackageManager.MATCH_SYSTEM_ONLY);
        return resolveInfo != null && resolveInfo.serviceInfo != null;
    }

    static boolean hasSufficientPermission(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final String rotationPackage = packageManager.getRotationResolverPackageName();
        return rotationPackage != null && packageManager.checkPermission(
                Manifest.permission.CAMERA, rotationPackage) == PackageManager.PERMISSION_GRANTED;
    }
}
