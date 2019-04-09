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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SubscriptionUtilTest {
    @Mock
    private SubscriptionManager mManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getAvailableSubscriptions_nullInfoFromSubscriptionManager_nonNullResult() {
        when(mManager.getSelectableSubscriptionInfoList()).thenReturn(null);
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mManager);
        assertThat(subs).isNotNull();
        assertThat(subs).isEmpty();
    }

    @Test
    public void getAvailableSubscriptions_oneSubscription_oneResult() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(info.getMncString()).thenReturn("fake1234");
        when(mManager.getSelectableSubscriptionInfoList()).thenReturn(Arrays.asList(info));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mManager);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
    }

    @Test
    public void getAvailableSubscriptions_twoSubscriptions_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getMncString()).thenReturn("fake1234");
        when(info2.getMncString()).thenReturn("fake5678");
        when(mManager.getSelectableSubscriptionInfoList()).thenReturn(Arrays.asList(info1, info2));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mManager);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(2);
    }

    @Test
    public void getAvailableSubscriptions_oneSubWithHiddenNetworks_oneResult() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info3 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(1);
        when(info1.getMncString()).thenReturn("fake1234");
        when(mManager.getSelectableSubscriptionInfoList()).thenReturn(
                new ArrayList<>(Arrays.asList(info1, info2, info3)));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mManager);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).getSubscriptionId()).isEqualTo(1);
    }

    @Test
    public void getAvailableSubscriptions_twoSubsWithHiddenNetworks_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info3 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info4 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(1);
        when(info1.getMncString()).thenReturn("fake1234");
        when(info4.getSubscriptionId()).thenReturn(4);
        when(info4.getMncString()).thenReturn("fake5678");
        when(mManager.getSelectableSubscriptionInfoList()).thenReturn(new ArrayList<>(
                Arrays.asList(info1, info2, info3, info4)));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getAvailableSubscriptions(mManager);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(2);
        assertThat(subs.get(0).getSubscriptionId()).isEqualTo(1);
        assertThat(subs.get(1).getSubscriptionId()).isEqualTo(4);
    }

    @Test
    public void getActiveSubscriptions_nullInfoFromSubscriptionManager_nonNullResult() {
        when(mManager.getActiveSubscriptionInfoList(anyBoolean())).thenReturn(null);
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mManager);
        assertThat(subs).isNotNull();
        assertThat(subs).isEmpty();
    }

    @Test
    public void getActiveSubscriptions_oneSubscription_oneResult() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(mManager.getActiveSubscriptionInfoList(anyBoolean())).thenReturn(Arrays.asList(info));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mManager);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(1);
    }

    @Test
    public void getActiveSubscriptions_twoSubscriptions_twoResults() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(mManager.getActiveSubscriptionInfoList(anyBoolean())).thenReturn(
                Arrays.asList(info1, info2));
        final List<SubscriptionInfo> subs = SubscriptionUtil.getActiveSubscriptions(mManager);
        assertThat(subs).isNotNull();
        assertThat(subs).hasSize(2);
    }
}
