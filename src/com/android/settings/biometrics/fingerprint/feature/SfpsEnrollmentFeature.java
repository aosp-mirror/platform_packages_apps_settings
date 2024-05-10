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

package com.android.settings.biometrics.fingerprint.feature;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling;

import java.util.function.Function;
import java.util.function.Supplier;

public interface SfpsEnrollmentFeature {

    /**
     * Gets current SFPS enrollment stage.
     * @param progressSteps current step of enrollment
     * @param mapper a mapper to map each stage to its threshold
     * @return current enrollment stage
     */
    int getCurrentSfpsEnrollStage(int progressSteps, Function<Integer, Integer> mapper);

    /**
     * Gets the vendor string by feature.
     * @param context Context
     * @param id An integer identifying the error message
     * @param msg A human-readable string that can be shown in UI
     * @return A human-readable string of specific feature
     */
    default CharSequence getFeaturedVendorString(Context context, int id, CharSequence msg) {
        return msg;
    }

    /**
     * Gets the stage header string by feature.
     * @param stage the specific stage
     * @return the resource id of the header text of the specific stage
     */
    int getFeaturedStageHeaderResource(int stage);

    /**
     * Gets the enrollment lottie resource id per stage
     * @param stage current enrollment stage
     * @return enrollment lottie resource id
     */
    int getSfpsEnrollLottiePerStage(int stage);

    /**
     * Handles extra stuffs on receiving enrollment help.
     * @param helpMsgId help message id
     * @param helpString help message
     * @param enrollingSupplier supplier of enrolling context
     */
    default void handleOnEnrollmentHelp(int helpMsgId, CharSequence helpString,
            Supplier<FingerprintEnrollEnrolling> enrollingSupplier) {}

    /**
     * Gets the fingerprint enrollment threshold.
     * @param context context
     * @param index the enrollment stage index
     * @return threshold
     */
    float getEnrollStageThreshold(@NonNull Context context, int index);
}
