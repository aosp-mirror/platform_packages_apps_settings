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

package com.android.settings.password;

import static android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_HIGH;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_LOW;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_MEDIUM;
import static android.app.admin.DevicePolicyManager.PASSWORD_COMPLEXITY_NONE;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;

import static com.android.internal.widget.LockPatternUtils.CREDENTIAL_TYPE_NONE;
import static com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment.KEY_LOCK_SETTINGS_FOOTER;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CALLER_APP_NAME;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_BIOMETRICS;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FACE;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_IS_CALLING_APP_ADMIN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.app.admin.PasswordMetrics;
import android.app.admin.PasswordPolicy;
import android.content.Context;
import android.content.Intent;
import android.hardware.face.FaceManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.provider.Settings.Global;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowInteractionJankMonitor;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowPersistentDataBlockManager;
import com.android.settings.testutils.shadow.ShadowStorageManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.widget.FooterPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                ShadowLockPatternUtils.class,
                ShadowPersistentDataBlockManager.class,
                ShadowStorageManager.class,
                ShadowUserManager.class,
                ShadowUtils.class,
                ShadowInteractionJankMonitor.class
        })
@Ignore("b/179136903: Tests failed with collapsing toolbar, plan to figure out root cause later.")
public class ChooseLockGenericTest {

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private ActivityController<ChooseLockGeneric> mActivityController;
    private FakeFeatureFactory mFakeFeatureFactory;
    private ChooseLockGenericFragment mFragment;
    private ChooseLockGeneric mActivity;
    @Mock
    private FingerprintManager mFingerprintManager;
    @Mock
    private FaceManager mFaceManager;

    @Before
    public void setUp() {
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
        mActivityController = Robolectric.buildActivity(ChooseLockGeneric.class);
        mActivity = mActivityController
                .create()
                .start()
                .postCreate(null)
                .resume()
                .get();

        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        mFragment = new ChooseLockGenericFragment();

        when(mFaceManager.isHardwareDetected()).thenReturn(true);
        when(mFingerprintManager.isHardwareDetected()).thenReturn(true);
        when(mFakeFeatureFactory.mFaceFeatureProvider.isSetupWizardSupported(any())).thenReturn(
                true);
        ShadowUtils.setFingerprintManager(mFingerprintManager);
        ShadowUtils.setFaceManager(mFaceManager);
    }

