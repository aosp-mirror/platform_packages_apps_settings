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
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.view.View;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollBase;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;

/**
 * Activity which concludes fingerprint enrollment.
 */
public class FingerprintEnrollFinish extends BiometricEnrollBase {

    private static final int REQUEST_ADD_ANOTHER = 1;

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
        setResult(RESULT_FINISHED);
        finish();
    }

    private void onAddAnotherButtonClick(View view) {
        startActivityForResult(getFingerprintEnrollingIntent(), REQUEST_ADD_ANOTHER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
