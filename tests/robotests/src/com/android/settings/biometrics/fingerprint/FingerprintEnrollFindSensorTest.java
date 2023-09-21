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
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_REAR;
import static android.hardware.fingerprint.FingerprintSensorProperties.TYPE_UDFPS_OPTICAL;
import static android.text.Layout.HYPHENATION_FREQUENCY_NORMAL;

import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_SKIP;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_TIMEOUT;
import static com.android.settings.biometrics.fingerprint.FingerprintEnrollEnrolling.TAG_SIDECAR;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.robolectric.RuntimeEnvironment.application;

import android.annotation.NonNull;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.biometrics.ComponentInfoInternal;
import android.hardware.biometrics.SensorProperties;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.hardware.fingerprint.FingerprintSensorProperties;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupcompat.PartnerCustomizationLayout;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;
import com.google.android.setupdesign.template.HeaderMixin;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowActivity.IntentForResult;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowUtils.class)
public class FingerprintEnrollFindSensorTest {

    private static final int DEFAULT_ACTIVITY_RESULT = Activity.RESULT_CANCELED;

    @Mock
    private FingerprintManager mFingerprintManager;

    private Resources.Theme mTheme;

    private ActivityController<FingerprintEnrollFindSensor> mActivityController;

    private FingerprintEnrollFindSensor mActivity;

