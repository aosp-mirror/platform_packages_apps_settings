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

package com.android.settings.wifi.calling;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.android.ims.ImsManager;
import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.ims.MockWifiCallingQueryImsState;
import com.android.settings.widget.RtlCompatibleViewPager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class WifiCallingSettingsTest {
    private static final int SUB_ID1 = 111;
    private static final int SUB_ID2 = 222;

    private Context mContext;

    @Mock
    private ImsManager mImsManager;

    private WifiCallingSettings mFragment;

    private MockWifiCallingQueryImsState mQueryImsState1;
    private MockWifiCallingQueryImsState mQueryImsState2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);

        mQueryImsState1 = new MockWifiCallingQueryImsState(mContext, SUB_ID1);
        mQueryImsState2 = new MockWifiCallingQueryImsState(mContext, SUB_ID2);
        mQueryImsState1.setIsEnabledByUser(true);
        mQueryImsState2.setIsEnabledByUser(true);
        mQueryImsState1.setIsEnabledByPlatform(true);
        mQueryImsState2.setIsEnabledByPlatform(true);
        mQueryImsState1.setIsProvisionedOnDevice(true);
        mQueryImsState2.setIsProvisionedOnDevice(true);

        mFragment = spy(new WifiCallingSettings());
        doReturn(mQueryImsState1).when(mFragment).queryImsState(SUB_ID1);
        doReturn(mQueryImsState2).when(mFragment).queryImsState(SUB_ID2);
    }

    @Test
    public void setupFragment_noSubscriptions_noCrash() {
        FragmentController.setupFragment(mFragment, FragmentActivity.class, 0 /* containerViewId*/,
                null /* bundle */);
    }

    @Test
    public void setupFragment_oneSubscription_noCrash() {
        final SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(info.getSubscriptionId()).thenReturn(SUB_ID1);

        SubscriptionUtil.setActiveSubscriptionsForTesting(new ArrayList<>(
                Collections.singletonList(info)));
        mQueryImsState1.setIsEnabledByPlatform(true);
        mQueryImsState1.setIsProvisionedOnDevice(true);

        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, info.getSubscriptionId());
        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        final View view = mFragment.getView();
        final RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        final WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(1);
    }

    @Test
    public void setupFragment_twoSubscriptions_correctSelection() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUB_ID1);
        when(info2.getSubscriptionId()).thenReturn(SUB_ID2);

        SubscriptionUtil.setActiveSubscriptionsForTesting(new ArrayList<>(
                Arrays.asList(info1, info2)));

        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, info2.getSubscriptionId());
        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        final View view = mFragment.getView();
        final RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(1);

        final WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(2);
    }

    @Test
    public void setupFragment_twoSubscriptionsOneNotProvisionedOnDevice_oneResult() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(SUB_ID1);
        when(info2.getSubscriptionId()).thenReturn(SUB_ID2);

        SubscriptionUtil.setActiveSubscriptionsForTesting(new ArrayList<>(
                Arrays.asList(info1, info2)));
        mQueryImsState2.setIsProvisionedOnDevice(false);

        final Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, info1.getSubscriptionId());
        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        final View view = mFragment.getView();
        final RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(0);

        final WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(1);
    }
}
