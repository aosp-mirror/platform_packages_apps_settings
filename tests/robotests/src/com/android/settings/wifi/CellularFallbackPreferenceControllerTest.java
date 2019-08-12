/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

@RunWith(RobolectricTestRunner.class)
public class CellularFallbackPreferenceControllerTest {
    private static final String KEY_CELLULAR_FALLBACK = "wifi_cellular_data_fallback";

    @Mock
    private SubscriptionManager mSubscriptionManager;

    private CellularFallbackPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowApplication shadowApplication = ShadowApplication.getInstance();
        shadowApplication.setSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE,
                mSubscriptionManager);
        mController = spy(new CellularFallbackPreferenceController(RuntimeEnvironment.application,
                KEY_CELLULAR_FALLBACK));
    }

    @Test
    public void isAvailable_avoidBadWifiConfigIsFalse_shouldReturnTrue() {
        setupMockIcc();
        final Resources resources = mock(Resources.class);

        doReturn(resources).when(mController).getResourcesForSubId(anyInt());
        when(resources.getInteger(
                com.android.internal.R.integer.config_networkAvoidBadWifi))
                .thenReturn(0);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_avoidBadWifiConfigIsTrue_shouldReturnFalse() {
        setupMockIcc();
        final Resources resources = mock(Resources.class);

        doReturn(resources).when(mController).getResourcesForSubId(anyInt());
        when(resources.getInteger(
                com.android.internal.R.integer.config_networkAvoidBadWifi))
                .thenReturn(1);

        assertThat(mController.isAvailable()).isFalse();
    }

    private void setupMockIcc() {
        final List<SubscriptionInfo> subscriptionInfoList = new ArrayList<>();
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        subscriptionInfoList.add(info1);
        subscriptionInfoList.add(info2);
        when(mSubscriptionManager.getActiveSubscriptionInfoList())
                .thenReturn(subscriptionInfoList);
    }
}
