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

import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowRecoverySystem;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.util.concurrent.PausedExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowPausedAsyncTask;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowRecoverySystem.class, ShadowBluetoothAdapter.class})
public class ResetNetworkConfirmTest {
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String TEST_PACKAGE = "com.android.settings";

    private FragmentActivity mActivity;

    @Mock
    private ResetNetworkConfirm mResetNetworkConfirm;
    private PausedExecutorService mExecutorService;

    @Before
    public void setUp() {
        mExecutorService = new PausedExecutorService();
        ShadowPausedAsyncTask.overrideExecutor(mExecutorService);
        mResetNetworkConfirm = new ResetNetworkConfirm();
        mActivity = spy(Robolectric.setupActivity(FragmentActivity.class));
        mResetNetworkConfirm.mActivity = mActivity;
    }

    @After
    public void tearDown() {
        ShadowRecoverySystem.reset();
    }

    @Test
    public void testResetNetworkData_notResetEsim() {
        mResetNetworkConfirm.mResetNetworkRequest =
                new ResetNetworkRequest(ResetNetworkRequest.RESET_NONE);
        mResetNetworkConfirm.mResetSubscriptionContract =
                new ResetSubscriptionContract(mActivity,
                mResetNetworkConfirm.mResetNetworkRequest) {
            @Override
            public void onSubscriptionInactive(int subscriptionId) {
                mActivity.onBackPressed();
            }
        };

        mResetNetworkConfirm.mFinalClickListener.onClick(null /* View */);
        mExecutorService.runAll();
        ShadowLooper.idleMainLooper();

        assertThat(ShadowRecoverySystem.getWipeEuiccCalledCount()).isEqualTo(0);
    }

    @Test
    public void setSubtitle_eraseEsim() {
        mResetNetworkConfirm.mResetNetworkRequest =
                new ResetNetworkRequest(ResetNetworkRequest.RESET_NONE);
        mResetNetworkConfirm.mResetNetworkRequest.setResetEsim(TEST_PACKAGE);

        mResetNetworkConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.reset_network_confirm, null);

        mResetNetworkConfirm.setSubtitle();

        assertThat(((TextView) mResetNetworkConfirm.mContentView
                .findViewById(R.id.reset_network_confirm)).getText())
                .isEqualTo(mActivity.getString(R.string.reset_network_final_desc_esim));
    }

    @Test
    public void setSubtitle_notEraseEsim() {
        mResetNetworkConfirm.mResetNetworkRequest =
                new ResetNetworkRequest(ResetNetworkRequest.RESET_NONE);

        mResetNetworkConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.reset_network_confirm, null);

        mResetNetworkConfirm.setSubtitle();

        assertThat(((TextView) mResetNetworkConfirm.mContentView
                .findViewById(R.id.reset_network_confirm)).getText())
                .isEqualTo(mActivity.getString(R.string.reset_network_final_desc));
    }
}
