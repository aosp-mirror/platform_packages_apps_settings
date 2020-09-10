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

import static com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment.KEY_LOCK_SETTINGS_FOOTER;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CALLER_APP_NAME;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_IS_CALLING_APP_ADMIN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_REQUESTED_MIN_COMPLEXITY;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.RuntimeEnvironment.application;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings.Global;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;
import com.android.settings.biometrics.BiometricEnrollBase;
import com.android.settings.password.ChooseLockGeneric.ChooseLockGenericFragment;
import com.android.settings.search.SearchFeatureProvider;
import com.android.settings.testutils.shadow.ShadowLockPatternUtils;
import com.android.settings.testutils.shadow.ShadowStorageManager;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settingslib.widget.FooterPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowPersistentDataBlockManager;

@RunWith(RobolectricTestRunner.class)
@Config(
        shadows = {
                ShadowLockPatternUtils.class,
                ShadowPersistentDataBlockManager.class,
                ShadowStorageManager.class,
                ShadowUserManager.class,
                ShadowUtils.class
        })
public class ChooseLockGenericTest {

    private ChooseLockGenericFragment mFragment;
    private ChooseLockGeneric mActivity;

    @Before
    public void setUp() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        mFragment = new ChooseLockGenericFragment();
    }

    @After
    public void tearDown() {
        Global.putInt(application.getContentResolver(), Global.DEVICE_PROVISIONED, 1);
        ShadowStorageManager.reset();
        ShadowPersistentDataBlockManager.reset();
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
        ShadowStorageManager.setIsFileEncryptedNativeOrEmulated(false);
        Intent intent = new Intent().putExtra(
                LockPatternUtils.PASSWORD_TYPE_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_NUMERIC);
        initActivity(intent);

        mFragment.updatePreferencesOrFinish(false /* isRecreatingActivity */);

        assertThat(shadowOf(mActivity).getNextStartedActivity()).isNull();
    }

    @Test
    public void updatePreferencesOrFinish_footerPreferenceAddedHighComplexityText() {
        ShadowStorageManager.setIsFileEncryptedNativeOrEmulated(false);
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
        ShadowStorageManager.setIsFileEncryptedNativeOrEmulated(false);
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
        ShadowStorageManager.setIsFileEncryptedNativeOrEmulated(false);
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
        ShadowStorageManager.setIsFileEncryptedNativeOrEmulated(false);
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
    @Ignore
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
    public void onActivityResult_requestcode101_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.ENABLE_ENCRYPTION_REQUEST, Activity.RESULT_OK,
                null /* data */);

        assertThat(mActivity.isFinishing()).isTrue();
    }

    @Test
    public void onActivityResult_requestcode102_shouldFinish() {
        initActivity(null);

        mFragment.onActivityResult(
                ChooseLockGenericFragment.CHOOSE_LOCK_REQUEST, Activity.RESULT_OK, null /* data */);

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
        assertThat(actualIntent.getIntExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE))
                .isEqualTo(PASSWORD_COMPLEXITY_HIGH);
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
        assertThat(actualIntent.getIntExtra(EXTRA_KEY_REQUESTED_MIN_COMPLEXITY, PASSWORD_COMPLEXITY_NONE))
                .isEqualTo(PASSWORD_COMPLEXITY_HIGH);
        assertThat(actualIntent.hasExtra(EXTRA_KEY_CALLER_APP_NAME)).isTrue();
        assertThat(actualIntent.getStringExtra(EXTRA_KEY_CALLER_APP_NAME))
                .isEqualTo("app name");
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

    private void initActivity(@Nullable Intent intent) {
        if (intent == null) {
            intent = new Intent();
        }
        intent.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, false);
        mActivity = Robolectric.buildActivity(ChooseLockGeneric.InternalActivity.class, intent)
                .setup().get();
        mActivity.getSupportFragmentManager().beginTransaction().add(mFragment, null).commitNow();
    }
}
