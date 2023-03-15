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

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricEnrollSidecar;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.widget.LottieColorUtils;
import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;
import com.android.systemui.unfold.updates.FoldProvider;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;

import java.util.List;

/**
 * Activity explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class FingerprintEnrollFindSensor extends BiometricEnrollBase implements
        BiometricEnrollSidecar.Listener, FoldProvider.FoldCallback {

    private static final String TAG = "FingerprintEnrollFindSensor";
    private static final String SAVED_STATE_IS_NEXT_CLICKED = "is_next_clicked";

    @Nullable
    private FingerprintFindSensorAnimation mAnimation;

    @Nullable
    private LottieAnimationView mIllustrationLottie;

    private FingerprintEnrollSidecar mSidecar;
    private boolean mNextClicked;
    private boolean mCanAssumeUdfps;
    private boolean mCanAssumeSfps;

    private OrientationEventListener mOrientationEventListener;
    private int mPreviousRotation = 0;
    private ScreenSizeFoldProvider mScreenSizeFoldProvider;
    private boolean mIsFolded;
    private boolean mIsReverseDefaultRotation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final FingerprintManager fingerprintManager = Utils.getFingerprintManagerOrNull(this);
        final List<FingerprintSensorPropertiesInternal> props =
                fingerprintManager.getSensorPropertiesInternal();
        mCanAssumeUdfps = props != null && props.size() == 1 && props.get(0).isAnyUdfpsType();
        mCanAssumeSfps = props != null && props.size() == 1 && props.get(0).isAnySidefpsType();
        setContentView(getContentView());
        mScreenSizeFoldProvider = new ScreenSizeFoldProvider(getApplicationContext());
        mScreenSizeFoldProvider.registerCallback(this, getApplicationContext().getMainExecutor());
        mScreenSizeFoldProvider
                .onConfigurationChange(getApplicationContext().getResources().getConfiguration());
        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        listenOrientationEvent();

        if (mCanAssumeUdfps) {
            setHeaderText(R.string.security_settings_udfps_enroll_find_sensor_title);
            setDescriptionText(R.string.security_settings_udfps_enroll_find_sensor_message);
            mFooterBarMixin.setPrimaryButton(
                    new FooterButton.Builder(this)
                    .setText(R.string.security_settings_udfps_enroll_find_sensor_start_button)
                    .setListener(this::onStartButtonClick)
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setTheme(R.style.SudGlifButton_Primary)
                    .build()
            );

            mIllustrationLottie = findViewById(R.id.illustration_lottie);
            AccessibilityManager am = getSystemService(AccessibilityManager.class);
            if (am.isEnabled()) {
                mIllustrationLottie.setAnimation(R.raw.udfps_edu_a11y_lottie);
            }
        } else if (mCanAssumeSfps) {
            setHeaderText(R.string.security_settings_sfps_enroll_find_sensor_title);
            setDescriptionText(R.string.security_settings_sfps_enroll_find_sensor_message);
            mIsReverseDefaultRotation = getApplicationContext().getResources().getBoolean(
                    com.android.internal.R.bool.config_reverseDefaultRotation);
        } else {
            setHeaderText(R.string.security_settings_fingerprint_enroll_find_sensor_title);
            setDescriptionText(R.string.security_settings_fingerprint_enroll_find_sensor_message);
        }
        if (savedInstanceState != null) {
            mNextClicked = savedInstanceState.getBoolean(SAVED_STATE_IS_NEXT_CLICKED, mNextClicked);
        }

        // This is an entry point for SetNewPasswordController, e.g.
        // adb shell am start -a android.app.action.SET_NEW_PASSWORD
        if (mToken == null && BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            fingerprintManager.generateChallenge(mUserId, (sensorId, userId, challenge) -> {
                mChallenge = challenge;
                mSensorId = sensorId;
                mToken = BiometricUtils.requestGatekeeperHat(this, getIntent(), mUserId, challenge);

                // Put this into the intent. This is really just to work around the fact that the
                // enrollment sidecar gets the HAT from the activity's intent, rather than having
                // it passed in.
                getIntent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);

                // Do not start looking for fingerprint if this activity is re-created because it is
                // waiting for activity result from enrolling activity.
                if (!mNextClicked) {
                    startLookingForFingerprint();
                }
            });
        } else if (mToken != null) {
            // Do not start looking for fingerprint if this activity is re-created because it is
            // waiting for activity result from enrolling activity.
            if (!mNextClicked) {
                // HAT passed in from somewhere else, such as FingerprintEnrollIntroduction
                startLookingForFingerprint();
            }
        } else {
            // There's something wrong with the enrollment flow, this should never happen.
            throw new IllegalStateException("HAT and GkPwHandle both missing...");
        }

        mAnimation = null;
        if (mCanAssumeUdfps) {
            mIllustrationLottie.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onStartButtonClick(v);
                }
            });
        } else if (!mCanAssumeSfps) {
            View animationView = findViewById(R.id.fingerprint_sensor_location_animation);
            if (animationView instanceof FingerprintFindSensorAnimation) {
                mAnimation = (FingerprintFindSensorAnimation) animationView;
            }
        }
    }

    private int getRotationFromDefault(int rotation) {
        if (mIsReverseDefaultRotation) {
            return (rotation + 1) % 4;
        } else {
            return rotation;
        }
    }

    private void updateSfpsFindSensorAnimationAsset() {
        mScreenSizeFoldProvider
                .onConfigurationChange(getApplicationContext().getResources().getConfiguration());
        mIllustrationLottie = findViewById(R.id.illustration_lottie);
        final int rotation = getRotationFromDefault(
                getApplicationContext().getDisplay().getRotation());

        switch (rotation) {
            case Surface.ROTATION_90:
                if (mIsFolded) {
                    mIllustrationLottie.setAnimation(
                            R.raw.fingerprint_edu_lottie_folded_top_left);
                } else {
                    mIllustrationLottie.setAnimation(
                            R.raw.fingerprint_edu_lottie_portrait_top_left);
                }
                break;
            case Surface.ROTATION_180:
                if (mIsFolded) {
                    mIllustrationLottie.setAnimation(
                            R.raw.fingerprint_edu_lottie_folded_bottom_left);
                } else {
                    mIllustrationLottie.setAnimation(
                            R.raw.fingerprint_edu_lottie_landscape_bottom_left);
                }
                break;
            case Surface.ROTATION_270:
                if (mIsFolded) {
                    mIllustrationLottie.setAnimation(
                            R.raw.fingerprint_edu_lottie_folded_bottom_right);
                } else {
                    mIllustrationLottie.setAnimation(
                            R.raw.fingerprint_edu_lottie_portrait_bottom_right);
                }
                break;
            default:
                if (mIsFolded) {
                    mIllustrationLottie.setAnimation(
                            R.raw.fingerprint_edu_lottie_folded_top_right);
                } else {
                    mIllustrationLottie.setAnimation(
                            R.raw.fingerprint_edu_lottie_landscape_top_right);
                }
                break;
        }

        LottieColorUtils.applyDynamicColors(getApplicationContext(), mIllustrationLottie);
        mIllustrationLottie.setVisibility(View.VISIBLE);
        mIllustrationLottie.playAnimation();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mScreenSizeFoldProvider.onConfigurationChange(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCanAssumeSfps) {
            updateSfpsFindSensorAnimationAsset();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_STATE_IS_NEXT_CLICKED, mNextClicked);
    }

    @Override
    public void onBackPressed() {
        stopLookingForFingerprint();
        super.onBackPressed();
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true);
        super.onApplyThemeResource(theme, resid, first);
    }

    protected int getContentView() {
        if (mCanAssumeUdfps) {
            return R.layout.udfps_enroll_find_sensor_layout;
        } else if (mCanAssumeSfps) {
            return R.layout.sfps_enroll_find_sensor_layout;
        }
        return R.layout.fingerprint_enroll_find_sensor;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAnimation != null) {
            mAnimation.startAnimation();
        }
    }

    private void stopLookingForFingerprint() {
        if (mSidecar != null) {
            mSidecar.setListener(null);
            mSidecar.cancelEnrollment();
            getSupportFragmentManager()
                    .beginTransaction().remove(mSidecar).commitAllowingStateLoss();
            mSidecar = null;
        }
    }

    private void startLookingForFingerprint() {
        if (mCanAssumeUdfps) {
            // UDFPS devices use this screen as an educational screen. Users should tap the
            // "Start" button to move to the next screen to begin enrollment.
            return;
        }
        mSidecar = (FingerprintEnrollSidecar) getSupportFragmentManager().findFragmentByTag(
                FingerprintEnrollEnrolling.TAG_SIDECAR);
        if (mSidecar == null) {
            mSidecar = new FingerprintEnrollSidecar(this,
                    FingerprintManager.ENROLL_FIND_SENSOR);
            getSupportFragmentManager().beginTransaction()
                    .add(mSidecar, FingerprintEnrollEnrolling.TAG_SIDECAR)
                    .commitAllowingStateLoss();
        }
        mSidecar.setListener(this);
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        mNextClicked = true;
        proceedToEnrolling(true /* cancelEnrollment */);
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        if (mNextClicked && errMsgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
            proceedToEnrolling(false /* cancelEnrollment */);
        } else {
            FingerprintErrorDialog.showErrorDialog(this, errMsgId, mCanAssumeUdfps);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mScreenSizeFoldProvider.unregisterCallback(this);
        if (mAnimation != null) {
            mAnimation.pauseAnimation();
        }
    }

    @Override
    protected boolean shouldFinishWhenBackgrounded() {
        return super.shouldFinishWhenBackgrounded() && !mNextClicked;
    }

    @Override
    protected void onDestroy() {
        stopListenOrientationEvent();
        super.onDestroy();
        if (mAnimation != null) {
            mAnimation.stopAnimation();
        }
    }

    private void onStartButtonClick(View view) {
        mNextClicked = true;
        startActivityForResult(getFingerprintEnrollingIntent(), ENROLL_REQUEST);
    }

    protected void onSkipButtonClick(View view) {
        stopLookingForFingerprint();
        setResult(RESULT_SKIP);
        finish();
    }

    private void proceedToEnrolling(boolean cancelEnrollment) {
        if (mSidecar != null) {
            if (cancelEnrollment) {
                if (mSidecar.cancelEnrollment()) {
                    // Enrollment cancel requested. When the cancellation is successful,
                    // onEnrollmentError will be called with FINGERPRINT_ERROR_CANCELED, calling
                    // this again.
                    return;
                }
            }
            mSidecar.setListener(null);
            getSupportFragmentManager().beginTransaction().remove(mSidecar).
                    commitAllowingStateLoss();
            mSidecar = null;
            startActivityForResult(getFingerprintEnrollingIntent(), ENROLL_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,
                "onActivityResult(requestCode=" + requestCode + ", resultCode=" + resultCode + ")");
        boolean enrolledFingerprint = false;
        if (data != null) {
            enrolledFingerprint = data.getBooleanExtra(EXTRA_FINISHED_ENROLL_FINGERPRINT, false);
        }

        if (resultCode == RESULT_CANCELED && enrolledFingerprint) {
            setResult(resultCode, data);
            finish();
            return;
        }

        if (requestCode == CONFIRM_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                throw new IllegalStateException("Pretty sure this is dead code");
                /*
                mToken = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                overridePendingTransition(R.anim.sud_slide_next_in, R.anim.sud_slide_next_out);
                getIntent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startLookingForFingerprint();
                */
            } else {
                finish();
            }
        } else if (requestCode == ENROLL_REQUEST) {
            switch (resultCode) {
                case RESULT_FINISHED:
                case RESULT_SKIP:
                case RESULT_TIMEOUT:
                    setResult(resultCode);
                    finish();
                    break;
                default:
                    FingerprintManager fpm = Utils.getFingerprintManagerOrNull(this);
                    int enrolled = fpm.getEnrolledFingerprints().size();
                    final List<FingerprintSensorPropertiesInternal> props =
                            fpm.getSensorPropertiesInternal();
                    final int maxEnrollments = props.get(0).maxEnrollmentsPerUser;
                    if (enrolled >= maxEnrollments) {
                        finish();
                    } else {
                        // We came back from enrolling but it wasn't completed, start again.
                        mNextClicked = false;
                        startLookingForFingerprint();
                    }
                    break;
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_FIND_SENSOR;
    }

    private void listenOrientationEvent() {
        if (!mCanAssumeSfps) {
            // Do nothing if the device doesn't support SideFPS.
            return;
        }
        mOrientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                final int currentRotation = getRotationFromDefault(getDisplay().getRotation());
                if ((currentRotation + 2) % 4 == mPreviousRotation) {
                    mPreviousRotation = currentRotation;
                    recreate();
                }
            }
        };
        mOrientationEventListener.enable();
        mPreviousRotation = getRotationFromDefault(getDisplay().getRotation());
    }

    private void stopListenOrientationEvent() {
        if (!mCanAssumeSfps) {
            // Do nothing if the device doesn't support SideFPS.
            return;
        }
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
        }
        mOrientationEventListener = null;
    }

    @Override
    public void onFoldUpdated(boolean isFolded) {
        Log.d(TAG, "onFoldUpdated= " + isFolded);
        mIsFolded = isFolded;
    }
}
