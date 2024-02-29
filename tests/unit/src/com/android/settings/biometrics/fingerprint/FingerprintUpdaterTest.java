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

package com.android.settings.biometrics.fingerprint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.CancellationSignal;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.safetycenter.SafetyCenterManagerWrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class FingerprintUpdaterTest {

    private static final byte[] HARDWARE_AUTH_TOKEN = new byte[] {0};
    private static final CancellationSignal CANCELLATION_SIGNAL = new CancellationSignal();
    private static final int USER_ID = 0;
    private static final int ENROLL_REASON = 0;
    private static final int ERR_MSG_ID = 0;
    private static final int HELP_MSG_ID = 0;
    private static final String HELP_STRING = "";
    private static final String ERR_STRING = "";
    private static final Fingerprint FINGERPRINT =
            new Fingerprint(/* name= */"", /* fingerId */ 0, /* deviceId= */ 0L);

    @Mock private FingerprintManager mFingerprintManager;
    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    private FingerprintUpdater mFingerprintUpdater;
    private Context mContext;
    private FingerprintManager.EnrollmentCallback mEnrollmentCallback;
    private FingerprintManager.RemovalCallback mRemovalCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mFingerprintUpdater = new FingerprintUpdater(mContext, mFingerprintManager);
        mEnrollmentCallback = spy(new TestEntrollmentCallback());
        mRemovalCallback = spy(new TestRemovalCallback());
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    @Test
    public void enroll_onEnrollmentCallbacks_triggerGivenCallback() {
        ArgumentCaptor<FingerprintManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FingerprintManager.EnrollmentCallback.class);
        mFingerprintUpdater.enroll(HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, USER_ID,
                mEnrollmentCallback, ENROLL_REASON, new Intent());
        verify(mFingerprintManager).enroll(
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                eq(USER_ID),
                callbackCaptor.capture(),
                eq(ENROLL_REASON),
                any());
        FingerprintManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentError(ERR_MSG_ID, ERR_STRING);
        callback.onEnrollmentProgress(/* remaining= */ 2);
        callback.onEnrollmentHelp(HELP_MSG_ID, HELP_STRING);

        verify(mEnrollmentCallback).onEnrollmentError(ERR_MSG_ID, ERR_STRING);
        verify(mEnrollmentCallback).onEnrollmentProgress(/* remaining= */ 2);
        verify(mEnrollmentCallback).onEnrollmentHelp(HELP_MSG_ID, HELP_STRING);
    }

    @Test
    public void enroll_onEnrollmentSuccess_invokedInteractionWithSafetyCenter() {
        ArgumentCaptor<FingerprintManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FingerprintManager.EnrollmentCallback.class);
        mFingerprintUpdater.enroll(HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, USER_ID,
                mEnrollmentCallback, ENROLL_REASON, new Intent());
        verify(mFingerprintManager).enroll(
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                eq(USER_ID),
                callbackCaptor.capture(),
                eq(ENROLL_REASON),
                any());
        FingerprintManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentProgress(/* remaining= */ 0);

        verify(mSafetyCenterManagerWrapper).isEnabled(mContext);
    }

    @Test
    public void enroll_onEnrollmentNotYetFinished_didntInvokeInteractionWithSafetyCenter() {
        ArgumentCaptor<FingerprintManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FingerprintManager.EnrollmentCallback.class);
        mFingerprintUpdater.enroll(HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, USER_ID,
                mEnrollmentCallback, ENROLL_REASON, new Intent());
        verify(mFingerprintManager).enroll(
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                eq(USER_ID),
                callbackCaptor.capture(),
                eq(ENROLL_REASON),
                any());
        FingerprintManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentProgress(/* remaining= */ 1);

        verify(mSafetyCenterManagerWrapper, never()).isEnabled(any());
    }

    @Test
    public void remove_onRemovalCallbacks_triggerGivenCallback() {
        ArgumentCaptor<FingerprintManager.RemovalCallback> callbackCaptor =
                ArgumentCaptor.forClass(FingerprintManager.RemovalCallback.class);
        mFingerprintUpdater.remove(FINGERPRINT, USER_ID, mRemovalCallback);
        verify(mFingerprintManager)
                .remove(same(FINGERPRINT), eq(USER_ID), callbackCaptor.capture());
        FingerprintManager.RemovalCallback callback = callbackCaptor.getValue();

        callback.onRemovalSucceeded(FINGERPRINT, /* remaining= */ 1);
        callback.onRemovalError(FINGERPRINT, ERR_MSG_ID, ERR_STRING);

        verify(mRemovalCallback).onRemovalSucceeded(any(), eq(1));
        verify(mRemovalCallback).onRemovalError(FINGERPRINT, ERR_MSG_ID, ERR_STRING);
    }

    @Test
    public void remove_onRemovalSuccess_invokedInteractionWithSafetyCenter() {
        ArgumentCaptor<FingerprintManager.RemovalCallback> callbackCaptor =
                ArgumentCaptor.forClass(FingerprintManager.RemovalCallback.class);
        mFingerprintUpdater.remove(FINGERPRINT, USER_ID, mRemovalCallback);
        verify(mFingerprintManager)
                .remove(same(FINGERPRINT), eq(USER_ID), callbackCaptor.capture());
        FingerprintManager.RemovalCallback callback = callbackCaptor.getValue();

        callback.onRemovalSucceeded(FINGERPRINT, /* remaining= */ 0);

        verify(mSafetyCenterManagerWrapper).isEnabled(mContext);
    }

    public static class TestEntrollmentCallback extends FingerprintManager.EnrollmentCallback {
        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {}

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {}

        @Override
        public void onEnrollmentProgress(int remaining) {}
    }

    public static class TestRemovalCallback extends FingerprintManager.RemovalCallback {
        @Override
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {}

        @Override
        public void onRemovalSucceeded(@Nullable Fingerprint fp, int remaining) {}
    }
}
