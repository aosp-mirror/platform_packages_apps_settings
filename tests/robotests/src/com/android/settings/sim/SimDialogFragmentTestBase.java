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

import static com.android.settings.sim.SimDialogActivity.DIALOG_TYPE_KEY;

import static org.mockito.Mockito.when;

import android.content.Intent;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.shadows.androidx.fragment.FragmentController;

public abstract class SimDialogFragmentTestBase<T extends SimDialogFragment> {
    protected static final int SIM1_ID = 111;
    protected static final int SIM2_ID = 222;
    protected static final String SIM1_NAME = "sim111";
    protected static final String SIM2_NAME = "sim222";

    @Mock
    protected SubscriptionManager mSubscriptionManager;
    @Mock
    protected SubscriptionInfo mSim1;
    @Mock
    protected SubscriptionInfo mSim2;

    protected T mFragment;
    protected Intent mIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mIntent = new Intent();

        when(mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(0)).thenReturn(mSim1);
        when(mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(1)).thenReturn(mSim2);

        when(mSim1.getSubscriptionId()).thenReturn(SIM1_ID);
        when(mSim1.getDisplayName()).thenReturn(SIM1_NAME);
        when(mSim2.getSubscriptionId()).thenReturn(SIM2_ID);
        when(mSim2.getDisplayName()).thenReturn(SIM2_NAME);
    }

    protected void setDialogType(int dialogType) {
        mIntent.putExtra(DIALOG_TYPE_KEY, dialogType);
    }

    protected AlertDialog startDialog() {
        final FragmentController controller = FragmentController.of(mFragment,
                SimDialogActivity.class, mIntent);
        controller.create(0 /* containerViewId */, null /* bundle */).start().visible();
        return ShadowAlertDialogCompat.getLatestAlertDialog();
    }
}
