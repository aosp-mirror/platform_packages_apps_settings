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

package com.android.settings.biometrics.fingerprint;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollIntroduction;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.span.LinkSpan;
import com.google.android.setupdesign.template.RequireScrollMixin;

import java.util.List;

public class FingerprintEnrollIntroduction extends BiometricEnrollIntroduction {

    private static final String TAG = "FingerprintIntro";

    private FingerprintManager mFingerprintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFingerprintManager = Utils.getFingerprintManagerOrNull(this);
        if (mFingerprintManager == null) {
            Log.e(TAG, "Null FingerprintManager");
            finish();
            return;
        }

        super.onCreate(savedInstanceState);

        final ImageView iconFingerprint = findViewById(R.id.icon_fingerprint);
        final ImageView iconLocked = findViewById(R.id.icon_locked);
        final ImageView iconDelete = findViewById(R.id.icon_delete);
        final ImageView iconInfo = findViewById(R.id.icon_info);
        final ImageView iconLink = findViewById(R.id.icon_link);
        iconFingerprint.getDrawable().setColorFilter(getIconColorFilter());
        iconLocked.getDrawable().setColorFilter(getIconColorFilter());
        iconDelete.getDrawable().setColorFilter(getIconColorFilter());
        iconInfo.getDrawable().setColorFilter(getIconColorFilter());
        iconLink.getDrawable().setColorFilter(getIconColorFilter());

        mFooterBarMixin = getLayout().getMixin(FooterBarMixin.class);
        mFooterBarMixin.setSecondaryButton(
                new FooterButton.Builder(this)
                        .setText(getNegativeButtonTextId())
                        .setListener(this::onSkipButtonClick)
                        .setButtonType(FooterButton.ButtonType.NEXT)
                        .setTheme(R.style.SudGlifButton_Primary)
                        .build(),
                true /* usePrimaryStyle */);

        final FooterButton nextButton = new FooterButton.Builder(this)
                .setText(R.string.security_settings_fingerprint_enroll_introduction_agree)
                .setListener(this::onNextButtonClick)
                .setButtonType(FooterButton.ButtonType.OPT_IN)
                .setTheme(R.style.SudGlifButton_Primary)
                .build();

        mFooterBarMixin.setPrimaryButton(nextButton);
        final RequireScrollMixin requireScrollMixin =
                getLayout().getMixin(RequireScrollMixin.class);
        requireScrollMixin.requireScrollWithButton(this, nextButton,
                R.string.security_settings_face_enroll_introduction_more, this::onNextButtonClick);
    }

    int getNegativeButtonTextId() {
        return R.string.security_settings_fingerprint_enroll_introduction_skip;
    }

    @Override
    protected boolean isDisabledByAdmin() {
        return RestrictedLockUtilsInternal.checkIfKeyguardFeaturesDisabled(
                this, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, mUserId) != null;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.fingerprint_enroll_introduction;
    }

    @Override
    protected int getHeaderResDisabledByAdmin() {
        return R.string.security_settings_fingerprint_enroll_introduction_title_unlock_disabled;
    }

    @Override
    protected int getHeaderResDefault() {
        return R.string.security_settings_fingerprint_enroll_introduction_title;
    }

    @Override
    protected int getDescriptionResDisabledByAdmin() {
        return R.string.security_settings_fingerprint_enroll_introduction_message_unlock_disabled;
    }

    @Override
    protected FooterButton getCancelButton() {
        if (mFooterBarMixin != null) {
            return mFooterBarMixin.getSecondaryButton();
        }
        return null;
    }

    @Override
    protected FooterButton getNextButton() {
        if (mFooterBarMixin != null) {
            return mFooterBarMixin.getPrimaryButton();
        }
        return null;
    }

    @Override
    protected TextView getErrorTextView() {
        return findViewById(R.id.error_text);
    }

    @Override
    protected int checkMaxEnrolled() {
        if (mFingerprintManager != null) {
            final List<FingerprintSensorPropertiesInternal> props =
                    mFingerprintManager.getSensorPropertiesInternal();
            // This will need to be updated for devices with multiple fingerprint sensors
            final int max = props.get(0).maxEnrollmentsPerUser;
            final int numEnrolledFingerprints =
                    mFingerprintManager.getEnrolledFingerprints(mUserId).size();
            if (numEnrolledFingerprints >= max) {
                return R.string.fingerprint_intro_error_max;
            }
        } else {
            return R.string.fingerprint_intro_error_unknown;
        }
        return 0;
    }

    @Override
    protected void getChallenge(GenerateChallengeCallback callback) {
        mFingerprintManager = Utils.getFingerprintManagerOrNull(this);
        if (mFingerprintManager == null) {
            callback.onChallengeGenerated(0, 0, 0L);
            return;
        }
        mFingerprintManager.generateChallenge(mUserId, callback::onChallengeGenerated);
    }

    @Override
    protected String getExtraKeyForBiometric() {
        return ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT;
    }

    @Override
    protected Intent getEnrollingIntent() {
        final Intent intent = new Intent(this, FingerprintEnrollFindSensor.class);
        if (BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                    BiometricUtils.getGatekeeperPasswordHandle(getIntent()));
        }
        return intent;
    }

    @Override
    protected int getConfirmLockTitleResId() {
        return R.string.security_settings_fingerprint_preference_title;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_ENROLL_INTRO;
    }

    @Override
    public void onClick(LinkSpan span) {
        if ("url".equals(span.getId())) {
            String url = getString(R.string.help_url_fingerprint);
            Intent intent = HelpUtils.getHelpIntent(this, url, getClass().getName());
            if (intent == null) {
                Log.w(TAG, "Null help intent.");
                return;
            }
            try {
                // This needs to be startActivityForResult even though we do not care about the
                // actual result because the help app needs to know about who invoked it.
                startActivityForResult(intent, LEARN_MORE_REQUEST);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity was not found for intent, " + e);
            }
        }
    }
}
