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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollSidecar;
import com.android.settings.biometrics.BiometricErrorDialog;
import com.android.settings.biometrics.BiometricsEnrollEnrolling;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.util.DescriptionStyler;

/**
 * Activity which handles the actual enrolling for fingerprint.
 */
public class FingerprintEnrollEnrolling extends BiometricsEnrollEnrolling {

    static final String TAG_SIDECAR = "sidecar";

    private static final int PROGRESS_BAR_MAX = 10000;
    private static final int FINISH_DELAY = 250;

    /**
     * If we don't see progress during this time, we show an error message to remind the user that
     * he needs to lift the finger and touch again.
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
    private static final AudioAttributes FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES =
            new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build();

    private ProgressBar mProgressBar;
    private ObjectAnimator mProgressAnim;
    private TextView mStartMessage;
    private TextView mRepeatMessage;
    private TextView mErrorText;
    private Interpolator mFastOutSlowInInterpolator;
    private Interpolator mLinearOutSlowInInterpolator;
    private Interpolator mFastOutLinearInInterpolator;
    private int mIconTouchCount;
    private boolean mAnimationCancelled;
    private AnimatedVectorDrawable mIconAnimationDrawable;
    private AnimatedVectorDrawable mIconBackgroundBlinksDrawable;
    private boolean mRestoring;
    private Vibrator mVibrator;

    public static class FingerprintErrorDialog extends BiometricErrorDialog {
        static FingerprintErrorDialog newInstance(CharSequence msg, int msgId) {
            FingerprintErrorDialog dialog = new FingerprintErrorDialog();
            Bundle args = new Bundle();
            args.putCharSequence(KEY_ERROR_MSG, msg);
            args.putInt(KEY_ERROR_ID, msgId);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_FINGERPINT_ERROR;
        }

        @Override
        public int getTitleResId() {
            return R.string.security_settings_fingerprint_enroll_error_dialog_title;
        }

        @Override
        public int getOkButtonTextResId() {
            return R.string.security_settings_fingerprint_enroll_dialog_ok;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_enrolling);
        setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title);
        mStartMessage = (TextView) findViewById(R.id.sud_layout_description);
        mRepeatMessage = (TextView) findViewById(R.id.repeat_message);
        mErrorText = (TextView) findViewById(R.id.error_text);
        mProgressBar = (ProgressBar) findViewById(R.id.fingerprint_progress_bar);
        mVibrator = getSystemService(Vibrator.class);

        if (getLayout().shouldApplyPartnerHeavyThemeResource()) {
            DescriptionStyler.applyPartnerCustomizationStyle(mRepeatMessage);
        }
        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_fingerprint_enroll_enrolling_skip)
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        final LayerDrawable fingerprintDrawable = (LayerDrawable) mProgressBar.getBackground();
        mIconAnimationDrawable = (AnimatedVectorDrawable)
                fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_animation);
        mIconBackgroundBlinksDrawable = (AnimatedVectorDrawable)
                fingerprintDrawable.findDrawableByLayerId(R.id.fingerprint_background);
        mIconAnimationDrawable.registerAnimationCallback(mIconAnimationCallback);
        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.fast_out_slow_in);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.linear_out_slow_in);
        mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.fast_out_linear_in);
        mProgressBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
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
            }
        });
        mRestoring = savedInstanceState != null;
    }

    @Override
    protected BiometricEnrollSidecar getSidecar() {
        return new FingerprintEnrollSidecar();
    }

    @Override
    protected boolean shouldStartAutomatically() {
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateProgress(false /* animate */);
        updateDescription();
        if (mRestoring) {
            startIconAnimation();
        }
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        mAnimationCancelled = false;
        startIconAnimation();
    }

    private void startIconAnimation() {
        mIconAnimationDrawable.start();
    }

    private void stopIconAnimation() {
        mAnimationCancelled = true;
        if (mIconAnimationDrawable != null) {
            mIconAnimationDrawable.stop();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopIconAnimation();
    }

    private void animateProgress(int progress) {
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
        mIconBackgroundBlinksDrawable.start();
    }

    protected Intent getFinishIntent() {
        return new Intent(this, FingerprintEnrollFinish.class);
    }

    private void updateDescription() {
        if (mSidecar.getEnrollmentSteps() == -1) {
            mStartMessage.setVisibility(View.VISIBLE);
            mRepeatMessage.setVisibility(View.INVISIBLE);
        } else {
            mStartMessage.setVisibility(View.INVISIBLE);
            mRepeatMessage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        if (!TextUtils.isEmpty(helpString)) {
            mErrorText.removeCallbacks(mTouchAgainRunnable);
            showError(helpString);
        }
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        int msgId;
        switch (errMsgId) {
            case FingerprintManager.FINGERPRINT_ERROR_TIMEOUT:
                // This message happens when the underlying crypto layer decides to revoke the
                // enrollment auth token.
                msgId = R.string.security_settings_fingerprint_enroll_error_timeout_dialog_message;
                break;
            default:
                // There's nothing specific to tell the user about. Ask them to try again.
                msgId = R.string.security_settings_fingerprint_enroll_error_generic_dialog_message;
                break;
        }
        showErrorDialog(getText(msgId), errMsgId);
        stopIconAnimation();
        mErrorText.removeCallbacks(mTouchAgainRunnable);
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        updateProgress(true /* animate */);
        updateDescription();
        clearError();
        animateFlash();
        mErrorText.removeCallbacks(mTouchAgainRunnable);
        mErrorText.postDelayed(mTouchAgainRunnable, HINT_TIMEOUT_DURATION);
    }

    private void updateProgress(boolean animate) {
        int progress = getProgress(
                mSidecar.getEnrollmentSteps(), mSidecar.getEnrollmentRemaining());
        if (animate) {
            animateProgress(progress);
        } else {
            mProgressBar.setProgress(progress);
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

    private void showErrorDialog(CharSequence msg, int msgId) {
        BiometricErrorDialog dlg = FingerprintErrorDialog.newInstance(msg, msgId);
        dlg.show(getSupportFragmentManager(), FingerprintErrorDialog.class.getName());
    }

    private void showIconTouchDialog() {
        mIconTouchCount = 0;
        new IconTouchDialog().show(getSupportFragmentManager(), null /* tag */);
    }

    private void showError(CharSequence error) {
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
        if (isResumed()) {
            mVibrator.vibrate(VIBRATE_EFFECT_ERROR, FINGERPRINT_ENROLLING_SONFICATION_ATTRIBUTES);
        }
    }

    private void clearError() {
        if (mErrorText.getVisibility() == View.VISIBLE) {
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

    private final Animator.AnimatorListener mProgressAnimationListener
            = new Animator.AnimatorListener() {

        @Override
        public void onAnimationStart(Animator animation) { }

        @Override
        public void onAnimationRepeat(Animator animation) { }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (mProgressBar.getProgress() >= PROGRESS_BAR_MAX) {
                mProgressBar.postDelayed(mDelayedFinishRunnable, FINISH_DELAY);
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) { }
    };

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

    public static class IconTouchDialog extends InstrumentedDialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
