/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics2.ui.viewmodel;

import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_NUMERIC;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
import static android.app.admin.DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_CHALLENGE;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_SENSOR_ID;
import static com.android.settings.biometrics2.ui.model.CredentialModel.INVALID_CHALLENGE;
import static com.android.settings.biometrics2.ui.model.CredentialModel.INVALID_GK_PW_HANDLE;
import static com.android.settings.biometrics2.ui.model.CredentialModel.INVALID_SENSOR_ID;
import static com.android.settings.biometrics2.ui.model.CredentialModelTest.newCredentialModelIntentExtras;
import static com.android.settings.biometrics2.ui.model.CredentialModelTest.newGkPwHandleCredentialIntentExtras;
import static com.android.settings.biometrics2.ui.model.CredentialModelTest.newOnlySensorValidCredentialIntentExtras;
import static com.android.settings.biometrics2.ui.model.CredentialModelTest.newValidTokenCredentialIntentExtras;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_IS_GENERATING_CHALLENGE;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_VALID;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.ChallengeGenerator;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CredentialAction;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.GenerateChallengeCallback;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.KEY_CREDENTIAL_MODEL;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.Mockito.when;

import android.annotation.NonNull;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.activity.result.ActivityResult;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class AutoCredentialViewModelTest {

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private LockPatternUtils mLockPatternUtils;
    private TestChallengeGenerator mChallengeGenerator = null;
    private AutoCredentialViewModel mViewModel;

    @Before
    public void setUp() {
        mChallengeGenerator = new TestChallengeGenerator();
        mViewModel = new AutoCredentialViewModel(
                ApplicationProvider.getApplicationContext(),
                mLockPatternUtils,
                mChallengeGenerator);
    }

    private void setupGenerateChallenge(int userId, int newSensorId, long newChallenge) {
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);
        mChallengeGenerator.mUserId = userId;
        mChallengeGenerator.mSensorId = newSensorId;
        mChallengeGenerator.mChallenge = newChallenge;
    }

    @Test
    public void testSetCredentialModel_sameResultFromSavedInstanceOrIntent() {
        final Bundle extras = newCredentialModelIntentExtras(12, 33, 1, new byte[] { 2, 3 }, 3L);

        AutoCredentialViewModel viewModel2 = new AutoCredentialViewModel(
                ApplicationProvider.getApplicationContext(),
                mLockPatternUtils,
                mChallengeGenerator);

        mViewModel.setCredentialModel(null, new Intent().putExtras(extras));
        final Bundle savedInstance = new Bundle();
        mViewModel.onSaveInstanceState(savedInstance);
        viewModel2.setCredentialModel(savedInstance, new Intent());

        final Bundle bundle1 = mViewModel.createCredentialIntentExtra();
        final Bundle bundle2 = viewModel2.createCredentialIntentExtra();
        assertThat(bundle1.getLong(EXTRA_KEY_GK_PW_HANDLE))
                .isEqualTo(bundle2.getLong(EXTRA_KEY_GK_PW_HANDLE));
        assertThat(bundle1.getLong(Intent.EXTRA_USER_ID))
                .isEqualTo(bundle2.getLong(Intent.EXTRA_USER_ID));
        assertThat(bundle1.getLong(EXTRA_KEY_CHALLENGE))
                .isEqualTo(bundle2.getLong(EXTRA_KEY_CHALLENGE));
        assertThat(bundle1.getInt(EXTRA_KEY_SENSOR_ID))
                .isEqualTo(bundle2.getInt(EXTRA_KEY_SENSOR_ID));
        final byte[] token1 = bundle1.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN);
        final byte[] token2 = bundle2.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN);
        assertThat(token1).isNotNull();
        assertThat(token2).isNotNull();
        assertThat(token1.length).isEqualTo(token2.length);
        for (int i = 0; i < token2.length; ++i) {
            assertThat(token1[i]).isEqualTo(token2[i]);
        }
    }

    @Test
    public void testSetCredentialModel_sameResultFromSavedInstanceOrIntent_invalidValues() {
        final Bundle extras = newCredentialModelIntentExtras(UserHandle.USER_NULL,
                INVALID_CHALLENGE, INVALID_SENSOR_ID, null, INVALID_GK_PW_HANDLE);

        AutoCredentialViewModel viewModel2 = new AutoCredentialViewModel(
                ApplicationProvider.getApplicationContext(),
                mLockPatternUtils,
                mChallengeGenerator);

        mViewModel.setCredentialModel(null, new Intent().putExtras(extras));
        final Bundle savedInstance = new Bundle();
        mViewModel.onSaveInstanceState(savedInstance);
        viewModel2.setCredentialModel(savedInstance, new Intent());

        final Bundle bundle1 = mViewModel.createCredentialIntentExtra();
        final Bundle bundle2 = viewModel2.createCredentialIntentExtra();
        assertThat(bundle1.containsKey(EXTRA_KEY_GK_PW_HANDLE)).isFalse();
        assertThat(bundle2.containsKey(EXTRA_KEY_GK_PW_HANDLE)).isFalse();
        assertThat(bundle1.containsKey(EXTRA_KEY_CHALLENGE_TOKEN)).isFalse();
        assertThat(bundle2.containsKey(EXTRA_KEY_CHALLENGE_TOKEN)).isFalse();
        assertThat(bundle1.containsKey(EXTRA_KEY_SENSOR_ID)).isTrue();
        assertThat(bundle2.containsKey(EXTRA_KEY_SENSOR_ID)).isTrue();
        assertThat(bundle1.containsKey(Intent.EXTRA_USER_ID)).isFalse();
        assertThat(bundle2.containsKey(Intent.EXTRA_USER_ID)).isFalse();
    }

    @Test
    public void testCheckCredential_validCredentialCase() {
        final int userId = 99;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newValidTokenCredentialIntentExtras(userId)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        // Run credential check
        @CredentialAction final int action = mViewModel.checkCredential();

        // Check viewModel behavior
        assertThat(action).isEqualTo(CREDENTIAL_VALID);
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();

        // Check createGeneratingChallengeExtras()
        assertThat(mViewModel.createGeneratingChallengeExtras()).isNull();

        // Check onSaveInstanceState()
        final Bundle actualBundle = new Bundle();
        mViewModel.onSaveInstanceState(actualBundle);
        assertThat(actualBundle.getBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL))
                .isFalse();
    }

    @Test
    public void testCheckCredential_needToChooseLock() {
        final int userId = 100;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newOnlySensorValidCredentialIntentExtras(userId)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_UNSPECIFIED);

        // Run credential check
        @CredentialAction final int action = mViewModel.checkCredential();

        // Check viewModel behavior
        assertThat(action).isEqualTo(CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK);
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();

        // Check createGeneratingChallengeExtras()
        assertThat(mViewModel.createGeneratingChallengeExtras()).isNull();

        // Check onSaveInstanceState()
        final Bundle actualBundle = new Bundle();
        mViewModel.onSaveInstanceState(actualBundle);
        assertThat(actualBundle.getBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL))
                .isFalse();
    }

    @Test
    public void testCheckCredential_needToConfirmLockForSomething() {
        final int userId = 101;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newOnlySensorValidCredentialIntentExtras(userId)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        // Run credential check
        @CredentialAction final int action = mViewModel.checkCredential();

        // Check viewModel behavior
        assertThat(action).isEqualTo(CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK);
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();

        // Check createGeneratingChallengeExtras()
        assertThat(mViewModel.createGeneratingChallengeExtras()).isNull();

        // Check onSaveInstanceState()
        final Bundle actualBundle = new Bundle();
        mViewModel.onSaveInstanceState(actualBundle);
        assertThat(actualBundle.getBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL))
                .isFalse();
    }

    @Test
    public void testCheckCredential_needToConfirmLockForNumeric() {
        final int userId = 102;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newOnlySensorValidCredentialIntentExtras(userId)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_NUMERIC);

        // Run credential check
        @CredentialAction final int action = mViewModel.checkCredential();

        // Check viewModel behavior
        assertThat(action).isEqualTo(CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK);
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();

        // Check createGeneratingChallengeExtras()
        assertThat(mViewModel.createGeneratingChallengeExtras()).isNull();

        // Check onSaveInstanceState()
        final Bundle actualBundle = new Bundle();
        mViewModel.onSaveInstanceState(actualBundle);
        assertThat(actualBundle.getBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL))
                .isFalse();
    }

    @Test
    public void testCheckCredential_needToConfirmLockForAlphabetic() {
        final int userId = 103;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newOnlySensorValidCredentialIntentExtras(userId)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_ALPHABETIC);

        // Run credential check
        @CredentialAction final int action = mViewModel.checkCredential();

        // Check viewModel behavior
        assertThat(action).isEqualTo(CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK);
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();

        // Check createGeneratingChallengeExtras()
        assertThat(mViewModel.createGeneratingChallengeExtras()).isNull();

        // Check onSaveInstanceState()
        final Bundle actualBundle = new Bundle();
        mViewModel.onSaveInstanceState(actualBundle);
        assertThat(actualBundle.getBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL))
                .isFalse();
    }

    @Test
    public void testCheckCredential_generateChallenge() {
        final int userId = 104;
        final long gkPwHandle = 1111L;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        final int newSensorId = 10;
        final long newChallenge = 20L;
        setupGenerateChallenge(userId, newSensorId, newChallenge);
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, newChallenge, userId))
                .thenReturn(newGoodCredential(gkPwHandle, new byte[] { 1 }));

        // Run credential check
        @CredentialAction final int action = mViewModel.checkCredential();

        // Check viewModel behavior
        assertThat(action).isEqualTo(CREDENTIAL_IS_GENERATING_CHALLENGE);
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();
        assertThat(mChallengeGenerator.mCallbackRunCount).isEqualTo(1);

        // Check data inside CredentialModel
        final Bundle extras = mViewModel.createCredentialIntentExtra();
        assertThat(extras.getInt(EXTRA_KEY_SENSOR_ID)).isEqualTo(newSensorId);
        assertThat(extras.getLong(EXTRA_KEY_CHALLENGE)).isEqualTo(newChallenge);
        assertThat(extras.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN)).isNotNull();
        assertThat(extras.getLong(EXTRA_KEY_GK_PW_HANDLE)).isEqualTo(INVALID_GK_PW_HANDLE);
        assertThat(extras.getLong(EXTRA_KEY_CHALLENGE)).isNotEqualTo(INVALID_CHALLENGE);

        // Check createGeneratingChallengeExtras()
        final Bundle generatingChallengeExtras = mViewModel.createGeneratingChallengeExtras();
        assertThat(generatingChallengeExtras).isNotNull();
        assertThat(generatingChallengeExtras.getLong(EXTRA_KEY_CHALLENGE)).isEqualTo(newChallenge);
        final byte[] tokens = generatingChallengeExtras.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN);
        assertThat(tokens).isNotNull();
        assertThat(tokens.length).isEqualTo(1);
        assertThat(tokens[0]).isEqualTo(1);

        // Check onSaveInstanceState()
        final Bundle actualBundle = new Bundle();
        mViewModel.onSaveInstanceState(actualBundle);
        assertThat(actualBundle.getBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL))
                .isTrue();
    }

    @Test
    public void testCheckCredential_generateChallengeFail() {
        final int userId = 104;
        final long gkPwHandle = 1111L;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        final int newSensorId = 10;
        final long newChallenge = 20L;
        setupGenerateChallenge(userId, newSensorId, newChallenge);
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, newChallenge, userId))
                .thenReturn(newBadCredential(0));

        // Run credential check
        @CredentialAction final int action = mViewModel.checkCredential();

        assertThat(action).isEqualTo(CREDENTIAL_IS_GENERATING_CHALLENGE);
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isTrue();
        assertThat(mChallengeGenerator.mCallbackRunCount).isEqualTo(1);

        // Check createGeneratingChallengeExtras()
        assertThat(mViewModel.createGeneratingChallengeExtras()).isNull();

        // Check onSaveInstanceState()
        final Bundle actualBundle = new Bundle();
        mViewModel.onSaveInstanceState(actualBundle);
        assertThat(actualBundle.getBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL))
                .isTrue();
    }

    @Test
    public void testGetUserId_fromIntent() {
        final int userId = 106;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newOnlySensorValidCredentialIntentExtras(userId)));

        // Get userId
        assertThat(mViewModel.getUserId()).isEqualTo(userId);
    }

    @Test
    public void testGetUserId_fromSavedInstance() {
        final int userId = 106;
        final Bundle savedInstance = new Bundle();
        savedInstance.putBundle(KEY_CREDENTIAL_MODEL,
                newOnlySensorValidCredentialIntentExtras(userId));
        mViewModel.setCredentialModel(savedInstance, new Intent());

        // Get userId
        assertThat(mViewModel.getUserId()).isEqualTo(userId);
    }

    @Test
    public void testCreateGeneratingChallengeExtras_generateChallenge() {
        final Bundle credentialExtras = newValidTokenCredentialIntentExtras(200);
        final Bundle savedInstance = new Bundle();
        savedInstance.putBundle(KEY_CREDENTIAL_MODEL, credentialExtras);
        savedInstance.putBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL, true);
        mViewModel.setCredentialModel(savedInstance, new Intent());

        // Check createGeneratingChallengeExtras()
        final Bundle actualExtras = mViewModel.createGeneratingChallengeExtras();
        assertThat(actualExtras).isNotNull();
        assertThat(actualExtras.getLong(EXTRA_KEY_CHALLENGE))
                .isEqualTo(credentialExtras.getLong(EXTRA_KEY_CHALLENGE));
        final byte[] actualToken = actualExtras.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN);
        final byte[] expectedToken = credentialExtras.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN);
        assertThat(actualToken).isNotNull();
        assertThat(expectedToken).isNotNull();
        assertThat(actualToken.length).isEqualTo(expectedToken.length);
        for (int i = 0; i < actualToken.length; ++i) {
            assertWithMessage("tokens[" + i + "] not match").that(actualToken[i])
                    .isEqualTo(expectedToken[i]);
        }
    }

    @Test
    public void testCreateGeneratingChallengeExtras_notGenerateChallenge() {
        final Bundle credentialExtras = newValidTokenCredentialIntentExtras(201);
        final Bundle savedInstance = new Bundle();
        savedInstance.putBundle(KEY_CREDENTIAL_MODEL, credentialExtras);
        savedInstance.putBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL, false);
        mViewModel.setCredentialModel(savedInstance, new Intent());

        // Check createGeneratingChallengeExtras()
        assertThat(mViewModel.createGeneratingChallengeExtras()).isNull();
    }

    @Test
    public void testCreateGeneratingChallengeExtras_invalidToken() {
        final Bundle credentialExtras = newOnlySensorValidCredentialIntentExtras(202);
        final Bundle savedInstance = new Bundle();
        savedInstance.putBundle(KEY_CREDENTIAL_MODEL, credentialExtras);
        savedInstance.putBoolean(KEY_IS_GENERATING_CHALLENGE_DURING_CHECKING_CREDENTIAL, true);
        mViewModel.setCredentialModel(savedInstance, new Intent());

        // Check createGeneratingChallengeExtras()
        assertThat(mViewModel.createGeneratingChallengeExtras()).isNull();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_invalidChooseLock() {
        final int userId = 107;
        final long gkPwHandle = 3333L;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle)));
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);

        // run checkNewCredentialFromActivityResult()
        final boolean ret = mViewModel.checkNewCredentialFromActivityResult(true,
                new ActivityResult(ChooseLockPattern.RESULT_FINISHED + 1, intent));

        assertThat(ret).isFalse();
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_invalidConfirmLock() {
        final int userId = 107;
        final long gkPwHandle = 3333L;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle)));
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);

        // run checkNewCredentialFromActivityResult()
        final boolean ret = mViewModel.checkNewCredentialFromActivityResult(false,
                new ActivityResult(Activity.RESULT_OK + 1, intent));

        assertThat(ret).isFalse();
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_nullDataChooseLock() {
        final int userId = 108;
        final long gkPwHandle = 4444L;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newGkPwHandleCredentialIntentExtras(userId, gkPwHandle)));

        // run checkNewCredentialFromActivityResult()
        final boolean ret = mViewModel.checkNewCredentialFromActivityResult(true,
                new ActivityResult(ChooseLockPattern.RESULT_FINISHED, null));

        assertThat(ret).isFalse();
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_nullDataConfirmLock() {
        final int userId = 109;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newOnlySensorValidCredentialIntentExtras(userId)));

        // run checkNewCredentialFromActivityResult()
        final boolean ret = mViewModel.checkNewCredentialFromActivityResult(false,
                new ActivityResult(Activity.RESULT_OK, null));

        assertThat(ret).isFalse();
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_validChooseLock() {
        final int userId = 108;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newOnlySensorValidCredentialIntentExtras(userId)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        final long gkPwHandle = 6666L;
        final int newSensorId = 50;
        final long newChallenge = 60L;
        setupGenerateChallenge(userId, newSensorId, newChallenge);
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, newChallenge, userId))
                .thenReturn(newGoodCredential(gkPwHandle, new byte[] { 1 }));

        // Run checkNewCredentialFromActivityResult()
        final Intent intent = new Intent().putExtra(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);
        final boolean ret = mViewModel.checkNewCredentialFromActivityResult(true,
                new ActivityResult(ChooseLockPattern.RESULT_FINISHED, intent));

        assertThat(ret).isTrue();
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();
        final Bundle extras = mViewModel.createCredentialIntentExtra();
        assertThat(extras.getInt(EXTRA_KEY_SENSOR_ID)).isEqualTo(newSensorId);
        assertThat(extras.getLong(EXTRA_KEY_CHALLENGE)).isEqualTo(newChallenge);
        assertThat(extras.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN)).isNotNull();
        assertThat(extras.getLong(EXTRA_KEY_GK_PW_HANDLE)).isEqualTo(INVALID_GK_PW_HANDLE);
        assertThat(mChallengeGenerator.mCallbackRunCount).isEqualTo(1);
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_validConfirmLock() {
        final int userId = 109;
        mViewModel.setCredentialModel(null,
                new Intent().putExtras(newOnlySensorValidCredentialIntentExtras(userId)));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        final long gkPwHandle = 5555L;
        final int newSensorId = 80;
        final long newChallenge = 90L;
        setupGenerateChallenge(userId, newSensorId, newChallenge);
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, newChallenge, userId))
                .thenReturn(newGoodCredential(gkPwHandle, new byte[] { 1 }));

        // Run checkNewCredentialFromActivityResult()
        final Intent intent = new Intent().putExtra(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);
        final boolean ret = mViewModel.checkNewCredentialFromActivityResult(false,
                new ActivityResult(Activity.RESULT_OK, intent));

        assertThat(ret).isTrue();
        assertThat(mViewModel.getGenerateChallengeFailedLiveData().getValue()).isNull();
        final Bundle extras = mViewModel.createCredentialIntentExtra();
        assertThat(extras.getInt(EXTRA_KEY_SENSOR_ID)).isEqualTo(newSensorId);
        assertThat(extras.getLong(EXTRA_KEY_CHALLENGE)).isEqualTo(newChallenge);
        assertThat(extras.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN)).isNotNull();
        assertThat(extras.getLong(EXTRA_KEY_GK_PW_HANDLE)).isEqualTo(INVALID_GK_PW_HANDLE);
        assertThat(mChallengeGenerator.mCallbackRunCount).isEqualTo(1);
    }

    public static class TestChallengeGenerator implements ChallengeGenerator {
        public int mSensorId = INVALID_SENSOR_ID;
        public int mUserId = UserHandle.myUserId();
        public long mChallenge = INVALID_CHALLENGE;
        public int mCallbackRunCount = 0;
        private GenerateChallengeCallback mCallback;

        @Nullable
        @Override
        public GenerateChallengeCallback getCallback() {
            return mCallback;
        }

        @Override
        public void setCallback(@Nullable GenerateChallengeCallback callback) {
            mCallback = callback;
        }

        @Override
        public void generateChallenge(int userId) {
            final GenerateChallengeCallback callback = mCallback;
            if (callback == null) {
                return;
            }
            callback.onChallengeGenerated(mSensorId, mUserId, mChallenge);
            ++mCallbackRunCount;
        }
    }

    private VerifyCredentialResponse newGoodCredential(long gkPwHandle, @NonNull byte[] hat) {
        return new VerifyCredentialResponse.Builder()
                .setGatekeeperPasswordHandle(gkPwHandle)
                .setGatekeeperHAT(hat)
                .build();
    }

    private VerifyCredentialResponse newBadCredential(int timeout) {
        if (timeout > 0) {
            return VerifyCredentialResponse.fromTimeout(timeout);
        } else {
            return VerifyCredentialResponse.fromError();
        }
    }
}
