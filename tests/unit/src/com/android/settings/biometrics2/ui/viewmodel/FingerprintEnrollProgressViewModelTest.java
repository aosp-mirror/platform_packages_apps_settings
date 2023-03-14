/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static android.hardware.fingerprint.FingerprintManager.ENROLL_ENROLL;
import static android.hardware.fingerprint.FingerprintManager.ENROLL_FIND_SENSOR;
import static android.hardware.fingerprint.FingerprintManager.EnrollReason;
import static android.hardware.fingerprint.FingerprintManager.EnrollmentCallback;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.res.Resources;
import android.os.CancellationSignal;

import androidx.lifecycle.LiveData;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.biometrics.fingerprint.FingerprintUpdater;
import com.android.settings.biometrics2.ui.model.EnrollmentProgress;
import com.android.settings.biometrics2.ui.model.EnrollmentStatusMessage;
import com.android.settings.testutils.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class FingerprintEnrollProgressViewModelTest {

    private static final int TEST_USER_ID = 334;

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();
    @Rule public final InstantTaskExecutorRule mTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock private Application mApplication;
    @Mock private Resources mResources;
    @Mock private FingerprintUpdater mFingerprintUpdater;

    private FingerprintEnrollProgressViewModel mViewModel;
    private final TestWrapper<CancellationSignal> mCancellationSignalWrapper = new TestWrapper<>();
    private final TestWrapper<EnrollmentCallback> mCallbackWrapper = new TestWrapper<>();

    @Before
    public void setUp() {
        when(mApplication.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(R.bool.enrollment_message_display_controller_flag))
                .thenReturn(false);
        mViewModel = new FingerprintEnrollProgressViewModel(mApplication, mFingerprintUpdater,
                TEST_USER_ID);

        mCancellationSignalWrapper.mValue = null;
        mCallbackWrapper.mValue = null;
        doAnswer(invocation -> {
            mCancellationSignalWrapper.mValue = invocation.getArgument(1);
            mCallbackWrapper.mValue = invocation.getArgument(3);
            return null;
        }).when(mFingerprintUpdater).enroll(any(byte[].class), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(EnrollmentCallback.class), anyInt());
    }

    @Test
    public void testStartEnrollment() {
        @EnrollReason final int enrollReason = ENROLL_FIND_SENSOR;
        final byte[] token = new byte[] { 1, 2, 3 };
        mViewModel.setToken(token);

        // Start enrollment
        final boolean ret = mViewModel.startEnrollment(enrollReason);

        assertThat(ret).isTrue();
        verify(mFingerprintUpdater, only()).enroll(eq(token), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(EnrollmentCallback.class), eq(enrollReason));
    }

    @Test
    public void testStartEnrollmentFailBecauseOfNoToken() {
        // Start enrollment
        final boolean ret = mViewModel.startEnrollment(ENROLL_FIND_SENSOR);

        assertThat(ret).isFalse();
        verify(mFingerprintUpdater, never()).enroll(any(byte[].class),
                any(CancellationSignal.class), anyInt(), any(EnrollmentCallback.class), anyInt());
    }

    @Test
    public void testCancelEnrollment() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final boolean ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isTrue();
        assertThat(mCancellationSignalWrapper.mValue).isNotNull();

        // Cancel enrollment
        mViewModel.cancelEnrollment();

        assertThat(mCancellationSignalWrapper.mValue.isCanceled()).isTrue();
    }

    @Test
    public void testProgressUpdate() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final boolean ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isTrue();
        assertThat(mCallbackWrapper.mValue).isNotNull();

        // Test default value
        final LiveData<EnrollmentProgress> progressLiveData = mViewModel.getProgressLiveData();
        EnrollmentProgress progress = progressLiveData.getValue();
        assertThat(progress).isNotNull();
        assertThat(progress.getSteps()).isEqualTo(-1);
        assertThat(progress.getRemaining()).isEqualTo(0);

        // Update first progress
        mCallbackWrapper.mValue.onEnrollmentProgress(25);
        progress = progressLiveData.getValue();
        assertThat(progress).isNotNull();
        assertThat(progress.getSteps()).isEqualTo(25);
        assertThat(progress.getRemaining()).isEqualTo(25);

        // Update second progress
        mCallbackWrapper.mValue.onEnrollmentProgress(20);
        progress = progressLiveData.getValue();
        assertThat(progress).isNotNull();
        assertThat(progress.getSteps()).isEqualTo(25);
        assertThat(progress.getRemaining()).isEqualTo(20);

        // Update latest progress
        mCallbackWrapper.mValue.onEnrollmentProgress(0);
        progress = progressLiveData.getValue();
        assertThat(progress).isNotNull();
        assertThat(progress.getSteps()).isEqualTo(25);
        assertThat(progress.getRemaining()).isEqualTo(0);
    }

    @Test
    public void testGetErrorMessageLiveData() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final boolean ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isTrue();
        assertThat(mCallbackWrapper.mValue).isNotNull();

        // Check default value
        final LiveData<EnrollmentStatusMessage> liveData = mViewModel.getErrorMessageLiveData();
        assertThat(liveData.getValue()).isNull();

        // Notify error message
        final int errMsgId = 3;
        final String errMsg = "test error message";
        mCallbackWrapper.mValue.onEnrollmentError(errMsgId, errMsg);
        final EnrollmentStatusMessage value = liveData.getValue();
        assertThat(value).isNotNull();
        assertThat(value.getMsgId()).isEqualTo(errMsgId);
        assertThat(value.getStr().toString()).isEqualTo(errMsg);
    }

    @Test
    public void testGetHelpMessageLiveData() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final boolean ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isTrue();
        assertThat(mCallbackWrapper.mValue).isNotNull();

        // Check default value
        final LiveData<EnrollmentStatusMessage> liveData = mViewModel.getHelpMessageLiveData();
        assertThat(liveData.getValue()).isNull();

        // Notify help message
        final int errMsgId = 3;
        final String errMsg = "test error message";
        mCallbackWrapper.mValue.onEnrollmentHelp(errMsgId, errMsg);
        final EnrollmentStatusMessage value = liveData.getValue();
        assertThat(value).isNotNull();
        assertThat(value.getMsgId()).isEqualTo(errMsgId);
        assertThat(value.getStr().toString()).isEqualTo(errMsg);
    }

    @Test
    public void testGetAcquireLiveData() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final boolean ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isTrue();
        assertThat(mCallbackWrapper.mValue).isNotNull();

        // Check default value
        final LiveData<Boolean> liveData = mViewModel.getAcquireLiveData();
        assertThat(liveData.getValue()).isNull();

        // Notify acquire message
        mCallbackWrapper.mValue.onAcquired(true);
        assertThat(liveData.getValue()).isTrue();
    }

    @Test
    public void testGetPointerDownLiveData() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final boolean ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isTrue();
        assertThat(mCallbackWrapper.mValue).isNotNull();

        // Check default value
        final LiveData<Integer> liveData = mViewModel.getPointerDownLiveData();
        assertThat(liveData.getValue()).isNull();

        // Notify acquire message
        final int value = 33;
        mCallbackWrapper.mValue.onPointerDown(value);
        assertThat(liveData.getValue()).isEqualTo(value);
    }

    @Test
    public void testGetPointerUpLiveData() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final boolean ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isTrue();
        assertThat(mCallbackWrapper.mValue).isNotNull();

        // Check default value
        final LiveData<Integer> liveData = mViewModel.getPointerUpLiveData();
        assertThat(liveData.getValue()).isNull();

        // Notify acquire message
        final int value = 44;
        mCallbackWrapper.mValue.onPointerUp(value);
        assertThat(liveData.getValue()).isEqualTo(value);
    }

    private static class TestWrapper<T> {
        T mValue;
    }
}
