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

import static com.android.settings.sim.SimDialogActivity.DATA_PICK;
import static com.android.settings.sim.SimDialogActivity.SMS_PICK;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.telephony.SubscriptionManager;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class SimListDialogFragmentTest extends SimDialogFragmentTestBase<SimListDialogFragment> {

    @Test
    public void onCreateDialog_noSubscriptions_dismissed() {
        final int dialogType = DATA_PICK;
        setDialogType(dialogType);
        mFragment = spy(SimListDialogFragment.newInstance(dialogType, R.string.select_sim_for_data,
                false /* includeAskEveryTime */));
        doReturn(null).when(mFragment).getCurrentSubscriptions();
        startDialog();
        verify(mFragment).dismiss();
    }

    @Test
    public void onCreateDialog_twoSubscriptionsNoAskEveryTime_twoSubsForDisplay() {
        final int dialogType = DATA_PICK;
        setDialogType(dialogType);
        mFragment = spy(SimListDialogFragment.newInstance(dialogType, R.string.select_sim_for_data,
                false /* includeAskEveryTime */));
        doReturn(Arrays.asList(mSim1, mSim2)).when(mFragment).getCurrentSubscriptions();
        // Avoid problems robolectric has with our real adapter.
        doNothing().when(mFragment).setAdapter(any());
        final AlertDialog alertDialog = startDialog();
        assertThat(mFragment.mSubscriptions).hasSize(2);

        final SimDialogActivity activity = (SimDialogActivity) spy(mFragment.getActivity());
        doReturn(activity).when(mFragment).getActivity();
        doNothing().when(activity).onSubscriptionSelected(anyInt(), anyInt());

        mFragment.onClick(alertDialog, 1);
        verify(activity).onSubscriptionSelected(dialogType, SIM2_ID);
    }

    @Test
    public void onSubscriptionsChanged_dialogUpdates() {
        final int dialogType = DATA_PICK;
        setDialogType(dialogType);
        mFragment = spy(SimListDialogFragment.newInstance(dialogType, R.string.select_sim_for_data,
                false /* includeAskEveryTime */));
        doReturn(Arrays.asList(mSim1, mSim2)).when(mFragment).getCurrentSubscriptions();
        // Avoid problems robolectric has with our real adapter.
        doNothing().when(mFragment).setAdapter(any());
        startDialog();
        verify(mFragment).updateDialog();

        mFragment.onSubscriptionsChanged();
        verify(mFragment, times(2)).updateDialog();
    }

    @Test
    @Ignore
    public void onCreateDialog_twoSubscriptionsAskEveryTime_threeSubsForDisplay() {
        final int dialogType = SMS_PICK;
        setDialogType(dialogType);
        mFragment = spy(SimListDialogFragment.newInstance(dialogType, R.string.select_sim_for_sms,
                true /* includeAskEveryTime */));
        doReturn(Arrays.asList(mSim1, mSim2)).when(mFragment).getCurrentSubscriptions();
        // Avoid problems robolectric has with our real adapter.
        doNothing().when(mFragment).setAdapter(any());
        final AlertDialog alertDialog = startDialog();
        assertThat(mFragment.mSubscriptions).hasSize(3);
        assertThat(mFragment.mSubscriptions.get(0)).isNull();

        final SimDialogActivity activity = (SimDialogActivity) spy(mFragment.getActivity());
        doReturn(activity).when(mFragment).getActivity();
        doNothing().when(activity).onSubscriptionSelected(anyInt(), anyInt());

        mFragment.onClick(alertDialog, 0);
        verify(activity).onSubscriptionSelected(dialogType,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }
}
