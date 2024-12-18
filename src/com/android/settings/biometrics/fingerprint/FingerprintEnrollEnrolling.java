/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static android.hardware.biometrics.BiometricAuthenticator.TYPE_FINGERPRINT;
import static android.hardware.biometrics.BiometricFingerprintConstants.FINGERPRINT_ACQUIRED_IMAGER_DIRTY;
import static android.text.Layout.HYPHENATION_FREQUENCY_NONE;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.IntDef;
import android.annotation.RawRes;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.os.Process;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollSidecar;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics.BiometricsEnrollEnrolling;
import com.android.settings.biometrics.BiometricsSplitScreenDialog;
import com.android.settings.biometrics.fingerprint.feature.SfpsEnrollmentFeature;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.display.DisplayDensityUtils;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieProperty;
import com.airbnb.lottie.model.KeyPath;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.template.DescriptionMixin;
import com.google.android.setupdesign.template.HeaderMixin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Activity which handles the actual enrolling for fingerprint.
 */
public class FingerprintEnrollEnrolling extends BiometricsEnrollEnrolling {

    private static final String TAG = "FingerprintEnrollEnrolling";
    static final String TAG_SIDECAR = "sidecar";
    static final String TAG_UDFPS_HELPER = "udfps_helper";
    static final String ICON_TOUCH_DIALOG = "fps_icon_touch_dialog";
    static final String KEY_STATE_CANCELED = "is_canceled";
    static final String KEY_STATE_PREVIOUS_ROTATION = "previous_rotation";

    private static final int PROGRESS_BAR_MAX = 10000;

    public static final int STAGE_UNKNOWN = -1;
    private static final int STAGE_CENTER = 0;
    private static final int STAGE_GUIDED = 1;
    private static final int STAGE_FINGERTIP = 2;
    private static final int STAGE_LEFT_EDGE = 3;
    private static final int STAGE_RIGHT_EDGE = 4;

    public static final int SFPS_STAGE_NO_ANIMATION = 0;

    public static final int SFPS_STAGE_CENTER = 1;

    public static final int SFPS_STAGE_FINGERTIP = 2;

    public static final int SFPS_STAGE_LEFT_EDGE = 3;

    public static final int SFPS_STAGE_RIGHT_EDGE = 4;

