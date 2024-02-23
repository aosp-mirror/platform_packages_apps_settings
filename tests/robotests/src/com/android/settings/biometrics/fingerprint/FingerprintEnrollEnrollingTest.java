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

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Rect;
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
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.RingProgressBar;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieTask;
import com.google.android.setupdesign.GlifLayout;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Ignore("b/295325503")
@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
public class FingerprintEnrollEnrollingTest {
    private static final String ENROLL_PROGRESS_COLOR_LIGHT = "#699FF3";
    private static final String ENROLL_PROGRESS_COLOR_DARK = "#7DA7F1";


    @Mock private FingerprintManager mFingerprintManager;

    @Mock private Vibrator mVibrator;

    @Mock private LottieAnimationView mIllustrationLottie;

    @Mock private ObjectAnimator mHelpAnimation;

    @Mock private FingerprintEnrollSidecar mSidecar;

    @Mock private Display mMockDisplay;

    private Resources.Theme mTheme;

    private static final int TOTAL_ENROLL_STEPS = 25;

    private final int[] mSfpsStageThresholds = new int[]{0, 9, 13, 19, 25};
    private final int[] mUdfpsStageThresholds = new int[]{0, 13, 17, 22};

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
    public void fingerprintUdfpsEnrollInitStage_afterOnEnrollmentHelp_shouldVibrate() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        assertThat(getLayout().getDescriptionText()).isNotEqualTo("");

        mActivity.configureEnrollmentStage(0 /* lottie */);
        mActivity.onEnrollmentHelp(1/* FINGERPRINT_ACQUIRED_PARTIAL */, mContext.getString(
                com.android.internal.R.string.fingerprint_acquired_partial));

        verify(mVibrator, never()).vibrate(anyInt(), anyString(), any(), anyString(), any());

        mActivity.onEnrollmentProgressChange(1, 1);
        verify(mVibrator).vibrate(anyInt(), anyString(), any(), anyString(), any());

    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_gainFocus_shouldNotCancel() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        mActivity.onEnrollmentProgressChange(1, 1);
        mActivity.onWindowFocusChanged(true);

        verify(mActivity, never()).onCancelEnrollment(anyInt());
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_loseFocus_shouldNotCancel() {
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
    public void fingerprintUdfpsOverlayEnrollment_PlaysAllAnimationsAssetsCorrectly() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);
        LottieTask.EXECUTOR = mContext.getMainExecutor();

        int initStageSteps = -1, initStageRemaining = 0;
        final int totalStages = mUdfpsStageThresholds.length;

        when(mSidecar.getEnrollmentSteps()).thenReturn(initStageSteps);
        when(mSidecar.getEnrollmentRemaining()).thenReturn(initStageRemaining);

        mActivity.onEnrollmentProgressChange(initStageSteps, initStageRemaining);

        when(mSidecar.getEnrollmentSteps()).thenReturn(TOTAL_ENROLL_STEPS);

        for (int remaining = TOTAL_ENROLL_STEPS; remaining > 0; remaining--) {
            when(mSidecar.getEnrollmentRemaining()).thenReturn(remaining);
            mActivity.onEnrollmentProgressChange(TOTAL_ENROLL_STEPS, remaining);
        }


