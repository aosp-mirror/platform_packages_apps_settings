/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.biometrics;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

/**
 * Prompts the user to hand the device to their parent or guardian.
 */
public class BiometricHandoffActivity extends BiometricEnrollBase {

    @Nullable
    private FooterButton mPrimaryFooterButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.biometric_handoff);

        setHeaderText(R.string.biometric_settings_hand_back_to_guardian);

        final GlifLayout layout = getLayout();
        mFooterBarMixin = layout.getMixin(FooterBarMixin.class);
        mFooterBarMixin.setPrimaryButton(getPrimaryFooterButton());
    }

    @NonNull
    protected FooterButton getPrimaryFooterButton() {
        if (mPrimaryFooterButton == null) {
            mPrimaryFooterButton = new FooterButton.Builder(this)
                    .setText(R.string.biometric_settings_hand_back_to_guardian_ok)
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setListener(this::onNextButtonClick)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build();
        }
        return mPrimaryFooterButton;
    }

    @Override
    protected void onNextButtonClick(View view) {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.BIOMETRIC_CONSENT_PARENT_TO_CHILD;
    }
}
