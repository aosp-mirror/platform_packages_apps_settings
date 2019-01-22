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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollIntroduction;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.setupcompat.item.FooterButton;
import com.google.android.setupcompat.template.ButtonFooterMixin;
import com.google.android.setupdesign.span.LinkSpan;

public class FaceEnrollIntroduction extends BiometricEnrollIntroduction {

    private static final String TAG = "FaceIntro";

    private FaceManager mFaceManager;
    private FaceEnrollAccessibilityToggle mSwitchDiversity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFaceManager = Utils.getFaceManagerOrNull(this);
        final LinearLayout accessibilityLayout = findViewById(R.id.accessibility_layout);
        final Button accessibilityButton = findViewById(R.id.accessibility_button);
        accessibilityButton.setOnClickListener(view -> {
            accessibilityButton.setVisibility(View.INVISIBLE);
            accessibilityLayout.setVisibility(View.VISIBLE);
        });

        mSwitchDiversity = findViewById(R.id.toggle_diversity);

        mButtonFooterMixin = getLayout().getMixin(ButtonFooterMixin.class);
        mButtonFooterMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.security_settings_face_enroll_introduction_cancel)
                        .setListener(this::onCancelButtonClick)
                        .setButtonType(FooterButton.ButtonType.SKIP)
                        .setTheme(R.style.SudGlifButton_Secondary)
                        .build()
        );

        mButtonFooterMixin.setPrimaryButton(
                new FooterButton.Builder(this)
                        .setText(R.string.wizard_next)
                        .setListener(this::onNextButtonClick)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build()
        );
    }

    @Override
    protected boolean isDisabledByAdmin() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                this, DevicePolicyManager.KEYGUARD_DISABLE_FACE, mUserId) != null;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.face_enroll_introduction;
    }

    @Override
    protected int getHeaderResDisabledByAdmin() {
        return R.string.security_settings_face_enroll_introduction_title_unlock_disabled;
    }

    @Override
    protected int getHeaderResDefault() {
        return R.string.security_settings_face_enroll_introduction_title;
    }

    @Override
    protected int getDescriptionResDisabledByAdmin() {
        return R.string.security_settings_fingerprint_enroll_introduction_message_unlock_disabled;
    }

    @Override
    protected FooterButton getCancelButton() {
        if (mButtonFooterMixin != null) {
            return mButtonFooterMixin.getSecondaryButton();
        }
        return null;
    }

    @Override
    protected FooterButton getNextButton() {
        if (mButtonFooterMixin != null) {
            return mButtonFooterMixin.getPrimaryButton();
        }
        return null;
    }

    @Override
    protected TextView getErrorTextView() {
        return findViewById(R.id.error_text);
    }

    @Override
    protected int checkMaxEnrolled() {
        if (mFaceManager != null) {
            final int max = getResources().getInteger(
                    com.android.internal.R.integer.config_faceMaxTemplatesPerUser);
            final int numEnrolledFaces = mFaceManager.getEnrolledFaces(mUserId).size();
            if (numEnrolledFaces >= max) {
                return R.string.face_intro_error_max;
            }
        } else {
            return R.string.face_intro_error_unknown;
        }
        return 0;
    }

    @Override
    protected long getChallenge() {
        mFaceManager = Utils.getFaceManagerOrNull(this);
        if (mFaceManager == null) {
            return 0;
        }
        return mFaceManager.generateChallenge();
    }

    @Override
    protected String getExtraKeyForBiometric() {
        return ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE;
    }

    @Override
    protected Intent getEnrollingIntent() {
        final String flattenedString = getString(R.string.config_face_enroll);
        final Intent intent = new Intent();
        if (!TextUtils.isEmpty(flattenedString)) {
            ComponentName componentName = ComponentName.unflattenFromString(flattenedString);
            intent.setComponent(componentName);

        } else {
            intent.setClass(this, FaceEnrollEnrolling.class);
        }
        intent.putExtra(EXTRA_KEY_REQUIRE_DIVERSITY, mSwitchDiversity.isChecked());
        return intent;
    }

    @Override
    protected int getConfirmLockTitleResId() {
        return R.string.security_settings_face_preference_title;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.FACE_ENROLL_INTRO;
    }

    @Override
    public void onClick(LinkSpan span) {
        // TODO(b/110906762)
    }
}