    @After
    public void tearDown() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        ShadowStorageManager.reset();
        ShadowPersistentDataBlockManager.reset();
        ShadowLockPatternUtils.reset();
    }

    @Test
    public void onCreate_deviceNotProvisioned_persistentDataExists_shouldFinishActivity() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        ShadowPersistentDataBlockManager.setDataBlockSize(1000);

        initActivity(null);
        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onCreate_deviceNotProvisioned_persistentDataServiceNotAvailable_shouldNotFinish() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        ShadowPersistentDataBlockManager.setDataBlockSize(1000);
        ShadowApplication.getInstance().setSystemService(Context.PERSISTENT_DATA_BLOCK_SERVICE,
                null);

        initActivity(null);
        assertThat(mActivity.isFinishing()).isFalse();
    }

    @Test
    public void onActivityResult_nullIntentData_shouldNotCrash() {
        initActivity(null);
        mFragment.onActivityResult(
                ChooseLockGenericFragment.CONFIRM_EXISTING_REQUEST, Activity.RESULT_OK,
                null /* data */);
        // no crash
    }

    @Test
    public void updatePreferencesOrFinish_passwordTypeSetPin_shouldStartChooseLockPassword() {
        Intent intent = new Intent().putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        assertThat(shadowOf(mActivity).getNextStartedActivity()).isNotNull();
    }

    @Test
    public void updatePreferencesOrFinish_passwordTypeSetPinNotFbe_shouldNotStartChooseLock() {
        ShadowStorageManager.setIsFileEncrypted(false);
        Intent intent = new Intent().putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        assertThat(shadowOf(mActivity).getNextStartedActivity()).isNull();
    }

    @Test
    public void updatePreferencesOrFinish_footerPreferenceAddedHighComplexityText() {
        ShadowStorageManager.setIsFileEncrypted(false);
        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_CALLER_APP_NAME, "app name")
                .putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        initActivity(intent);
        CharSequence expectedTitle =
              mActivity.getString(R.string.unlock_footer_high_complexity_requested, "app name");

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);
        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);

        assertThat(footer.getTitle()).isEqualTo(expectedTitle);
    }

    @Test
    public void updatePreferencesOrFinish_footerPreferenceAddedMediumComplexityText() {
        ShadowStorageManager.setIsFileEncrypted(false);
        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_CALLER_APP_NAME, "app name")
                .putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_MEDIUM);
        initActivity(intent);
        CharSequence expectedTitle =
                mActivity.getString(R.string.unlock_footer_medium_complexity_requested, "app name");

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);
        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);

        assertThat(footer.getTitle()).isEqualTo(expectedTitle);
    }

    @Test
    public void updatePreferencesOrFinish_footerPreferenceAddedLowComplexityText() {
        ShadowStorageManager.setIsFileEncrypted(false);
        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_CALLER_APP_NAME, "app name")
                .putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_LOW);
        initActivity(intent);
        CharSequence expectedTitle =
                mActivity.getString(R.string.unlock_footer_low_complexity_requested, "app name");

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);
        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);

        assertThat(footer.getTitle()).isEqualTo(expectedTitle);
    }

    @Test
    public void updatePreferencesOrFinish_footerPreferenceAddedNoneComplexityText() {
        ShadowStorageManager.setIsFileEncrypted(false);
        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_CALLER_APP_NAME, "app name")
                .putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE);
        initActivity(intent);
        CharSequence expectedTitle =
                mActivity.getString(R.string.unlock_footer_none_complexity_requested, "app name");

        mFragment.updatePreferencesOrFinish(/* isRecreatingActivity= */ false);
        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);

        assertThat(footer.getTitle()).isEqualTo(expectedTitle);
    }

    @Test
    public void updatePreferencesOrFinish_callingAppIsAdmin_deviceProvisioned_footerInvisible() {
        initActivity(new Intent().putExtra(EXTRA_KEY_IS_CALLING_APP_ADMIN, true));

        mFragment.updatePreferencesOrFinish(/* isRecreatingActivity= */ false);

        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);
        assertThat(footer.isVisible()).isFalse();
    }

    @Test
    public void updatePreferencesOrFinish_callingAppIsAdmin_deviceNotProvisioned_footerInvisible() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 0);
        initActivity(new Intent().putExtra(EXTRA_KEY_IS_CALLING_APP_ADMIN, true));

        mFragment.updatePreferencesOrFinish(/* isRecreatingActivity= */ false);

        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);
        assertThat(footer.isVisible()).isFalse();
    }

    @Test
    public void onActivityResult_requestcode0_shouldNotFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                SearchFeatureProvider.REQUEST_CODE, Activity.RESULT_OK, null /* data */);

        assertThat(mActivity.isFinishing()).isFalse();
    }

    @Test
    public void onActivityResult_requestcode102_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.CHOOSE_LOCK_REQUEST, Activity.RESULT_OK, null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_requestcode102_resultCancel_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(ChooseLockGenericFragment.CHOOSE_LOCK_REQUEST,
                Activity.RESULT_CANCELED, null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_requestcode103_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.CHOOSE_LOCK_BEFORE_BIOMETRIC_REQUEST,
                BiometricEnrollBase.RESULT_FINISHED, null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_requestcode104_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.SKIP_FINGERPRINT_REQUEST, Activity.RESULT_OK,
                null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void securedScreenLock_notChangingConfig_notWaitForConfirmation_onStopFinishSelf() {
        Intent intent = new Intent().putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY, DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        intent.putExtra("waiting_for_confirmation", true);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);
        mActivityController.configurationChange();
        mActivityController.stop();

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onPreferenceTreeClick_fingerprintPassesMinComplexityInfoOntoNextActivity() {
        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD)
                .putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH)
                .putExtra(EXTRA_KEY_CALLER_APP_NAME, "app name");
        initActivity(intent);

        Preference fingerprintPref = new Preference(application);
        fingerprintPref.setKey("unlock_skip_fingerprint");
        boolean result = mFragment.onPreferenceTreeClick(fingerprintPref);

        assertThat(result).isTrue();
        Intent actualIntent = shadowOf(mActivity).getNextStartedActivityForResult().intent;
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isTrue();
        assertThat(actualIntent.getIntExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY,
                PASSWORD_COMPLEXITY_NONE)).isEqualTo(PASSWORD_COMPLEXITY_HIGH);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME))
                .isEqualTo("app name");
    }

    @Test
    public void onPreferenceTreeClick_facePassesMinComplexityInfoOntoNextActivity() {
        Intent intent = new Intent(ACTION_SET_NEW_PASSWORD)
                .putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH)
                .putExtra(EXTRA_KEY_CALLER_APP_NAME, "app name");
        initActivity(intent);

        Preference facePref = new Preference(application);
        facePref.setKey("unlock_skip_face");
        boolean result = mFragment.onPreferenceTreeClick(facePref);

        assertThat(result).isTrue();
        Intent actualIntent = shadowOf(mActivity).getNextStartedActivityForResult().intent;
        assertThat(actualIntent.hasExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY)).isTrue();
        assertThat(actualIntent.getIntExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY,
                PASSWORD_COMPLEXITY_NONE)).isEqualTo(PASSWORD_COMPLEXITY_HIGH);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME))
                .isEqualTo("app name");
    }

    @Test
    public void onSetNewPassword_withTitleAndDescription_displaysPassedTitleAndDescription() {
        Intent intent =
                new Intent(ACTION_SET_NEW_PASSWORD)
                        .putExtra(
                                EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE,
                                R.string.private_space_lock_setup_title)
                        .putExtra(
                                EXTRA_KEY_CHOOSE_LOCK_SCREEN_DESCRIPTION,
                                R.string.private_space_lock_setup_description);
        initActivity(intent);

        CharSequence expectedTitle = mActivity.getString(R.string.private_space_lock_setup_title);
        CharSequence expectedDescription =
                mActivity.getString(R.string.private_space_lock_setup_description);
        assertThat(mActivity.getTitle().toString().contentEquals(expectedTitle)).isTrue();
        TextView textView =
                mFragment.getHeaderView().findViewById(R.id.biometric_header_description);
        assertThat(textView.getText().toString().contentEquals(expectedDescription)).isTrue();
    }

    @Test
    public void onSetNewPassword_withLockScreenTitle_titlePassedOntoNextActivity() {
        Intent intent =
                new Intent(ACTION_SET_NEW_PASSWORD)
                        .putExtra(
                                EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE,
                                R.string.private_space_lock_setup_title);
        initActivity(intent);

        Preference facePref = new Preference(application);
        facePref.setKey("unlock_skip_biometrics");
        boolean result = mFragment.onPreferenceTreeClick(facePref);

        assertThat(result).isTrue();
        Intent actualIntent = shadowOf(mActivity).getNextStartedActivityForResult().intent;
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE)).isTrue();
        assertThat(actualIntent.getIntExtra(EXTRA_KEY_CHOOSE_LOCK_SCREEN_TITLE, -1))
                .isEqualTo(R.string.private_space_lock_setup_title);
    }

    @Test
    public void testUnifyProfile_IntentPassedToChooseLockPassword() {
        final Bundle arguments = new Bundle();
        arguments.putInt(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID, 11);
        arguments.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL,
                LockscreenCredential.createNone());
        mFragment.setArguments(arguments);

        Intent intent = new Intent().putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        Intent nextIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(nextIntent).isNotNull();
        assertThat(nextIntent.getComponent().getClassName()).isEqualTo(
                ChooseLockPassword.class.getName());
        assertThat(nextIntent.getIntExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID, 0)).isEqualTo(11);
        assertThat((LockscreenCredential) nextIntent.getParcelableExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL)).isNotNull();
    }

    @Test
    public void testUnifyProfile_IntentPassedToChooseLockPattern() {
        final Bundle arguments = new Bundle();
        arguments.putInt(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID, 13);
        arguments.putParcelable(ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL,
                LockscreenCredential.createNone());
        mFragment.setArguments(arguments);

        Intent intent = new Intent().putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        Intent nextIntent = shadowOf(mActivity).getNextStartedActivity();
        assertThat(nextIntent).isNotNull();
        assertThat(nextIntent.getComponent().getClassName()).isEqualTo(
                ChooseLockPattern.class.getName());
        assertThat(nextIntent.getIntExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_ID, 0)).isEqualTo(13);
        assertThat((LockscreenCredential) nextIntent.getParcelableExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_UNIFICATION_PROFILE_CREDENTIAL)).isNotNull();
    }

    @Test
    public void updatePreferencesOrFinish_ComplexityIsReadFromDPM() {
        ShadowStorageManager.setIsFileEncrypted(false);
        ShadowLockPatternUtils.setRequiredPasswordComplexity(PASSWORD_COMPLEXITY_HIGH);

        initActivity(null);
        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);
        assertThat(footer.getTitle()).isEqualTo(null);

        Intent intent = mFragment.getLockPasswordIntent(PASSWORD_QUALITY_COMPLEX);
        assertThat(intent.getIntExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY,
                PASSWORD_COMPLEXITY_NONE)).isEqualTo(PASSWORD_COMPLEXITY_HIGH);
    }

    @Test
    public void updatePreferencesOrFinish_ComplexityIsMergedWithDPM() {
        ShadowStorageManager.setIsFileEncrypted(false);
        ShadowLockPatternUtils.setRequiredPasswordComplexity(PASSWORD_COMPLEXITY_HIGH);
        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_CALLER_APP_NAME, "app name")
                .putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_LOW);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        // Footer should be null because admin complexity wins.
        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);
        assertThat(footer.getTitle()).isEqualTo(null);

        Intent passwordIntent = mFragment.getLockPasswordIntent(PASSWORD_QUALITY_COMPLEX);
        assertThat(passwordIntent.getIntExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY,
                PASSWORD_COMPLEXITY_NONE)).isEqualTo(PASSWORD_COMPLEXITY_HIGH);
    }

    @Test
    public void updatePreferencesOrFinish_ComplexityIsMergedWithDPM_AppIsHigher() {
        ShadowStorageManager.setIsFileEncrypted(false);
        ShadowLockPatternUtils.setRequiredPasswordComplexity(PASSWORD_COMPLEXITY_LOW);
        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_CALLER_APP_NAME, "app name")
                .putExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_HIGH);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        // Footer should include app name because app requirement is higher.
        CharSequence expectedTitle =
                mActivity.getString(R.string.unlock_footer_high_complexity_requested, "app name");
        FooterPreference footer = mFragment.findPreference(KEY_LOCK_SETTINGS_FOOTER);
        assertThat(footer.getTitle()).isEqualTo(expectedTitle);

        Intent passwordIntent = mFragment.getLockPasswordIntent(PASSWORD_QUALITY_COMPLEX);
        assertThat(passwordIntent.getIntExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY,
                PASSWORD_COMPLEXITY_NONE)).isEqualTo(PASSWORD_COMPLEXITY_HIGH);
    }

    @Test
    public void getLockPasswordIntent_DevicePasswordRequirementOnly_PasswordComplexityPassedOn() {
        ShadowLockPatternUtils.setRequiredPasswordComplexity(PASSWORD_COMPLEXITY_LOW);
        ShadowLockPatternUtils.setRequiredProfilePasswordComplexity(PASSWORD_COMPLEXITY_HIGH);

        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY, true);
        initActivity(intent);

        Intent passwordIntent = mFragment.getLockPasswordIntent(PASSWORD_QUALITY_ALPHABETIC);
        assertThat(passwordIntent.getIntExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY,
                PASSWORD_COMPLEXITY_NONE)).isEqualTo(PASSWORD_COMPLEXITY_LOW);
        assertThat(passwordIntent.<PasswordMetrics>getParcelableExtra(
                ChooseLockPassword.EXTRA_KEY_MIN_METRICS)).isEqualTo(
                new PasswordMetrics(CREDENTIAL_TYPE_NONE));
    }

    @Test
    public void getLockPasswordIntent_DevicePasswordRequirementOnly_PasswordQualityPassedOn() {
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_SOMETHING;
        ShadowLockPatternUtils.setRequestedPasswordMetrics(policy.getMinMetrics());
        PasswordPolicy profilePolicy = new PasswordPolicy();
        profilePolicy.quality = PASSWORD_QUALITY_ALPHABETIC;
        ShadowLockPatternUtils.setRequestedProfilePasswordMetrics(profilePolicy.getMinMetrics());

        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY, true);
        initActivity(intent);

        Intent passwordIntent = mFragment.getLockPasswordIntent(PASSWORD_QUALITY_ALPHABETIC);
        assertThat(passwordIntent.getIntExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY,
                PASSWORD_COMPLEXITY_NONE)).isEqualTo(PASSWORD_COMPLEXITY_NONE);
        assertThat(passwordIntent.<PasswordMetrics>getParcelableExtra(
                ChooseLockPassword.EXTRA_KEY_MIN_METRICS)).isEqualTo(policy.getMinMetrics());
    }

    @Test
    public void getLockPasswordIntent_DevicePasswordRequirementOnly_ComplexityAndQualityPassedOn() {
        ShadowLockPatternUtils.setRequiredPasswordComplexity(PASSWORD_COMPLEXITY_LOW);
        PasswordPolicy policy = new PasswordPolicy();
        policy.quality = PASSWORD_QUALITY_ALPHABETIC;
        ShadowLockPatternUtils.setRequestedProfilePasswordMetrics(policy.getMinMetrics());

        Intent intent = new Intent()
                .putExtra(EXTRA_KEY_DEVICE_PASSWORD_REQUIREMENT_ONLY, true);
        initActivity(intent);

        Intent passwordIntent = mFragment.getLockPasswordIntent(PASSWORD_QUALITY_ALPHABETIC);
        assertThat(passwordIntent.getIntExtra(ChooseLockPassword.EXTRA_KEY_MIN_COMPLEXITY,
                PASSWORD_COMPLEXITY_NONE)).isEqualTo(PASSWORD_COMPLEXITY_LOW);
        assertThat(passwordIntent.<PasswordMetrics>getParcelableExtra(
                ChooseLockPassword.EXTRA_KEY_MIN_METRICS)).isEqualTo(
                        new PasswordMetrics(CREDENTIAL_TYPE_NONE));
    }

    @Test
    public void updatePreferenceText_supportBiometrics_setScreenLockFingerprintFace_inOrder() {
        ShadowStorageManager.setIsFileEncrypted(false);
        final Intent intent = new Intent().putExtra(EXTRA_KEY_FOR_BIOMETRICS, true);
        initActivity(intent);

        final String supportFingerprint = capitalize(mActivity.getResources().getString(
                R.string.security_settings_fingerprint));
        final String supportFace = capitalize(mActivity.getResources().getString(
                R.string.keywords_face_settings));

        // The strings of golden copy
        final String pinFingerprintFace = mActivity.getText(R.string.unlock_set_unlock_pin_title)
                + BiometricUtils.SEPARATOR + supportFingerprint + BiometricUtils.SEPARATOR
                + supportFace;
        final String patternFingerprintFace = mActivity.getText(
                R.string.unlock_set_unlock_pattern_title) + BiometricUtils.SEPARATOR
                + supportFingerprint + BiometricUtils.SEPARATOR + supportFace;
        final String passwordFingerprintFace = mActivity.getText(
                R.string.unlock_set_unlock_password_title) + BiometricUtils.SEPARATOR
                + supportFingerprint + BiometricUtils.SEPARATOR + supportFace;

        // The strings obtain from preferences
        final String pinTitle =
                (String) mFragment.findPreference(ScreenLockType.PIN.preferenceKey).getTitle();
        final String patternTitle =
                (String) mFragment.findPreference(ScreenLockType.PATTERN.preferenceKey).getTitle();
        final String passwordTitle =
                (String) mFragment.findPreference(ScreenLockType.PASSWORD.preferenceKey).getTitle();

        assertThat(pinTitle).isEqualTo(pinFingerprintFace);
        assertThat(patternTitle).isEqualTo(patternFingerprintFace);
        assertThat(passwordTitle).isEqualTo(passwordFingerprintFace);
    }

    @Test
    public void updatePreferenceText_supportFingerprint_showFingerprint() {
        ShadowStorageManager.setIsFileEncrypted(false);
        final Intent intent = new Intent().putExtra(EXTRA_KEY_FOR_FINGERPRINT, true);
        initActivity(intent);
        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        assertThat(mFragment.findPreference(ScreenLockType.PIN.preferenceKey).getTitle()).isEqualTo(
                mFragment.getString(R.string.fingerprint_unlock_set_unlock_pin));
        assertThat(mFragment.findPreference(
                ScreenLockType.PATTERN.preferenceKey).getTitle()).isEqualTo(
                mFragment.getString(R.string.fingerprint_unlock_set_unlock_pattern));
        assertThat(mFragment.findPreference(
                ScreenLockType.PASSWORD.preferenceKey).getTitle()).isEqualTo(
                mFragment.getString(R.string.fingerprint_unlock_set_unlock_password));
    }

    @Test
    public void updatePreferenceText_supportFace_showFace() {

        ShadowStorageManager.setIsFileEncrypted(false);
        final Intent intent = new Intent().putExtra(EXTRA_KEY_FOR_FACE, true);
        initActivity(intent);
        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        assertThat(mFragment.findPreference(ScreenLockType.PIN.preferenceKey).getTitle()).isEqualTo(
                mFragment.getString(R.string.face_unlock_set_unlock_pin));
        assertThat(mFragment.findPreference(
                ScreenLockType.PATTERN.preferenceKey).getTitle()).isEqualTo(
                mFragment.getString(R.string.face_unlock_set_unlock_pattern));
        assertThat(mFragment.findPreference(
                ScreenLockType.PASSWORD.preferenceKey).getTitle()).isEqualTo(
                mFragment.getString(R.string.face_unlock_set_unlock_password));
    }

    private void initActivity(@Nullable Intent intent) {
        if (intent == null) {
            intent = new Intent();
        }
        intent.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, false);
        // TODO(b/275023433) This presents the activity from being made 'visible` is workaround
        mActivity = Robolectric.buildActivity(ChooseLockGeneric.InternalActivity.class, intent)
                .create().start().postCreate(null).resume().get();
        mActivity.getSupportFragmentManager().beginTransaction().add(mFragment, null).commitNow();
    }

    private static String capitalize(final String input) {
        return Character.toUpperCase(input.charAt(0)) + input.substring(1);
    }
}
