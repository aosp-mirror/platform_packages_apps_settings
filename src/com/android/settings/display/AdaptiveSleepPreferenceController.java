/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import android.Manifest;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.hardware.SensorPrivacyManager;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.service.attention.AttentionService;
import android.text.TextUtils;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.bluetooth.RestrictionUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedSwitchPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.common.annotations.VisibleForTesting;

/** The controller for Screen attention switch preference. */
public class AdaptiveSleepPreferenceController {
    public static final String PREFERENCE_KEY = "adaptive_sleep";
    private static final int DEFAULT_VALUE = 0;
    private final SensorPrivacyManager mPrivacyManager;
    private final RestrictionUtils mRestrictionUtils;
    private final PackageManager mPackageManager;
    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final PowerManager mPowerManager;

    @VisibleForTesting
    RestrictedSwitchPreference mPreference;

    public AdaptiveSleepPreferenceController(Context context, RestrictionUtils restrictionUtils) {
        mContext = context;
        mRestrictionUtils = restrictionUtils;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mPrivacyManager = SensorPrivacyManager.getInstance(context);
        mPowerManager = context.getSystemService(PowerManager.class);
        mPackageManager = context.getPackageManager();
    }

    public AdaptiveSleepPreferenceController(Context context) {
        this(context, new RestrictionUtils());
    }

    /**
     * Adds the controlled preference to the provided preference screen.
     */
    public void addToScreen(PreferenceScreen screen) {
        updatePreference();
        screen.addPreference(mPreference);
    }

    /**
     * Updates the appearance of the preference.
     */
    public void updatePreference() {
        initializePreference();
        final EnforcedAdmin enforcedAdmin = mRestrictionUtils.checkIfRestrictionEnforced(mContext,
                UserManager.DISALLOW_CONFIG_SCREEN_TIMEOUT);
        if (enforcedAdmin != null) {
            mPreference.setDisabledByAdmin(enforcedAdmin);
        } else {
            mPreference.setChecked(isChecked());
            mPreference.setEnabled(hasSufficientPermission(mPackageManager) && !isCameraLocked()
                    && !isPowerSaveMode());
        }
    }

    @VisibleForTesting
    void initializePreference() {
        if (mPreference == null) {
            mPreference = new RestrictedSwitchPreference(mContext);
            mPreference.setTitle(R.string.adaptive_sleep_title);
            mPreference.setSummary(R.string.adaptive_sleep_description);
            mPreference.setChecked(isChecked());
            mPreference.setKey(PREFERENCE_KEY);
            mPreference.setOnPreferenceClickListener(preference -> {
                final boolean isChecked = ((RestrictedSwitchPreference) preference).isChecked();
                mMetricsFeatureProvider.action(mContext,
                        SettingsEnums.ACTION_SCREEN_ATTENTION_CHANGED,
                        isChecked);
                Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.ADAPTIVE_SLEEP, isChecked ? 1 : DEFAULT_VALUE);
                return true;
            });
        }
    }

    @VisibleForTesting
    boolean isChecked() {
        return hasSufficientPermission(mContext.getPackageManager()) && !isCameraLocked()
                && !isPowerSaveMode() && Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ADAPTIVE_SLEEP, DEFAULT_VALUE)
                != DEFAULT_VALUE;
    }

    /**
     * Need this because all controller tests use RoboElectric. No easy way to mock this service,
     * so we mock the call we need
     */
    @VisibleForTesting
    boolean isCameraLocked() {
        return mPrivacyManager.isSensorPrivacyEnabled(CAMERA);
    }

    @VisibleForTesting
    boolean isPowerSaveMode() {
        return mPowerManager.isPowerSaveMode();
    }

    public static int isControllerAvailable(Context context) {
        return isAdaptiveSleepSupported(context)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    static boolean isAdaptiveSleepSupported(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_adaptive_sleep_available)
                && isAttentionServiceAvailable(context);
    }

    private static boolean isAttentionServiceAvailable(Context context) {
        final PackageManager packageManager = context.getPackageManager();
        final String resolvePackage = packageManager.getAttentionServicePackageName();
        if (TextUtils.isEmpty(resolvePackage)) {
            return false;
        }
        final Intent intent = new Intent(AttentionService.SERVICE_INTERFACE).setPackage(
                resolvePackage);
        final ResolveInfo resolveInfo = packageManager.resolveService(intent,
                PackageManager.MATCH_SYSTEM_ONLY);
        return resolveInfo != null && resolveInfo.serviceInfo != null;
    }

    static boolean hasSufficientPermission(PackageManager packageManager) {
        final String attentionPackage = packageManager.getAttentionServicePackageName();
        return attentionPackage != null && packageManager.checkPermission(
                Manifest.permission.CAMERA, attentionPackage) == PackageManager.PERMISSION_GRANTED;
    }
}
