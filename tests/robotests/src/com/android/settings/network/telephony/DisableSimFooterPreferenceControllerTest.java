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
 * limitations under the License.
 */

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import com.android.settings.network.SubscriptionUtil;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class DisableSimFooterPreferenceControllerTest {
    private static final String PREF_KEY = "pref_key";
    private static final int SUB_ID = 111;

    @Mock
    private SubscriptionInfo mInfo;

    private Context mContext;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    private DisableSimFooterPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        when(mInfo.getSubscriptionId()).thenReturn(SUB_ID);
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(mInfo));
        mController = new DisableSimFooterPreferenceController(mContext, PREF_KEY);
    }

    @Test
    public void isAvailable_noInit_notAvailable() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_eSIM_notAvailable() {
        when(mInfo.isEmbedded()).thenReturn(true);
        mController.init(SUB_ID);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_pSIM_available_cannot_disable_pSIM() {
        when(mInfo.isEmbedded()).thenReturn(false);
        mController.init(SUB_ID);
        doReturn(false).when(mSubscriptionManager).canDisablePhysicalSubscription();
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_pSIM_available_can_disable_pSIM() {
        when(mInfo.isEmbedded()).thenReturn(false);
        mController.init(SUB_ID);
        doReturn(true).when(mSubscriptionManager).canDisablePhysicalSubscription();
        assertThat(mController.isAvailable()).isFalse();
    }
}
