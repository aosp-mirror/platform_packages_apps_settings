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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class MessageDisplayControllerTest {
    @Rule
    public final MockitoRule mockito = MockitoJUnit.rule();

    private static final long START_TIME = 0L;
    private static final int HELP_ID = 0;
    private static final String HELP_MESSAGE = "Default Help Message";
    private static final int REMAINING = 5;
    private static final int HELP_MINIMUM_DISPLAY_TIME = 300;
    private static final int PROGRESS_MINIMUM_DISPLAY_TIME = 250;
    private static final int COLLECT_TIME = 100;

    private MessageDisplayController mMessageDisplayController;
    @Mock
    private FingerprintManager.EnrollmentCallback mEnrollmentCallback;
    @Mock
    private Clock mClock;

    @Before
    public void setup() {
        mMessageDisplayController = new MessageDisplayController(new Handler(), mEnrollmentCallback,
                mClock,
                HELP_MINIMUM_DISPLAY_TIME,   /* progressPriorityOverHelp */
                PROGRESS_MINIMUM_DISPLAY_TIME,   /* prioritizeAcquireMessages */
                false, false, COLLECT_TIME);
    }

    private void setMessageDisplayController(boolean progressPriorityOverHelp,
            boolean prioritizeAcquireMessages) {
        mMessageDisplayController = new MessageDisplayController(new Handler(), mEnrollmentCallback,
                mClock, HELP_MINIMUM_DISPLAY_TIME, PROGRESS_MINIMUM_DISPLAY_TIME,
                progressPriorityOverHelp, prioritizeAcquireMessages, COLLECT_TIME);
    }

    @Test
    public void showsHelpMessageAfterCollectTime() {
        when(mClock.millis()).thenReturn(START_TIME);

        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        when(mClock.millis()).thenReturn((long) COLLECT_TIME);
        ShadowLooper.idleMainLooper(COLLECT_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        verifyNoMoreInteractions(mEnrollmentCallback);
    }

    @Test
    public void showsProgressMessageAfterCollectTime() {
        when(mClock.millis()).thenReturn(START_TIME);

        mMessageDisplayController.onEnrollmentProgress(REMAINING);
        when(mClock.millis()).thenReturn((long) COLLECT_TIME);
        ShadowLooper.idleMainLooper(COLLECT_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentProgress(REMAINING);
        verifyNoMoreInteractions(mEnrollmentCallback);
    }

    @Test
    public void helpDisplayedForMinimumDisplayTime() {
        when(mClock.millis()).thenReturn(START_TIME);

        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        when(mClock.millis()).thenReturn((long) COLLECT_TIME);
        ShadowLooper.idleMainLooper(COLLECT_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentHelp(HELP_ID, HELP_MESSAGE);

        mMessageDisplayController.onEnrollmentProgress(REMAINING);

        verifyNoMoreInteractions(mEnrollmentCallback);

        when(mClock.millis()).thenReturn((long) (HELP_MINIMUM_DISPLAY_TIME + COLLECT_TIME));
        ShadowLooper.idleMainLooper(HELP_MINIMUM_DISPLAY_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentProgress(REMAINING);
    }

    @Test
    public void progressDisplayedForMinimumDisplayTime() {
        when(mClock.millis()).thenReturn(START_TIME);

        mMessageDisplayController.onEnrollmentProgress(REMAINING);
        when(mClock.millis()).thenReturn((long) COLLECT_TIME);
        ShadowLooper.idleMainLooper(COLLECT_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentProgress(REMAINING);

        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);

        verifyNoMoreInteractions(mEnrollmentCallback);

        when(mClock.millis()).thenReturn((long) (COLLECT_TIME + PROGRESS_MINIMUM_DISPLAY_TIME));
        ShadowLooper.idleMainLooper(PROGRESS_MINIMUM_DISPLAY_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
    }

    @Test
    public void prioritizeHelpMessage_thenShowProgress() {
        when(mClock.millis()).thenReturn(START_TIME);

        mMessageDisplayController.onEnrollmentProgress(REMAINING);
        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        when(mClock.millis()).thenReturn((long) COLLECT_TIME);
        ShadowLooper.idleMainLooper(COLLECT_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        verifyNoMoreInteractions(mEnrollmentCallback);

        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        when(mClock.millis()).thenReturn((long) (COLLECT_TIME + HELP_MINIMUM_DISPLAY_TIME));
        ShadowLooper.idleMainLooper(HELP_MINIMUM_DISPLAY_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentProgress(REMAINING);
    }

    @Test
    public void prioritizeProgressOverHelp() {
        when(mClock.millis()).thenReturn(START_TIME);
        setMessageDisplayController(true /* progressPriorityOverHelp */,
                false /* prioritizeAcquireMessages */);

        mMessageDisplayController.onEnrollmentProgress(REMAINING);
        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        when(mClock.millis()).thenReturn((long) COLLECT_TIME);
        ShadowLooper.idleMainLooper(COLLECT_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentProgress(REMAINING);
        verifyNoMoreInteractions(mEnrollmentCallback);
    }

    @Test
    public void prioritizeHelpMessageByCount() {
        String newHelpMessage = "New message";
        when(mClock.millis()).thenReturn(START_TIME);
        setMessageDisplayController(false /* progressPriorityOverHelp */,
                true /* prioritizeAcquireMessages */);

        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        mMessageDisplayController.onEnrollmentHelp(HELP_ID, newHelpMessage);
        when(mClock.millis()).thenReturn((long) COLLECT_TIME);
        ShadowLooper.idleMainLooper(COLLECT_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        verifyNoMoreInteractions(mEnrollmentCallback);
    }

    @Test
    public void ignoreSameProgress() {
        int progressChange = REMAINING - 1;
        when(mClock.millis()).thenReturn(START_TIME);
        setMessageDisplayController(true /* progressPriorityOverHelp */,
                false /* prioritizeAcquireMessages */);

        mMessageDisplayController.onEnrollmentProgress(REMAINING);
        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        when(mClock.millis()).thenReturn((long) COLLECT_TIME);
        ShadowLooper.idleMainLooper(COLLECT_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentProgress(REMAINING);
        verifyNoMoreInteractions(mEnrollmentCallback);

        mMessageDisplayController.onEnrollmentProgress(REMAINING);
        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        when(mClock.millis()).thenReturn((long) (COLLECT_TIME + PROGRESS_MINIMUM_DISPLAY_TIME));
        ShadowLooper.idleMainLooper(PROGRESS_MINIMUM_DISPLAY_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentHelp(HELP_ID, HELP_MESSAGE);

        mMessageDisplayController.onEnrollmentProgress(progressChange);
        mMessageDisplayController.onEnrollmentHelp(HELP_ID, HELP_MESSAGE);
        when(mClock.millis()).thenReturn((long) (COLLECT_TIME + PROGRESS_MINIMUM_DISPLAY_TIME
                + HELP_MINIMUM_DISPLAY_TIME));
        ShadowLooper.idleMainLooper(HELP_MINIMUM_DISPLAY_TIME, TimeUnit.MILLISECONDS);

        verify(mEnrollmentCallback).onEnrollmentProgress(progressChange);
    }
}
