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

import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_POWER_BUTTON;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;

import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.KEY_STATE_PREVIOUS_ROTATION;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_NO_ANIMATION;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.SFPS_STAGE_RIGHT_EDGE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Vibrator;
import android.view.Display;
import android.view.Surface;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.RingProgressBar;

import com.airbnb.lottie.LottieAnimationView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class FingerprintEnrollEnrollingTest {

    @Mock private FingerprintManager mFingerprintManager;

    @Mock private Vibrator mVibrator;

    @Mock private LottieAnimationView mIllustrationLottie;

    @Mock private FingerprintEnrollSidecar mSidecar;

    @Mock private Display mMockDisplay;

    private Resources.Theme mTheme;

    private final int[] mSfpsStageThresholds = new int[]{0, 9, 13, 19, 25};

    private FingerprintEnrollEnrolling mActivity;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
    }

    @Test
    public void fingerprintUdfpsEnrollSuccessProgress_shouldNotVibrate() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        mActivity.onEnrollmentProgressChange(1, 1);

        verify(mVibrator, never()).vibrate(anyInt(), anyString(), any(), anyString(), any());
    }

    @Test
    public void fingerprintRearEnrollSuccessProgress_shouldNotVibrate() {
        initializeActivityFor(FingerprintSensorProperties.TYPE_REAR);

        mActivity.onEnrollmentProgressChange(1, 1);

        verify(mVibrator, never()).vibrate(anyInt(), anyString(), any(), anyString(), any());
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_gainFocus_shouldNotCancel() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        mActivity.onEnrollmentProgressChange(1, 1);
        mActivity.onWindowFocusChanged(true);

        verify(mActivity, never()).onCancelEnrollment(anyInt());
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_loseFocus_shouldCancel() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        mActivity.onEnrollmentProgressChange(1, 1);
        mActivity.onWindowFocusChanged(false);

        verify(mActivity, never()).onCancelEnrollment(anyInt());
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_loseFocusWithCancelFlag_shouldNotCancelAgain() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        mActivity.mIsCanceled = true;
        mActivity.onWindowFocusChanged(true);

        verify(mActivity, never()).onCancelEnrollment(anyInt());
    }

    @Test
    public void fingerprintSfpsEnroll_PlaysAllAnimationsAssetsCorrectly() {
        initializeActivityFor(TYPE_POWER_BUTTON);

        int totalEnrollSteps = 25;
        int initStageSteps = -1, initStageRemaining = 0;

        when(mSidecar.getEnrollmentSteps()).thenReturn(initStageSteps);
        when(mSidecar.getEnrollmentRemaining()).thenReturn(initStageRemaining);

        mActivity.onEnrollmentProgressChange(initStageSteps, initStageRemaining);

        when(mSidecar.getEnrollmentSteps()).thenReturn(totalEnrollSteps);

        for (int remaining = totalEnrollSteps; remaining > 0; remaining--) {
            when(mSidecar.getEnrollmentRemaining()).thenReturn(remaining);
            mActivity.onEnrollmentProgressChange(totalEnrollSteps, remaining);
        }

        List<Integer> expectedLottieAssetOrder = List.of(
                R.raw.sfps_lottie_no_animation,
                R.raw.sfps_lottie_pad_center,
                R.raw.sfps_lottie_tip,
                R.raw.sfps_lottie_left_edge,
                R.raw.sfps_lottie_right_edge
        );

        ArgumentCaptor<Integer> lottieAssetCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mIllustrationLottie, times(5)).setAnimation(lottieAssetCaptor.capture());
        List<Integer> observedLottieAssetOrder = lottieAssetCaptor.getAllValues();
        assertThat(observedLottieAssetOrder).isEqualTo(expectedLottieAssetOrder);
    }

    // SFPS_STAGE_CENTER is first stage with progress bar colors, starts at steps=25, remaining=25
    private void configureSfpsStageColorTest() {
        when(mSidecar.getEnrollmentSteps()).thenReturn(25);
        when(mSidecar.getEnrollmentRemaining()).thenReturn(25);
        mActivity.onEnrollmentProgressChange(25, 25);
    }

    @Test
    public void fingerprintSfpsEnroll_usesCorrectProgressBarFillColor() {
        initializeActivityFor(TYPE_POWER_BUTTON);

        configureSfpsStageColorTest();

        int progress_bar_fill_color = mActivity.getApplicationContext().getColor(
                R.color.sfps_enrollment_progress_bar_fill_color
        );

        RingProgressBar mProgressBar = mActivity.findViewById(R.id.fingerprint_progress_bar);
        assertThat(mProgressBar.getProgressTintList()).isEqualTo(
                ColorStateList.valueOf(progress_bar_fill_color)
        );
    }

    @Test
    public void fingerprintSfpsEnroll_usesCorrectProgressBarHelpColor() {
        initializeActivityFor(TYPE_POWER_BUTTON);

        configureSfpsStageColorTest();

        int progress_bar_error_color = mActivity.getApplicationContext().getColor(
                R.color.sfps_enrollment_progress_bar_error_color
        );
        mActivity.onEnrollmentHelp(
                FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS,
                "test enrollment help"
        );

        RingProgressBar mProgressBar = mActivity.findViewById(R.id.fingerprint_progress_bar);
        assertThat(mProgressBar.getProgressTintList()).isEqualTo(
                ColorStateList.valueOf(progress_bar_error_color)
        );
    }

    @Test
    public void fingerprintSfpsEnrollment_loseFocus_shouldNotCancel() {
        initializeActivityFor(FingerprintSensorProperties.TYPE_POWER_BUTTON);

        mActivity.onEnrollmentProgressChange(1, 1);
        mActivity.onWindowFocusChanged(true);

        verify(mActivity, never()).onCancelEnrollment(anyInt());
    }

    @Test
    public void fingerprintUdfpsEnroll_activityApplyDarkLightStyle() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        mActivity.onApplyThemeResource(mTheme, R.style.GlifTheme, true /* first */);

        final String appliedThemes = mTheme.toString();
        assertThat(appliedThemes.contains("SetupWizardPartnerResource")).isTrue();
    }

    @Test
    public void fingerprintSfpsEnroll_activityApplyDarkLightStyle() {
        initializeActivityFor(TYPE_POWER_BUTTON);

        mActivity.onApplyThemeResource(mTheme, R.style.GlifTheme, true /* first */);

        final String appliedThemes = mTheme.toString();
        assertThat(appliedThemes.contains("SetupWizardPartnerResource")).isTrue();
    }

    private void initializeActivityFor(int sensorType) {
        final List<ComponentInfoInternal> componentInfo = new ArrayList<>();
        final FingerprintSensorPropertiesInternal prop =
                new FingerprintSensorPropertiesInternal(
                        0 /* sensorId */,
                        SensorProperties.STRENGTH_STRONG,
                        1 /* maxEnrollmentsPerUser */,
                        componentInfo,
                        sensorType,
                        true /* resetLockoutRequiresHardwareAuthToken */);
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        final Bundle savedInstanceState = new Bundle();
        savedInstanceState.putInt(KEY_STATE_PREVIOUS_ROTATION, Surface.ROTATION_90);
        props.add(prop);
        when(mFingerprintManager.getSensorPropertiesInternal()).thenReturn(props);
        mContext = spy(RuntimeEnvironment.application);
        mActivity = spy(FingerprintEnrollEnrolling.class);

        when(mFingerprintManager.getSensorPropertiesInternal()).thenReturn(props);
        when(mContext.getDisplay()).thenReturn(mMockDisplay);
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);

        doReturn(true).when(mActivity).shouldShowLottie();
        doReturn(mFingerprintManager).when(mActivity).getSystemService(FingerprintManager.class);
        doReturn(mVibrator).when(mActivity).getSystemService(Vibrator.class);
        doReturn(mIllustrationLottie).when(mActivity).findViewById(R.id.illustration_lottie);
        ReflectionHelpers.setField(mActivity, "mSidecar", mSidecar);

        if (sensorType == TYPE_POWER_BUTTON) {
            // SFPS_STAGE_NO_ANIMATION = 0, ... , SFPS_STAGE_RIGHT_EDGE = 4
            for (int stage = SFPS_STAGE_NO_ANIMATION; stage <= SFPS_STAGE_RIGHT_EDGE; stage++) {
                doReturn(mSfpsStageThresholds[stage]).when(mActivity).getStageThresholdSteps(stage);
            }
            doReturn(true).when(mSidecar).isEnrolling();
        }

        ActivityController.of(mActivity).create(savedInstanceState);
        mTheme = mActivity.getTheme();
    }

    private EnrollmentCallback verifyAndCaptureEnrollmentCallback() {
        ArgumentCaptor<EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(EnrollmentCallback.class);
        verify(mFingerprintManager)
                .enroll(
                        any(byte[].class),
                        any(CancellationSignal.class),
                        anyInt(),
                        callbackCaptor.capture(),
                        eq(FingerprintManager.ENROLL_ENROLL));

        return callbackCaptor.getValue();
    }
}