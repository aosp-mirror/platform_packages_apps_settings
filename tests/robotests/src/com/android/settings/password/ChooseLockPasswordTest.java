/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.password;

import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_LOW;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_MEDIUM;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import static com.android.internal.widget.LockPatternUtils.PASSWORD_TYPE_KEY;
import static com.android.settings.password.ChooseLockGeneric.CONFIRM_CREDENTIALS;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;

import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManager.PasswordComplexity;
import android.content.Intent;
import android.os.UserHandle;

import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settings.password.ChooseLockPassword.ChooseLockPasswordFragment;
import com.android.settings.password.ChooseLockPassword.IntentBuilder;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.google.android.setupdesign.GlifLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDrawable;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        SettingsShadowResources.class,
        ShadowUtils.class,
        ShadowDevicePolicyManager.class,
})
public class ChooseLockPasswordTest {

    private ShadowDevicePolicyManager mShadowDpm;

    @Before
    public void setUp() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_headlineFontFamily, "");
        mShadowDpm = ShadowDevicePolicyManager.getShadow();
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void intentBuilder_setPassword_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setPassword(LockscreenCredential.createPassword("password"))
                .setPasswordQuality(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
                .setUserId(123)
                .build();

        assertThat(intent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true))
                .named("EXTRA_KEY_HAS_CHALLENGE")
                .isFalse();
        assertThat((LockscreenCredential) intent.getParcelableExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD))
                .named("EXTRA_KEY_PASSWORD")
                .isEqualTo(LockscreenCredential.createPassword("password"));
        assertThat(intent.getIntExtra(PASSWORD_TYPE_KEY, 0))
                .named("PASSWORD_TYPE_KEY")
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        assertThat(intent.getIntExtra(Intent.EXTRA_USER_ID, 0))
                .named("EXTRA_USER_ID")
                .isEqualTo(123);
    }

    @Test
    public void intentBuilder_setChallenge_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setChallenge(12345L)
                .setPasswordQuality(PASSWORD_QUALITY_ALPHANUMERIC)
                .setUserId(123)
                .build();

        assertThat(intent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false))
                .named("EXTRA_KEY_HAS_CHALLENGE")
                .isTrue();
        assertThat(intent.getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0L))
                .named("EXTRA_KEY_CHALLENGE")
                .isEqualTo(12345L);
        assertThat(intent.getIntExtra(PASSWORD_TYPE_KEY, 0))
                .named("PASSWORD_TYPE_KEY")
                .isEqualTo(PASSWORD_QUALITY_ALPHANUMERIC);
        assertThat(intent.getIntExtra(Intent.EXTRA_USER_ID, 0))
                .named("EXTRA_USER_ID")
                .isEqualTo(123);
    }

    @Test
    public void intentBuilder_setMinComplexityMedium_hasMinComplexityExtraMedium() {
        Intent intent = new IntentBuilder(application)
                .setRequestedMinComplexity(PASSWORD_COMPLEXITY_MEDIUM)
                .build();

        assertThat(intent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isTrue();
        assertThat(intent.getIntExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE))
                .isEqualTo(PASSWORD_COMPLEXITY_MEDIUM);
    }

    @Test
    public void intentBuilder_setMinComplexityNotCalled() {
        Intent intent = new IntentBuilder(application).build();

        assertThat(intent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isFalse();
    }

    @Test
    public void intentBuilder_setProfileToUnify_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setProfileToUnify(23, LockscreenCredential.createNone())
                .build();

        assertThat(intent.getIntExtra(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID, 0))
                .named("EXTRA_KEY_UNIFICATION_PROFILE_ID")
                .isEqualTo(23);
        assertThat((LockscreenCredential) intent.getParcelableExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL))
                .named("EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL")
                .isNotNull();
    }

    @Test
    public void processAndValidatePasswordRequirements_noMinPasswordComplexity() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_ALPHABETIC);
        mShadowDpm.setPasswordMinimumLength(10);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createNone(),
                "Must contain at least 1 non-numerical character",
                "Must be at least 10 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_minPasswordComplexityStricter_pin() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_SOMETHING);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createNone(),
                "PIN must be at least 8 digits");
    }

    @Test
    public void processAndValidatePasswordRequirements_minPasswordComplexityStricter_password() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_SOMETHING);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_MEDIUM,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createNone(),
                "Must contain at least 1 non-numerical character",
                "Must be at least 4 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_dpmRestrictionsStricter_password() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_ALPHANUMERIC);
        mShadowDpm.setPasswordMinimumLength(9);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_LOW,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createNone(),
                "Must contain at least 1 non-numerical character",
                "Must contain at least 1 numerical digit",
                "Must be at least 9 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_dpmLengthLonger_pin() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_NUMERIC);
        mShadowDpm.setPasswordMinimumLength(11);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_MEDIUM,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createNone(),
                "PIN must be at least 11 digits");
    }

    @Test
    public void processAndValidatePasswordRequirements_dpmQualityComplex() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_COMPLEX);
        mShadowDpm.setPasswordMinimumSymbols(2);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createNone(),
                "Must contain at least 2 special symbols",
                "Must be at least 6 characters",
                "Must contain at least 1 letter",
                "Must contain at least 1 numerical digit");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericComplexNoMinComplexity_pinRequested() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_NUMERIC_COMPLEX);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of digits isn't allowed");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericComplexNoMinComplexity_passwordRequested() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_NUMERIC_COMPLEX);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of digits isn't allowed");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericComplexHighComplexity_pinRequested() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_NUMERIC_COMPLEX);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of digits isn't allowed");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericHighComplexity_pinRequested() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_NUMERIC);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of digits isn't allowed");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericComplexLowComplexity_passwordRequested() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_NUMERIC_COMPLEX);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_LOW,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of digits isn't allowed");
    }

    @Test
    public void processAndValidatePasswordRequirements_requirementsUpdateAccordingToMinComplexityAndUserInput_empty() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_UNSPECIFIED);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createNone(),
                "Must be at least 6 characters",
                "Must contain at least 1 non-numerical character");
    }

    @Test
    public void processAndValidatePasswordRequirements_requirementsUpdateAccordingToMinComplexityAndUserInput_numeric() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_UNSPECIFIED);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("1"),
                "Must be at least 6 characters",
                "Must contain at least 1 non-numerical character");
    }

    @Test
    public void processAndValidatePasswordRequirements_requirementsUpdateAccordingToMinComplexityAndUserInput_alphabetic() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_UNSPECIFIED);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("b"),
                "Must be at least 6 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_requirementsUpdateAccordingToMinComplexityAndUserInput_alphanumeric() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_UNSPECIFIED);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("b1"),
                "Must be at least 6 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_defaultPinMinimumLength() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_UNSPECIFIED);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("11"),
                "PIN must be at least 4 digits");
    }

    @Test
    public void processAndValidatePasswordRequirements_maximumLength() {
        mShadowDpm.setPasswordQuality(PASSWORD_QUALITY_UNSPECIFIED);

        assertPasswordValidationResult(
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                LockscreenCredential.createPassword("01234567890123456789"),
                "Must be fewer than 17 characters");
    }

    @Test
    public void assertThat_chooseLockIconChanged_WhenFingerprintExtraSet() {
        ShadowDrawable drawable = setActivityAndGetIconDrawable(true);
        assertThat(drawable.getCreatedFromResId()).isEqualTo(R.drawable.ic_fingerprint_header);
    }

    @Test
    public void assertThat_chooseLockIconNotChanged_WhenFingerprintExtraSet() {
        ShadowDrawable drawable = setActivityAndGetIconDrawable(false);
        assertThat(drawable.getCreatedFromResId()).isNotEqualTo(R.drawable.ic_fingerprint_header);
    }

    private ChooseLockPassword buildChooseLockPasswordActivity(Intent intent) {
        return Robolectric.buildActivity(ChooseLockPassword.class, intent).setup().get();
    }

    private ChooseLockPasswordFragment getChooseLockPasswordFragment(ChooseLockPassword activity) {
        return (ChooseLockPasswordFragment)
                activity.getSupportFragmentManager().findFragmentById(R.id.main_content);
    }

    private ShadowDrawable setActivityAndGetIconDrawable(boolean addFingerprintExtra) {
        ChooseLockPassword passwordActivity = buildChooseLockPasswordActivity(
                new IntentBuilder(application)
                        .setUserId(UserHandle.myUserId())
                        .setForFingerprint(addFingerprintExtra)
                        .build());
        ChooseLockPasswordFragment fragment = getChooseLockPasswordFragment(passwordActivity);
        return Shadows.shadowOf(((GlifLayout) fragment.getView()).getIcon());
    }

    private void assertPasswordValidationResult(@PasswordComplexity int minComplexity,
            int passwordType, LockscreenCredential userEnteredPassword,
            String... expectedValidationResult) {
        Intent intent = new Intent();
        intent.putExtra(CONFIRM_CREDENTIALS, false);
        intent.putExtra(PASSWORD_TYPE_KEY, passwordType);
        intent.putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, minComplexity);
        ChooseLockPassword activity = buildChooseLockPasswordActivity(intent);
        ChooseLockPasswordFragment fragment = getChooseLockPasswordFragment(activity);
        fragment.validatePassword(userEnteredPassword);
        String[] messages = fragment.convertErrorCodeToMessages();
        assertThat(messages).asList().containsExactly((Object[]) expectedValidationResult);
    }
}
