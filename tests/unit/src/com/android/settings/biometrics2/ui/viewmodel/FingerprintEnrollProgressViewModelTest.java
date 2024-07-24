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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.res.Resources;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.biometrics.fingerprint.FingerprintUpdater;
import com.android.settings.biometrics.fingerprint.MessageDisplayController;
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
    private int mEnrollmentMessageDisplayControllerFlagResId;

    @Before
    public void setUp() {
        mEnrollmentMessageDisplayControllerFlagResId = ApplicationProvider.getApplicationContext()
                .getResources().getIdentifier("enrollment_message_display_controller_flag", "bool",
                        SETTINGS_PACKAGE_NAME);

        when(mApplication.getResources()).thenReturn(mResources);

        // Not use MessageDisplayController by default
        when(mResources.getBoolean(mEnrollmentMessageDisplayControllerFlagResId)).thenReturn(false);
        mViewModel = new FingerprintEnrollProgressViewModel(mApplication, mFingerprintUpdater,
                TEST_USER_ID);

        mCancellationSignalWrapper.mValue = null;
        mCallbackWrapper.mValue = null;
        doAnswer(invocation -> {
            mCancellationSignalWrapper.mValue = invocation.getArgument(1);
            mCallbackWrapper.mValue = invocation.getArgument(3);
            return null;
        }).when(mFingerprintUpdater).enroll(any(byte[].class), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(EnrollmentCallback.class), anyInt(), any());
    }

    @Test
    public void testStartFindSensor() {
        @EnrollReason final int enrollReason = ENROLL_FIND_SENSOR;
        final byte[] token = new byte[] { 1, 2, 3 };
        mViewModel.setToken(token);

        // Start enrollment
        final Object ret = mViewModel.startEnrollment(enrollReason);

        assertThat(ret).isNotNull();
        verify(mFingerprintUpdater, only()).enroll(eq(token), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(EnrollmentCallback.class), eq(enrollReason), any());
        assertThat(mCallbackWrapper.mValue instanceof MessageDisplayController).isFalse();
    }

    @Test
    public void testStartEnrolling() {
        @EnrollReason final int enrollReason = ENROLL_ENROLL;
        final byte[] token = new byte[] { 1, 2, 3 };
        mViewModel.setToken(token);

        // Start enrollment
        final Object ret = mViewModel.startEnrollment(enrollReason);

        assertThat(ret).isNotNull();
        verify(mFingerprintUpdater, only()).enroll(eq(token), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(EnrollmentCallback.class), eq(enrollReason), any());
        assertThat(mCallbackWrapper.mValue instanceof MessageDisplayController).isFalse();
    }

    @Test
    public void testStartEnrollingWithMessageDisplayController() {
        // Enable MessageDisplayController and mock handler for it
        when(mResources.getBoolean(mEnrollmentMessageDisplayControllerFlagResId)).thenReturn(true);
        when(mApplication.getMainThreadHandler()).thenReturn(new TestHandler());

        @EnrollReason final int enrollReason = ENROLL_ENROLL;
        final byte[] token = new byte[] { 1, 2, 3 };
        mViewModel.setToken(token);

        // Start enrollment
        final Object ret = mViewModel.startEnrollment(enrollReason);

        assertThat(ret).isNotNull();
        verify(mFingerprintUpdater, only()).enroll(eq(token), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(MessageDisplayController.class), eq(enrollReason), any());
        assertThat(mCallbackWrapper.mValue).isNotNull();

        assertThat(mCallbackWrapper.mValue instanceof MessageDisplayController).isTrue();
        final EnrollmentCallback callback1 = mCallbackWrapper.mValue;

        // Cancel and start again
        mViewModel.cancelEnrollment();
        mViewModel.startEnrollment(enrollReason);

        // Shall not use the same MessageDisplayController
        verify(mFingerprintUpdater, times(2)).enroll(eq(token), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(MessageDisplayController.class), eq(enrollReason), any());
        assertThat(mCallbackWrapper.mValue).isNotNull();
        assertThat(callback1).isNotEqualTo(mCallbackWrapper.mValue);
    }

    @Test
    public void testStartEnrollmentFailBecauseOfNoToken() {
        // Start enrollment
        final Object ret = mViewModel.startEnrollment(ENROLL_FIND_SENSOR);

        assertThat(ret).isNull();
        verify(mFingerprintUpdater, never()).enroll(any(byte[].class),
                any(CancellationSignal.class), anyInt(), any(EnrollmentCallback.class), anyInt(),
                any());
    }

    @Test
    public void testCancelEnrollment() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
        assertThat(mCancellationSignalWrapper.mValue).isNotNull();

        // Cancel enrollment
        mViewModel.cancelEnrollment();

        assertThat(mCancellationSignalWrapper.mValue.isCanceled()).isTrue();
    }

    @Test
    public void testProgressUpdate() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
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
    public void testProgressUpdateClearHelpMessage() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
        assertThat(mCallbackWrapper.mValue).isNotNull();
        final LiveData<EnrollmentProgress> progressLiveData = mViewModel.getProgressLiveData();
        final LiveData<EnrollmentStatusMessage> helpMsgLiveData =
                mViewModel.getHelpMessageLiveData();

        // Update first progress
        mCallbackWrapper.mValue.onEnrollmentProgress(25);
        EnrollmentProgress progress = progressLiveData.getValue();
        assertThat(progress).isNotNull();
        assertThat(progress.getSteps()).isEqualTo(25);
        assertThat(progress.getRemaining()).isEqualTo(25);

        // Update help message
        final int testHelpMsgId = 3;
        final String testHelpString = "Test Help String";
        mCallbackWrapper.mValue.onEnrollmentHelp(testHelpMsgId, testHelpString);
        final EnrollmentStatusMessage helpMsg = helpMsgLiveData.getValue();
        assertThat(helpMsg).isNotNull();
        assertThat(helpMsg.getMsgId()).isEqualTo(testHelpMsgId);
        assertThat(helpMsg.getStr().toString()).isEqualTo(testHelpString);

        // Update second progress
        mCallbackWrapper.mValue.onEnrollmentProgress(20);
        progress = progressLiveData.getValue();
        assertThat(progress).isNotNull();
        assertThat(progress.getSteps()).isEqualTo(25);
        assertThat(progress.getRemaining()).isEqualTo(20);

        // Help message shall be set to null
        assertThat(helpMsgLiveData.getValue()).isNull();
    }

    @Test
    public void testProgressUpdateWithMessageDisplayController() {
        // Enable MessageDisplayController and mock handler for it
        when(mResources.getBoolean(mEnrollmentMessageDisplayControllerFlagResId)).thenReturn(true);
        when(mApplication.getMainThreadHandler()).thenReturn(new TestHandler());

        mViewModel.setToken(new byte[] { 1, 2, 3 });

        // Start enrollment
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
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
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
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
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
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
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
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
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
        assertThat(mCallbackWrapper.mValue).isNotNull();

        // Check default value
        final LiveData<Integer> liveData = mViewModel.getPointerDownLiveData();
        assertThat(liveData.getValue()).isNull();

        // Notify acquire message
        final int value = 33;
        mCallbackWrapper.mValue.onUdfpsPointerDown(value);
        assertThat(liveData.getValue()).isEqualTo(value);
    }

    @Test
    public void testGetPointerUpLiveData() {
        // Start enrollment
        mViewModel.setToken(new byte[] { 1, 2, 3 });
        final Object ret = mViewModel.startEnrollment(ENROLL_ENROLL);
        assertThat(ret).isNotNull();
        assertThat(mCallbackWrapper.mValue).isNotNull();

        // Check default value
        final LiveData<Integer> liveData = mViewModel.getPointerUpLiveData();
        assertThat(liveData.getValue()).isNull();

        // Notify acquire message
        final int value = 44;
        mCallbackWrapper.mValue.onUdfpsPointerUp(value);
        assertThat(liveData.getValue()).isEqualTo(value);
    }

    private static class TestWrapper<T> {
        T mValue;
    }

    private static class TestHandler extends Handler {

        TestHandler() {
            super(Looper.getMainLooper());
        }

        @Override
        public boolean sendMessageAtTime(@NonNull Message msg, long uptimeMillis) {
            msg.getCallback().run();
            return true;
        }
    }
}
