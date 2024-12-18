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

import static android.app.admin.DevicePolicyResources.Strings.Settings.FINGERPRINT_UNLOCK_DISABLED;

import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.biometrics.BiometricAuthenticator;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintSensorPropertiesInternal;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollIntroduction;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.biometrics.GatekeeperPasswordProvider;
import com.android.settings.biometrics.MultiBiometricEnrollHelper;
import com.android.settings.flags.Flags;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.span.LinkSpan;
import com.google.android.setupdesign.util.DeviceHelper;

import java.util.List;

public class FingerprintEnrollIntroduction extends BiometricEnrollIntroduction {
    private static final String TAG = "FingerprintIntro";

    @VisibleForTesting
    private FingerprintManager mFingerprintManager;
    @Nullable private FooterButton mPrimaryFooterButton;
    @Nullable private FooterButton mSecondaryFooterButton;

    private DevicePolicyManager mDevicePolicyManager;
    private boolean mCanAssumeUdfps;
    @Nullable
    protected UdfpsEnrollCalibrator mCalibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFingerprintManager = getFingerprintManager();
        if (mFingerprintManager == null) {
            Log.e(TAG, "Null FingerprintManager");
            finish();
            return;
        }

        super.onCreate(savedInstanceState);
        final FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);
        final List<FingerprintSensorPropertiesInternal> props =
                fingerprintManager.getSensorPropertiesInternal();
        mCanAssumeUdfps = props != null && props.size() == 1 && props.get(0).isAnyUdfpsType();

        mDevicePolicyManager = getSystemService(DevicePolicyManager.class);

        if (Flags.udfpsEnrollCalibration()) {
            mCalibrator = FeatureFactory.getFeatureFactory().getFingerprintFeatureProvider()
                    .getUdfpsEnrollCalibrator(getApplicationContext(), savedInstanceState, null);
        }

        final ImageView iconFingerprint = findViewById(R.id.icon_fingerprint);
        final ImageView iconDeviceLocked = findViewById(R.id.icon_device_locked);
        final ImageView iconTrashCan = findViewById(R.id.icon_trash_can);
        final ImageView iconInfo = findViewById(R.id.icon_info);
        final ImageView iconShield = findViewById(R.id.icon_shield);
        final ImageView iconLink = findViewById(R.id.icon_link);
        iconFingerprint.getDrawable().setColorFilter(getIconColorFilter());
        iconDeviceLocked.getDrawable().setColorFilter(getIconColorFilter());
        iconTrashCan.getDrawable().setColorFilter(getIconColorFilter());
        iconInfo.getDrawable().setColorFilter(getIconColorFilter());
        iconShield.getDrawable().setColorFilter(getIconColorFilter());
        iconLink.getDrawable().setColorFilter(getIconColorFilter());

        final TextView footerMessage2 = findViewById(R.id.footer_message_2);
        final TextView footerMessage3 = findViewById(R.id.footer_message_3);
        final TextView footerMessage4 = findViewById(R.id.footer_message_4);
        final TextView footerMessage5 = findViewById(R.id.footer_message_5);
        final TextView footerMessage6 = findViewById(R.id.footer_message_6);
        footerMessage2.setText(getFooterMessage2());
        footerMessage3.setText(getFooterMessage3());
        footerMessage4.setText(getFooterMessage4());
        footerMessage5.setText(getFooterMessage5());
        footerMessage6.setText(getFooterMessage6());

        final TextView footerLink = findViewById(R.id.footer_learn_more);
        footerLink.setMovementMethod(LinkMovementMethod.getInstance());
        footerLink.setText(Html.fromHtml(getString(getFooterLearnMore()),
                Html.FROM_HTML_MODE_LEGACY));

        if (mCanAssumeUdfps) {
            footerMessage6.setVisibility(View.VISIBLE);
            iconShield.setVisibility(View.VISIBLE);
        } else {
            footerMessage6.setVisibility(View.GONE);
            iconShield.setVisibility(View.GONE);
        }

        final TextView footerTitle1 = findViewById(R.id.footer_title_1);
        final TextView footerTitle2 = findViewById(R.id.footer_title_2);
        footerTitle1.setText(getFooterTitle1());
        footerTitle2.setText(getFooterTitle2());

        final ScrollView scrollView =
                findViewById(com.google.android.setupdesign.R.id.sud_scroll_view);
        scrollView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);

        final Intent intent = getIntent();
        if (mFromSettingsSummary
                && GatekeeperPasswordProvider.containsGatekeeperPasswordHandle(intent)) {
            overridePendingTransition(
                    com.google.android.setupdesign.R.anim.sud_slide_next_in,
                    com.google.android.setupdesign.R.anim.sud_slide_next_out);
            getNextButton().setEnabled(false);
            getChallenge(((sensorId, userId, challenge) -> {
                if (isFinishing()) {
                    // Do nothing if activity is finishing
                    Log.w(TAG, "activity finished before challenge callback launched.");
                    return;
                }

                mSensorId = sensorId;
                mChallenge = challenge;
                final GatekeeperPasswordProvider provider = getGatekeeperPasswordProvider();
                mToken = provider.requestGatekeeperHat(intent, challenge, mUserId);
                provider.removeGatekeeperPasswordHandle(intent, true);
                getNextButton().setEnabled(true);
            }));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (Flags.udfpsEnrollCalibration()) {
            if (mCalibrator != null) {
                mCalibrator.onSaveInstanceState(outState);
            }
        }
    }

    @Override
    protected void initViews() {
        setDescriptionText(getString(
                isPrivateProfile()
                        ? R.string.private_space_fingerprint_enroll_introduction_message
                        : R.string.security_settings_fingerprint_enroll_introduction_v3_message,
                DeviceHelper.getDeviceName(this)));

        super.initViews();
    }

    @VisibleForTesting
    @Nullable
    protected FingerprintManager getFingerprintManager() {
        return Utils.getFingerprintManagerOrNull(this);
    }

    /**
     * Returns the intent extra data for setResult(), null means nothing need to been sent back
     */
    @Nullable
    @Override
    protected Intent getSetResultIntentExtra(@Nullable Intent activityResultIntent) {
        Intent intent = super.getSetResultIntentExtra(activityResultIntent);
        if (mFromSettingsSummary && mToken != null && mChallenge != -1L) {
            if (intent == null) {
                intent = new Intent();
            }
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            intent.putExtra(EXTRA_KEY_CHALLENGE, mChallenge);
        }
        return intent;
    }

    @Override
    protected void onCancelButtonClick(View view) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(
                this, ENROLL_NEXT_BIOMETRIC_REQUEST, "cancel")) {
            super.onCancelButtonClick(view);
        }
    }

    @Override
    protected void onSkipButtonClick(View view) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(
                this, ENROLL_NEXT_BIOMETRIC_REQUEST, "skipped")) {
            super.onSkipButtonClick(view);
        }
    }

    @Override
    protected void onFinishedEnrolling(@Nullable Intent data) {
        if (!BiometricUtils.tryStartingNextBiometricEnroll(
                this, ENROLL_NEXT_BIOMETRIC_REQUEST, "finished")) {
            super.onFinishedEnrolling(data);
        }
    }

    @StringRes
    int getNegativeButtonTextId() {
        return R.string.security_settings_fingerprint_enroll_introduction_no_thanks;
    }

    @StringRes
    protected int getFooterTitle1() {
        return R.string.security_settings_fingerprint_enroll_introduction_footer_title_1;
    }

    @StringRes
    protected int getFooterTitle2() {
        return R.string.security_settings_fingerprint_enroll_introduction_footer_title_2;
    }

    @StringRes
    protected int getFooterMessage2() {
        return R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_2;
    }

    @StringRes
    protected int getFooterMessage3() {
        return R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_3;
    }

    @StringRes
    protected int getFooterMessage4() {
        return R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_4;
    }

    @StringRes
    protected int getFooterMessage5() {
        if (isPrivateProfile()) {
            return R.string.private_space_fingerprint_enroll_introduction_footer_message;
        }
        return R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_5;
    }

    @StringRes
    protected int getFooterMessage6() {
        return R.string.security_settings_fingerprint_v2_enroll_introduction_footer_message_6;
    }

    @StringRes
    protected int getFooterLearnMore() {
        return R.string.security_settings_fingerprint_v2_enroll_introduction_message_learn_more;
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
        if (isPrivateProfile()) {
            return R.string.private_space_fingerprint_enroll_introduction_title;
        }
        return R.string.security_settings_fingerprint_enroll_introduction_title;
    }

    @Override
    protected String getDescriptionDisabledByAdmin() {
        return mDevicePolicyManager.getResources().getString(
                FINGERPRINT_UNLOCK_DISABLED,
                () -> getString(R.string.security_settings_fingerprint_enroll_introduction_message_unlock_disabled));
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

    private boolean isFromSetupWizardSuggestAction(@Nullable Intent intent) {
        return intent != null && intent.getBooleanExtra(
                WizardManagerHelper.EXTRA_IS_SUW_SUGGESTED_ACTION_FLOW, false);
    }

    @Override
    protected int checkMaxEnrolled() {
        final boolean isSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
        final boolean isDeferredSetupWizard =
                WizardManagerHelper.isDeferredSetupWizard(getIntent());
        final boolean isPortalSetupWizard =
                WizardManagerHelper.isPortalSetupWizard(getIntent());
        final boolean isFromSetupWizardSuggestAction = isFromSetupWizardSuggestAction(getIntent());
        if (mFingerprintManager != null) {
            final List<FingerprintSensorPropertiesInternal> props =
                    mFingerprintManager.getSensorPropertiesInternal();
            // This will need to be updated for devices with multiple fingerprint sensors
            if (props == null || props.isEmpty()) {
                return R.string.fingerprint_intro_error_unknown;
            }
            final int max = props.get(0).maxEnrollmentsPerUser;
            final int numEnrolledFingerprints =
                    mFingerprintManager.getEnrolledFingerprints(mUserId).size();
            final int maxFingerprintsEnrollableIfSUW =
                    getApplicationContext()
                            .getResources()
                            .getInteger(R.integer.suw_max_fingerprints_enrollable);
            if (isSetupWizard && !isDeferredSetupWizard && !isPortalSetupWizard
                    && !isFromSetupWizardSuggestAction) {
                if (numEnrolledFingerprints >= maxFingerprintsEnrollableIfSUW) {
                    return R.string.fingerprint_intro_error_max;
                } else {
                    return 0;
                }
            } else if (numEnrolledFingerprints >= max) {
                return R.string.fingerprint_intro_error_max;
            } else {
                return 0;
            }
        } else {
            return R.string.fingerprint_intro_error_unknown;
        }
    }

    @Override
    protected void getChallenge(GenerateChallengeCallback callback) {
        mFingerprintManager.generateChallenge(mUserId, callback::onChallengeGenerated);
    }

    @Override
    protected String getExtraKeyForBiometric() {
        return ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT;
    }

    @Override
    protected Intent getEnrollingIntent() {
        final Intent intent = new Intent(this, FingerprintEnrollFindSensor.class);
        BiometricUtils.copyMultiBiometricExtras(getIntent(), intent);
        if (BiometricUtils.containsGatekeeperPasswordHandle(getIntent())) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                    BiometricUtils.getGatekeeperPasswordHandle(getIntent()));
        }
        if (Flags.udfpsEnrollCalibration()) {
            if (mCalibrator != null) {
                intent.putExtras(mCalibrator.getExtrasForNextIntent());
            }
        }
        intent.putExtra(BiometricUtils.EXTRA_ENROLL_REASON,
                getIntent().getIntExtra(BiometricUtils.EXTRA_ENROLL_REASON, -1));
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
        if ("url".equals(span.getLink())) {
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

    @Override
    public @BiometricAuthenticator.Modality int getModality() {
        return BiometricAuthenticator.TYPE_FINGERPRINT;
    }

    @Override
    @NonNull
    protected FooterButton getPrimaryFooterButton() {
        if (mPrimaryFooterButton == null) {
            mPrimaryFooterButton = new FooterButton.Builder(this)
                    .setText(R.string.security_settings_fingerprint_enroll_introduction_agree)
                    .setListener(this::onNextButtonClick)
                    .setButtonType(FooterButton.ButtonType.OPT_IN)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build();
        }
        return mPrimaryFooterButton;
    }

    @Override
    @NonNull
    protected FooterButton getSecondaryFooterButton() {
        if (mSecondaryFooterButton == null) {
            mSecondaryFooterButton = new FooterButton.Builder(this)
                    .setText(getNegativeButtonTextId())
                    .setListener(this::onSkipButtonClick)
                    .setButtonType(FooterButton.ButtonType.NEXT)
                    .setTheme(com.google.android.setupdesign.R.style.SudGlifButton_Primary)
                    .build();
        }
        return mSecondaryFooterButton;
    }

    @Override
    @StringRes
    protected int getAgreeButtonTextRes() {
        return R.string.security_settings_fingerprint_enroll_introduction_agree;
    }

    @Override
    @StringRes
    protected int getMoreButtonTextRes() {
        return R.string.security_settings_face_enroll_introduction_more;
    }

    @NonNull
    protected static Intent setSkipPendingEnroll(@Nullable Intent data) {
        if (data == null) {
            data = new Intent();
        }
        data.putExtra(MultiBiometricEnrollHelper.EXTRA_SKIP_PENDING_ENROLL, true);
        return data;
    }

    private boolean isPrivateProfile() {
        return Utils.isPrivateProfile(mUserId, getApplicationContext());
    }
}
