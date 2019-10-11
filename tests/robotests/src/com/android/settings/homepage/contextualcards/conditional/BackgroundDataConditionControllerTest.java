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

import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkPolicyManager;

import com.android.settings.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class BackgroundDataConditionControllerTest {

    @Mock
    private ConditionManager mConditionManager;
    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    private Activity mActivity;
    private BackgroundDataConditionController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication.getInstance().setSystemService(Context.NETWORK_POLICY_SERVICE,
                mNetworkPolicyManager);
        mActivity = Robolectric.setupActivity(Activity.class);
        mController = new BackgroundDataConditionController(mActivity, mConditionManager);
    }

    @Test
    public void onPrimaryClick_shouldReturn2SummaryActivity() {
        final ComponentName componentName =
                new ComponentName(mActivity, Settings.DataUsageSummaryActivity.class);

        mController.onPrimaryClick(mActivity);

        final ShadowActivity shadowActivity = Shadow.extract(mActivity);
        assertThat(shadowActivity.getNextStartedActivity().getComponent()).isEqualTo(componentName);
    }

    @Test
    public void onActionClick_shouldRefreshCondition() {
        mController.onActionClick();
        verify(mConditionManager).onConditionChanged();
    }
}
