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

import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_SKIP;
import static com.android.settings.biometrics.BiometricEnrollBase.RESULT_TIMEOUT;
import static com.android.settings.biometrics.fingerprint.SetupFingerprintEnrollIntroduction.EXTRA_FINGERPRINT_ENROLLED_COUNT;
import static com.android.settings.biometrics2.ui.viewmodel.FingerprintEnrollmentViewModel.SAVED_STATE_IS_WAITING_ACTIVITY_RESULT;
import static com.android.settings.biometrics2.util.EnrollmentRequestUtil.newAllFalseRequest;
import static com.android.settings.biometrics2.util.EnrollmentRequestUtil.newIsSuwRequest;
import static com.android.settings.biometrics2.util.FingerprintManagerUtil.setupFingerprintEnrolledFingerprints;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.Application;
import android.app.KeyguardManager;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.biometrics2.data.repository.FingerprintRepository;
import com.android.settings.biometrics2.ui.model.EnrollmentRequest;
import com.android.settings.password.SetupSkipDialog;
import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollmentViewModelTest {

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private FingerprintManager mFingerprintManager;
    @Mock private KeyguardManager mKeyguardManager;

    private Application mApplication;
    private FingerprintRepository mFingerprintRepository;
    private FingerprintEnrollmentViewModel mViewModel;

    @Before
    public void setUp() {
        mApplication = ApplicationProvider.getApplicationContext();
        mFingerprintRepository = new FingerprintRepository(mFingerprintManager);
        mViewModel = new FingerprintEnrollmentViewModel(mApplication, mFingerprintRepository,
                mKeyguardManager);
    }

    @Test
    public void testGetRequest() {
        when(mKeyguardManager.isKeyguardSecure()).thenReturn(true);
        assertThat(mViewModel.getRequest()).isNull();

        final EnrollmentRequest request = newAllFalseRequest(mApplication);
        mViewModel.setRequest(request);
        assertThat(mViewModel.getRequest()).isEqualTo(request);
    }

    @Test
    public void testGetNextActivityBaseIntentExtras() {
        mViewModel.setRequest(newAllFalseRequest(mApplication));
        assertThat(mViewModel.getNextActivityBaseIntentExtras()).isNotNull();
    }

    @Test
    public void testOnContinueEnrollActivityResult_shouldRelaySkip1Result() {
        mViewModel.setRequest(newAllFalseRequest(mApplication));
        final ActivityResult result = new ActivityResult(RESULT_SKIP, null);

        // Run onContinueEnrollActivityResult
        mViewModel.onContinueEnrollActivityResult(result, 100);

        assertThat(mViewModel.getSetResultLiveData().getValue()).isEqualTo(result);
    }

    @Test
    public void testOnContinueEnrollActivityResult_shouldRelaySkip2Result() {
        mViewModel.setRequest(newAllFalseRequest(mApplication));
        final ActivityResult result = new ActivityResult(SetupSkipDialog.RESULT_SKIP, null);

        // Run onContinueEnrollActivityResult
        mViewModel.onContinueEnrollActivityResult(result, 100);

        assertThat(mViewModel.getSetResultLiveData().getValue()).isEqualTo(result);
    }

    @Test
    public void testOnContinueEnrollActivityResult_shouldRelayNullDataTimeoutResult() {
        mViewModel.setRequest(newAllFalseRequest(mApplication));
        final ActivityResult result = new ActivityResult(RESULT_TIMEOUT, null);

        // Run onContinueEnrollActivityResult
        mViewModel.onContinueEnrollActivityResult(result, 100);
        final ActivityResult setResult = mViewModel.getSetResultLiveData().getValue();

        assertThat(setResult).isNotNull();
        assertThat(setResult.getResultCode()).isEqualTo(result.getResultCode());
        assertThat(setResult.getData()).isEqualTo(result.getData());
    }

    @Test
    public void testOnContinueEnrollActivityResult_shouldRelayWithDataTimeoutResult() {
        mViewModel.setRequest(newAllFalseRequest(mApplication));
        final Intent intent = new Intent("testAction");
        intent.putExtra("testKey", "testValue");
        final ActivityResult result = new ActivityResult(RESULT_TIMEOUT, intent);

        // Run onContinueEnrollActivityResult
        mViewModel.onContinueEnrollActivityResult(result, 100);
        final ActivityResult setResult = mViewModel.getSetResultLiveData().getValue();

        assertThat(setResult).isNotNull();
        assertThat(setResult.getResultCode()).isEqualTo(result.getResultCode());
        assertThat(setResult.getData()).isEqualTo(intent);
    }

    @Test
    public void testOnContinueEnrollActivityResult_shouldRelayNullDataFinishResult() {
        mViewModel.setRequest(newAllFalseRequest(mApplication));
        final ActivityResult result = new ActivityResult(RESULT_FINISHED, null);

        // Run onContinueEnrollActivityResult
        mViewModel.onContinueEnrollActivityResult(result, 100);
        final ActivityResult setResult = mViewModel.getSetResultLiveData().getValue();

        assertThat(setResult).isNotNull();
        assertThat(setResult.getResultCode()).isEqualTo(result.getResultCode());
        assertThat(setResult.getData()).isEqualTo(result.getData());
    }

    @Test
    public void testOnContinueEnrollActivityResult_shouldRelayWithDataFinishResult() {
        mViewModel.setRequest(newAllFalseRequest(mApplication));
        final Intent intent = new Intent("testAction");
        intent.putExtra("testKey", "testValue");
        final ActivityResult result = new ActivityResult(RESULT_FINISHED, intent);

        // Run onContinueEnrollActivityResult
        mViewModel.onContinueEnrollActivityResult(result, 100);
        final ActivityResult setResult = mViewModel.getSetResultLiveData().getValue();

        assertThat(setResult).isNotNull();
        assertThat(setResult.getResultCode()).isEqualTo(result.getResultCode());
        assertThat(setResult.getData()).isEqualTo(intent);
    }

    @Test
    public void testOnContinueEnrollActivityResult_shouldRelayNullDataFinishResultAsNewData() {
        when(mKeyguardManager.isKeyguardSecure()).thenReturn(true);
        final int userId = 111;
        final int numOfFp = 4;
        setupFingerprintEnrolledFingerprints(mFingerprintManager, userId, numOfFp);
        mViewModel.setRequest(newIsSuwRequest(mApplication));
        final ActivityResult result = new ActivityResult(RESULT_FINISHED, null);

        // Run onContinueEnrollActivityResult
        mViewModel.onContinueEnrollActivityResult(result, userId);
        final ActivityResult setResult = mViewModel.getSetResultLiveData().getValue();

        assertThat(setResult).isNotNull();
        assertThat(setResult.getResultCode()).isEqualTo(result.getResultCode());
        assertThat(setResult.getData()).isNotNull();
        assertThat(setResult.getData().getExtras()).isNotNull();
        assertThat(setResult.getData().getExtras().getInt(EXTRA_FINGERPRINT_ENROLLED_COUNT, -1))
                .isEqualTo(numOfFp);
    }

    @Test
    public void testOnContinueEnrollActivityResult_shouldRelayWithDataFinishResultAsNewData() {
        when(mKeyguardManager.isKeyguardSecure()).thenReturn(true);
        final int userId = 20;
        final int numOfFp = 9;
        setupFingerprintEnrolledFingerprints(mFingerprintManager, userId, numOfFp);
        mViewModel.setRequest(newIsSuwRequest(mApplication));
        final String action = "testAction";
        final String key = "testKey";
        final String value = "testValue";
        final Intent intent = new Intent(action);
        intent.putExtra(key, value);
        final ActivityResult result = new ActivityResult(RESULT_FINISHED, intent);

        // Run onContinueEnrollActivityResult
        mViewModel.onContinueEnrollActivityResult(result, userId);
        final ActivityResult setResult = mViewModel.getSetResultLiveData().getValue();

        assertThat(setResult).isNotNull();
        assertThat(setResult.getResultCode()).isEqualTo(result.getResultCode());
        assertThat(setResult.getData()).isNotNull();
        assertThat(setResult.getData().getExtras()).isNotNull();
        assertThat(setResult.getData().getExtras().getInt(EXTRA_FINGERPRINT_ENROLLED_COUNT, -1))
                .isEqualTo(numOfFp);
        assertThat(setResult.getData().getExtras().getString(key)).isEqualTo(value);
    }

    @Test
    public void testSetSavedInstanceState() {
        final Bundle bundle = new Bundle();
        mViewModel.isWaitingActivityResult().set(true);

        // setSavedInstanceState() as false
        bundle.putBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT, false);
        mViewModel.setSavedInstanceState(bundle);
        assertThat(mViewModel.isWaitingActivityResult().get()).isFalse();

        // setSavedInstanceState() as false
        bundle.putBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT, true);
        mViewModel.setSavedInstanceState(bundle);
        assertThat(mViewModel.isWaitingActivityResult().get()).isTrue();
    }

    @Test
    public void testOnSaveInstanceState() {
        final Bundle bundle = new Bundle();

        // setSavedInstanceState() as false
        mViewModel.isWaitingActivityResult().set(false);
        mViewModel.onSaveInstanceState(bundle);
        assertThat(bundle.getBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT)).isFalse();

        // setSavedInstanceState() as false
        mViewModel.isWaitingActivityResult().set(true);
        mViewModel.onSaveInstanceState(bundle);
        assertThat(bundle.getBoolean(SAVED_STATE_IS_WAITING_ACTIVITY_RESULT)).isTrue();
    }
}
