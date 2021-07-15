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
import static android.hardware.SensorPrivacyManager.Sources.DIALOG;

import static com.android.settings.display.SmartAutoRotateController.isRotationResolverServiceAvailable;

import android.content.Context;
import android.hardware.SensorPrivacyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.BannerMessagePreference;

/**
 * The controller of camera based rotate privacy sensor warning preference. The preference appears
 * when the privacy sensor service disables camera functionality completely.
 */
public class SmartAutoRotateCameraStateController extends BasePreferenceController {

    private final SensorPrivacyManager mPrivacyManager;
    private Preference mPreference;

    public SmartAutoRotateCameraStateController(Context context, String key) {
        super(context, key);
        mPrivacyManager = SensorPrivacyManager.getInstance(context);
        mPrivacyManager.addSensorPrivacyListener(CAMERA, (sensor, enabled) -> {
            if (mPreference != null) {
                mPreference.setVisible(isAvailable());
            }
            updateState(mPreference);
        });
    }

    @VisibleForTesting
    boolean isCameraLocked() {
        return mPrivacyManager.isSensorPrivacyEnabled(SensorPrivacyManager.Sensors.CAMERA);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        ((BannerMessagePreference) mPreference)
                .setPositiveButtonText(R.string.allow)
                .setPositiveButtonOnClickListener(v -> {
                    mPrivacyManager.setSensorPrivacy(DIALOG, CAMERA, false);
                });
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return isRotationResolverServiceAvailable(mContext)
                && isCameraLocked() ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }
}
