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

import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_CENTER;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_FINGERTIP;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_LEFT_EDGE;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_NO_ANIMATION;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_RIGHT_EDGE;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.STAGE_UNKNOWN;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;

import java.util.function.Function;

public class SfpsEnrollmentFeatureImpl implements SfpsEnrollmentFeature {
    @VisibleForTesting
    public static final int HELP_ANIMATOR_DURATION = 550;

    @Nullable
    private FingerprintManager mFingerprintManager = null;

    @Override
    public int getCurrentSfpsEnrollStage(int progressSteps, Function<Integer, Integer> mapper) {
        if (mapper == null) {
            return STAGE_UNKNOWN;
        }
        if (progressSteps < mapper.apply(0)) {
            return SFPS_STAGE_NO_ANIMATION;
        } else if (progressSteps < mapper.apply(1)) {
            return SFPS_STAGE_CENTER;
        } else if (progressSteps < mapper.apply(2)) {
            return SFPS_STAGE_FINGERTIP;
        } else if (progressSteps < mapper.apply(3)) {
            return SFPS_STAGE_LEFT_EDGE;
        } else {
            return SFPS_STAGE_RIGHT_EDGE;
        }
    }

    @Override
    public int getFeaturedStageHeaderResource(int stage) {
        return switch (stage) {
            case SFPS_STAGE_NO_ANIMATION
                    -> R.string.security_settings_fingerprint_enroll_repeat_title;
            case SFPS_STAGE_CENTER -> R.string.security_settings_sfps_enroll_finger_center_title;
            case SFPS_STAGE_FINGERTIP -> R.string.security_settings_sfps_enroll_fingertip_title;
            case SFPS_STAGE_LEFT_EDGE -> R.string.security_settings_sfps_enroll_left_edge_title;
            case SFPS_STAGE_RIGHT_EDGE -> R.string.security_settings_sfps_enroll_right_edge_title;
            default -> throw new IllegalArgumentException("Invalid stage: " + stage);
        };
    }

    @Override
    public int getSfpsEnrollLottiePerStage(int stage) {
        return switch (stage) {
            case SFPS_STAGE_NO_ANIMATION -> R.raw.sfps_lottie_no_animation;
            case SFPS_STAGE_CENTER -> R.raw.sfps_lottie_pad_center;
            case SFPS_STAGE_FINGERTIP -> R.raw.sfps_lottie_tip;
            case SFPS_STAGE_LEFT_EDGE -> R.raw.sfps_lottie_left_edge;
            case SFPS_STAGE_RIGHT_EDGE -> R.raw.sfps_lottie_right_edge;
            default -> throw new IllegalArgumentException("Invalid stage: " + stage);
        };
    }

    @Override
    public float getEnrollStageThreshold(@NonNull Context context, int index) {
        if (mFingerprintManager == null) {
            mFingerprintManager = context.getSystemService(FingerprintManager.class);
        }
        return mFingerprintManager.getEnrollStageThreshold(index);
    }

    @Override
    public Animator getHelpAnimator(@NonNull View target) {
        final float translationX = 40;
        final ObjectAnimator help = ObjectAnimator.ofFloat(target,
                "translationX" /* propertyName */,
                0, translationX, -1 * translationX, translationX, 0f);
        help.setInterpolator(new AccelerateInterpolator());
        help.setDuration(HELP_ANIMATOR_DURATION);
        help.setAutoCancel(false);
        return help;
    }
}
