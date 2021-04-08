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

import android.Manifest;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.SensorPrivacyManager;
import android.provider.Settings;
import android.service.rotationresolver.RotationResolverService;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.view.RotationPolicy;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * SmartAutoRotateController controls whether auto rotation is enabled
 */
public class SmartAutoRotateController extends TogglePreferenceController implements
        Preference.OnPreferenceChangeListener {

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final SensorPrivacyManager mPrivacyManager;
    private Preference mPreference;

    public SmartAutoRotateController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mPrivacyManager = SensorPrivacyManager.getInstance(context);
        mPrivacyManager.addSensorPrivacyListener(CAMERA, enabled -> updateState(mPreference));
    }

    @Override
    public int getAvailabilityStatus() {
        if (!isRotationResolverServiceAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return !RotationPolicy.isRotationLocked(mContext) && hasSufficientPermission(mContext)
                && !isCameraLocked() ? AVAILABLE : DISABLED_DEPENDENT_SETTING;
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

    @Override
    public boolean isChecked() {
        return hasSufficientPermission(mContext) && !isCameraLocked() && Settings.Secure.getInt(
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

    static boolean isRotationResolverServiceAvailable(Context context) {
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
