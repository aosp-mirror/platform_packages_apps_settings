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

import static android.telephony.PhoneStateListener.LISTEN_NONE;
import static android.telephony.PhoneStateListener.LISTEN_SIGNAL_STRENGTHS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.PhoneStateListener;
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
        ArgumentCaptor<PhoneStateListener> captor1 = ArgumentCaptor.forClass(
                PhoneStateListener.class);
        ArgumentCaptor<PhoneStateListener> captor2 = ArgumentCaptor.forClass(
                PhoneStateListener.class);
        verify(mManager1).listen(captor1.capture(), eq(LISTEN_SIGNAL_STRENGTHS));
        verify(mManager2).listen(captor2.capture(), eq(LISTEN_SIGNAL_STRENGTHS));
        verify(mManager3, never()).listen(any(), anyInt());
        assertThat(captor1.getValue()).isNotNull();
        assertThat(captor2.getValue()).isNotNull();

        // Make sure the two listeners are separate objects.
        assertThat(captor1.getValue() != captor2.getValue()).isTrue();
    }

    @Test
    public void updateSubscriptionIds_twoCalls_oneIdAdded() {
        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2));
        verify(mManager1).listen(any(PhoneStateListener.class), eq(LISTEN_SIGNAL_STRENGTHS));
        verify(mManager2).listen(any(PhoneStateListener.class), eq(LISTEN_SIGNAL_STRENGTHS));

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2, SUB_ID_3));
        verify(mManager1, never()).listen(any(PhoneStateListener.class), eq(LISTEN_NONE));
        verify(mManager2, never()).listen(any(PhoneStateListener.class), eq(LISTEN_NONE));
        verify(mManager3).listen(any(PhoneStateListener.class), eq(LISTEN_SIGNAL_STRENGTHS));
    }

    @Test
    public void updateSubscriptionIds_twoCalls_oneIdRemoved() {
        ArgumentCaptor<PhoneStateListener> captor1 = ArgumentCaptor.forClass(
                PhoneStateListener.class);

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2));
        verify(mManager1).listen(captor1.capture(), eq(LISTEN_SIGNAL_STRENGTHS));
        verify(mManager2).listen(any(PhoneStateListener.class), eq(LISTEN_SIGNAL_STRENGTHS));

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_2));
        verify(mManager1).listen(captor1.capture(), eq(LISTEN_NONE));
        verify(mManager2, never()).listen(any(PhoneStateListener.class), eq(LISTEN_NONE));
        // Make sure the correct listener was removed.
        assertThat(captor1.getAllValues().get(0) == captor1.getAllValues().get(1)).isTrue();
    }

    @Test
    public void updateSubscriptionIds_twoCalls_twoIdsRemovedOneAdded() {
        ArgumentCaptor<PhoneStateListener> captor1 = ArgumentCaptor.forClass(
                PhoneStateListener.class);
        ArgumentCaptor<PhoneStateListener> captor2 = ArgumentCaptor.forClass(
                PhoneStateListener.class);

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_1, SUB_ID_2));
        verify(mManager1).listen(captor1.capture(), eq(LISTEN_SIGNAL_STRENGTHS));
        verify(mManager2).listen(captor2.capture(), eq(LISTEN_SIGNAL_STRENGTHS));

        mListener.updateSubscriptionIds(Sets.newSet(SUB_ID_3));
        verify(mManager1).listen(captor1.capture(), eq(LISTEN_NONE));
        verify(mManager2).listen(captor2.capture(), eq(LISTEN_NONE));
        verify(mManager3).listen(any(PhoneStateListener.class), eq(LISTEN_SIGNAL_STRENGTHS));
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

        ArgumentCaptor<PhoneStateListener> captor1 = ArgumentCaptor.forClass(
                PhoneStateListener.class);
        ArgumentCaptor<PhoneStateListener> captor2 = ArgumentCaptor.forClass(
                PhoneStateListener.class);
        verify(mManager1, times(2)).listen(captor1.capture(), eq(LISTEN_SIGNAL_STRENGTHS));
        verify(mManager1).listen(captor1.capture(), eq(LISTEN_NONE));

        verify(mManager2, times(2)).listen(captor2.capture(), eq(LISTEN_SIGNAL_STRENGTHS));
        verify(mManager2).listen(captor2.capture(), eq(LISTEN_NONE));

        assertThat(captor1.getAllValues().get(0) == captor1.getAllValues().get(1)).isTrue();
        assertThat(captor1.getAllValues().get(0) == captor1.getAllValues().get(2)).isTrue();

        assertThat(captor2.getAllValues().get(0) == captor2.getAllValues().get(1)).isTrue();
        assertThat(captor2.getAllValues().get(0) == captor2.getAllValues().get(2)).isTrue();
    }
}
