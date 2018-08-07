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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.spy;

import android.app.Activity;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowRecoverySystem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowRecoverySystem.class})
public class ResetNetworkConfirmTest {

    private Activity mActivity;
    @Mock
    private ResetNetworkConfirm mResetNetworkConfirm;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mResetNetworkConfirm = spy(new ResetNetworkConfirm());
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    @After
    public void tearDown() {
        ShadowRecoverySystem.reset();
    }

    @Test
    public void testResetNetworkData_resetEsim() {
        mResetNetworkConfirm.mEraseEsim = true;

        mResetNetworkConfirm.esimFactoryReset(mActivity, "" /* packageName */);
        Robolectric.getBackgroundThreadScheduler().advanceToLastPostedRunnable();

        assertThat(mResetNetworkConfirm.mEraseEsimTask).isNotNull();
        assertThat(ShadowRecoverySystem.getWipeEuiccCalledCount())
                .isEqualTo(1);
    }

    @Test
    public void testResetNetworkData_notResetEsim() {
        mResetNetworkConfirm.mEraseEsim = false;

        mResetNetworkConfirm.esimFactoryReset(mActivity, "" /* packageName */);

        assertThat(mResetNetworkConfirm.mEraseEsimTask).isNull();
        assertThat(ShadowRecoverySystem.getWipeEuiccCalledCount())
                .isEqualTo(0);
    }
}
