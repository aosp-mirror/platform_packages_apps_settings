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

package com.android.settings.fingerprint;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;

/**
 * Activity which handles the actual enrolling for fingerprint.
 */
public class FingerprintEnrollEnrolling extends FingerprintEnrollBase {

    private static final int PROGRESS_BAR_MAX = 10000;
    private static final int FINISH_DELAY = 250;

    /**
     * How many times the user needs to touch the icon until we show the dialog that this is not the
     * fingerprint sensor.
     */
    private static final int ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN = 3;

    private PowerManager mPowerManager;
    private CancellationSignal mEnrollmentCancel = new CancellationSignal();
    private int mEnrollmentSteps;
    private boolean mEnrolling;
    private ProgressBar mProgressBar;
    private ImageView mFingerprintAnimator;
    private ObjectAnimator mProgressAnim;
    private TextView mStartMessage;
    private TextView mRepeatMessage;
    private TextView mErrorText;
    private Interpolator mFastOutSlowInInterpolator;
    private int mIconTouchCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_enrolling);
        setHeaderText(R.string.security_settings_fingerprint_enroll_start_title);
        mPowerManager = getSystemService(PowerManager.class);
        mStartMessage = (TextView) findViewById(R.id.start_message);
        mRepeatMessage = (TextView) findViewById(R.id.repeat_message);
        mErrorText = (TextView) findViewById(R.id.error_text);
        mProgressBar = (ProgressBar) findViewById(R.id.fingerprint_progress_bar);
        mFingerprintAnimator = (ImageView) findViewById(R.id.fingerprint_animator);
        mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.fast_out_slow_in);
        findViewById(R.id.fingerprint_animator).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mIconTouchCount++;
                    if (mIconTouchCount == ICON_TOUCH_COUNT_SHOW_UNTIL_DIALOG_SHOWN) {
                        showIconTouchDialog();
                        mIconTouchCount = 0;
                    }
                }
                return true;
            }
        });
        startEnrollment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelEnrollment();
    }

    private void startEnrollment() {
        mEnrollmentSteps = -1;
        getSystemService(FingerprintManager.class).enroll(mToken, mEnrollmentCancel,
                mEnrollmentCallback, 0);
        mProgressBar.setProgress(0);
        mEnrolling = true;
    }

    private void cancelEnrollment() {
        if (mEnrolling) {
            mEnrollmentCancel.cancel();
            mEnrolling = false;
        }
    }

    private void updateProgress(int progress) {
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

    private void launchFinish(byte[] token) {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings", FingerprintEnrollFinish.class.getName());
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        startActivity(intent);
        setResult(RESULT_FINISHED);
        finish();
    }

    private void updateDescription() {
        if (mEnrollmentSteps == -1) {
            setHeaderText(R.string.security_settings_fingerprint_enroll_start_title);
            mStartMessage.setVisibility(View.VISIBLE);
            mRepeatMessage.setVisibility(View.INVISIBLE);
        } else {
            setHeaderText(R.string.security_settings_fingerprint_enroll_repeat_title);
            mStartMessage.setVisibility(View.INVISIBLE);
            mRepeatMessage.setVisibility(View.VISIBLE);
        }
    }

    private void showIconTouchDialog() {
        new IconTouchDialog().show(getFragmentManager(), null /* tag */);
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

    private FingerprintManager.EnrollmentCallback mEnrollmentCallback
            = new FingerprintManager.EnrollmentCallback() {

        @Override
        public void onEnrollmentProgress(int remaining) {
            if (mEnrollmentSteps == -1) {
                mEnrollmentSteps = remaining;
                updateDescription();
            }
            if (remaining >= 0) {
                int progress = Math.max(0, mEnrollmentSteps + 1 - remaining);
                updateProgress(PROGRESS_BAR_MAX * progress / (mEnrollmentSteps + 1));

                // Treat fingerprint like a touch event
                mPowerManager.userActivity(SystemClock.uptimeMillis(),
                        PowerManager.USER_ACTIVITY_EVENT_OTHER,
                        PowerManager.USER_ACTIVITY_FLAG_NO_CHANGE_LIGHTS);
            }
            mErrorText.setText("");
        }

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
            mErrorText.setText(helpString);
        }

        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {
            mErrorText.setText(errString);
        }
    };

    // Give the user a chance to see progress completed before jumping to the next stage.
    Runnable mDelayedFinishRunnable = new Runnable() {
        @Override
        public void run() {
            launchFinish(mToken);
        }
    };

    private static class IconTouchDialog extends DialogFragment {

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
    }
}
