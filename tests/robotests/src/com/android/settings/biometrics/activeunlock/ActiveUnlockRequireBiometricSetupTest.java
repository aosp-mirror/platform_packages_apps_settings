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

package com.android.settings.biometrics.activeunlock;

import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED;
import static com.android.settings.biometrics.activeunlock.ActiveUnlockRequireBiometricSetup.BIOMETRIC_ENROLL_REQUEST;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.robolectric.RuntimeEnvironment.application;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollActivity;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowActivity;

@RunWith(RobolectricTestRunner.class)
public class ActiveUnlockRequireBiometricSetupTest {

    private ActiveUnlockRequireBiometricSetup mActivity;
    private PartnerCustomizationLayout mLayout;

    @Before
    public void setUp() {
        mActivity = Robolectric.buildActivity(
                ActiveUnlockRequireBiometricSetup.class).setup().get();
        mLayout = mActivity.findViewById(R.id.setup_wizard_layout);
    }

    @Test
    public void onBackPressed_shouldFinish() {
        mActivity.onBackPressed();

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void clickCancel_shouldFinish() {
        mLayout.getMixin(FooterBarMixin.class).getSecondaryButtonView().performClick();

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void clickNext_shouldLaunchBiometricSetup() {
        final ComponentName expectedComponent = new ComponentName(application,
                BiometricEnrollActivity.InternalActivity.class);

        mLayout.getMixin(FooterBarMixin.class).getPrimaryButtonView().performClick();

        ShadowActivity.IntentForResult startedActivity = Shadows.shadowOf(
                mActivity).getNextStartedActivityForResult();
        assertWithMessage("Next activity").that(startedActivity).isNotNull();
        assertThat(startedActivity.intent.getComponent()).isEqualTo(expectedComponent);
    }

    @Test
    public void onActivityResult_shouldFinish() {
        mActivity.onActivityResult(BIOMETRIC_ENROLL_REQUEST, RESULT_FINISHED, null);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mActivity.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACTIVE_UNLOCK_REQUIRE_BIOMETRIC_SETUP);
    }
}