    @IntDef({STAGE_UNKNOWN, STAGE_CENTER, STAGE_GUIDED, STAGE_FINGERTIP, STAGE_LEFT_EDGE,
            STAGE_RIGHT_EDGE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface EnrollStage {}


    @VisibleForTesting
    @IntDef({STAGE_UNKNOWN, SFPS_STAGE_NO_ANIMATION, SFPS_STAGE_CENTER, SFPS_STAGE_FINGERTIP,
            SFPS_STAGE_LEFT_EDGE, SFPS_STAGE_RIGHT_EDGE})
    @Retention(RetentionPolicy.SOURCE)
    protected @interface SfpsEnrollStage {}

    /**
     * If we don't see progress during this time, we show an error message to remind the users that
     * they need to lift the finger and touch again.
     */
    private static final int HINT_TIMEOUT_DURATION = 2500;

    /**
     * How long the user needs to touch the icon until we show the dialog.
     */
    private static final long ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN = 500;

    /**
     * How many times the user needs to touch the icon until we show the dialog that this is not the
     * fingerprint sensor.
     */
    private static final int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;

    private static final VibrationEffect VIBRATE_EFFECT_ERROR =
            VibrationEffect.createWaveform(new long[] {0, 5, 55, 60}, -1);
    private static final VibrationAttributes FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES =
            VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ACCESSIBILITY);

    private FingerprintManager mFingerprintManager;
    private boolean mCanAssumeUdfps;
    private boolean mCanAssumeSfps;
    @Nullable private ProgressBar mProgressBar;
    @VisibleForTesting
    @Nullable
    UdfpsEnrollHelper mUdfpsEnrollHelper;
    private ObjectAnimator mProgressAnim;
    private TextView mErrorText;
    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;
    private Interpolator mFastOutLinearInInterpolator;
    private int mIconTouchCount;
    private boolean mAnimationCancelled;
    @Nullable private AnimatedVectorDrawable mIconAnimationDrawable;
    @Nullable private AnimatedVectorDrawable mIconBackgroundBlinksDrawable;
    private boolean mRestoring;
    private Vibrator mVibrator;
    private boolean mIsSetupWizard;
    @VisibleForTesting
    boolean mIsCanceled;
    private AccessibilityManager mAccessibilityManager;
    private boolean mIsAccessibilityEnabled;
    private LottieAnimationView mIllustrationLottie;
    private boolean mHaveShownUdfpsTipLottie;
    private boolean mHaveShownUdfpsLeftEdgeLottie;
    private boolean mHaveShownUdfpsRightEdgeLottie;
    private boolean mHaveShownUdfpsCenterLottie;
    private boolean mHaveShownUdfpsGuideLottie;
    private boolean mHaveShownSfpsNoAnimationLottie;
    private boolean mHaveShownSfpsCenterLottie;
    private boolean mHaveShownSfpsTipLottie;
    private boolean mHaveShownSfpsLeftEdgeLottie;
    private boolean mHaveShownSfpsRightEdgeLottie;
    private boolean mShouldShowLottie;

    private Animator mHelpAnimation;

    private OrientationEventListener mOrientationEventListener;
    private int mPreviousRotation = 0;

    private boolean mIsFolded = false;

    private SfpsEnrollmentFeature mSfpsEnrollmentFeature;

    @Nullable
    private UdfpsEnrollCalibrator mCalibrator;

    @VisibleForTesting
    protected boolean shouldShowLottie() {
        DisplayDensityUtils displayDensity = new DisplayDensityUtils(getApplicationContext());
        int currentDensityIndex = displayDensity.getCurrentIndexForDefaultDisplay();
        final int currentDensity = displayDensity.getDefaultDisplayDensityValues()
                [currentDensityIndex];
        final int defaultDensity = displayDensity.getDefaultDensityForDefaultDisplay();

        if (getResources().getConfiguration().fontScale > 1) {
            return false;
        }
        return defaultDensity == currentDensity;
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (shouldShowSplitScreenDialog()) {
            BiometricsSplitScreenDialog.newInstance(TYPE_FINGERPRINT, true /*destroyActivity*/)
                    .show(getSupportFragmentManager(), BiometricsSplitScreenDialog.class.getName());
        }
        if (savedInstanceState != null) {
            restoreSavedState(savedInstanceState);
        }
        mFingerprintManager = getSystemService(FingerprintManager.class);
        final List<FingerprintSensorPropertiesInternal> props =
                mFingerprintManager.getSensorPropertiesInternal();
        mCanAssumeUdfps = props != null && props.size() == 1 && props.get(0).isAnyUdfpsType();
        mCanAssumeSfps = props != null && props.size() == 1 && props.get(0).isAnySidefpsType();

        mAccessibilityManager = getSystemService(AccessibilityManager.class);
        mIsAccessibilityEnabled = mAccessibilityManager.isEnabled();

        listenOrientationEvent();

        if (mCanAssumeUdfps) {
            final UdfpsEnrollEnrollingView layout =
                    (UdfpsEnrollEnrollingView) getLayoutInflater().inflate(
                            R.layout.udfps_enroll_enrolling, null, false);
            setUdfpsEnrollHelper();
            layout.initView(props.get(0), mUdfpsEnrollHelper, mAccessibilityManager);

            setContentView(layout);
            setDescriptionText(R.string.security_settings_udfps_enroll_start_message);
        } else if (mCanAssumeSfps) {
            mSfpsEnrollmentFeature = FeatureFactory.getFeatureFactory()
                    .getFingerprintFeatureProvider().getSfpsEnrollmentFeature();
            setContentView(R.layout.sfps_enroll_enrolling);
            setHelpAnimation();
        } else {
            setContentView(R.layout.fingerprint_enroll_enrolling);
            setDescriptionText(R.string.security_settings_fingerprint_enroll_start_message);
        }

        mIsSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
        if (mCanAssumeUdfps || mCanAssumeSfps) {
            updateTitleAndDescription(true);
        } else {
            setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title);
        }

        mShouldShowLottie = shouldShowLottie();
        // On non-SFPS devices, only show the lottie if the current display density is the default
        // density. Otherwise, the lottie will overlap with the settings header text.
        boolean isLandscape = BiometricUtils.isReverseLandscape(getApplicationContext())
                || BiometricUtils.isLandscape(getApplicationContext());

        updateOrientation((isLandscape
                ? Configuration.ORIENTATION_LANDSCAPE : Configuration.ORIENTATION_PORTRAIT));

        mErrorText = findViewById(R.id.error_text);
        mProgressBar = findViewById(R.id.fingerprint_progress_bar);
        mVibrator = getSystemService(Vibrator.class);

        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build()
        );

        // If it's udfps, set the background color only for secondary button if necessary.
        if (mCanAssumeUdfps) {
            mShouldSetFooterBarBackground = false;
            ((UdfpsEnrollEnrollingView) getLayout()).setSecondaryButtonBackground(
                    getBackgroundColor());
        }

