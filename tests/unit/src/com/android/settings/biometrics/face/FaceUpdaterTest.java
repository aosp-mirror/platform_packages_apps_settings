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

package com.android.settings.biometrics.face;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.hardware.face.Face;
import android.hardware.face.FaceEnrollCell;
import android.hardware.face.FaceEnrollStages;
import android.hardware.face.FaceManager;
import android.os.CancellationSignal;
import android.view.Surface;

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
public class FaceUpdaterTest {

    private static final byte[] HARDWARE_AUTH_TOKEN = new byte[] {0};
    private static final CancellationSignal CANCELLATION_SIGNAL = new CancellationSignal();
    private static final int USER_ID = 0;
    private static final int ERR_MSG_ID = 0;
    private static final int HELP_MSG_ID = 0;
    private static final String HELP_STRING = "";
    private static final String ERR_STRING = "";
    private static final Face FACE =
            new Face(/* name= */"", /* faceId */ 0, /* deviceId= */ 0L);
    private static final int[] DISABLED_FEATURES = new int[] {0};
    private static final boolean DEBUG_CONSENT = false;
    private static final Surface PREVIEW_SURFACE = new Surface();
    private static final int HELP_CODE = 0;
    private static final CharSequence HELP_MESSAGE = "";
    private static final FaceEnrollCell CELL =
            new FaceEnrollCell(/* x= */ 0, /* y= */ 0, /* z= */ 0);
    private static final int STAGE = FaceEnrollStages.UNKNOWN;
    private static final float PAN = 0;
    private static final float TILT = 0;
    private static final float DISTANCE = 0;


    @Mock private FaceManager mFaceManager;
    @Mock private SafetyCenterManagerWrapper mSafetyCenterManagerWrapper;

