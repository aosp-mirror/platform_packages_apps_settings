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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.telephony.SubscriptionInfo;

import com.android.settings.network.telephony.MobileNetworkActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class MobileNetworkSummaryControllerTest {
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    PreferenceScreen mPreferenceScreen;

    Preference mPreference;

    private Context mContext;
    private MobileNetworkSummaryController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(Robolectric.setupActivity(Activity.class));
        mController = new MobileNetworkSummaryController(mContext, mLifecycle);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        when(mPreferenceScreen.findPreference(eq(mController.getPreferenceKey()))).thenReturn(
                mPreference);
    }

    @After
    public void tearDown() {
        SubscriptionUtil.setAvailableSubscriptionsForTesting(null);
    }

    @Test
    public void getSummary_noSubscriptions_correctSummary() {
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mController.getSummary()).isEqualTo("Add a network");
    }

    @Test
    public void getSummary_oneSubscription_correctSummaryAndClickHandler() {
        SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        when(sub1.getSubscriptionId()).thenReturn(1);
        when(sub1.getDisplayName()).thenReturn("sub1");
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mController.getSummary()).isEqualTo("sub1");
        assertThat(mPreference.getFragment()).isNull();
        mPreference.getOnPreferenceClickListener().onPreferenceClick(mPreference);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getComponent().getClassName()).isEqualTo(
                MobileNetworkActivity.class.getName());
    }

    @Test
    public void getSummary_twoSubscriptions_correctSummaryAndFragment() {
        SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        when(sub1.getSubscriptionId()).thenReturn(1);
        when(sub2.getSubscriptionId()).thenReturn(2);

        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mController.getSummary()).isEqualTo("2 SIMs");
        assertThat(mPreference.getFragment()).isEqualTo(MobileNetworkListFragment.class.getName());
    }

    @Test
    public void getSummaryAfterUpdate_twoSubscriptionsBecomesOne_correctSummaryAndFragment() {
        SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        when(sub1.getSubscriptionId()).thenReturn(1);
        when(sub2.getSubscriptionId()).thenReturn(2);
        when(sub1.getDisplayName()).thenReturn("sub1");
        when(sub2.getDisplayName()).thenReturn("sub2");

        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mController.getSummary()).isEqualTo("2 SIMs");
        assertThat(mPreference.getFragment()).isEqualTo(MobileNetworkListFragment.class.getName());

        // Simulate sub2 having disappeared - the end result should change to be the same as
        // if there were just one subscription.
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.onSubscriptionsChanged();
        assertThat(mController.getSummary()).isEqualTo("sub1");
        assertThat(mPreference.getFragment()).isNull();
        mPreference.getOnPreferenceClickListener().onPreferenceClick(mPreference);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getComponent().getClassName()).isEqualTo(
                MobileNetworkActivity.class.getName());
    }

    @Test
    public void getSummaryAfterUpdate_oneSubscriptionBecomesTwo_correctSummaryAndFragment() {
        SubscriptionInfo sub1 = mock(SubscriptionInfo.class);
        SubscriptionInfo sub2 = mock(SubscriptionInfo.class);
        when(sub1.getSubscriptionId()).thenReturn(1);
        when(sub2.getSubscriptionId()).thenReturn(2);
        when(sub1.getDisplayName()).thenReturn("sub1");
        when(sub2.getDisplayName()).thenReturn("sub2");

        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mController.getSummary()).isEqualTo("sub1");
        assertThat(mPreference.getFragment()).isNull();
        mPreference.getOnPreferenceClickListener().onPreferenceClick(mPreference);
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mContext).startActivity(intentCaptor.capture());
        assertThat(intentCaptor.getValue().getComponent().getClassName()).isEqualTo(
                MobileNetworkActivity.class.getName());

        // Simulate sub2 appearing in the list of subscriptions and check the results.
        SubscriptionUtil.setAvailableSubscriptionsForTesting(Arrays.asList(sub1, sub2));
        mController.displayPreference(mPreferenceScreen);
        mController.onResume();
        assertThat(mController.getSummary()).isEqualTo("2 SIMs");
        assertThat(mPreference.getFragment()).isEqualTo(MobileNetworkListFragment.class.getName());
    }
}
