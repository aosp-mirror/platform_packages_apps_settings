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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Intent;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.view.View;

import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.widget.RtlCompatibleViewPager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.androidx.fragment.FragmentController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class WifiCallingSettingsTest {

    private WifiCallingSettings mFragment;

    @Before
    public void setUp() {
        mFragment = spy(new WifiCallingSettings());
    }

    @Test
    public void setupFragment_noSubscriptions_noCrash() {
        FragmentController.setupFragment(mFragment, FragmentActivity.class, 0 /* containerViewId*/,
                null /* bundle */);
    }

    @Test
    public void setupFragment_oneSubscription_noCrash() {
        SubscriptionInfo info = mock(SubscriptionInfo.class);
        when(info.getSubscriptionId()).thenReturn(111);

        SubscriptionUtil.setActiveSubscriptionsForTesting(new ArrayList<>(
                Collections.singletonList(info)));
        doReturn(true).when(mFragment).isWfcEnabledByPlatform(any(SubscriptionInfo.class));

        Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, info.getSubscriptionId());
        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        View view = mFragment.getView();
        RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(1);
    }

    @Test
    public void setupFragment_twoSubscriptions_correctSelection() {
        SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(111);
        when(info2.getSubscriptionId()).thenReturn(222);

        SubscriptionUtil.setActiveSubscriptionsForTesting(new ArrayList<>(
                Arrays.asList(info1, info2)));
        doReturn(true).when(mFragment).isWfcEnabledByPlatform(any(SubscriptionInfo.class));

        Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, info2.getSubscriptionId());
        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        View view = mFragment.getView();
        RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(1);

        WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(2);
    }

    @Test
    public void setupFragment_twoSubscriptionsOneNotProvisionedOnDevice_oneResult() {
        SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info1.getSubscriptionId()).thenReturn(111);
        when(info2.getSubscriptionId()).thenReturn(222);

        SubscriptionUtil.setActiveSubscriptionsForTesting(new ArrayList<>(
                Arrays.asList(info1, info2)));
        doReturn(true).when(mFragment).isWfcEnabledByPlatform(any(SubscriptionInfo.class));
        doReturn(false).when(mFragment).isWfcProvisionedOnDevice(eq(info2));

        Intent intent = new Intent();
        intent.putExtra(Settings.EXTRA_SUB_ID, info1.getSubscriptionId());
        FragmentController.of(mFragment, intent).create(0 /* containerViewId*/,
                null /* bundle */).start().resume().visible().get();

        View view = mFragment.getView();
        RtlCompatibleViewPager pager = view.findViewById(R.id.view_pager);
        assertThat(pager.getCurrentItem()).isEqualTo(0);

        WifiCallingSettings.WifiCallingViewPagerAdapter adapter =
                (WifiCallingSettings.WifiCallingViewPagerAdapter) pager.getAdapter();
        assertThat(adapter.getCount()).isEqualTo(1);
    }
}
