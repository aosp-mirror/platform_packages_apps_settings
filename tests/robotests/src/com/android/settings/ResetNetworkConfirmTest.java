/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.wrapper.RecoverySystemWrapper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;

@RunWith(SettingsRobolectricTestRunner.class)
public class ResetNetworkConfirmTest {

    private Activity mActivity;
    @Mock
    private ResetNetworkConfirm mResetNetworkConfirm;
    @Mock
    private RecoverySystemWrapper mRecoverySystem;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mResetNetworkConfirm = spy(new ResetNetworkConfirm());
        mRecoverySystem = spy(new RecoverySystemWrapper());
        ResetNetworkConfirm.mRecoverySystem = mRecoverySystem;
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    @Test
    public void testResetNetworkData_resetEsim() {
        mResetNetworkConfirm.mEraseEsim = true;
        doReturn(true).when(mRecoverySystem).wipeEuiccData(any(Context.class), anyString());

        mResetNetworkConfirm.esimFactoryReset(mActivity, "" /* packageName */);
        Robolectric.getBackgroundThreadScheduler().advanceToLastPostedRunnable();

        Assert.assertNotNull(mResetNetworkConfirm.mEraseEsimTask);
        verify(mRecoverySystem).wipeEuiccData(any(Context.class), anyString());
    }

    @Test
    public void testResetNetworkData_notResetEsim() {
        mResetNetworkConfirm.mEraseEsim = false;

        mResetNetworkConfirm.esimFactoryReset(mActivity, "" /* packageName */);

        Assert.assertNull(mResetNetworkConfirm.mEraseEsimTask);
        verify(mRecoverySystem, never()).wipeEuiccData(any(Context.class), anyString());
    }
}
