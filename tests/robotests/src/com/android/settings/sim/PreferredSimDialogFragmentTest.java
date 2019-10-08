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

package com.android.settings.sim;

import static com.android.settings.sim.SimDialogActivity.PREFERRED_PICK;
import static com.android.settings.sim.SimDialogActivity.PREFERRED_SIM;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.DialogInterface;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class PreferredSimDialogFragmentTest extends
        SimDialogFragmentTestBase<PreferredSimDialogFragment> {

    @Override
    public void setUp() {
        super.setUp();
        setDialogType(PREFERRED_PICK);
        mFragment = spy(PreferredSimDialogFragment.newInstance());
        doReturn(mSubscriptionManager).when(mFragment).getSubscriptionManager();
    }

    @Test
    public void onCreateDialog_noSims_dismissed() {
        when(mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(anyInt()))
                .thenReturn(null);
        mIntent.putExtra(PREFERRED_SIM, 0);
        startDialog();
        verify(mFragment).dismiss();
    }

    @Test
    public void onCreateDialog_oneSimWrongSlotArgument_dismissed() {
        when(mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1)).thenReturn(null);
        mIntent.putExtra(PREFERRED_SIM, 1);
        startDialog();
        verify(mFragment).dismiss();
    }

    @Test
    public void onCreateDialog_twoSimsSelectFirst_correctMessage() {
        mIntent.putExtra(PREFERRED_SIM, 0);

        final AlertDialog alertDialog = startDialog();
        final ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        final String message = (String) shadowDialog.getMessage();
        assertThat(message).contains(SIM1_NAME);
        assertThat(message).doesNotContain(SIM2_NAME);
    }

    @Test
    public void onCreateDialog_twoSimsSelectSecond_correctMessage() {
        mIntent.putExtra(PREFERRED_SIM, 1);

        final AlertDialog alertDialog = startDialog();
        final ShadowAlertDialogCompat shadowDialog = ShadowAlertDialogCompat.shadowOf(alertDialog);
        final String message = (String) shadowDialog.getMessage();
        assertThat(message).contains(SIM2_NAME);
        assertThat(message).doesNotContain(SIM1_NAME);
    }

    @Test
    public void onClick_yesClicked_callsOnSubscriptionSelected() {
        mIntent.putExtra(PREFERRED_SIM, 0);

        final AlertDialog alertDialog = startDialog();

        final SimDialogActivity activity = (SimDialogActivity) spy(mFragment.getActivity());
        doReturn(activity).when(mFragment).getActivity();
        doNothing().when(activity).onSubscriptionSelected(anyInt(), anyInt());

        mFragment.onClick(alertDialog, DialogInterface.BUTTON_POSITIVE);
        verify(activity).onSubscriptionSelected(PREFERRED_PICK, SIM1_ID);
    }

    @Test
    public void onClick_noClicked_doesNotCallOnSubscriptionSelected() {
        mIntent.putExtra(PREFERRED_SIM, 0);

        final AlertDialog alertDialog = startDialog();

        final SimDialogActivity activity = (SimDialogActivity) spy(mFragment.getActivity());
        doReturn(activity).when(mFragment).getActivity();
        doNothing().when(activity).onSubscriptionSelected(anyInt(), anyInt());

        mFragment.onClick(alertDialog, DialogInterface.BUTTON_NEGATIVE);
        verify(activity, never()).onSubscriptionSelected(anyInt(), anyInt());
    }
}
