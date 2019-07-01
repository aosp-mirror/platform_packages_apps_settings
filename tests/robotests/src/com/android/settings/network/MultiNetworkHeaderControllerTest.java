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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionManager;

import com.android.settings.wifi.WifiConnectionPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

@RunWith(RobolectricTestRunner.class)
public class MultiNetworkHeaderControllerTest {
    private static final String KEY_HEADER = "multi_network_header";
    private static final int EXPANDED_CHILDREN_COUNT = 5;

    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private WifiConnectionPreferenceController mWifiController;
    @Mock
    private SubscriptionsPreferenceController mSubscriptionsController;
    @Mock
    private SubscriptionManager mSubscriptionManager;

    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private MultiNetworkHeaderController mHeaderController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mPreferenceScreen.findPreference(eq(KEY_HEADER))).thenReturn(mPreferenceCategory);
        when(mPreferenceCategory.getPreferenceCount()).thenReturn(3);
        when(mPreferenceScreen.getInitialExpandedChildrenCount()).thenReturn(
                EXPANDED_CHILDREN_COUNT);

        mHeaderController = spy(new MultiNetworkHeaderController(mContext, KEY_HEADER));
        doReturn(mWifiController).when(mHeaderController).createWifiController(mLifecycle);
        doReturn(mSubscriptionsController).when(mHeaderController).createSubscriptionsController(
                mLifecycle);
    }

    @Test
    public void isAvailable_beforeInitIsCalled_notAvailable() {
        assertThat(mHeaderController.isAvailable()).isFalse();
    }

    // When calling displayPreference, the header itself should only be visible if the
    // subscriptions controller says it is available. This is a helper for test cases of this logic.
    private void displayPreferenceTest(boolean wifiAvailable, boolean subscriptionsAvailable,
            boolean setVisibleExpectedValue) {
        when(mWifiController.isAvailable()).thenReturn(wifiAvailable);
        when(mSubscriptionsController.isAvailable()).thenReturn(subscriptionsAvailable);

        mHeaderController.init(mLifecycle);
        mHeaderController.displayPreference(mPreferenceScreen);
        verify(mPreferenceCategory, never()).setVisible(eq(!setVisibleExpectedValue));
        verify(mPreferenceCategory, atLeastOnce()).setVisible(eq(setVisibleExpectedValue));
    }

    @Test
    public void displayPreference_bothNotAvailable_categoryIsNotVisible() {
        displayPreferenceTest(false, false, false);
    }

    @Test
    public void displayPreference_wifiAvailableButNotSubscriptions_categoryIsNotVisible() {
        displayPreferenceTest(true, false, false);
    }

    @Test
    public void displayPreference_subscriptionsAvailableButNotWifi_categoryIsVisible() {
        displayPreferenceTest(false, true, true);
    }

    @Test
    public void displayPreference_bothAvailable_categoryIsVisible() {
        displayPreferenceTest(true, true, true);
    }

    @Test
    public void onChildUpdated_subscriptionsBecameAvailable_categoryIsVisible() {
        when(mSubscriptionsController.isAvailable()).thenReturn(false);
        mHeaderController.init(mLifecycle);
        mHeaderController.displayPreference(mPreferenceScreen);

        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        mHeaderController.onChildrenUpdated();
        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);

        verify(mPreferenceCategory, atLeastOnce()).setVisible(captor.capture());
        List<Boolean> values = captor.getAllValues();
        assertThat(values.get(values.size()-1)).isEqualTo(Boolean.TRUE);

        ArgumentCaptor<Integer> expandedCountCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mPreferenceScreen).setInitialExpandedChildrenCount(expandedCountCaptor.capture());
        assertThat(expandedCountCaptor.getValue()).isEqualTo(
                EXPANDED_CHILDREN_COUNT + mPreferenceCategory.getPreferenceCount());
    }

    @Test
    public void onChildUpdated_subscriptionsBecameUnavailable_categoryIsNotVisible() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        mHeaderController.init(mLifecycle);
        mHeaderController.displayPreference(mPreferenceScreen);

        when(mSubscriptionsController.isAvailable()).thenReturn(false);
        mHeaderController.onChildrenUpdated();
        ArgumentCaptor<Boolean> captor = ArgumentCaptor.forClass(Boolean.class);

        verify(mPreferenceCategory, atLeastOnce()).setVisible(captor.capture());
        List<Boolean> values = captor.getAllValues();
        assertThat(values.get(values.size()-1)).isEqualTo(Boolean.FALSE);

        ArgumentCaptor<Integer> expandedCountCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mPreferenceScreen).setInitialExpandedChildrenCount(expandedCountCaptor.capture());
        assertThat(expandedCountCaptor.getValue()).isEqualTo(EXPANDED_CHILDREN_COUNT);
    }

    @Test
    public void onChildUpdated_noExpandedChildCountAndAvailable_doesNotSetExpandedCount() {
        when(mPreferenceScreen.getInitialExpandedChildrenCount()).thenReturn(Integer.MAX_VALUE);

        when(mSubscriptionsController.isAvailable()).thenReturn(false);
        mHeaderController.init(mLifecycle);
        mHeaderController.displayPreference(mPreferenceScreen);

        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        mHeaderController.onChildrenUpdated();

        verify(mPreferenceScreen, never()).setInitialExpandedChildrenCount(anyInt());
    }
}