        final LayerDrawable fingerprintDrawable = mProgressBar != null
                ? (LayerDrawable) mProgressBar.getBackground() : null;
        if (fingerprintDrawable != null) {
            mIconAnimationDrawable = (AnimatedVectorDrawable)
                    fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_animation);
            mIconBackgroundBlinksDrawable = (AnimatedVectorDrawable)
                    fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_background);
            mIconAnimationDrawable.registerAnimationCallback(mIconAnimationCallback);
        }

        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.fast_out_slow_in);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.linear_out_slow_in);
        mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.fast_out_linear_in);
        if (mProgressBar != null) {
            mProgressBar.setProgressBackgroundTintMode(PorterDuff.Mode.SRC);
            mProgressBar.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mIconTouchCount++;
                    if (mIconTouchCount == ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN) {
                        showIconTouchDialog();
                    } else {
                        mProgressBar.postDelayed(mShowDialogRunnable,
                                ICON_TOUCH_DURATION_UNTIL_DIALOG_SHOWN);
                    }
                } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL
                        || event.getActionMasked() == MotionEvent.ACTION_UP) {
                    mProgressBar.removeCallbacks(mShowDialogRunnable);
                }
                return true;
            });
        }

        final Configuration config = getApplicationContext().getResources().getConfiguration();
        maybeHideSfpsText(config);

        if (!mIsSetupWizard) {
            mCalibrator = FeatureFactory.getFeatureFactory().getFingerprintFeatureProvider()
                    .getUdfpsEnrollCalibrator(getApplicationContext(), null, getIntent());
            if (mCalibrator != null) {
                mCalibrator.onWaitingPage(getLifecycle(),
                        getSupportFragmentManager(), null);
            }
        }
    }

    private void setHelpAnimation() {
        final RelativeLayout progressLottieLayout = findViewById(R.id.progress_lottie);
        mHelpAnimation = mSfpsEnrollmentFeature.getHelpAnimator(progressLottieLayout);
    }

    @Override
    protected BiometricEnrollSidecar getSidecar() {
        final FingerprintEnrollSidecar sidecar = new FingerprintEnrollSidecar(this,
                FingerprintManager.ENROLL_ENROLL, getIntent());
        return sidecar;
    }

    @Override
    protected boolean shouldStartAutomatically() {
        if (mCanAssumeUdfps) {
            // Continue enrollment if restoring (e.g. configuration changed). Otherwise, wait
            // for the entry animation to complete before starting.
            return mRestoring && !mIsCanceled;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_STATE_CANCELED, mIsCanceled);
        outState.putInt(KEY_STATE_PREVIOUS_ROTATION, mPreviousRotation);
    }

    private void restoreSavedState(Bundle savedInstanceState) {
        mRestoring = true;
        mIsCanceled = savedInstanceState.getBoolean(KEY_STATE_CANCELED, false);
        mPreviousRotation = savedInstanceState.getInt(KEY_STATE_PREVIOUS_ROTATION,
                getDisplay().getRotation());
    }

    @Override
    protected void onStart() {
        super.onStart();
        setupScreenFoldCallbackWhenNecessary();
        updateProgress(false /* animate */);
        updateTitleAndDescription(true);
        if (mRestoring) {
            startIconAnimation();
        }
    }

    private void setupScreenFoldCallbackWhenNecessary() {
        if (mCanAssumeSfps) {
            // These two fields will be cleaned up in BiometricEnrollBase#onStop.
            mScreenSizeFoldProvider = new ScreenSizeFoldProvider(getApplicationContext());
            mFoldCallback = isFolded -> {
                mIsFolded = isFolded;
                maybeHideSfpsText(getResources().getConfiguration());
            };
            // The callback will be unregistered in BiometricEnrollBase#onStop.
            mScreenSizeFoldProvider.registerCallback(mFoldCallback, getMainExecutor());
        }
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();

        if (mCanAssumeUdfps) {
            startEnrollment();
        }

        mAnimationCancelled = false;
        startIconAnimation();
    }

    private void startIconAnimation() {
        if (mIconAnimationDrawable != null) {
            mIconAnimationDrawable.start();
        }
    }

    private void stopIconAnimation() {
        mAnimationCancelled = true;
        if (mIconAnimationDrawable != null) {
            mIconAnimationDrawable.stop();
        }
    }

    @VisibleForTesting
    void onCancelEnrollment(@IdRes int errorMsgId) {
        // showErrorDialog() will cause onWindowFocusChanged(false), set mIsCanceled to false
        // before showErrorDialog() to prevent that another error dialog is triggered again.
        mIsCanceled = true;
        FingerprintErrorDialog.showErrorDialog(this, errorMsgId,
                this instanceof SetupFingerprintEnrollEnrolling);
        cancelEnrollment();
        stopIconAnimation();
        stopListenOrientationEvent();
        if (!mCanAssumeUdfps) {
            mErrorText.removeCallbacks(mTouchAgainRunnable);
        }
    }

    @Override
    protected void onStop() {
        if (!isChangingConfigurations()) {
            if (!WizardManagerHelper.isAnySetupWizard(getIntent())
                    && !BiometricUtils.isAnyMultiBiometricFlow(this)
                    && !mFromSettingsSummary) {
                setResult(RESULT_TIMEOUT);
            }
            finish();
        }
        stopIconAnimation();

        super.onStop();
    }

    @Override
    protected boolean shouldFinishWhenBackgrounded() {
        // Prevent super.onStop() from finishing, since we handle this in our onStop().
        return false;
    }

    @Override
    protected void onDestroy() {
        stopListenOrientationEvent();
        super.onDestroy();
    }

    private void animateProgress(int progress) {
        if (mCanAssumeUdfps) {
            // UDFPS animations are owned by SystemUI
            if (progress >= PROGRESS_BAR_MAX) {
                // Wait for any animations in SysUI to finish, then proceed to next page
                getMainThreadHandler().postDelayed(mDelayedFinishRunnable, getFinishDelay());
            }
            return;
        }
        if (mProgressAnim != null) {
            mProgressAnim.cancel();
        }
        ObjectAnimator anim = ObjectAnimator.ofInt(mProgressBar, "progress",
                mProgressBar.getProgress(), progress);
        anim.addListener(mProgressAnimationListener);
        anim.setInterpolator(mFastOutSlowInInterpolator);
        anim.setDuration(250);
        anim.start();
        mProgressAnim = anim;
    }

    private void animateFlash() {
        if (mIconBackgroundBlinksDrawable != null) {
            mIconBackgroundBlinksDrawable.start();
        }
    }

    protected Intent getFinishIntent() {
        return new Intent(this, FingerprintEnrollFinish.class);
    }

    private void updateTitleAndDescription(boolean force) {
        if (mCanAssumeUdfps) {
            updateTitleAndDescriptionForUdfps();
            return;
        } else if (mCanAssumeSfps) {
            if (force || mSfpsEnrollmentFeature.shouldUpdateTitleAndDescription()) {
                updateTitleAndDescriptionForSfps();
            }
            return;
        }

        if (mSidecar == null || mSidecar.getEnrollmentSteps() == -1) {
            setDescriptionText(R.string.security_settings_fingerprint_enroll_start_message);
        } else {
            setDescriptionText(R.string.security_settings_fingerprint_enroll_repeat_message);
        }
    }

    private void updateTitleAndDescriptionForUdfps() {
        switch (getCurrentStage()) {
            case STAGE_CENTER:
                setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title);
                if (mIsAccessibilityEnabled || mIllustrationLottie == null) {
                    setDescriptionText(R.string.security_settings_udfps_enroll_start_message);
                } else if (!mHaveShownUdfpsCenterLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsCenterLottie = true;
                    // Note: Update string reference when differentiate in between udfps & sfps
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_sfps_enroll_finger_center_title)
                    );
                    configureEnrollmentStage(R.raw.udfps_center_hint_lottie);
                }
                break;

            case STAGE_GUIDED:
                setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title);
                if (mIsAccessibilityEnabled || mIllustrationLottie == null) {
                    setDescriptionText(R.string.security_settings_udfps_enroll_repeat_a11y_message);
                } else if (!mHaveShownUdfpsGuideLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsGuideLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_fingerprint_enroll_repeat_message)
                    );
                    // TODO(b/228100413) Could customize guided lottie animation
                    configureEnrollmentStage(R.raw.udfps_center_hint_lottie);
                }
                break;
            case STAGE_FINGERTIP:
                setHeaderText(R.string.security_settings_udfps_enroll_fingertip_title);
                if (!mHaveShownUdfpsTipLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsTipLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_tip_fingerprint_help)
                    );
                    configureEnrollmentStage(R.raw.udfps_tip_hint_lottie);
                }
                break;
            case STAGE_LEFT_EDGE:
                setHeaderText(R.string.security_settings_udfps_enroll_left_edge_title);
                if (!mHaveShownUdfpsLeftEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsLeftEdgeLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_side_fingerprint_help)
                    );
                    configureEnrollmentStage(R.raw.udfps_left_edge_hint_lottie);
                } else if (mIllustrationLottie == null) {
                    if (isStageHalfCompleted()) {
                        setDescriptionText(
                                R.string.security_settings_fingerprint_enroll_repeat_message);
                    } else {
                        setDescriptionText(R.string.security_settings_udfps_enroll_edge_message);
                    }
                }
                break;
            case STAGE_RIGHT_EDGE:
                setHeaderText(R.string.security_settings_udfps_enroll_right_edge_title);
                if (!mHaveShownUdfpsRightEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownUdfpsRightEdgeLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(R.string.security_settings_udfps_side_fingerprint_help)
                    );
                    configureEnrollmentStage(R.raw.udfps_right_edge_hint_lottie);

                } else if (mIllustrationLottie == null) {
                    if (isStageHalfCompleted()) {
                        setDescriptionText(
                                R.string.security_settings_fingerprint_enroll_repeat_message);
                    } else {
                        setDescriptionText(R.string.security_settings_udfps_enroll_edge_message);
                    }
                }
                break;

            case STAGE_UNKNOWN:
            default:
                // setHeaderText(R.string.security_settings_fingerprint_enroll_udfps_title);
                // Don't use BiometricEnrollBase#setHeaderText, since that invokes setTitle,
                // which gets announced for a11y upon entering the page. For UDFPS, we want to
                // announce a different string for a11y upon entering the page.
                getLayout().setHeaderText(
                        R.string.security_settings_fingerprint_enroll_udfps_title);
                setDescriptionText(R.string.security_settings_udfps_enroll_start_message);
                final CharSequence description = getString(
                        R.string.security_settings_udfps_enroll_a11y);
                getLayout().getHeaderTextView().setContentDescription(description);
                setTitle(description);
                break;

        }
    }

    // Interrupt any existing talkback speech to prevent stacking talkback messages
    private void clearTalkback() {
        AccessibilityManager.getInstance(getApplicationContext()).interrupt();
    }

    private void updateTitleAndDescriptionForSfps() {
        if (mIsAccessibilityEnabled) {
            clearTalkback();
            getLayout().getDescriptionTextView().setAccessibilityLiveRegion(
                    View.ACCESSIBILITY_LIVE_REGION_POLITE);
        }
        switch (getCurrentSfpsStage()) {
            case SFPS_STAGE_NO_ANIMATION:
                setHeaderText(mSfpsEnrollmentFeature
                        .getFeaturedStageHeaderResource(SFPS_STAGE_NO_ANIMATION));
                if (!mHaveShownSfpsNoAnimationLottie && mIllustrationLottie != null) {
                    mHaveShownSfpsNoAnimationLottie = true;
                    mIllustrationLottie.setContentDescription(
                            getString(
                                    R.string.security_settings_sfps_animation_a11y_label,
                                    0
                            )
                    );
                    configureEnrollmentStage(mSfpsEnrollmentFeature
                            .getSfpsEnrollLottiePerStage(SFPS_STAGE_NO_ANIMATION));
                }
                break;

            case SFPS_STAGE_CENTER:
                setHeaderText(mSfpsEnrollmentFeature
                        .getFeaturedStageHeaderResource(SFPS_STAGE_CENTER));
                if (!mHaveShownSfpsCenterLottie && mIllustrationLottie != null) {
                    mHaveShownSfpsCenterLottie = true;
                    configureEnrollmentStage(mSfpsEnrollmentFeature
                            .getSfpsEnrollLottiePerStage(SFPS_STAGE_CENTER));
                }
                break;

            case SFPS_STAGE_FINGERTIP:
                setHeaderText(mSfpsEnrollmentFeature
                        .getFeaturedStageHeaderResource(SFPS_STAGE_FINGERTIP));
                if (!mHaveShownSfpsTipLottie && mIllustrationLottie != null) {
                    mHaveShownSfpsTipLottie = true;
                    configureEnrollmentStage(mSfpsEnrollmentFeature
                            .getSfpsEnrollLottiePerStage(SFPS_STAGE_FINGERTIP));
                }
                break;

            case SFPS_STAGE_LEFT_EDGE:
                setHeaderText(mSfpsEnrollmentFeature
                        .getFeaturedStageHeaderResource(SFPS_STAGE_LEFT_EDGE));
                if (!mHaveShownSfpsLeftEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownSfpsLeftEdgeLottie = true;
                    configureEnrollmentStage(mSfpsEnrollmentFeature
                            .getSfpsEnrollLottiePerStage(SFPS_STAGE_LEFT_EDGE));
                }
                break;

            case SFPS_STAGE_RIGHT_EDGE:
                setHeaderText(mSfpsEnrollmentFeature
                        .getFeaturedStageHeaderResource(SFPS_STAGE_RIGHT_EDGE));
                if (!mHaveShownSfpsRightEdgeLottie && mIllustrationLottie != null) {
                    mHaveShownSfpsRightEdgeLottie = true;
                    configureEnrollmentStage(mSfpsEnrollmentFeature
                            .getSfpsEnrollLottiePerStage(SFPS_STAGE_RIGHT_EDGE));
                }
                break;

            case STAGE_UNKNOWN:
            default:
                // Don't use BiometricEnrollBase#setHeaderText, since that invokes setTitle,
                // which gets announced for a11y upon entering the page. For SFPS, we want to
                // announce a different string for a11y upon entering the page.
                getLayout().setHeaderText(
                        R.string.security_settings_sfps_enroll_find_sensor_title);
                final CharSequence description = getString(
                        R.string.security_settings_sfps_enroll_find_sensor_message);
                getLayout().getHeaderTextView().setContentDescription(description);
                setTitle(description);
                break;

        }
    }

    @VisibleForTesting void configureEnrollmentStage(@RawRes int lottie) {
        if (!mCanAssumeSfps) {
            setDescriptionText("");
        }
        LottieCompositionFactory.fromRawRes(this, lottie)
                .addListener((c) -> onLottieComposition(mIllustrationLottie, c));
    }

    private void onLottieComposition(LottieAnimationView view, LottieComposition composition) {
        if (view == null || composition == null) {
            return;
        }
        view.setComposition(composition);
        view.setVisibility(View.VISIBLE);
        view.playAnimation();
        if (mCanAssumeSfps) {
            mSfpsEnrollmentFeature.handleOnEnrollmentLottieComposition(view);
        }
    }

    @EnrollStage
    private int getCurrentStage() {
        if (mSidecar == null || mSidecar.getEnrollmentSteps() == -1) {
            return STAGE_UNKNOWN;
        }

        final int progressSteps = mSidecar.getEnrollmentSteps() - mSidecar.getEnrollmentRemaining();
        if (progressSteps < getStageThresholdSteps(0)) {
            return STAGE_CENTER;
        } else if (progressSteps < getStageThresholdSteps(1)) {
            return STAGE_GUIDED;
        } else if (progressSteps < getStageThresholdSteps(2)) {
            return STAGE_FINGERTIP;
        } else if (progressSteps < getStageThresholdSteps(3)) {
            return STAGE_LEFT_EDGE;
        } else {
            return STAGE_RIGHT_EDGE;
        }
    }

    @SfpsEnrollStage
    private int getCurrentSfpsStage() {
        if (mSidecar == null) {
            return STAGE_UNKNOWN;
        }

        final int progressSteps = mSidecar.getEnrollmentSteps() - mSidecar.getEnrollmentRemaining();
        return mSfpsEnrollmentFeature
                .getCurrentSfpsEnrollStage(progressSteps, this::getStageThresholdSteps);
    }

    private boolean isStageHalfCompleted() {
        // Prior to first enrollment step.
        if (mSidecar == null || mSidecar.getEnrollmentSteps() == -1) {
            return false;
        }

        final int progressSteps = mSidecar.getEnrollmentSteps() - mSidecar.getEnrollmentRemaining();
        int prevThresholdSteps = 0;
        for (int i = 0; i < mFingerprintManager.getEnrollStageCount(); i++) {
            final int thresholdSteps = getStageThresholdSteps(i);
            if (progressSteps >= prevThresholdSteps && progressSteps < thresholdSteps) {
                final int adjustedProgress = progressSteps - prevThresholdSteps;
                final int adjustedThreshold = thresholdSteps - prevThresholdSteps;
                return adjustedProgress >= adjustedThreshold / 2;
            }
            prevThresholdSteps = thresholdSteps;
        }

        // After last enrollment step.
        return true;
    }

    @VisibleForTesting
    protected int getStageThresholdSteps(int index) {
        if (mSidecar == null || mSidecar.getEnrollmentSteps() == -1) {
            Log.w(TAG, "getStageThresholdSteps: Enrollment not started yet");
            return 1;
        }
        final float threshold = mCanAssumeSfps
                ? mSfpsEnrollmentFeature.getEnrollStageThreshold(this, index)
                : mFingerprintManager.getEnrollStageThreshold(index);
        return Math.round(mSidecar.getEnrollmentSteps() * threshold);
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        CharSequence featuredString = mCanAssumeSfps
                ? mSfpsEnrollmentFeature.getFeaturedVendorString(this, helpMsgId, helpString)
                : helpString;

        if (helpMsgId == FINGERPRINT_ACQUIRED_IMAGER_DIRTY && mCanAssumeUdfps) {
            featuredString = getResources().getString(
                    R.string.fingerprint_acquired_imager_dirty_udfps);
        }

        if (!TextUtils.isEmpty(featuredString)) {
            if (!(mCanAssumeUdfps || mCanAssumeSfps)) {
                mErrorText.removeCallbacks(mTouchAgainRunnable);
            }
            showError(featuredString);

            if (mUdfpsEnrollHelper != null) mUdfpsEnrollHelper.onEnrollmentHelp();
        }

        dismissTouchDialogIfSfps();
        if (mCanAssumeSfps) {
            mSfpsEnrollmentFeature.handleOnEnrollmentHelp(helpMsgId, featuredString, () -> this);
        }
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        onCancelEnrollment(errMsgId);
        dismissTouchDialogIfSfps();
    }

    private void announceEnrollmentProgress(CharSequence announcement) {
        AccessibilityEvent e = AccessibilityEvent.obtain();
        e.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
        e.setClassName(getClass().getName());
        e.setPackageName(getPackageName());
        e.getText().add(announcement);
        mAccessibilityManager.sendAccessibilityEvent(e);
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        updateProgress(true /* animate */);
        final int percent = (int) (((float) (steps - remaining) / (float) steps) * 100);
        if (mCanAssumeSfps) {
            mSfpsEnrollmentFeature.handleOnEnrollmentProgressChange(steps, remaining);
            if (mIsAccessibilityEnabled) {
                CharSequence announcement = getString(
                        R.string.security_settings_sfps_enroll_progress_a11y_message, percent);
                announceEnrollmentProgress(announcement);
            }
        }
        updateTitleAndDescription(false);
        animateFlash();
        if (mCanAssumeUdfps) {
            if (mIsAccessibilityEnabled) {
                CharSequence announcement = getString(
                        R.string.security_settings_udfps_enroll_progress_a11y_message, percent);
                announceEnrollmentProgress(announcement);
            }
        } else if (!mCanAssumeSfps) {
            mErrorText.removeCallbacks(mTouchAgainRunnable);
            mErrorText.postDelayed(mTouchAgainRunnable, HINT_TIMEOUT_DURATION);
        }
        dismissTouchDialogIfSfps();
    }

    private void dismissTouchDialogIfSfps() {
        if (!mCanAssumeSfps) {
            return;
        }
        final IconTouchDialog dialog = (IconTouchDialog)
                getSupportFragmentManager().findFragmentByTag(ICON_TOUCH_DIALOG);
        if (dialog != null && dialog.isResumed()) {
            dialog.dismiss();
        }
    }

    @Override
    public void onAcquired(boolean isAcquiredGood) {
        if (mUdfpsEnrollHelper != null) {
            mUdfpsEnrollHelper.onAcquired(isAcquiredGood);
        }
        if (mCanAssumeSfps) {
            mSfpsEnrollmentFeature.handleOnAcquired(isAcquiredGood);
        }
    }

    @Override
    public void onUdfpsPointerDown(int sensorId) {
        if (mUdfpsEnrollHelper != null) {
            mUdfpsEnrollHelper.onPointerDown(sensorId);
        }
    }

    @Override
    public void onUdfpsPointerUp(int sensorId) {
        if (mUdfpsEnrollHelper != null) {
            mUdfpsEnrollHelper.onPointerUp(sensorId);
        }
    }

    @Override
    public void onUdfpsOverlayShown() {
        if (mCanAssumeUdfps) {
            findViewById(R.id.udfps_animation_view).setVisibility(View.VISIBLE);
        }
    }

    private void updateProgress(boolean animate) {
        if (mSidecar == null || !mSidecar.isEnrolling()) {
            Log.d(TAG, "Enrollment not started yet");
            return;
        }

        int progress = getProgress(
                mSidecar.getEnrollmentSteps(), mSidecar.getEnrollmentRemaining());
        // Only clear the error when progress has been made.
        // TODO (b/234772728) Add tests.
        if (mProgressBar != null && mProgressBar.getProgress() < progress) {
            clearError();
        }

        if (mUdfpsEnrollHelper != null) {
            mUdfpsEnrollHelper.onEnrollmentProgress(mSidecar.getEnrollmentSteps(),
                    mSidecar.getEnrollmentRemaining());
        }

        if (animate) {
            animateProgress(progress);
        } else {
            if (mProgressBar != null) {
                mProgressBar.setProgress(progress);
            }
            if (progress >= PROGRESS_BAR_MAX) {
                mDelayedFinishRunnable.run();
            }
        }
    }

    private int getProgress(int steps, int remaining) {
        if (steps == -1) {
            return 0;
        }
        int progress = Math.max(0, steps + 1 - remaining);
        return PROGRESS_BAR_MAX * progress / (steps + 1);
    }

    private void showIconTouchDialog() {
        mIconTouchCount = 0;
        new IconTouchDialog().show(getSupportFragmentManager(), ICON_TOUCH_DIALOG);
    }

    private void showError(CharSequence error) {
        if (mCanAssumeSfps) {
            setHeaderText(error);
            if (!mHelpAnimation.isRunning()) {
                mHelpAnimation.start();
            }
            applySfpsErrorDynamicColors(getApplicationContext(), true);
        } else if (mCanAssumeUdfps) {
            setHeaderText(error);
            // Show nothing for subtitle when getting an error message.
            setDescriptionText("");
        } else {
            mErrorText.setText(error);
            if (mErrorText.getVisibility() == View.INVISIBLE) {
                mErrorText.setVisibility(View.VISIBLE);
                mErrorText.setTranslationY(getResources().getDimensionPixelSize(
                        R.dimen.fingerprint_error_text_appear_distance));
                mErrorText.setAlpha(0f);
                mErrorText.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(200)
                        .setInterpolator(mLinearOutSlowInInterpolator)
                        .start();
            } else {
                mErrorText.animate().cancel();
                mErrorText.setAlpha(1f);
                mErrorText.setTranslationY(0f);
            }
        }
        if (isResumed() && mIsAccessibilityEnabled && !mCanAssumeUdfps) {
            mVibrator.vibrate(Process.myUid(), getApplicationContext().getOpPackageName(),
                    VIBRATE_EFFECT_ERROR, getClass().getSimpleName() + "::showError",
                    FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES);
        }
    }

    private void clearError() {
        if (mCanAssumeSfps) {
            applySfpsErrorDynamicColors(getApplicationContext(), false);
        }
        if ((!(mCanAssumeUdfps || mCanAssumeSfps)) && mErrorText.getVisibility() == View.VISIBLE) {
            mErrorText.animate()
                    .alpha(0f)
                    .translationY(getResources().getDimensionPixelSize(
                            R.dimen.fingerprint_error_text_disappear_distance))
                    .setDuration(100)
                    .setInterpolator(mFastOutLinearInInterpolator)
                    .withEndAction(() -> mErrorText.setVisibility(View.INVISIBLE))
                    .start();
        }
    }

    /**
     * Applies dynamic colors corresponding to showing or clearing errors on the progress bar
     * and finger lottie for SFPS
     */
    private void applySfpsErrorDynamicColors(Context context, boolean isError) {
        applyProgressBarDynamicColor(context, isError);
        if (mIllustrationLottie != null) {
            applyLottieDynamicColor(context, isError);
        }
    }

    private void applyProgressBarDynamicColor(Context context, boolean isError) {
        if (mProgressBar != null) {
            int error_color = context.getColor(R.color.sfps_enrollment_progress_bar_error_color);
            int progress_bar_fill_color = context.getColor(
                    R.color.sfps_enrollment_progress_bar_fill_color);
            ColorStateList fillColor = ColorStateList.valueOf(
                    isError ? error_color : progress_bar_fill_color);
            mProgressBar.setProgressTintList(fillColor);
            mProgressBar.setProgressTintMode(PorterDuff.Mode.SRC);
            mProgressBar.invalidate();
        }
    }

    private void applyLottieDynamicColor(Context context, boolean isError) {
        int error_color = context.getColor(R.color.sfps_enrollment_fp_error_color);
        int fp_captured_color = context.getColor(R.color.sfps_enrollment_fp_captured_color);
        int color = isError ? error_color : fp_captured_color;
        mIllustrationLottie.addValueCallback(
                new KeyPath(".blue100", "**"),
                LottieProperty.COLOR_FILTER,
                frameInfo -> new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        );
        mIllustrationLottie.invalidate();
    }

    private void listenOrientationEvent() {
        mOrientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                final int currentRotation = getDisplay().getRotation();
                if ((mPreviousRotation == Surface.ROTATION_90
                        && currentRotation == Surface.ROTATION_270) || (
                        mPreviousRotation == Surface.ROTATION_270
                                && currentRotation == Surface.ROTATION_90)) {
                    mPreviousRotation = currentRotation;
                    recreate();
                }
            }
        };
        mOrientationEventListener.enable();
        mPreviousRotation = getDisplay().getRotation();
    }

    private void stopListenOrientationEvent() {
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
        }
        mOrientationEventListener = null;
    }

    private final Animator.AnimatorListener mProgressAnimationListener =
            new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                    startIconAnimation();
                }

                @Override
                public void onAnimationRepeat(Animator animation) { }

                @Override
                public void onAnimationEnd(Animator animation) {
                    stopIconAnimation();

                    if (mProgressBar.getProgress() >= PROGRESS_BAR_MAX) {
                        mProgressBar.postDelayed(mDelayedFinishRunnable, getFinishDelay());
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) { }
            };

    private long getFinishDelay() {
        return mCanAssumeUdfps ? 400L : 250L;
    }

    // Give the user a chance to see progress completed before jumping to the next stage.
    private final Runnable mDelayedFinishRunnable = new Runnable() {
        @Override
        public void run() {
            launchFinish(mToken);
        }
    };

    private final Animatable2.AnimationCallback mIconAnimationCallback =
            new Animatable2.AnimationCallback() {
        @Override
        public void onAnimationEnd(Drawable d) {
            if (mAnimationCancelled) {
                return;
            }

            // Start animation after it has ended.
            mProgressBar.post(new Runnable() {
                @Override
                public void run() {
                    startIconAnimation();
                }
            });
        }
    };

    private final Runnable mShowDialogRunnable = new Runnable() {
        @Override
        public void run() {
            showIconTouchDialog();
        }
    };

    private final Runnable mTouchAgainRunnable = new Runnable() {
        @Override
        public void run() {
            showError(getString(R.string.security_settings_fingerprint_enroll_lift_touch_again));
        }
    };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_ENROLLING;
    }

    private void updateOrientation(int orientation) {
        if (mCanAssumeSfps) {
            mIllustrationLottie = findViewById(R.id.illustration_lottie);
        } else {
            switch(orientation) {
                case Configuration.ORIENTATION_LANDSCAPE: {
                    mIllustrationLottie = null;
                    break;
                }
                case Configuration.ORIENTATION_PORTRAIT: {
                    if (mShouldShowLottie) {
                        mIllustrationLottie = findViewById(R.id.illustration_lottie);
                    }
                    break;
                }
                default:
                    Log.e(TAG, "Error unhandled configuration change");
                    break;
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        maybeHideSfpsText(newConfig);
        switch(newConfig.orientation) {
            case Configuration.ORIENTATION_LANDSCAPE: {
                updateOrientation(Configuration.ORIENTATION_LANDSCAPE);
                break;
            }
            case Configuration.ORIENTATION_PORTRAIT: {
                updateOrientation(Configuration.ORIENTATION_PORTRAIT);
                break;
            }
            default:
                Log.e(TAG, "Error unhandled configuration change");
                break;
        }
    }

    private void maybeHideSfpsText(@NonNull Configuration newConfig) {
        final HeaderMixin headerMixin = getLayout().getMixin(HeaderMixin.class);
        final DescriptionMixin descriptionMixin = getLayout().getMixin(DescriptionMixin.class);

        if (mCanAssumeSfps) {
            // hide the description
            descriptionMixin.getTextView().setVisibility(View.GONE);
            headerMixin.getTextView().setHyphenationFrequency(HYPHENATION_FREQUENCY_NONE);
            if (mSfpsEnrollmentFeature.shouldAdjustHeaderText(newConfig, mIsFolded)) {
                headerMixin.setAutoTextSizeEnabled(true);
                headerMixin.getTextView().setMinLines(0);
                headerMixin.getTextView().setMaxLines(10);
            } else {
                headerMixin.setAutoTextSizeEnabled(false);
                headerMixin.getTextView().setLines(4);
            }
        }
    }

    private void setUdfpsEnrollHelper() {
        mUdfpsEnrollHelper = (UdfpsEnrollHelper) getSupportFragmentManager().findFragmentByTag(
                FingerprintEnrollEnrolling.TAG_UDFPS_HELPER);
        if (mUdfpsEnrollHelper == null) {
            mUdfpsEnrollHelper = new UdfpsEnrollHelper(getApplicationContext(),
                    mFingerprintManager);
            getSupportFragmentManager().beginTransaction()
                    .add(mUdfpsEnrollHelper, FingerprintEnrollEnrolling.TAG_UDFPS_HELPER)
                    .commitAllowingStateLoss();
        }
    }

    public static class IconTouchDialog extends InstrumentedDialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(),
                    R.style.Theme_AlertDialog);
            builder.setTitle(R.string.security_settings_fingerprint_enroll_touch_dialog_title)
                    .setMessage(R.string.security_settings_fingerprint_enroll_touch_dialog_message)
                    .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
            return builder.create();
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_FINGERPRINT_ICON_TOUCH;
        }
    }

}
