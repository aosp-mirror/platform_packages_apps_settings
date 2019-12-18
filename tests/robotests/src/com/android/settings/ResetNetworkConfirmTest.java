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

import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowRecoverySystem;
import com.android.settings.testutils.shadow.ShadowWifiP2pManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRecoverySystem.class,
        ShadowWifiP2pManager.class, ShadowBluetoothAdapter.class
})
public class ResetNetworkConfirmTest {

    private FragmentActivity mActivity;
    @Mock
    private ResetNetworkConfirm mResetNetworkConfirm;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mResetNetworkConfirm = new ResetNetworkConfirm();
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mResetNetworkConfirm.mActivity = mActivity;
    }

    @After
    public void tearDown() {
        ShadowRecoverySystem.reset();
        ShadowWifiP2pManager.reset();
    }

    @Test
    @Ignore
    public void testResetNetworkData_resetEsim() {
        mResetNetworkConfirm.mEraseEsim = true;

        mResetNetworkConfirm.mFinalClickListener.onClick(null /* View */);
        Robolectric.getBackgroundThreadScheduler().advanceToLastPostedRunnable();

        assertThat(ShadowRecoverySystem.getWipeEuiccCalledCount()).isEqualTo(1);
    }

    @Test
    @Ignore
    public void testResetNetworkData_notResetEsim() {
        mResetNetworkConfirm.mEraseEsim = false;

        mResetNetworkConfirm.mFinalClickListener.onClick(null /* View */);
        Robolectric.getBackgroundThreadScheduler().advanceToLastPostedRunnable();

        assertThat(ShadowRecoverySystem.getWipeEuiccCalledCount()).isEqualTo(0);
    }

    /**
     * Test for WifiP2pManager factoryReset method.
     */
    @Test
    public void testResetNetworkData_resetP2p() {
        mResetNetworkConfirm.p2pFactoryReset(mActivity);

        assertThat(ShadowWifiP2pManager.getFactoryResetCount()).isEqualTo(1);
    }

    @Test
    public void setSubtitle_eraseEsim() {
        mResetNetworkConfirm.mEraseEsim = true;
        mResetNetworkConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.reset_network_confirm, null);

        mResetNetworkConfirm.setSubtitle();

        assertThat(((TextView) mResetNetworkConfirm.mContentView
                .findViewById(R.id.reset_network_confirm)).getText())
                .isEqualTo(mActivity.getString(R.string.reset_network_final_desc_esim));
    }

    @Test
    public void setSubtitle_notEraseEsim() {
        mResetNetworkConfirm.mEraseEsim = false;
        mResetNetworkConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.reset_network_confirm, null);

        mResetNetworkConfirm.setSubtitle();

        assertThat(((TextView) mResetNetworkConfirm.mContentView
                .findViewById(R.id.reset_network_confirm)).getText())
                .isEqualTo(mActivity.getString(R.string.reset_network_final_desc));
    }
}
