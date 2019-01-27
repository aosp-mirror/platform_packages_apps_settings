/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.fingerprint;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.os.CancellationSignal;
import android.widget.Button;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class FingerprintEnrollFindSensorTest {

    @Mock
    private FingerprintManager mFingerprintManager;

    private FingerprintEnrollFindSensor mActivity;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        FakeFeatureFactory.setupForTest();

        mActivity = Robolectric.buildActivity(
                FingerprintEnrollFindSensor.class,
                new Intent()
                        // Set the challenge token so the confirm screen will not be shown
                        .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]))
                .setup().get();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    @Test
    public void enrollFingerprint_shouldProceed() {
        EnrollmentCallback enrollmentCallback = verifyAndCaptureEnrollmentCallback();

        enrollmentCallback.onEnrollmentProgress(123);
        enrollmentCallback.onEnrollmentError(FingerprintManager.FINGERPRINT_ERROR_CANCELED, "test");

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        IntentForResult startedActivity =
                shadowActivity.getNextStartedActivityForResult();
        assertThat(startedActivity).named("Next activity 1").isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(new ComponentName(application, FingerprintEnrollEnrolling.class));
    }

    @Test
    public void enrollFingerprintTwice_shouldStartOneEnrolling() {
        EnrollmentCallback enrollmentCallback = verifyAndCaptureEnrollmentCallback();

        enrollmentCallback.onEnrollmentProgress(123);
        enrollmentCallback.onEnrollmentProgress(123);  // A second enroll should be a no-op

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        IntentForResult startedActivity =
                shadowActivity.getNextStartedActivityForResult();
        assertThat(startedActivity).named("Next activity 1").isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(new ComponentName(application, FingerprintEnrollEnrolling.class));

        // Should only start one next activity
        assertThat(shadowActivity.getNextStartedActivityForResult()).named("Next activity 2")
                .isNull();
    }

    // Use a non-default resource qualifier to load the test layout in
    // robotests/res/layout-mcc999/fingerprint_enroll_find_sensor. This layout is a copy of the
    // regular find sensor layout, with the animation removed.
    @Config(qualifiers = "mcc999")
    @Test
    public void layoutWithoutAnimation_shouldNotCrash() {
        EnrollmentCallback enrollmentCallback = verifyAndCaptureEnrollmentCallback();
        enrollmentCallback.onEnrollmentProgress(123);
        enrollmentCallback.onEnrollmentError(FingerprintManager.FINGERPRINT_ERROR_CANCELED, "test");

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        IntentForResult startedActivity =
                shadowActivity.getNextStartedActivityForResult();
        assertThat(startedActivity).named("Next activity").isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(new ComponentName(application, FingerprintEnrollEnrolling.class));
    }

    @Test
    public void clickSkip_shouldReturnResultSkip() {
        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        layout.getMixin(FooterBarMixin.class).getSecondaryButtonView().performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        assertThat(shadowActivity.getResultCode()).named("result code")
                .isEqualTo(BiometricEnrollBase.RESULT_SKIP);
    }

    private EnrollmentCallback verifyAndCaptureEnrollmentCallback() {
        ArgumentCaptor<EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(EnrollmentCallback.class);
        verify(mFingerprintManager).enroll(
                any(byte[].class),
                any(CancellationSignal.class),
                anyInt(),
                anyInt(),
                callbackCaptor.capture());

        return callbackCaptor.getValue();
    }

    @Test
    public void onActivityResult_withNullIntentShouldNotCrash() {
        // this should not crash
        mActivity.onActivityResult(BiometricEnrollBase.CONFIRM_REQUEST, Activity.RESULT_OK,
            null);
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(Activity.RESULT_CANCELED);
    }
}
