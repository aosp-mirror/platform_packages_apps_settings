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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.biometrics.fingerprint.FingerprintUpdater;
import com.android.settings.biometrics2.ui.model.EnrollmentProgress;
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

    @Before
    public void setUp() {
        when(mApplication.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(R.bool.enrollment_message_display_controller_flag))
                .thenReturn(false);
        mViewModel = new FingerprintEnrollProgressViewModel(mApplication, mFingerprintUpdater,
                TEST_USER_ID);
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
        @EnrollReason final int enrollReason = ENROLL_ENROLL;
        final byte[] token = new byte[] { 1, 2, 3 };
        mViewModel.setToken(token);

        final TestWrapper<CancellationSignal> signalWrapper = new TestWrapper<>();
        doAnswer(invocation -> {
            signalWrapper.mValue = invocation.getArgument(1);
            return null;
        }).when(mFingerprintUpdater).enroll(any(byte[].class), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(EnrollmentCallback.class), anyInt());

        // Start enrollment
        final boolean ret = mViewModel.startEnrollment(enrollReason);
        assertThat(ret).isTrue();
        assertThat(signalWrapper.mValue).isNotNull();

        // Cancel enrollment
        mViewModel.cancelEnrollment();

        assertThat(signalWrapper.mValue.isCanceled()).isTrue();
    }

    @Test
    public void testProgressUpdate() {
        @EnrollReason final int enrollReason = ENROLL_ENROLL;
        final byte[] token = new byte[] { 1, 2, 3 };
        mViewModel.setToken(token);

        final TestWrapper<EnrollmentCallback> callbackWrapper = new TestWrapper<>();
        doAnswer(invocation -> {
            callbackWrapper.mValue = invocation.getArgument(3);
            return null;
        }).when(mFingerprintUpdater).enroll(any(byte[].class), any(CancellationSignal.class),
                eq(TEST_USER_ID), any(EnrollmentCallback.class), anyInt());

        // Start enrollment
        final boolean ret = mViewModel.startEnrollment(enrollReason);
        assertThat(ret).isTrue();
        assertThat(callbackWrapper.mValue).isNotNull();

        // Update first progress
        callbackWrapper.mValue.onEnrollmentProgress(25);
        EnrollmentProgress progress = mViewModel.getProgressLiveData().getValue();
        assertThat(progress).isNotNull();
        assertThat(progress.getSteps()).isEqualTo(25);
        assertThat(progress.getRemaining()).isEqualTo(25);

        // Update second progress
        callbackWrapper.mValue.onEnrollmentProgress(20);
        progress = mViewModel.getProgressLiveData().getValue();
        assertThat(progress).isNotNull();
        assertThat(progress.getSteps()).isEqualTo(25);
        assertThat(progress.getRemaining()).isEqualTo(20);
    }

    // TODO(b/260957933): FingerprintEnrollProgressViewModel::getErrorLiveData() and
    // FingerprintEnrollProgressViewModel::getHelpLiveData() doesn't built into apk because no one
    // uses it. We shall test it when new FingerprintEnrollEnrolling has used these 2 methods.

    private static class TestWrapper<T> {
        T mValue;
    }
}
