/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricEnrollSidecar;
import com.android.settings.biometrics.BiometricErrorDialog;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics.BiometricsEnrollEnrolling;
import com.android.settings.slices.CustomSliceRegistry;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.ArrayList;

public class FaceEnrollEnrolling extends BiometricsEnrollEnrolling {

    private static final String TAG = "FaceEnrollEnrolling";
    private static final boolean DEBUG = false;
    private static final String TAG_FACE_PREVIEW = "tag_preview";

    private TextView mErrorText;
    private Interpolator mLinearOutSlowInInterpolator;
    private FaceEnrollPreviewFragment mPreviewFragment;

    private ArrayList<Integer> mDisabledFeatures = new ArrayList<>();
    private ParticleCollection.Listener mListener = new ParticleCollection.Listener() {
        @Override
        public void onEnrolled() {
            FaceEnrollEnrolling.this.launchFinish(mToken);
        }
    };

    public static class FaceErrorDialog extends BiometricErrorDialog {
        static FaceErrorDialog newInstance(CharSequence msg, int msgId) {
            FaceErrorDialog dialog = new FaceErrorDialog();
            Bundle args = new Bundle();
            args.putCharSequence(KEY_ERROR_MSG, msg);
            args.putInt(KEY_ERROR_ID, msgId);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_FACE_ERROR;
        }

        @Override
        public int getTitleResId() {
            return R.string.security_settings_face_enroll_error_dialog_title;
        }

        @Override
        public int getOkButtonTextResId() {
            return R.string.security_settings_face_enroll_dialog_ok;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.face_enroll_enrolling);
        setHeaderText(R.string.security_settings_face_enroll_repeat_title);
        mErrorText = findViewById(R.id.error_text);
        mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(
                this, android.R.interpolator.linear_out_slow_in);

        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_face_enroll_enrolling_skip)
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build()
        );

        if (!getIntent().getBooleanExtra(BiometricEnrollBase.EXTRA_KEY_REQUIRE_DIVERSITY, true)) {
            mDisabledFeatures.add(FaceManager.FEATURE_REQUIRE_REQUIRE_DIVERSITY);
        }
        if (!getIntent().getBooleanExtra(BiometricEnrollBase.EXTRA_KEY_REQUIRE_VISION, true)) {
            mDisabledFeatures.add(FaceManager.FEATURE_REQUIRE_ATTENTION);
        }

        startEnrollment();
    }

    @Override
    protected void onStop() {
        if (!isChangingConfigurations()) {
            if (!WizardManagerHelper.isAnySetupWizard(getIntent())
                    && !BiometricUtils.isAnyMultiBiometricFlow(this)) {
                setResult(RESULT_TIMEOUT);
            }
            finish();
        }

        super.onStop();
    }

    @Override
    protected boolean shouldFinishWhenBackgrounded() {
        // Prevent super.onStop() from finishing, since we handle this in our onStop().
        return false;
    }

    @Override
    public void startEnrollment() {
        super.startEnrollment();
        mPreviewFragment = (FaceEnrollPreviewFragment) getSupportFragmentManager()
                .findFragmentByTag(TAG_FACE_PREVIEW);
        if (mPreviewFragment == null) {
            mPreviewFragment = new FaceEnrollPreviewFragment();
            getSupportFragmentManager().beginTransaction().add(mPreviewFragment, TAG_FACE_PREVIEW)
                    .commitAllowingStateLoss();
        }
        mPreviewFragment.setListener(mListener);
    }

    @Override
    protected Intent getFinishIntent() {
        return new Intent(this, FaceEnrollFinish.class);
    }

    @Override
    protected BiometricEnrollSidecar getSidecar() {
        final int[] disabledFeatures = new int[mDisabledFeatures.size()];
        for (int i = 0; i < mDisabledFeatures.size(); i++) {
            disabledFeatures[i] = mDisabledFeatures.get(i);
        }

        return new FaceEnrollSidecar(disabledFeatures);
    }

    @Override
    protected boolean shouldStartAutomatically() {
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FACE_ENROLL_ENROLLING;
    }

    @Override
    public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {
        if (!TextUtils.isEmpty(helpString)) {
            showError(helpString);
        }
        mPreviewFragment.onEnrollmentHelp(helpMsgId, helpString);
    }

    @Override
    public void onEnrollmentError(int errMsgId, CharSequence errString) {
        int msgId;
        switch (errMsgId) {
            case FaceManager.FACE_ERROR_TIMEOUT:
                msgId = R.string.security_settings_face_enroll_error_timeout_dialog_message;
                break;
            default:
                msgId = R.string.security_settings_face_enroll_error_generic_dialog_message;
                break;
        }
        mPreviewFragment.onEnrollmentError(errMsgId, errString);
        showErrorDialog(getText(msgId), errMsgId);
    }

    @Override
    public void onEnrollmentProgressChange(int steps, int remaining) {
        if (DEBUG) {
            Log.v(TAG, "Steps: " + steps + " Remaining: " + remaining);
        }
        mPreviewFragment.onEnrollmentProgressChange(steps, remaining);

        // TODO: Update the actual animation
        showError("Steps: " + steps + " Remaining: " + remaining);

        // TODO: Have this match any animations that UX comes up with
        if (remaining == 0) {
            // Force the reload of the FaceEnroll slice in case a user has enrolled,
            // this will cause the slice to no longer appear.
            getApplicationContext().getContentResolver().notifyChange(
                    CustomSliceRegistry.FACE_ENROLL_SLICE_URI, null);
            launchFinish(mToken);
        }
    }

    private void showErrorDialog(CharSequence msg, int msgId) {
        BiometricErrorDialog dialog = FaceErrorDialog.newInstance(msg, msgId);
        dialog.show(getSupportFragmentManager(), FaceErrorDialog.class.getName());
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
    }
}
