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
 * limitations under the License.
 */

package com.android.settings;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.Vibrator;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;

/**
 * Wizard to enroll a fingerprint
 */
public class FingerprintEnroll extends SettingsActivity {
    /**
     * Used by the choose fingerprint wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    static final int RESULT_FINISHED = RESULT_FIRST_USER;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintEnrollFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintEnrollFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.security_settings_fingerprint_preference_title);
        setTitle(msg);
    }

    public static class FingerprintEnrollFragment extends InstrumentedFragment
            implements View.OnClickListener {
        private static final String EXTRA_PROGRESS = "progress";
        private static final String EXTRA_STAGE = "stage";
        private static final int PROGRESS_BAR_MAX = 10000;
        private static final String TAG = "FingerprintEnroll";
        private static final boolean DEBUG = true;
        private static final int CONFIRM_REQUEST = 101;
        private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;
        private static final int FINISH_DELAY = 250;

        private PowerManager mPowerManager;
        private FingerprintManager mFingerprintManager;
        private View mContentView;
        private TextView mTitleText;
        private TextView mMessageText;
        private Stage mStage;
        private int mEnrollmentSteps;
        private boolean mEnrolling;
        private Vibrator mVibrator;
        private ProgressBar mProgressBar;
        private ImageView mFingerprintAnimator;
        private ObjectAnimator mProgressAnim;

        // Give the user a chance to see progress completed before jumping to the next stage.
        Runnable mDelayedFinishRunnable = new Runnable() {
            @Override
            public void run() {
                updateStage(Stage.EnrollingFinish);
            }
        };

        private final AnimatorListener mProgressAnimationListener = new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (mProgressBar.getProgress() >= PROGRESS_BAR_MAX) {
                    mContentView.postDelayed(mDelayedFinishRunnable, FINISH_DELAY);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) { }
        };
        private CancellationSignal mEnrollmentCancel = new CancellationSignal();

        // This contains a list of all views managed by the UI. Used to determine which views
        // need to be shown/hidden at each stage. It should be the union of the lists that follow
        private static final int MANAGED_VIEWS[] = {
            R.id.fingerprint_sensor_location,
            R.id.fingerprint_animator,
            R.id.fingerprint_enroll_button_area,
            R.id.fingerprint_in_app_indicator,
            R.id.fingerprint_enroll_button_add,
            R.id.fingerprint_enroll_button_next,
            R.id.fingerprint_progress_bar
        };

        private static final int VIEWS_ENROLL_ONBOARD[] = {
            R.id.fingerprint_enroll_button_area,
            R.id.fingerprint_enroll_button_next
        };

        private static final int VIEWS_ENROLL_FIND_SENSOR[] = {
            R.id.fingerprint_sensor_location,
            R.id.fingerprint_enroll_button_area,
            R.id.fingerprint_enroll_button_next
        };

        private static final int VIEWS_ENROLL_START[] = {
            R.id.fingerprint_animator,
        };

        private static final int VIEWS_ENROLL_REPEAT[] = {
            R.id.fingerprint_animator,
            R.id.fingerprint_progress_bar
        };

        private static final int VIEWS_ENROLL_FINISH[] = {
            R.id.fingerprint_enroll_button_area,
            R.id.fingerprint_in_app_indicator,
            R.id.fingerprint_enroll_button_add,
            R.id.fingerprint_enroll_button_next
        };
        private static final boolean ALWAYS_SHOW_FIND_SCREEN = true;

        @Override
        protected int getMetricsCategory() {
            return MetricsLogger.FINGERPRINT_ENROLL;
        }

        private enum Stage {
            EnrollingOnboard(R.string.security_settings_fingerprint_enroll_onboard_title,
                    R.string.security_settings_fingerprint_enroll_onboard_message,
                    VIEWS_ENROLL_ONBOARD),
            EnrollingFindSensor(R.string.security_settings_fingerprint_enroll_find_sensor_title,
                    R.string.security_settings_fingerprint_enroll_find_sensor_message,
                    VIEWS_ENROLL_FIND_SENSOR),
            EnrollingStart(R.string.security_settings_fingerprint_enroll_start_title,
                    R.string.security_settings_fingerprint_enroll_start_message,
                    VIEWS_ENROLL_START),
            EnrollingRepeat(R.string.security_settings_fingerprint_enroll_repeat_title,
                    R.string.security_settings_fingerprint_enroll_repeat_message,
                    VIEWS_ENROLL_REPEAT),
            EnrollingFinish(R.string.security_settings_fingerprint_enroll_finish_title,
                    R.string.security_settings_fingerprint_enroll_finish_message,
                    VIEWS_ENROLL_FINISH);

            Stage(int title, int message, int[] enabledViewIds) {
                this.title = title;
                this.message = message;
                this.enabledViewIds = enabledViewIds;
            }

            public int title;
            public int message;
            public int[] enabledViewIds;
        };

        void updateStage(Stage stage) {
            if (DEBUG) Log.v(TAG, "updateStage(" + stage.toString() + ")");

            // Show/hide views
            for (int i = 0; i < MANAGED_VIEWS.length; i++) {
                mContentView.findViewById(MANAGED_VIEWS[i]).setVisibility(View.INVISIBLE);
            }
            for (int i = 0; i < stage.enabledViewIds.length; i++) {
                mContentView.findViewById(stage.enabledViewIds[i]).setVisibility(View.VISIBLE);
            }

            setTitleMessage(stage.title);
            setMessage(stage.message);

            if (mStage != stage) {
                onStageChanged(stage);
                mStage = stage;
            }
        }

        private void startFingerprintAnimator() {
            final Drawable d = mFingerprintAnimator.getDrawable();
            if (d instanceof AnimationDrawable) {
                ((AnimationDrawable) d).start();
            }
        }

        private void stopFingerprintAnimator() {
            final Drawable d = mFingerprintAnimator.getDrawable();
            if (d instanceof AnimationDrawable) {
                final AnimationDrawable drawable = (AnimationDrawable) d;
                drawable.stop();
                drawable.setLevel(0);
            }
        }

        private void onStageChanged(Stage stage) {
            // Update state
            switch (stage) {
                case EnrollingOnboard: // pass through
                case EnrollingFindSensor:
                    mEnrollmentSteps = -1;
                    mEnrolling = false;
                    break;

                case EnrollingStart:
                    mEnrollmentSteps = -1;
                    long challenge = 0x12345; // TODO: get from keyguard confirmation
                    mFingerprintManager.enroll(challenge, mEnrollmentCancel, mEnrollmentCallback, 0);
                    mProgressBar.setProgress(0);
                    mEnrolling = true;
                    startFingerprintAnimator(); // XXX hack - this should follow fingerprint detection
                    break;

                case EnrollingRepeat:
                    break;

                case EnrollingFinish:
                    stopFingerprintAnimator(); // XXX hack - this should follow fingerprint detection
                    mEnrolling = false;
                    break;

                default:
                    break;
            }
        }

        private void cancelEnrollment() {
            if (mEnrolling) {
                if (DEBUG) Log.v(TAG, "Cancel enrollment\n");
                mEnrollmentCancel.cancel();
                mEnrolling = false;
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            cancelEnrollment(); // Do a little cleanup
        }

        private void updateProgress(int progress) {
            if (DEBUG) Log.v(TAG, "Progress: " + progress);
            if (mVibrator != null) {
                mVibrator.vibrate(100, new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build());
            }
            if (mProgressAnim != null) {
                mProgressAnim.cancel();
            }
            ObjectAnimator anim = ObjectAnimator.ofInt(mProgressBar, "progress",
                    mProgressBar.getProgress(), progress);
            anim.addListener(mProgressAnimationListener);
            anim.start();
            mProgressAnim = anim;
        }

        protected void setMessage(CharSequence msg) {
            if (msg != null) mMessageText.setText(msg);
        }

        private void setMessage(int id) {
            if (id != 0) mMessageText.setText(id);
        }

        private void setTitleMessage(int title) {
            if (title != 0) mTitleText.setText(title);
        }

        private EnrollmentCallback mEnrollmentCallback = new EnrollmentCallback() {

            @Override
            public void onEnrollmentProgress(int remaining) {
                if (DEBUG) Log.v(TAG, "onEnrollResult(id=" + ", rem=" + remaining);
                if (mEnrollmentSteps == -1) {
                    mEnrollmentSteps = remaining;
                    updateStage(Stage.EnrollingRepeat);
                }
                if (remaining >= 0) {
                    int progress = Math.max(0, mEnrollmentSteps + 1 - remaining);
                    updateProgress(PROGRESS_BAR_MAX * progress / (mEnrollmentSteps + 1));
                    // Treat fingerprint like a touch event
                    mPowerManager.userActivity(SystemClock.uptimeMillis(),
                            PowerManager.USER_ACTIVITY_EVENT_OTHER,
                            PowerManager.USER_ACTIVITY_FLAG_NO_CHANGE_LIGHTS);
                }
            }

            @Override
            public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
                setMessage(helpString);
            }

            @Override
            public void onEnrollmentError(int errMsgId, CharSequence errString) {
                setMessage(errString);
            }
        };

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST) {
                if (resultCode == RESULT_FINISHED) {
                    // The lock pin/pattern/password was set. Start enrolling!
                    updateStage(Stage.EnrollingFindSensor);
                }
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            final Activity activity = getActivity();
            mFingerprintManager = (FingerprintManager)activity
                    .getSystemService(Context.FINGERPRINT_SERVICE);
            mVibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
            mPowerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);

            mContentView = inflater.inflate(R.layout.fingerprint_enroll, null);
            mTitleText = (TextView) mContentView.findViewById(R.id.fingerprint_enroll_title);
            mMessageText = (TextView) mContentView.findViewById(R.id.fingerprint_enroll_message);
            mProgressBar = (ProgressBar) mContentView.findViewById(R.id.fingerprint_progress_bar);
            mFingerprintAnimator = (ImageView) mContentView.findViewById(R.id.fingerprint_animator);

            final int buttons[] = {
                R.id.fingerprint_enroll_button_add,
                R.id.fingerprint_enroll_button_next };
            for (int i = 0; i < buttons.length; i++) {
                mContentView.findViewById(buttons[i]).setOnClickListener(this);
            }

            LockPatternUtils utils = new LockPatternUtils(activity);
            if (!utils.isSecure()) {
                // Device doesn't have any security. Set that up first.
                updateStage(Stage.EnrollingOnboard);
            } else if (ALWAYS_SHOW_FIND_SCREEN
                    || mFingerprintManager.getEnrolledFingerprints().size() == 0) {
                updateStage(Stage.EnrollingFindSensor);
            } else {
                updateStage(Stage.EnrollingStart);
            }
            return mContentView;
        }

        @Override
        public void onSaveInstanceState(final Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putString(EXTRA_STAGE, mStage.toString());
            if (mStage == Stage.EnrollingRepeat) {
                outState.putInt(EXTRA_PROGRESS, mProgressBar.getProgress());
            }
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            if (savedInstanceState != null) {
                //probably orientation change
                String stageSaved = savedInstanceState.getString(EXTRA_STAGE, null);
                if (stageSaved != null) {
                    Stage stage = Stage.valueOf(stageSaved);
                    updateStage(stage);
                    if (stage == Stage.EnrollingRepeat) {
                        mProgressBar.setProgress(savedInstanceState.getInt(EXTRA_PROGRESS));
                    }
                }
            }
        }

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.fingerprint_enroll_button_add:
                    updateStage(Stage.EnrollingStart);
                    break;
                case R.id.fingerprint_enroll_button_next:
                    if (mStage == Stage.EnrollingOnboard) {
                        launchChooseLock();
                    } else if (mStage == Stage.EnrollingFindSensor) {
                        updateStage(Stage.EnrollingStart);
                    } else if (mStage == Stage.EnrollingFinish) {
                        getActivity().finish();
                    } else {
                        Log.v(TAG, "No idea what to do next!");
                    }
                    break;
            }
        }

        private void launchChooseLock() {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                    DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
            intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS, true);
            startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
        }
    }
}
