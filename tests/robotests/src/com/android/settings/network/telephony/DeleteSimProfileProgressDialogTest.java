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

import static com.android.settings.network.telephony.DeleteSimProfileProgressDialog.KEY_DELETE_STARTED;
import static com.android.settings.network.telephony.DeleteSimProfileProgressDialog.PENDING_INTENT;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.euicc.EuiccManager;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class DeleteSimProfileProgressDialogTest {
    private static final int SUB_ID = 111;

    @Mock
    private FragmentActivity mActivity;
    @Mock
    private Fragment mTargetFragment;
    @Mock
    private EuiccManager mEuiccManager;

    private Context mContext;
    private DeleteSimProfileProgressDialog mDialogFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        when(mContext.getSystemService(EuiccManager.class)).thenReturn(mEuiccManager);
        mDialogFragment = spy(DeleteSimProfileProgressDialog.newInstance(SUB_ID));
        when(mDialogFragment.getContext()).thenReturn(mContext);
        when(mDialogFragment.getTargetFragment()).thenReturn(mTargetFragment);
        when(mDialogFragment.getActivity()).thenReturn(mActivity);
    }

    @Test
    public void onCreateDialog_firstShowing_deleteStartedAndRecordedInOutState() {
        mDialogFragment.onCreateDialog(null);
        verify(mEuiccManager).deleteSubscription(eq(SUB_ID), notNull());

        final Bundle outState = new Bundle();
        mDialogFragment.onSaveInstanceState(outState);
        assertThat(outState.containsKey(KEY_DELETE_STARTED)).isTrue();
        assertThat(outState.getBoolean(KEY_DELETE_STARTED)).isTrue();
    }

    @Test
    public void showDialog_secondShowing_deleteNotStarted() {
        final Bundle inState = new Bundle();
        inState.putBoolean(KEY_DELETE_STARTED, true);
        mDialogFragment.onCreateDialog(inState);

        verify(mEuiccManager, never()).deleteSubscription(anyInt(), any());

        final Bundle outState = new Bundle();
        mDialogFragment.onSaveInstanceState(outState);
        assertThat(outState.containsKey(KEY_DELETE_STARTED)).isTrue();
        assertThat(outState.getBoolean(KEY_DELETE_STARTED)).isTrue();
    }

    @Test
    public void showDialog_pendingIntentReceiverFired_activityFinished() {
        mDialogFragment.onCreateDialog(null);

        final ArgumentCaptor<PendingIntent> intentCaptor = ArgumentCaptor.forClass(
                PendingIntent.class);
        verify(mEuiccManager).deleteSubscription(eq(SUB_ID), intentCaptor.capture());
        assertThat(intentCaptor.getValue()).isNotNull();

        final ArgumentCaptor<BroadcastReceiver> receiverCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        verify(mContext).registerReceiver(receiverCaptor.capture(), any(IntentFilter.class));

        doNothing().when(mDialogFragment).dismiss();
        receiverCaptor.getValue().onReceive(mContext, new Intent(PENDING_INTENT));
        verify(mDialogFragment).dismiss();
        verify(mActivity).finish();
    }

    @Test
    public void onDismiss_receiverUnregistered() {
        Dialog dialog = mDialogFragment.onCreateDialog(null);
        final ArgumentCaptor<BroadcastReceiver> receiverCaptor = ArgumentCaptor.forClass(
                BroadcastReceiver.class);
        verify(mContext).registerReceiver(receiverCaptor.capture(), any(IntentFilter.class));

        mDialogFragment.onDismiss(dialog);
        verify(mContext).unregisterReceiver(eq(receiverCaptor.getValue()));
    }
}