    private void buildActivity() {
        mActivityController = Robolectric.buildActivity(
                FingerprintEnrollFindSensor.class,
                new Intent()
                        // Set the challenge token so the confirm screen will not be shown
                        .putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0])
        );
        mActivity = mActivityController.get();
        mTheme = mActivity.getTheme();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        FakeFeatureFactory.setupForTest();
        buildActivity();
    }

    private void setupActivity_onRearDevice() {
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(newFingerprintSensorPropertiesInternal(TYPE_REAR));
        doReturn(props).when(mFingerprintManager).getSensorPropertiesInternal();

        mActivityController.setup();
    }

    private void setupActivity_onUdfpsDevice() {
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(newFingerprintSensorPropertiesInternal(TYPE_UDFPS_OPTICAL));
        doReturn(props).when(mFingerprintManager).getSensorPropertiesInternal();

        mActivityController.setup();
    }

    private void setupActivity_onSfpsDevice() {
        final ArrayList<FingerprintSensorPropertiesInternal> props = new ArrayList<>();
        props.add(newFingerprintSensorPropertiesInternal(TYPE_POWER_BUTTON));
        doReturn(props).when(mFingerprintManager).getSensorPropertiesInternal();

        mActivityController.setup();
    }

    private FingerprintSensorPropertiesInternal newFingerprintSensorPropertiesInternal(
            @FingerprintSensorProperties.SensorType int sensorType) {
        return new FingerprintSensorPropertiesInternal(
                0 /* sensorId */,
                SensorProperties.STRENGTH_STRONG,
                1 /* maxEnrollmentsPerUser */,
                new ArrayList<ComponentInfoInternal>(),
                sensorType,
                true /* resetLockoutRequiresHardwareAuthToken */);
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
    }

    private void verifyStartEnrollingActivity() {
        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        IntentForResult startedActivity =
                shadowActivity.getNextStartedActivityForResult();
        assertWithMessage("Next activity 1").that(startedActivity).isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(new ComponentName(application, FingerprintEnrollEnrolling.class));
    }

    @Test
    public void enrollFingerprintTwice_shouldStartOneEnrolling() {
        setupActivity_onRearDevice();
        EnrollmentCallback enrollmentCallback = verifyAndCaptureEnrollmentCallback();

        enrollmentCallback.onEnrollmentProgress(123);
        enrollmentCallback.onEnrollmentProgress(123);  // A second enroll should be a no-op

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        IntentForResult startedActivity =
                shadowActivity.getNextStartedActivityForResult();
        assertWithMessage("Next activity 1").that(startedActivity).isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(new ComponentName(application, FingerprintEnrollEnrolling.class));

        // Should only start one next activity
        assertWithMessage("Next activity 2").that(shadowActivity.getNextStartedActivityForResult())
                .isNull();
    }

    // Use a non-default resource qualifier to load the test layout in
    // robotests/res/layout-mcc999/fingerprint_enroll_find_sensor. This layout is a copy of the
    // regular find sensor layout, with the animation removed.
    @Config(qualifiers = "mcc999")
    @Test
    public void layoutWithoutAnimation_shouldNotCrash() {
        setupActivity_onRearDevice();

        EnrollmentCallback enrollmentCallback = verifyAndCaptureEnrollmentCallback();
        enrollmentCallback.onEnrollmentProgress(123);
        enrollmentCallback.onEnrollmentError(FingerprintManager.FINGERPRINT_ERROR_CANCELED, "test");

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        IntentForResult startedActivity =
                shadowActivity.getNextStartedActivityForResult();
        assertWithMessage("Next activity").that(startedActivity).isNotNull();
        assertThat(startedActivity.intent.getComponent())
                .isEqualTo(new ComponentName(application, FingerprintEnrollEnrolling.class));
    }

    @Test
    public void clickSkip_shouldReturnResultSkip() {
        setupActivity_onRearDevice();

        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        layout.getMixin(FooterBarMixin.class).getSecondaryButtonView().performClick();

        ShadowActivity shadowActivity = Shadows.shadowOf(mActivity);
        assertWithMessage("result code").that(shadowActivity.getResultCode())
                .isEqualTo(RESULT_SKIP);
    }

    private EnrollmentCallback verifyAndCaptureEnrollmentCallback() {
        ArgumentCaptor<EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(EnrollmentCallback.class);
        verify(mFingerprintManager).enroll(
                any(byte[].class),
                any(CancellationSignal.class),
                anyInt(),
                callbackCaptor.capture(),
                eq(FingerprintManager.ENROLL_FIND_SENSOR));

        return callbackCaptor.getValue();
    }

    @Test
    public void onActivityResult_withNullIntentShouldNotCrash() {
        setupActivity_onRearDevice();

        // this should not crash
        mActivity.onActivityResult(BiometricEnrollBase.CONFIRM_REQUEST, Activity.RESULT_OK,
            null);
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(Activity.RESULT_CANCELED);
    }

    @Test
    public void resultFinishShallForward_onRearDevice() {
        setupActivity_onRearDevice();
        triggerEnrollProgressAndError_onRearDevice();
        verifyStartEnrollingActivity();

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_FINISHED);
    }

    @Test
    public void resultFinishShallForward_onRearDevice_recreate() {
        setupActivity_onRearDevice();
        triggerEnrollProgressAndError_onRearDevice();
        verifyStartEnrollingActivity();

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_FINISHED, bundle);
    }

    @Test
    public void resultSkipShallForward_onRearDevice() {
        setupActivity_onRearDevice();
        verifySidecar_onRearOrSfpsDevice();

        triggerEnrollProgressAndError_onRearDevice();
        verifyStartEnrollingActivity();

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_SKIP);
    }

    @Test
    public void resultSkipShallForward_onRearDevice_recreate() {
        setupActivity_onRearDevice();
        verifySidecar_onRearOrSfpsDevice();

        triggerEnrollProgressAndError_onRearDevice();
        verifyStartEnrollingActivity();

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_SKIP, bundle);
    }

    @Test
    public void resultTimeoutShallForward_onRearDevice() {
        setupActivity_onRearDevice();
        verifySidecar_onRearOrSfpsDevice();

        triggerEnrollProgressAndError_onRearDevice();
        verifyStartEnrollingActivity();

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_TIMEOUT);
    }

    @Test
    public void resultTimeoutShallForward_onRearDevice_recreate() {
        setupActivity_onRearDevice();
        verifySidecar_onRearOrSfpsDevice();

        triggerEnrollProgressAndError_onRearDevice();
        verifyStartEnrollingActivity();

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_TIMEOUT, bundle);
    }

    @Test
    public void clickLottieResultFinishShallForward_onUdfpsDevice() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickLottieView_onUdfpsDevice();
        verifyStartEnrollingActivity();
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_FINISHED);
    }

    @Test
    public void clickLottieResultFinishShallForward_onUdfpsDevice_ifActivityRecycled() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickLottieView_onUdfpsDevice();
        verifyStartEnrollingActivity();
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_FINISHED, bundle);
    }

    @Test
    public void clickLottieResultSkipShallForward_onUdfpsDevice() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickLottieView_onUdfpsDevice();
        verifyStartEnrollingActivity();
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_SKIP);
    }

    @Test
    public void clickLottieResultSkipShallForward_onUdfpsDevice_ifActivityRecycled() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickLottieView_onUdfpsDevice();
        verifyStartEnrollingActivity();
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_SKIP, bundle);
    }

    @Test
    public void clickLottieResultTimeoutShallForward_onUdfpsDevice() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickLottieView_onUdfpsDevice();
        verifyStartEnrollingActivity();
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_TIMEOUT);
    }

    @Test
    public void clickLottieResultTimeoutShallForward_onUdfpsDevice_ifActivityRecycled() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickLottieView_onUdfpsDevice();
        verifyStartEnrollingActivity();
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_TIMEOUT, bundle);
    }

    @Test
    public void clickPrimiaryButtonResultFinishShallForward_onUdfpsDevice() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickPrimaryButton_onUdfpsDevice();
        verifyStartEnrollingActivity();

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_FINISHED);
    }

    @Test
    public void clickPrimiaryButtonResultFinishShallForward_onUdfpsDevice_ifActivityRecycled() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickPrimaryButton_onUdfpsDevice();
        verifyStartEnrollingActivity();

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_FINISHED, bundle);
    }

    @Test
    public void clickPrimiaryButtonResultSkipShallForward_onUdfpsDevice() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickPrimaryButton_onUdfpsDevice();
        verifyStartEnrollingActivity();

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_SKIP);
    }

    @Test
    public void clickPrimaryButtonResultSkipShallForward_onUdfpsDevice_ifActivityRecycled() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickPrimaryButton_onUdfpsDevice();
        verifyStartEnrollingActivity();

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_SKIP, bundle);
    }

    @Test
    public void clickPrimaryButtonResultTimeoutShallForward_onUdfpsDevice() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickPrimaryButton_onUdfpsDevice();
        verifyStartEnrollingActivity();

        // pause activity
        mActivityController.pause().stop();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_resumeActivityAndVerifyResultThenForward(RESULT_TIMEOUT);
    }

    @Test
    public void clickPrimaryButtonResultTimeoutShallForward_onUdfpsDevice_ifActivityRecycled() {
        setupActivity_onUdfpsDevice();
        verifyNoSidecar();

        clickPrimaryButton_onUdfpsDevice();
        verifyStartEnrollingActivity();

        // recycle activity
        final Bundle bundle = new Bundle();
        mActivityController.pause().stop().saveInstanceState(bundle).destroy();

        // onStop shall not change default activity result
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(DEFAULT_ACTIVITY_RESULT);

        gotEnrollingResult_recreateActivityAndVerifyResultThenForward(RESULT_TIMEOUT, bundle);
    }

    @Test
    @Ignore("b/295325503")
    public void fingerprintEnrollFindSensor_activityApplyDarkLightStyle() {
        setupActivity_onSfpsDevice();
        verifySidecar_onRearOrSfpsDevice();

        mActivity.onApplyThemeResource(mTheme, R.style.GlifTheme, true /* first */);

        final String appliedThemes = mTheme.toString();
        assertThat(appliedThemes.contains("SetupWizardPartnerResource")).isTrue();
    }

    @Test
    public void fingerprintEnrollFindSensor_setHyphenationFrequencyNormalOnHeader() {
        setupActivity_onUdfpsDevice();
        PartnerCustomizationLayout layout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView textView = layout.getMixin(HeaderMixin.class).getTextView();

        assertThat(textView.getHyphenationFrequency()).isEqualTo(HYPHENATION_FREQUENCY_NORMAL);
    }

    private void triggerEnrollProgressAndError_onRearDevice() {
        EnrollmentCallback enrollmentCallback = verifyAndCaptureEnrollmentCallback();
        enrollmentCallback.onEnrollmentProgress(123);
        enrollmentCallback.onEnrollmentError(FingerprintManager.FINGERPRINT_ERROR_CANCELED, "test");
    }

    private void clickPrimaryButton_onUdfpsDevice() {
        final FooterBarMixin footerBarMixin =
                ((GlifLayout) mActivity.findViewById(R.id.setup_wizard_layout))
                        .getMixin(FooterBarMixin.class);
        final FooterButton primaryButton = footerBarMixin.getPrimaryButton();
        assertThat(primaryButton).isNotNull();
        assertThat(primaryButton.getVisibility()).isEqualTo(View.VISIBLE);
        primaryButton.onClick(null);
    }

    private void clickLottieView_onUdfpsDevice() {
        final View lottieView = mActivity.findViewById(R.id.illustration_lottie);
        assertThat(lottieView).isNotNull();
        lottieView.performClick();
    }

    private void gotEnrollingResult_resumeActivityAndVerifyResultThenForward(
            int testActivityResult) {
        // resume activity
        mActivityController.start().resume().visible();
        verifyNoSidecar();

        // onActivityResult from Enrolling activity shall be forward back
        Shadows.shadowOf(mActivity).receiveResult(
                new Intent(mActivity, FingerprintEnrollEnrolling.class),
                testActivityResult,
                null);
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(testActivityResult);
        assertThat(mActivity.isFinishing()).isEqualTo(true);

        // onStop shall not change last activity result
        mActivityController.pause().stop().destroy();
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(testActivityResult);
    }

    private void gotEnrollingResult_recreateActivityAndVerifyResultThenForward(
            int testActivityResult, @NonNull Bundle savedInstance) {
        // Rebuild activity and use savedInstance to restore.
        buildActivity();
        mActivityController.setup(savedInstance);
        verifyNoSidecar();

        // onActivityResult from Enrolling activity shall be forward back
        Shadows.shadowOf(mActivity).receiveResult(
                new Intent(mActivity, FingerprintEnrollEnrolling.class),
                testActivityResult,
                null);
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(testActivityResult);
        assertThat(mActivity.isFinishing()).isEqualTo(true);

        // onStop shall not change last activity result
        mActivityController.pause().stop().destroy();
        assertThat(Shadows.shadowOf(mActivity).getResultCode()).isEqualTo(testActivityResult);
    }

    private void verifySidecar_onRearOrSfpsDevice() {
        final Fragment sidecar = mActivity.getSupportFragmentManager().findFragmentByTag(
                TAG_SIDECAR);
        assertThat(sidecar).isNotNull();
        assertThat(sidecar.isAdded()).isTrue();
    }

    private void verifyNoSidecar() {
        final Fragment sidecar = mActivity.getSupportFragmentManager().findFragmentByTag(
                TAG_SIDECAR);
        if (sidecar != null) {
            assertThat(sidecar.isAdded()).isFalse();
        }
    }
}
