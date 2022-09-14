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
import static com.android.settings.biometrics2.ui.model.CredentialModel.INVALID_SENSOR_ID;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.ChallengeGenerator;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.CredentialAction;
import static com.android.settings.biometrics2.ui.viewmodel.AutoCredentialViewModel.GenerateChallengeCallback;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.annotation.NonNull;
import android.app.Activity;
import android.content.Intent;
import android.os.SystemClock;
import android.os.UserHandle;

import androidx.activity.result.ActivityResult;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.biometrics2.ui.model.CredentialModel;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.password.ChooseLockSettingsHelper;
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

    @Mock private LifecycleOwner mLifecycleOwner;
    @Mock private LockPatternUtils mLockPatternUtils;
    private TestChallengeGenerator mChallengeGenerator = null;
    private AutoCredentialViewModel mAutoCredentialViewModel;

    @Before
    public void setUp() {
        mChallengeGenerator = new TestChallengeGenerator();
        mAutoCredentialViewModel = new AutoCredentialViewModel(
                ApplicationProvider.getApplicationContext(),
                mLockPatternUtils,
                mChallengeGenerator);
    }

    private CredentialModel newCredentialModel(int userId, long challenge,
            @Nullable byte[] token, long gkPwHandle) {
        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_USER_ID, userId);
        intent.putExtra(EXTRA_KEY_SENSOR_ID, 1);
        intent.putExtra(EXTRA_KEY_CHALLENGE, challenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);
        return new CredentialModel(intent, SystemClock.elapsedRealtimeClock());
    }

    private CredentialModel newValidTokenCredentialModel(int userId) {
        return newCredentialModel(userId, 1L, new byte[] { 0 }, 0L);
    }

    private CredentialModel newInvalidChallengeCredentialModel(int userId) {
        return newCredentialModel(userId, INVALID_CHALLENGE, null, 0L);
    }

    private CredentialModel newGkPwHandleCredentialModel(int userId, long gkPwHandle) {
        return newCredentialModel(userId, INVALID_CHALLENGE, null, gkPwHandle);
    }

    private void verifyNothingHappen() {
        assertThat(mAutoCredentialViewModel.getActionLiveData().getValue()).isNull();
    }

    private void verifyOnlyActionLiveData(@CredentialAction int action) {
        final Integer value = mAutoCredentialViewModel.getActionLiveData().getValue();
        assertThat(value).isEqualTo(action);
    }

    private void setupGenerateTokenFlow(long gkPwHandle, int userId, int newSensorId,
            long newChallenge) {
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);
        mChallengeGenerator.mUserId = userId;
        mChallengeGenerator.mSensorId = newSensorId;
        mChallengeGenerator.mChallenge = newChallenge;
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, newChallenge, userId))
                .thenReturn(newGoodCredential(gkPwHandle, new byte[] { 1 }));
    }

    @Test
    public void checkCredential_validCredentialCase() {
        final int userId = 99;
        mAutoCredentialViewModel.setCredentialModel(newValidTokenCredentialModel(userId));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        // Run credential check
        mAutoCredentialViewModel.onCreate(mLifecycleOwner);

        verifyNothingHappen();
    }

    @Test
    public void checkCredential_needToChooseLock() {
        final int userId = 100;
        mAutoCredentialViewModel.setCredentialModel(newInvalidChallengeCredentialModel(userId));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_UNSPECIFIED);

        // Run credential check
        mAutoCredentialViewModel.onCreate(mLifecycleOwner);

        verifyOnlyActionLiveData(CREDENTIAL_FAIL_NEED_TO_CHOOSE_LOCK);
    }

    @Test
    public void checkCredential_needToConfirmLockFoSomething() {
        final int userId = 101;
        mAutoCredentialViewModel.setCredentialModel(newInvalidChallengeCredentialModel(userId));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        // Run credential check
        mAutoCredentialViewModel.onCreate(mLifecycleOwner);

        verifyOnlyActionLiveData(CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK);
    }

    @Test
    public void checkCredential_needToConfirmLockForNumeric() {
        final int userId = 102;
        mAutoCredentialViewModel.setCredentialModel(newInvalidChallengeCredentialModel(userId));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_NUMERIC);

        // Run credential check
        mAutoCredentialViewModel.onCreate(mLifecycleOwner);

        verifyOnlyActionLiveData(CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK);
    }

    @Test
    public void checkCredential_needToConfirmLockForAlphabetic() {
        final int userId = 103;
        mAutoCredentialViewModel.setCredentialModel(newInvalidChallengeCredentialModel(userId));
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_ALPHABETIC);

        // Run credential check
        mAutoCredentialViewModel.onCreate(mLifecycleOwner);

        verifyOnlyActionLiveData(CREDENTIAL_FAIL_NEED_TO_CONFIRM_LOCK);
    }

    @Test
    public void checkCredential_generateChallenge() {
        final int userId = 104;
        final long gkPwHandle = 1111L;
        final CredentialModel credentialModel = newGkPwHandleCredentialModel(userId, gkPwHandle);
        mAutoCredentialViewModel.setCredentialModel(credentialModel);
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        final int newSensorId = 10;
        final long newChallenge = 20L;
        setupGenerateTokenFlow(gkPwHandle, userId, newSensorId, newChallenge);

        // Run credential check
        mAutoCredentialViewModel.onCreate(mLifecycleOwner);

        assertThat(mAutoCredentialViewModel.getActionLiveData().getValue()).isNull();
        assertThat(credentialModel.getSensorId()).isEqualTo(newSensorId);
        assertThat(credentialModel.getChallenge()).isEqualTo(newChallenge);
        assertThat(CredentialModel.isValidToken(credentialModel.getToken())).isTrue();
        assertThat(CredentialModel.isValidGkPwHandle(credentialModel.getGkPwHandle())).isFalse();
        assertThat(mChallengeGenerator.mCallbackRunCount).isEqualTo(1);
    }

    @Test
    public void testGetUserId() {
        final int userId = 106;
        mAutoCredentialViewModel.setCredentialModel(newInvalidChallengeCredentialModel(userId));

        // Get userId
        assertThat(mAutoCredentialViewModel.getUserId()).isEqualTo(userId);
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_invalidChooseLock() {
        final int userId = 107;
        final long gkPwHandle = 3333L;
        mAutoCredentialViewModel.setCredentialModel(
                newGkPwHandleCredentialModel(userId, gkPwHandle));
        final Intent intent = new Intent();
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);

        // run checkNewCredentialFromActivityResult()
        final boolean ret = mAutoCredentialViewModel.checkNewCredentialFromActivityResult(true,
                new ActivityResult(ChooseLockPattern.RESULT_FINISHED + 1, intent));

        assertThat(ret).isFalse();
        verifyNothingHappen();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_invalidConfirmLock() {
        final int userId = 107;
        final long gkPwHandle = 3333L;
        mAutoCredentialViewModel.setCredentialModel(
                newGkPwHandleCredentialModel(userId, gkPwHandle));
        final Intent intent = new Intent();
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);

        // run checkNewCredentialFromActivityResult()
        final boolean ret = mAutoCredentialViewModel.checkNewCredentialFromActivityResult(false,
                new ActivityResult(Activity.RESULT_OK + 1, intent));

        assertThat(ret).isFalse();
        verifyNothingHappen();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_nullDataChooseLock() {
        final int userId = 108;
        final long gkPwHandle = 4444L;
        mAutoCredentialViewModel.setCredentialModel(
                newGkPwHandleCredentialModel(userId, gkPwHandle));

        // run checkNewCredentialFromActivityResult()
        final boolean ret = mAutoCredentialViewModel.checkNewCredentialFromActivityResult(true,
                new ActivityResult(ChooseLockPattern.RESULT_FINISHED, null));

        assertThat(ret).isFalse();
        verifyNothingHappen();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_nullDataConfirmLock() {
        final int userId = 109;
        mAutoCredentialViewModel.setCredentialModel(newInvalidChallengeCredentialModel(userId));

        // run checkNewCredentialFromActivityResult()
        final boolean ret = mAutoCredentialViewModel.checkNewCredentialFromActivityResult(false,
                new ActivityResult(Activity.RESULT_OK, null));

        assertThat(ret).isFalse();
        verifyNothingHappen();
    }

    @Test
    public void testCheckNewCredentialFromActivityResult_validChooseLock() {
        final int userId = 108;
        final CredentialModel credentialModel = newInvalidChallengeCredentialModel(userId);
        mAutoCredentialViewModel.setCredentialModel(credentialModel);
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        final long gkPwHandle = 6666L;
        final int newSensorId = 50;
        final long newChallenge = 60L;
        setupGenerateTokenFlow(gkPwHandle, userId, newSensorId, newChallenge);
        final Intent intent = new Intent();
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);

        // Run checkNewCredentialFromActivityResult()
        final boolean ret = mAutoCredentialViewModel.checkNewCredentialFromActivityResult(true,
                new ActivityResult(ChooseLockPattern.RESULT_FINISHED, intent));

        assertThat(ret).isTrue();
        assertThat(mAutoCredentialViewModel.getActionLiveData().getValue()).isNull();
        assertThat(credentialModel.getSensorId()).isEqualTo(newSensorId);
        assertThat(credentialModel.getChallenge()).isEqualTo(newChallenge);
        assertThat(CredentialModel.isValidToken(credentialModel.getToken())).isTrue();
        assertThat(CredentialModel.isValidGkPwHandle(credentialModel.getGkPwHandle())).isFalse();
        assertThat(mChallengeGenerator.mCallbackRunCount).isEqualTo(1);
    }


    @Test
    public void testCheckNewCredentialFromActivityResult_validConfirmLock() {
        final int userId = 109;
        final CredentialModel credentialModel = newInvalidChallengeCredentialModel(userId);
        mAutoCredentialViewModel.setCredentialModel(credentialModel);
        when(mLockPatternUtils.getActivePasswordQuality(userId)).thenReturn(
                PASSWORD_QUALITY_SOMETHING);

        final long gkPwHandle = 5555L;
        final int newSensorId = 80;
        final long newChallenge = 90L;
        setupGenerateTokenFlow(gkPwHandle, userId, newSensorId, newChallenge);
        final Intent intent = new Intent();
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);

        // Run checkNewCredentialFromActivityResult()
        final boolean ret = mAutoCredentialViewModel.checkNewCredentialFromActivityResult(false,
                new ActivityResult(Activity.RESULT_OK, intent));

        assertThat(ret).isTrue();
        assertThat(mAutoCredentialViewModel.getActionLiveData().getValue()).isNull();
        assertThat(credentialModel.getSensorId()).isEqualTo(newSensorId);
        assertThat(credentialModel.getChallenge()).isEqualTo(newChallenge);
        assertThat(CredentialModel.isValidToken(credentialModel.getToken())).isTrue();
        assertThat(CredentialModel.isValidGkPwHandle(credentialModel.getGkPwHandle())).isFalse();
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
}
