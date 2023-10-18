/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.fingerprint;

import androidx.annotation.Nullable;

import com.android.settings.biometrics.fingerprint.feature.SfpsEnrollmentFeature;

import java.util.UUID;

public interface FingerprintFeatureProvider {
    /**
     * Gets the feature implementation of SFPS enrollment.
     * @return the feature implementation
     */
    SfpsEnrollmentFeature getSfpsEnrollmentFeature();

    /**
     * Gets calibrator to calibrate the FPS before enrolling udfps
     * @param uuid unique id for passed between different activities
     * @return udfps calibrator
     */
    @Nullable
    UdfpsEnrollCalibrator getUdfpsEnrollCalibrator(@Nullable UUID uuid);
}
