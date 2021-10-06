/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.concurrent.Executor;

@RunWith(RobolectricTestRunner.class)
public class SignalStrengthListenerTest {
    private static final int SUB_ID_1 = 111;
    private static final int SUB_ID_2 = 222;
    private static final int SUB_ID_3 = 333;

    @Mock
    private SignalStrengthListener.Callback mCallback;
    @Mock
    private TelephonyManager mBaseManager;
    @Mock
    private TelephonyManager mManager1;
    @Mock
    private TelephonyManager mManager2;
    @Mock
    private TelephonyManager mManager3;

    private Context mContext;
    private SignalStrengthListener mListener;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mBaseManager);
        when(mBaseManager.createForSubscriptionId(SUB_ID_1)).thenReturn(mManager1);
        when(mBaseManager.createForSubscriptionId(SUB_ID_2)).thenReturn(mManager2);
        when(mBaseManager.createForSubscriptionId(SUB_ID_3)).thenReturn(mManager3);
        mListener = new SignalStrengthListener(mContext, mCallback);
    }

    @Test
    public void resume_noIds_noCrash() {
        mListener.resume();
    }

    @Test
    public void pause_noIds_noCrash() {
        mListener.resume();
    }

    @Test
    public void updateSubscriptionIds_beforeResume_startedListening() {
        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2));
        ArgumentCaptor<SignalStrengthListener.SignalStrengthTelephonyCallback> captor1 =
                ArgumentCaptor.forClass(
                        SignalStrengthListener.SignalStrengthTelephonyCallback.class);
        ArgumentCaptor<SignalStrengthListener.SignalStrengthTelephonyCallback> captor2 =
                ArgumentCaptor.forClass(
                        SignalStrengthListener.SignalStrengthTelephonyCallback.class);

        verify(mManager1).registerTelephonyCallback(
                any(Executor.class), captor1.capture());
        verify(mManager2).registerTelephonyCallback(
                any(Executor.class), captor2.capture());
        verify(mManager3, never()).registerTelephonyCallback(any(), any());

        assertThat(captor1.getValue()).isNotNull();
        assertThat(captor2.getValue()).isNotNull();

        // Make sure the two listeners are separate objects.
        assertThat(captor1.getValue() != captor2.getValue()).isTrue();
    }

    @Test
    public void updateSubscriptionIds_twoCalls_oneIdAdded() {
        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2));

        verify(mManager1).registerTelephonyCallback(any(Executor.class),
                eq(mListener.mTelephonyCallbacks.get(SUB_ID_1)));
        verify(mManager2).registerTelephonyCallback(any(Executor.class),
                eq(mListener.mTelephonyCallbacks.get(SUB_ID_2)));

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2, SUB_ID_3));
        verify(mManager1, never()).unregisterTelephonyCallback(
                mListener.mTelephonyCallbacks.get(SUB_ID_1));
        verify(mManager2, never()).unregisterTelephonyCallback(
                mListener.mTelephonyCallbacks.get(SUB_ID_2));
        verify(mManager3).registerTelephonyCallback(
                any(Executor.class), eq(mListener.mTelephonyCallbacks.get(SUB_ID_3)));
    }

    @Test
    public void updateSubscriptionIds_twoCalls_oneIdRemoved() {
        ArgumentCaptor<SignalStrengthListener.SignalStrengthTelephonyCallback> captor1 =
                ArgumentCaptor.forClass(
                        SignalStrengthListener.SignalStrengthTelephonyCallback.class);

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2));
        verify(mManager1).registerTelephonyCallback(any(Executor.class), captor1.capture());
        verify(mManager2).registerTelephonyCallback(
                any(Executor.class), any(TelephonyCallback.class));

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_2));
        verify(mManager1).unregisterTelephonyCallback(captor1.capture());
        verify(mManager2, never()).unregisterTelephonyCallback(any(TelephonyCallback.class));
        // Make sure the correct listener was removed.
        assertThat(captor1.getAllValues().get(0) == captor1.getAllValues().get(1)).isTrue();
    }

    @Test
    public void updateSubscriptionIds_twoCalls_twoIdsRemovedOneAdded() {
        ArgumentCaptor<SignalStrengthListener.SignalStrengthTelephonyCallback> captor1 =
                ArgumentCaptor.forClass(
                        SignalStrengthListener.SignalStrengthTelephonyCallback.class);
        ArgumentCaptor<SignalStrengthListener.SignalStrengthTelephonyCallback> captor2 =
                ArgumentCaptor.forClass(
                        SignalStrengthListener.SignalStrengthTelephonyCallback.class);

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2));
        verify(mManager1).registerTelephonyCallback(any(Executor.class), captor1.capture());
        verify(mManager2).registerTelephonyCallback(any(Executor.class), captor2.capture());

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_3));
        verify(mManager1).unregisterTelephonyCallback(captor1.capture());
        verify(mManager2).unregisterTelephonyCallback(captor2.capture());
        verify(mManager3).registerTelephonyCallback(
                any(Executor.class), any(TelephonyCallback.class));
        // Make sure the correct listeners were removed.
        assertThat(captor1.getValue() != captor2.getValue()).isTrue();
        assertThat(captor1.getAllValues().get(0) == captor1.getAllValues().get(1)).isTrue();
        assertThat(captor2.getAllValues().get(0) == captor2.getAllValues().get(1)).isTrue();
    }

    @Test
    public void updateSubscriptionIds_thenPauseResume_correctlyStartsAndStops() {
        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2));
        mListener.pause();
        mListener.resume();

        ArgumentCaptor<SignalStrengthListener.SignalStrengthTelephonyCallback> captor1 =
                ArgumentCaptor.forClass(
                        SignalStrengthListener.SignalStrengthTelephonyCallback.class);
        ArgumentCaptor<SignalStrengthListener.SignalStrengthTelephonyCallback> captor2 =
                ArgumentCaptor.forClass(
                        SignalStrengthListener.SignalStrengthTelephonyCallback.class);
        verify(mManager1, times(2)).registerTelephonyCallback(
                any(Executor.class), captor1.capture());
        verify(mManager1).unregisterTelephonyCallback(captor1.capture());

        verify(mManager2, times(2)).registerTelephonyCallback(
                any(Executor.class), captor2.capture());
        verify(mManager2).unregisterTelephonyCallback(captor2.capture());

        assertThat(captor1.getAllValues().get(0) == captor1.getAllValues().get(1)).isTrue();
        assertThat(captor1.getAllValues().get(0) == captor1.getAllValues().get(2)).isTrue();

        assertThat(captor2.getAllValues().get(0) == captor2.getAllValues().get(1)).isTrue();
        assertThat(captor2.getAllValues().get(0) == captor2.getAllValues().get(2)).isTrue();
    }
}