    private FaceUpdater mFaceUpdater;
    private Context mContext;
    private FaceManager.EnrollmentCallback mEnrollmentCallback;
    private FaceManager.RemovalCallback mRemovalCallback;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mFaceUpdater = new FaceUpdater(mContext, mFaceManager);
        mEnrollmentCallback = spy(new TestEnrollmentCallback());
        mRemovalCallback = spy(new TestRemovalCallback());
        SafetyCenterManagerWrapper.sInstance = mSafetyCenterManagerWrapper;
    }

    @Test
    public void enroll_firstVersion_onEnrollmentCallbacks_triggerGivenCallback() {
        ArgumentCaptor<FaceManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FaceManager.EnrollmentCallback.class);
        mFaceUpdater.enroll(USER_ID, HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, mEnrollmentCallback,
                DISABLED_FEATURES);
        verify(mFaceManager).enroll(
                eq(USER_ID),
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                callbackCaptor.capture(),
                same(DISABLED_FEATURES),
                same(null),
                eq(false));
        FaceManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentError(ERR_MSG_ID, ERR_STRING);
        callback.onEnrollmentProgress(/* remaining= */ 2);
        callback.onEnrollmentHelp(HELP_MSG_ID, HELP_STRING);
        callback.onEnrollmentFrame(HELP_CODE, HELP_MESSAGE, CELL, STAGE, PAN, TILT, DISTANCE);

        verify(mEnrollmentCallback, atLeast(1)).onEnrollmentError(ERR_MSG_ID, ERR_STRING);
        verify(mEnrollmentCallback, atLeast(1)).onEnrollmentProgress(/* remaining= */ 2);
        verify(mEnrollmentCallback, atLeast(1)).onEnrollmentHelp(HELP_MSG_ID, HELP_STRING);
        verify(mEnrollmentCallback, atLeast(1))
                .onEnrollmentFrame(HELP_CODE, HELP_MESSAGE, CELL, STAGE, PAN, TILT, DISTANCE);
    }

    @Test
    public void enroll_firstVersion_onEnrollmentSuccess_invokedInteractionWithSafetyCenter() {
        ArgumentCaptor<FaceManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FaceManager.EnrollmentCallback.class);
        mFaceUpdater.enroll(USER_ID, HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, mEnrollmentCallback,
                DISABLED_FEATURES);
        verify(mFaceManager).enroll(
                eq(USER_ID),
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                callbackCaptor.capture(),
                same(DISABLED_FEATURES),
                same(null),
                eq(false));
        FaceManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentProgress(/* remaining= */ 0);

        verify(mSafetyCenterManagerWrapper, atLeast(1)).isEnabled(mContext);
    }

    @Test
    public void enroll_firstVersion_onEnrollmentNotYetFinished_didntInvokeInteractionWithSafetyCenter() {
        ArgumentCaptor<FaceManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FaceManager.EnrollmentCallback.class);
        mFaceUpdater.enroll(USER_ID, HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, mEnrollmentCallback,
                DISABLED_FEATURES);
        verify(mFaceManager).enroll(
                eq(USER_ID),
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                callbackCaptor.capture(),
                same(DISABLED_FEATURES),
                same(null),
                eq(false));
        FaceManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentProgress(/* remaining= */ 1);

        verify(mSafetyCenterManagerWrapper, never()).isEnabled(any());
    }

    @Test
    public void enroll_secondVersion_onEnrollmentCallbacks_triggerGivenCallback() {
        ArgumentCaptor<FaceManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FaceManager.EnrollmentCallback.class);
        mFaceUpdater.enroll(USER_ID, HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, mEnrollmentCallback,
                DISABLED_FEATURES, PREVIEW_SURFACE, DEBUG_CONSENT);
        verify(mFaceManager).enroll(
                eq(USER_ID),
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                callbackCaptor.capture(),
                same(DISABLED_FEATURES),
                same(PREVIEW_SURFACE),
                eq(DEBUG_CONSENT));
        FaceManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentError(ERR_MSG_ID, ERR_STRING);
        callback.onEnrollmentProgress(/* remaining= */ 2);
        callback.onEnrollmentHelp(HELP_MSG_ID, HELP_STRING);
        callback.onEnrollmentFrame(HELP_CODE, HELP_MESSAGE, CELL, STAGE, PAN, TILT, DISTANCE);

        verify(mEnrollmentCallback, atLeast(1)).onEnrollmentError(ERR_MSG_ID, ERR_STRING);
        verify(mEnrollmentCallback, atLeast(1)).onEnrollmentProgress(/* remaining= */ 2);
        verify(mEnrollmentCallback, atLeast(1)).onEnrollmentHelp(HELP_MSG_ID, HELP_STRING);
        verify(mEnrollmentCallback, atLeast(1))
                .onEnrollmentFrame(HELP_CODE, HELP_MESSAGE, CELL, STAGE, PAN, TILT, DISTANCE);
    }

    @Test
    public void enroll_secondVersion_onEnrollmentSuccess_invokedInteractionWithSafetyCenter() {
        ArgumentCaptor<FaceManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FaceManager.EnrollmentCallback.class);
        mFaceUpdater.enroll(USER_ID, HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, mEnrollmentCallback,
                DISABLED_FEATURES, PREVIEW_SURFACE, DEBUG_CONSENT);
        verify(mFaceManager).enroll(
                eq(USER_ID),
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                callbackCaptor.capture(),
                same(DISABLED_FEATURES),
                same(PREVIEW_SURFACE),
                eq(DEBUG_CONSENT));
        FaceManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentProgress(/* remaining= */ 0);

        verify(mSafetyCenterManagerWrapper).isEnabled(mContext);
    }

    @Test
    public void enroll_secondVersion_onEnrollmentNotYetFinished_didntInvokeInteractionWithSafetyCenter() {
        ArgumentCaptor<FaceManager.EnrollmentCallback> callbackCaptor =
                ArgumentCaptor.forClass(FaceManager.EnrollmentCallback.class);
        mFaceUpdater.enroll(USER_ID, HARDWARE_AUTH_TOKEN, CANCELLATION_SIGNAL, mEnrollmentCallback,
                DISABLED_FEATURES, PREVIEW_SURFACE, DEBUG_CONSENT);
        verify(mFaceManager).enroll(
                eq(USER_ID),
                same(HARDWARE_AUTH_TOKEN),
                same(CANCELLATION_SIGNAL),
                callbackCaptor.capture(),
                same(DISABLED_FEATURES),
                same(PREVIEW_SURFACE),
                eq(DEBUG_CONSENT));
        FaceManager.EnrollmentCallback callback = callbackCaptor.getValue();

        callback.onEnrollmentProgress(/* remaining= */ 1);

        verify(mSafetyCenterManagerWrapper, never()).isEnabled(any());
    }

    @Test
    public void remove_onRemovalCallbacks_triggerGivenCallback() {
        ArgumentCaptor<FaceManager.RemovalCallback> callbackCaptor =
                ArgumentCaptor.forClass(FaceManager.RemovalCallback.class);
        mFaceUpdater.remove(FACE, USER_ID, mRemovalCallback);
        verify(mFaceManager)
                .remove(same(FACE), eq(USER_ID), callbackCaptor.capture());
        FaceManager.RemovalCallback callback = callbackCaptor.getValue();

        callback.onRemovalSucceeded(FACE, /* remaining= */ 1);
        callback.onRemovalError(FACE, ERR_MSG_ID, ERR_STRING);

        verify(mRemovalCallback).onRemovalSucceeded(any(), eq(1));
        verify(mRemovalCallback).onRemovalError(FACE, ERR_MSG_ID, ERR_STRING);
    }

    @Test
    public void remove_onRemovalSuccess_invokedInteractionWithSafetyCenter() {
        ArgumentCaptor<FaceManager.RemovalCallback> callbackCaptor =
                ArgumentCaptor.forClass(FaceManager.RemovalCallback.class);
        mFaceUpdater.remove(FACE, USER_ID, mRemovalCallback);
        verify(mFaceManager)
                .remove(same(FACE), eq(USER_ID), callbackCaptor.capture());
        FaceManager.RemovalCallback callback = callbackCaptor.getValue();

        callback.onRemovalSucceeded(FACE, /* remaining= */ 0);

        verify(mSafetyCenterManagerWrapper).isEnabled(mContext);
    }

    public static class TestEnrollmentCallback extends FaceManager.EnrollmentCallback {
        @Override
        public void onEnrollmentError(int errMsgId, CharSequence errString) {}

        @Override
        public void onEnrollmentHelp(int helpMsgId, CharSequence helpString) {}

        @Override
        public void onEnrollmentProgress(int remaining) {}

        @Override
        public void onEnrollmentFrame(int helpCode, @Nullable CharSequence helpMessage,
                @Nullable FaceEnrollCell cell, int stage, float pan, float tilt, float distance) {}
    }

    public static class TestRemovalCallback extends FaceManager.RemovalCallback {
        @Override
        public void onRemovalError(Face fp, int errMsgId, CharSequence errString) {}

        @Override
        public void onRemovalSucceeded(@Nullable Face fp, int remaining) {}
    }
}
