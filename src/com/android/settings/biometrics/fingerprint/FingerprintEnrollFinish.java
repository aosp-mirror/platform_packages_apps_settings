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
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.password.ChooseLockSettingsHelper;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;

/**
 * Activity which concludes fingerprint enrollment.
 */
public class FingerprintEnrollFinish extends BiometricEnrollBase {

    private static final String TAG = "FingerprintEnrollFinish";
    private static final String ACTION_FINGERPRINT_SETTINGS =
            "android.settings.FINGERPRINT_SETTINGS";
    @VisibleForTesting
    static final int REQUEST_ADD_ANOTHER = 1;
    @VisibleForTesting
    static final String FINGERPRINT_SUGGESTION_ACTIVITY =
            "com.android.settings.SetupFingerprintSuggestionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_finish);
        setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title);

        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.fingerprint_enroll_button_add)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        mFooterBarMixin.setPrimaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_fingerprint_enroll_done)
                        .setListener(this::onNextButtonClick)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build()
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        updateFingerprintSuggestionEnableState();
    }

    @Override
    protected void onResume() {
        super.onResume();

        FooterButton addButton = mFooterBarMixin.getSecondaryButton();

        final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(this);
        boolean hideAddAnother = false;
        if (fpm != null) {
            int enrolled = fpm.getEnrolledFingerprints(mUserId).size();
            int max = getResources().getInteger(
                    com.android.internal.R.integer.config_fingerprintMaxTemplatesPerUser);
            hideAddAnother = enrolled >= max;
        }
        if (hideAddAnother) {
            // Don't show "Add" button if too many fingerprints already added
            addButton.setVisibility(View.INVISIBLE);
        } else {
            addButton.setOnClickListener(this::onAddAnotherButtonClick);
        }
    }

    @Override
    protected void onNextButtonClick(View view) {
        updateFingerprintSuggestionEnableState();
        setResult(RESULT_FINISHED);
        if (WizardManagerHelper.isAnySetupWizard(getIntent())) {
            postEnroll();
        } else if (mFromSettingsSummary) {
            // Only launch fingerprint settings if enrollment was triggered through settings summary
            launchFingerprintSettings();
        }
        finish();
    }

    private void updateFingerprintSuggestionEnableState() {
        final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(this);
        if (fpm != null) {
            int enrolled = fpm.getEnrolledFingerprints(mUserId).size();

            // Only show "Add another fingerprint" if the user already enrolled one.
            // "Add fingerprint" will be shown in the main flow if the user hasn't enrolled any
            // fingerprints. If the user already added more than one fingerprint, they already know
            // to add multiple fingerprints so we don't show the suggestion.
            int flag = (enrolled == 1) ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

            ComponentName componentName = new ComponentName(getApplicationContext(),
                    FINGERPRINT_SUGGESTION_ACTIVITY);
            getPackageManager().setComponentEnabledSetting(
                    componentName, flag, PackageManager.DONT_KILL_APP);
            Log.d(TAG, FINGERPRINT_SUGGESTION_ACTIVITY + " enabled state = " + (enrolled == 1));
        }
    }

    private void postEnroll() {
        final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(this);
        if (fpm != null) {
            int result = fpm.postEnroll();
            if (result < 0) {
                Log.w(TAG, "postEnroll failed: result = " + result);
            }
        }
    }

    private void launchFingerprintSettings() {
        final Intent intent = new Intent(ACTION_FINGERPRINT_SETTINGS);
        intent.setPackage(Utils.SETTINGS_PACKAGE_NAME);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        startActivity(intent);
    }

    private void onAddAnotherButtonClick(View view) {
        startActivityForResult(getFingerprintEnrollingIntent(), REQUEST_ADD_ANOTHER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        updateFingerprintSuggestionEnableState();
        if (requestCode == REQUEST_ADD_ANOTHER && resultCode != RESULT_CANCELED) {
            setResult(resultCode, data);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_ENROLL_FINISH;
    }
}
