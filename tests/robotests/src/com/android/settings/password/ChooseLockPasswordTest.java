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
import static android.view.WindowManager.LayoutParams.FLAG_SECURE;

import static com.android.internal.widget.LockPatternUtils.PASSWORD_TYPE_KEY;
import static com.android.settings.password.ChooseLockGeneric.CONFIRM_CREDENTIALS;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;

import android.annotation.ColorInt;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyManager.PasswordComplexity;
import android.app.admin.PasswordMetrics;
import android.app.admin.PasswordPolicy;
import android.content.Intent;
import android.os.Looper;
import android.os.UserHandle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.password.ChooseLockPassword.ChooseLockPasswordFragment;
import com.android.settings.password.ChooseLockPassword.IntentBuilder;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowDevicePolicyManager;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.ScrollToParentEditText;

import com.google.android.setupdesign.GlifLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDrawable;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        SettingsShadowResources.class,
        ShadowLockPatternUtils.class,
        ShadowUtils.class,
        ShadowDevicePolicyManager.class,
})
public class ChooseLockPasswordTest {
    @Before
    public void setUp() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.string.config_headlineFontFamily, "");
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
        ShadowLockPatternUtils.reset();
    }

    @Test
    public void intentBuilder_setPassword_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setPassword(LockscreenCredential.createPassword("password"))
                .setPasswordType(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC)
                .setUserId(123)
                .build();

        assertWithMessage("EXTRA_KEY_FORCE_VERIFY").that(
                intent.getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_FORCE_VERIFY, false))
                .isFalse();
        assertWithMessage("EXTRA_KEY_PASSWORD").that(
                (LockscreenCredential) intent.getParcelableExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD))
                .isEqualTo(LockscreenCredential.createPassword("password"));
        assertWithMessage("PASSWORD_TYPE_KEY").that(intent.getIntExtra(PASSWORD_TYPE_KEY, 0))
                .isEqualTo(DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        assertWithMessage("EXTRA_USER_ID").that(intent.getIntExtra(Intent.EXTRA_USER_ID, 0))
                .isEqualTo(123);
    }

    @Test
    public void intentBuilder_setRequestGatekeeperPassword_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setRequestGatekeeperPasswordHandle(true)
                .setPasswordType(PASSWORD_QUALITY_ALPHANUMERIC)
                .setUserId(123)
                .build();

        assertWithMessage("EXTRA_KEY_REQUEST_GK_PW").that(
                intent.getBooleanExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_REQUEST_GK_PW_HANDLE, false))
                .isTrue();
        assertWithMessage("PASSWORD_TYPE_KEY").that(intent.getIntExtra(PASSWORD_TYPE_KEY, 0))
                .isEqualTo(PASSWORD_QUALITY_ALPHANUMERIC);
        assertWithMessage("EXTRA_USER_ID").that(intent.getIntExtra(Intent.EXTRA_USER_ID, 0))
                .isEqualTo(123);
    }

    @Test
    public void intentBuilder_setMinComplexityMedium_hasMinComplexityExtraMedium() {
        Intent intent = new IntentBuilder(application)
                .setPasswordRequirement(PASSWORD_COMPLEXITY_MEDIUM, null)
                .build();

        assertThat(intent.hasExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY)).isTrue();
        assertThat(intent.getIntExtra(
                ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE))
                .isEqualTo(PASSWORD_COMPLEXITY_MEDIUM);
    }

    @Test
    public void intentBuilder_setMinComplexityNotCalled() {
        Intent intent = new IntentBuilder(application).build();

        assertThat(intent.hasExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY)).isFalse();
    }

    @Test
    public void intentBuilder_setProfileToUnify_shouldAddExtras() {
        Intent intent = new IntentBuilder(application)
                .setProfileToUnify(23, LockscreenCredential.createNone())
                .build();

        assertWithMessage("EXTRA_KEY_UNIFICATION_PROFILE_ID").that(
                intent.getIntExtra(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID, 0))
                .isEqualTo(23);
        assertWithMessage("EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL").that(
                (LockscreenCredential) intent.getParcelableExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL))
                .isNotNull();
    }

    @Test
    public void activity_shouldHaveSecureFlag() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_ALPHABETIC;
        policy.length = 10;

        Intent intent = createIntentForPasswordValidation(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC);
        ChooseLockPassword activity = buildChooseLockPasswordActivity(intent);
        final int flags = activity.getWindow().getAttributes().flags;
        assertThat(flags & FLAG_SECURE).isEqualTo(FLAG_SECURE);
    }

    @Test
    public void processAndValidatePasswordRequirements_cannotIncludeInvalidChar() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_UNSPECIFIED;
        // Only ASCII 31–127 should be allowed.  The invalid character error should also take
        // priority over the error that says the password is too short.
        String[] passwords = new String[] { "§µ¿¶¥£", "™™™™", "\n\n\n\n", "¡", "é" };

        for (String password : passwords) {
            assertPasswordValidationResult(
                    /* minMetrics */ policy.getMinMetrics(),
                    /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                    /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                    /* userEnteredPassword= */ LockscreenCredential.createPassword(password),
                    "This can't include an invalid character");
        }
    }

    @Test
    public void processAndValidatePasswordRequirements_noMinPasswordComplexity() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_ALPHABETIC;
        policy.length = 10;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword(""),
                "Must contain at least 1 non-numerical character",
                "Must be at least 10 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_minPasswordComplexityStricter_pin() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_SOMETHING;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPin(""),
                "PIN must be at least 8 digits");
    }

    @Test
    public void processAndValidatePasswordRequirements_minPasswordComplexityStricter_password() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_SOMETHING;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_MEDIUM,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword(""),
                "Must be at least 4 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_dpmRestrictionsStricter_password() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_ALPHANUMERIC;
        policy.length = 9;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_LOW,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword(""),
                "Must contain at least 1 non-numerical character",
                "Must contain at least 1 numerical digit",
                "Must be at least 9 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_dpmLengthLonger_pin() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_NUMERIC;
        policy.length = 11;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_MEDIUM,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPin(""),
                "PIN must be at least 11 digits");
    }

    @Test
    public void processAndValidatePasswordRequirements_dpmQualityComplex() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_COMPLEX;
        policy.symbols = 2;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword(""),
                "Must contain at least 2 special symbols",
                "Must be at least 6 characters",
                "Must contain at least 1 letter",
                "Must contain at least 1 numerical digit");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericComplexNoMinComplexity_pinRequested() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_NUMERIC_COMPLEX;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of digits isn't allowed");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericComplexNoMinComplexity_passwordRequested() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_NUMERIC_COMPLEX;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of characters isn't allowed");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericComplexHighComplexity_pinRequested() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_NUMERIC_COMPLEX;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of digits isn't allowed");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericHighComplexity_pinRequested() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_NUMERIC_COMPLEX;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of digits isn't allowed");
    }

    @Test
    @Config(shadows = ShadowLockPatternUtils.class)
    public void processAndValidatePasswordRequirements_numericComplexLowComplexity_passwordRequested() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_NUMERIC_COMPLEX;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_LOW,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("12345678"),
                "Ascending, descending, or repeated sequence of characters isn't allowed");
    }

    @Test
    public void processAndValidatePasswordRequirements_requirementsUpdateAccordingToMinComplexityAndUserInput_empty() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_UNSPECIFIED;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword(""),
                "Must be at least 6 characters",
                "If using only numbers, must be at least 8 digits");
    }

    @Test
    public void processAndValidatePasswordRequirements_requirementsUpdateAccordingToMinComplexityAndUserInput_numeric() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_UNSPECIFIED;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("1"),
                "Must be at least 6 characters",
                "If using only numbers, must be at least 8 digits");
    }

    @Test
    public void processAndValidatePasswordRequirements_requirementsUpdateAccordingToMinComplexityAndUserInput_alphabetic() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_UNSPECIFIED;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("b"),
                "Must be at least 6 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_requirementsUpdateAccordingToMinComplexityAndUserInput_alphanumeric() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_UNSPECIFIED;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("b1"),
                "Must be at least 6 characters");
    }

    @Test
    public void processAndValidatePasswordRequirements_defaultPinMinimumLength() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_UNSPECIFIED;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPassword("11"),
                "PIN must be at least 4 digits"
                        + ", but a 6-digit PIN is recommended for added security");
    }

    @Test
    public void processAndValidatePasswordRequirements_maximumLength() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_UNSPECIFIED;

        assertPasswordValidationResult(
                /* minMetrics */ policy.getMinMetrics(),
                /* minComplexity= */ PASSWORD_COMPLEXITY_NONE,
                /* passwordType= */ PASSWORD_QUALITY_ALPHABETIC,
                LockscreenCredential.createPassword("01234567890123456789"),
                "Must be fewer than 17 characters");
    }

    @Test
    public void validateComplexityMergedFromDpmOnCreate() {
        ShadowLockPatternUtils.setRequiredPasswordComplexity(PASSWORD_COMPLEXITY_LOW);

        assertPasswordValidationResult(
                /* minMetrics */ null,
                /* minComplexity= */ PASSWORD_COMPLEXITY_HIGH,
                /* passwordType= */ PASSWORD_QUALITY_NUMERIC,
                /* userEnteredPassword= */ LockscreenCredential.createPin(""),
                "PIN must be at least 8 digits");
    }

    @Test
    public void autoPinConfirmOption_featureEnabledAndUntouchedByUser_changeStateAsPerRules() {
        ChooseLockPassword passwordActivity = setupActivityWithPinTypeAndDefaultPolicy();

        ChooseLockPasswordFragment fragment = getChooseLockPasswordFragment(passwordActivity);
        ScrollToParentEditText passwordEntry = passwordActivity.findViewById(R.id.password_entry);
        CheckBox pinAutoConfirmOption = passwordActivity
                .findViewById(R.id.auto_pin_confirm_enabler);
        TextView securityMessage =
                passwordActivity.findViewById(R.id.auto_pin_confirm_security_message);

        passwordEntry.setText("1234");
        fragment.updateUi();
        assertThat(pinAutoConfirmOption.getVisibility()).isEqualTo(View.GONE);
        assertThat(securityMessage.getVisibility()).isEqualTo(View.GONE);
        assertThat(pinAutoConfirmOption.isChecked()).isFalse();

        passwordEntry.setText("123456");
        fragment.updateUi();
        assertThat(pinAutoConfirmOption.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(securityMessage.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(pinAutoConfirmOption.isChecked()).isTrue();

        passwordEntry.setText("12345678");
        fragment.updateUi();
        assertThat(pinAutoConfirmOption.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(securityMessage.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(pinAutoConfirmOption.isChecked()).isFalse();

        passwordEntry.setText("123456");
        fragment.updateUi();
        assertThat(pinAutoConfirmOption.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(securityMessage.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(pinAutoConfirmOption.isChecked()).isTrue();
    }

    @Test
    public void autoPinConfirmOption_featureEnabledAndModifiedByUser_shouldChangeStateAsPerRules() {
        ChooseLockPassword passwordActivity = setupActivityWithPinTypeAndDefaultPolicy();

        ChooseLockPasswordFragment fragment = getChooseLockPasswordFragment(passwordActivity);
        ScrollToParentEditText passwordEntry = passwordActivity.findViewById(R.id.password_entry);
        CheckBox pinAutoConfirmOption = passwordActivity
                .findViewById(R.id.auto_pin_confirm_enabler);
        TextView securityMessage =
                passwordActivity.findViewById(R.id.auto_pin_confirm_security_message);

        passwordEntry.setText("123456");
        fragment.updateUi();
        assertThat(pinAutoConfirmOption.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(securityMessage.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(pinAutoConfirmOption.isChecked()).isTrue();

        pinAutoConfirmOption.performClick();
        assertThat(pinAutoConfirmOption.isChecked()).isFalse();

        passwordEntry.setText("12345678");
        fragment.updateUi();
        assertThat(pinAutoConfirmOption.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(securityMessage.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(pinAutoConfirmOption.isChecked()).isFalse();

        passwordEntry.setText("123456");
        fragment.updateUi();
        assertThat(pinAutoConfirmOption.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(securityMessage.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(pinAutoConfirmOption.isChecked()).isFalse();
    }

    @Test
    public void defaultMessage_shouldBeInTextColorPrimary() {
        final ChooseLockPassword passwordActivity = setupActivityWithPinTypeAndDefaultPolicy();

        final ChooseLockPasswordFragment fragment = getChooseLockPasswordFragment(passwordActivity);
        final ScrollToParentEditText passwordEntry = passwordActivity.findViewById(R.id.password_entry);
        final RecyclerView view = (RecyclerView) fragment.getPasswordRequirementsView();
        @ColorInt final int textColorPrimary = Utils.getColorAttrDefaultColor(passwordActivity,
                android.R.attr.textColorPrimary);

        passwordEntry.setText("");
        fragment.updateUi();
        shadowOf(Looper.getMainLooper()).idle();
        TextView textView = (TextView)view.getLayoutManager().findViewByPosition(0);

        assertThat(textView.getCurrentTextColor()).isEqualTo(textColorPrimary);
    }

    @Test
    public void errorMessage_shouldBeColorError() {
        final ChooseLockPassword passwordActivity = setupActivityWithPinTypeAndDefaultPolicy();

        final ChooseLockPasswordFragment fragment = getChooseLockPasswordFragment(passwordActivity);
        final ScrollToParentEditText passwordEntry = passwordActivity.findViewById(R.id.password_entry);
        final RecyclerView view = (RecyclerView) fragment.getPasswordRequirementsView();
        @ColorInt final int textColorPrimary = Utils.getColorAttrDefaultColor(passwordActivity,
                android.R.attr.textColorPrimary);
        @ColorInt final int colorError = Utils.getColorAttrDefaultColor(passwordActivity,
                android.R.attr.colorError);

        passwordEntry.setText("");
        fragment.updateUi();
        shadowOf(Looper.getMainLooper()).idle();
        TextView textView = (TextView)view.getLayoutManager().findViewByPosition(0);

        assertThat(textView.getCurrentTextColor()).isEqualTo(textColorPrimary);

        // Password must be fewer than 17 digits, so this should give an error.
        passwordEntry.setText("a".repeat(17));
        fragment.updateUi();
        shadowOf(Looper.getMainLooper()).idle();
        textView = (TextView)view.getLayoutManager().findViewByPosition(0);

        assertThat(textView.getCurrentTextColor()).isEqualTo(colorError);
    }

    private ChooseLockPassword setupActivityWithPinTypeAndDefaultPolicy() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_UNSPECIFIED;

        return buildChooseLockPasswordActivity(
                new IntentBuilder(application)
                        .setUserId(UserHandle.myUserId())
                        .setPasswordType(PASSWORD_QUALITY_NUMERIC)
                        .setPasswordRequirement(PASSWORD_COMPLEXITY_NONE, policy.getMinMetrics())
                        .build());
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
        return shadowOf(((GlifLayout) fragment.getView()).getIcon());
    }

    private void assertPasswordValidationResult(PasswordMetrics minMetrics,
            @PasswordComplexity int minComplexity,
            int passwordType, LockscreenCredential userEnteredPassword,
            String... expectedValidationResult) {
        Intent intent = createIntentForPasswordValidation(minMetrics, minComplexity, passwordType);
        assertPasswordValidationResultForIntent(userEnteredPassword, intent,
                expectedValidationResult);
    }

    private void assertPasswordValidationResultForIntent(LockscreenCredential userEnteredPassword,
            Intent intent, String... expectedValidationResult) {
        ChooseLockPassword activity = buildChooseLockPasswordActivity(intent);
        ChooseLockPasswordFragment fragment = getChooseLockPasswordFragment(activity);
        fragment.validatePassword(userEnteredPassword);
        String[] messages = fragment.convertErrorCodeToMessages();
        assertThat(messages).asList().containsExactly(expectedValidationResult);
    }

    private Intent createIntentForPasswordValidation(
            PasswordMetrics minMetrics,
            @PasswordComplexity int minComplexity,
            int passwordType) {
        Intent intent = new Intent();
        intent.putExtra(CONFIRM_CREDENTIALS, false);
        intent.putExtra(PASSWORD_TYPE_KEY, passwordType);
        intent.putExtra(ChooseLockPassword.EXTRA_KEY_MIN_METRICS, minMetrics);
        intent.putExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY, minComplexity);
        return intent;
    }
}
