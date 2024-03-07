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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.biometrics.fingerprint.feature.SfpsEnrollmentFeature;

public interface FingerprintFeatureProvider {
    /**
     * Gets the feature implementation of SFPS enrollment.
     * @return the feature implementation
     */
    SfpsEnrollmentFeature getSfpsEnrollmentFeature();


    /**
     * Gets calibrator for udfps pre-enroll
     * @param appContext application context
     * @param activitySavedInstanceState activity savedInstanceState
     * @param activityIntent activity intent
     */
    @Nullable
    default UdfpsEnrollCalibrator getUdfpsEnrollCalibrator(@NonNull Context appContext,
            @Nullable Bundle activitySavedInstanceState, @Nullable Intent activityIntent) {
        return null;
    }
}
