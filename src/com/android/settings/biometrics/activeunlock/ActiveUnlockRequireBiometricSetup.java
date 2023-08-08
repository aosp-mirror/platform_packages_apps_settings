/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.biometrics.activeunlock;

import static android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG;
import static android.provider.Settings.ACTION_BIOMETRIC_ENROLL;
import static android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED;

import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollActivity;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.combination.CombinedBiometricStatusUtils;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;

/**
 * Activity which instructs the user to set up face or fingerprint unlock before setting the watch
 * unlock.
 */
public class ActiveUnlockRequireBiometricSetup extends BiometricEnrollBase {
    private static final String TAG = "ActiveUnlockRequireBiometricSetup";

    @VisibleForTesting
    static final int BIOMETRIC_ENROLL_REQUEST = 1001;
    private static final int ACTIVE_UNLOCK_REQUEST = 1002;
    private long mGkPwHandle;
    private boolean mNextClicked;
    private ActiveUnlockStatusUtils mActiveUnlockStatusUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activeunlock_require_biometric_setup);

        mActiveUnlockStatusUtils = new ActiveUnlockStatusUtils(this);
        mUserId = getIntent().getIntExtra(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        Log.i(TAG, "mUserId = " + mUserId);
        mGkPwHandle = getIntent().getLongExtra(EXTRA_KEY_GK_PW_HANDLE, 0L);

        final PackageManager pm = getApplicationContext().getPackageManager();
        boolean hasFeatureFace = pm.hasSystemFeature(PackageManager.FEATURE_FACE);
        boolean hasFeatureFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        if (hasFeatureFace && hasFeatureFingerprint) {
            setHeaderText(
                    R.string.security_settings_activeunlock_require_face_fingerprint_setup_title);
            setDescriptionText(
                    R.string.security_settings_activeunlock_require_face_fingerprint_setup_message);
        } else if (hasFeatureFingerprint) {
            setHeaderText(R.string.security_settings_activeunlock_require_fingerprint_setup_title);
            setDescriptionText(
                    R.string.security_settings_activeunlock_require_fingerprint_setup_message);
        } else if (hasFeatureFace) {
            setHeaderText(R.string.security_settings_activeunlock_require_face_setup_title);
            setDescriptionText(
                    R.string.security_settings_activeunlock_require_face_setup_message);
        }

        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.cancel)
                        .setListener(this::onCancelClick)
                        .setButtonType(FooterButton.ButtonType.CANCEL)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Secondary)
                        .build()
        );

        mFooterBarMixin.setPrimaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_activeunlock_biometric_setup)
                        .setListener(this::onNextButtonClick)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                        .build()
        );
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void onCancelClick(View view) {
        finish();
    }

    @Override
    protected boolean shouldFinishWhenBackgrounded() {
        return super.shouldFinishWhenBackgrounded() && !mNextClicked;
    }

    @Override
    protected void onNextButtonClick(View view) {
        mNextClicked = true;
        Intent intent = new Intent(this, BiometricEnrollActivity.InternalActivity.class);
        intent.setAction(ACTION_BIOMETRIC_ENROLL);
        intent.putExtra(EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG);
        intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, mGkPwHandle);
        startActivityForResult(intent, BIOMETRIC_ENROLL_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == BIOMETRIC_ENROLL_REQUEST && resultCode != RESULT_CANCELED) {
            CombinedBiometricStatusUtils combinedBiometricStatusUtils =
                    new CombinedBiometricStatusUtils(this, mUserId);
            if (combinedBiometricStatusUtils.hasEnrolled()) {
                Intent activeUnlockIntent = mActiveUnlockStatusUtils.getIntent();
                if (activeUnlockIntent != null) {
                    startActivityForResult(activeUnlockIntent, ACTIVE_UNLOCK_REQUEST);
                }
            }
        }
        mNextClicked = false;
        finish();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACTIVE_UNLOCK_REQUIRE_BIOMETRIC_SETUP;
    }
}