        verify(mIllustrationLottie, times(totalStages)).setComposition(any());
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_showOverlayPortrait() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);

        final FrameLayout portraitLayoutContainer = mActivity.findViewById(R.id.layout_container);
        final UdfpsEnrollView udfpsEnrollView =
                portraitLayoutContainer.findViewById(R.id.udfps_animation_view);
        assertThat(udfpsEnrollView).isNotNull();
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_showOverlayLandscape() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_90);

        final GlifLayout defaultLayout = mActivity.findViewById(R.id.setup_wizard_layout);
        final UdfpsEnrollView udfpsEnrollView =
                defaultLayout.findViewById(R.id.udfps_animation_view);
        assertThat(udfpsEnrollView).isNotNull();
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_usesCorrectProgressBarFillColor() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);
        final TypedArray ta = mActivity.obtainStyledAttributes(null,
                R.styleable.BiometricsEnrollView, R.attr.biometricsEnrollStyle,
                R.style.BiometricsEnrollStyle);
        final int progressColor = ta.getColor(
                R.styleable.BiometricsEnrollView_biometricsEnrollProgress, 0);
        final ImageView progressBar = mActivity.findViewById(
                R.id.udfps_enroll_animation_fp_progress_view);

        configureSfpsStageColorTest();

        assertThat(
                ((UdfpsEnrollProgressBarDrawable) (progressBar.getDrawable()))
                        .mFillPaint.getColor())
                .isEqualTo(progressColor);
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_checkViewOverlapPortrait() {
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        final GlifLayout defaultLayout = mActivity.findViewById(R.id.setup_wizard_layout);
        final TextView headerTextView = defaultLayout.getHeaderTextView();
        final TextView descriptionTextView = defaultLayout.getDescriptionTextView();
        final FrameLayout lottieAnimationContainer = mActivity.findViewById(R.id.layout_container);
        final UdfpsEnrollView udfpsEnrollView =
                defaultLayout.findViewById(R.id.udfps_animation_view);

        final int[] headerTextViewPosition = new int[2];
        final int[] descriptionTextViewPosition = new int[2];
        final int[] lottieAnimationPosition = new int[2];
        final int[] udfpsEnrollViewPosition = new int[2];
        final AtomicReference<Rect> rectHeaderTextView = new AtomicReference<>(
                new Rect(0, 0, 0, 0));
        final AtomicReference<Rect> rectDescriptionTextView =
                new AtomicReference<>(new Rect(0, 0, 0, 0));
        final AtomicReference<Rect> rectLottieAnimationView = new AtomicReference<>(
                new Rect(0, 0, 0, 0));
        final AtomicReference<Rect> rectUdfpsEnrollView = new AtomicReference<>(
                new Rect(0, 0, 0, 0));

        headerTextView.getViewTreeObserver().addOnDrawListener(() -> {
            headerTextView.getLocationOnScreen(headerTextViewPosition);
            rectHeaderTextView.set(new Rect(headerTextViewPosition[0], headerTextViewPosition[1],
                    headerTextViewPosition[0] + headerTextView.getWidth(),
                    headerTextViewPosition[1] + headerTextView.getHeight()));
        });

        descriptionTextView.getViewTreeObserver().addOnDrawListener(() -> {
            descriptionTextView.getLocationOnScreen(descriptionTextViewPosition);
            rectDescriptionTextView.set(new Rect(descriptionTextViewPosition[0],
                    descriptionTextViewPosition[1], descriptionTextViewPosition[0]
                    + descriptionTextView.getWidth(), descriptionTextViewPosition[1]
                    + descriptionTextView.getHeight()));

        });

        udfpsEnrollView.getViewTreeObserver().addOnDrawListener(() -> {
            udfpsEnrollView.getLocationOnScreen(udfpsEnrollViewPosition);
            rectUdfpsEnrollView.set(new Rect(udfpsEnrollViewPosition[0],
                    udfpsEnrollViewPosition[1], udfpsEnrollViewPosition[0]
                    + udfpsEnrollView.getWidth(), udfpsEnrollViewPosition[1]
                    + udfpsEnrollView.getHeight()));
        });

        lottieAnimationContainer.getViewTreeObserver().addOnDrawListener(() -> {
            lottieAnimationContainer.getLocationOnScreen(lottieAnimationPosition);
            rectLottieAnimationView.set(new Rect(lottieAnimationPosition[0],
                    lottieAnimationPosition[1], lottieAnimationPosition[0]
                    + lottieAnimationContainer.getWidth(), lottieAnimationPosition[1]
                    + lottieAnimationContainer.getHeight()));
        });

        // Check if the HeaderTextView and DescriptionTextView overlapped
        assertThat(rectHeaderTextView.get()
                .intersect(rectDescriptionTextView.get())).isFalse();

        // Check if the DescriptionTextView and Lottie animation overlapped
        assertThat(rectDescriptionTextView.get()
                .intersect(rectLottieAnimationView.get())).isFalse();

        // Check if the Lottie animation and UDSPFEnrollView overlapped
        assertThat(rectLottieAnimationView.get()
                .intersect(rectUdfpsEnrollView.get())).isFalse();
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_descriptionViewGoneWithOverlap() {
        initializeActivityWithoutCreate(TYPE_UDFPS_OPTICAL);
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);
        createActivity();

        final UdfpsEnrollEnrollingView defaultLayout = spy(
                mActivity.findViewById(R.id.setup_wizard_layout));
        doReturn(true).when(defaultLayout).hasOverlap(any(), any());

        // Somehow spy doesn't work, and we need to call initView manually.
        defaultLayout.initView(mFingerprintManager.getSensorPropertiesInternal().get(0),
                mActivity.mUdfpsEnrollHelper,
                mActivity.getSystemService(AccessibilityManager.class));
        final TextView descriptionTextView = defaultLayout.getDescriptionTextView();

        defaultLayout.getViewTreeObserver().dispatchOnDraw();
        assertThat(descriptionTextView.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_descriptionViewVisibleWithoutOverlap() {
        initializeActivityWithoutCreate(TYPE_UDFPS_OPTICAL);
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);
        createActivity();

        final UdfpsEnrollEnrollingView defaultLayout = spy(
                mActivity.findViewById(R.id.setup_wizard_layout));
        doReturn(false).when(defaultLayout).hasOverlap(any(), any());

        // Somehow spy doesn't work, and we need to call initView manually.
        defaultLayout.initView(mFingerprintManager.getSensorPropertiesInternal().get(0),
                mActivity.mUdfpsEnrollHelper,
                mActivity.getSystemService(AccessibilityManager.class));
        final TextView descriptionTextView = defaultLayout.getDescriptionTextView();

        defaultLayout.getViewTreeObserver().dispatchOnDraw();
        assertThat(descriptionTextView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void fingerprintUdfpsOverlayEnrollment_udfpsAnimationViewVisibility() {
        initializeActivityWithoutCreate(TYPE_UDFPS_OPTICAL);
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);
        createActivity();

        final UdfpsEnrollView enrollView = mActivity.findViewById(R.id.udfps_animation_view);
        assertThat(enrollView.getVisibility()).isEqualTo(View.GONE);

        mActivity.onUdfpsOverlayShown();
        assertThat(enrollView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void forwardEnrollProgressEvents() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        EnrollListener listener = new EnrollListener(mActivity);
        mActivity.onEnrollmentProgressChange(20, 10);
        assertThat(listener.mProgress).isTrue();
        assertThat(listener.mHelp).isFalse();
        assertThat(listener.mAcquired).isFalse();
        assertThat(listener.mPointerUp).isFalse();
        assertThat(listener.mPointerDown).isFalse();
    }

    @Test
    public void forwardEnrollHelpEvents() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        EnrollListener listener = new EnrollListener(mActivity);
        mActivity.onEnrollmentHelp(20, "test enrollment help");
        assertThat(listener.mProgress).isFalse();
        assertThat(listener.mHelp).isTrue();
        assertThat(listener.mAcquired).isFalse();
        assertThat(listener.mPointerUp).isFalse();
        assertThat(listener.mPointerDown).isFalse();
    }

    @Test
    public void forwardEnrollAcquiredEvents() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        EnrollListener listener = new EnrollListener(mActivity);
        mActivity.onEnrollmentProgressChange(20, 10);
        mActivity.onAcquired(false);
        assertThat(listener.mProgress).isTrue();
        assertThat(listener.mHelp).isFalse();
        assertThat(listener.mAcquired).isTrue();
        assertThat(listener.mPointerUp).isFalse();
        assertThat(listener.mPointerDown).isFalse();
    }

    @Test
    public void forwardUdfpsEnrollPointerDownEvents() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        EnrollListener listener = new EnrollListener(mActivity);
        mActivity.onUdfpsPointerDown(0);
        assertThat(listener.mProgress).isFalse();
        assertThat(listener.mHelp).isFalse();
        assertThat(listener.mAcquired).isFalse();
        assertThat(listener.mPointerUp).isFalse();
        assertThat(listener.mPointerDown).isTrue();
    }

    @Test
    public void forwardUdfpsEnrollPointerUpEvents() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        EnrollListener listener = new EnrollListener(mActivity);
        mActivity.onUdfpsPointerUp(0);
        assertThat(listener.mProgress).isFalse();
        assertThat(listener.mHelp).isFalse();
        assertThat(listener.mAcquired).isFalse();
        assertThat(listener.mPointerUp).isTrue();
        assertThat(listener.mPointerDown).isFalse();
    }

    @Test
    public void fingerprintSfpsEnroll_PlaysAnimations() {
        initializeActivityFor(TYPE_POWER_BUTTON);
        LottieTask.EXECUTOR = mContext.getMainExecutor();

        int initStageSteps = -1, initStageRemaining = 0;

        when(mSidecar.getEnrollmentSteps()).thenReturn(initStageSteps);
        when(mSidecar.getEnrollmentRemaining()).thenReturn(initStageRemaining);

        mActivity.onEnrollmentProgressChange(initStageSteps, initStageRemaining);

        when(mSidecar.getEnrollmentSteps()).thenReturn(TOTAL_ENROLL_STEPS);

        for (int remaining = TOTAL_ENROLL_STEPS; remaining > 0; remaining--) {
            when(mSidecar.getEnrollmentRemaining()).thenReturn(remaining);
            mActivity.onEnrollmentProgressChange(TOTAL_ENROLL_STEPS, remaining);
        }


        verify(mIllustrationLottie, times(5)).setComposition(any());
    }

    @Test
    public void fingerprintSfpsEnrollHelpAnimation() {
        initializeActivityFor(TYPE_POWER_BUTTON);
        ReflectionHelpers.setField(mActivity, "mHelpAnimation", mHelpAnimation);
        mActivity.onEnrollmentHelp(0 /* helpMsgId */, "Test help message" /* helpString */);

        verify(mHelpAnimation).start();
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

        final Configuration config = mContext.getResources().getConfiguration();
        final boolean isDarkThemeOn = (config.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        final int currentColor = mContext.getColor(R.color.udfps_enroll_progress);
        assertThat(currentColor).isEqualTo(Color.parseColor(isDarkThemeOn
                ? ENROLL_PROGRESS_COLOR_DARK : ENROLL_PROGRESS_COLOR_LIGHT));

    }

    @Test
    public void fingerprintSfpsEnroll_activityApplyDarkLightStyle() {
        initializeActivityFor(TYPE_POWER_BUTTON);

        mActivity.onApplyThemeResource(mTheme, R.style.GlifTheme, true /* first */);

        final String appliedThemes = mTheme.toString();
        assertThat(appliedThemes.contains("SetupWizardPartnerResource")).isTrue();
    }

    @Test
    public void fingerprintSfpsEnroll_descriptionTextVisibility() {
        initializeActivityFor(TYPE_POWER_BUTTON);

        mActivity.onEnrollmentProgressChange(1 /* steps */, 1 /* remaining */);

        assertThat(getLayout().getDescriptionTextView().getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void fingerprintUdfpsEnroll_descriptionTextVisibility() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        mActivity.onEnrollmentProgressChange(1 /* steps */, 1 /* remaining */);

        assertThat(getLayout().getDescriptionTextView().getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void testUdfpsConfigureEnrollmentStage_descriptionText() {
        initializeActivityFor(TYPE_UDFPS_OPTICAL);

        assertThat(getLayout().getDescriptionText()).isNotEqualTo("");

        mActivity.configureEnrollmentStage(0 /* lottie */);

        assertThat(getLayout().getDescriptionText()).isEqualTo("");
    }

    @Test
    public void testSfpsConfigureEnrollmentStage_descriptionText() {
        initializeActivityFor(TYPE_POWER_BUTTON);

        assertThat(getLayout().getDescriptionTextView().getVisibility()).isEqualTo(View.GONE);

        mActivity.configureEnrollmentStage(0 /* lottie */);

        assertThat(getLayout().getDescriptionTextView().getVisibility()).isEqualTo(View.GONE);
    }

    private GlifLayout getLayout() {
        return (GlifLayout) mActivity.findViewById(R.id.setup_wizard_layout);
    }

    private void initializeActivityWithoutCreate(int sensorType) {
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
        props.add(prop);
        when(mFingerprintManager.getSensorPropertiesInternal()).thenReturn(props);
        mContext = spy(RuntimeEnvironment.application);
        mActivity = spy(FingerprintEnrollEnrolling.class);

        doReturn(mMockDisplay).when(mContext).getDisplay();
        when(mMockDisplay.getRotation()).thenReturn(Surface.ROTATION_0);

        doReturn(mMockDisplay).when(mActivity).getDisplay();
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
            ReflectionHelpers.setField(mActivity, "mCanAssumeSfps", true);
        } else if (sensorType == TYPE_UDFPS_OPTICAL) {
            ReflectionHelpers.setField(mActivity, "mCanAssumeUdfps", true);
        }

        if (sensorType == TYPE_UDFPS_OPTICAL) {
            // UDFPS : STAGE_CENTER = 0, ... , STAGE_RIGHT_EDGE = 3
            final int totalStages = mUdfpsStageThresholds.length - 1;
            for (int stage = 0; stage <= totalStages; stage++) {
                doReturn(mUdfpsStageThresholds[stage]).when(mActivity).getStageThresholdSteps(
                        stage);
            }
            doReturn(true).when(mSidecar).isEnrolling();
        }
    }

    private void createActivity() {
        System.setProperty("robolectric.createActivityContexts", "true");
        final Bundle savedInstanceState = new Bundle();
        savedInstanceState.putInt(KEY_STATE_PREVIOUS_ROTATION, Surface.ROTATION_90);

        ActivityController.of(mActivity).create(savedInstanceState);
        mTheme = mActivity.getTheme();
    }

    private void initializeActivityFor(int sensorType) {
        initializeActivityWithoutCreate(sensorType);
        createActivity();
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
                        eq(FingerprintManager.ENROLL_ENROLL),
                        any());

        return callbackCaptor.getValue();
    }

    private static class EnrollListener implements  UdfpsEnrollHelper.Listener {
        private final FingerprintEnrollEnrolling mActivity;
        private boolean mProgress = false;
        private boolean mHelp = false;
        private boolean mAcquired = false;
        private boolean mPointerDown = false;
        private boolean mPointerUp = false;

        EnrollListener(FingerprintEnrollEnrolling activity) {
            mActivity = activity;
            mActivity.mUdfpsEnrollHelper.setListener(this);
        }

        @Override
        public void onEnrollmentProgress(int remaining, int totalSteps) {
            mProgress = true;
        }

        @Override
        public void onEnrollmentHelp(int remaining, int totalSteps) {
            mHelp = true;
        }

        @Override
        public void onAcquired(boolean animateIfLastStepGood) {
            mAcquired = true;
        }

        @Override
        public void onPointerDown(int sensorId) {
            mPointerDown = true;
        }

        @Override
        public void onPointerUp(int sensorId) {
            mPointerUp = true;
        }
    }
}
