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
package com.android.settings.homepage.contextualcards.conditional;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkPolicyManager;

import com.android.settings.Settings;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(SettingsRobolectricTestRunner.class)
public class BackgroundDataConditionControllerTest {

    @Mock
    private ConditionManager mConditionManager;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    private Context mContext;
    private BackgroundDataConditionController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication.getInstance().setSystemService(Context.NETWORK_POLICY_SERVICE,
                mNetworkPolicyManager);
        mContext = spy(RuntimeEnvironment.application);
        mController = new BackgroundDataConditionController(mContext, mConditionManager);
    }

    @Test
    public void onPrimaryClick_shouldReturn2SummaryActivity() {
        final ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        mController.onPrimaryClick(mContext);
        verify(mContext).startActivity(argumentCaptor.capture());
        Intent intent = argumentCaptor.getValue();

        assertThat(intent.getComponent().getClassName()).isEqualTo(
                Settings.DataUsageSummaryActivity.class.getName());
    }

    @Test
    public void onActionClick_shouldRefreshCondition() {
        mController.onActionClick();
        verify(mConditionManager).onConditionChanged();
    }
}
