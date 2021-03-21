/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.telephony.SubscriptionManager;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.wifi.WifiConnectionPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class MultiNetworkHeaderControllerTest {
    private static final String KEY_HEADER = "multi_network_header";
    private static final int EXPANDED_CHILDREN_COUNT = 5;

    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private WifiConnectionPreferenceController mWifiController;
    @Mock
    private SubscriptionsPreferenceController mSubscriptionsController;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private Lifecycle mLifecycle;

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private MultiNetworkHeaderController mHeaderController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        mHeaderController = new MultiNetworkHeaderController(mContext, KEY_HEADER) {
            @Override
            WifiConnectionPreferenceController createWifiController(Lifecycle lifecycle) {
                return mWifiController;
            }

            @Override
            SubscriptionsPreferenceController createSubscriptionsController(Lifecycle lifecycle) {
                return mSubscriptionsController;
            }
        };

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.setInitialExpandedChildrenCount(EXPANDED_CHILDREN_COUNT);
        when(mPreferenceCategory.getKey()).thenReturn(KEY_HEADER);
        when(mPreferenceCategory.getPreferenceCount()).thenReturn(3);
        mPreferenceScreen.addPreference(mPreferenceCategory);
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
        Assert.assertEquals(mPreferenceCategory.isVisible(), setVisibleExpectedValue);
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

        Assert.assertTrue(mPreferenceCategory.isVisible());
        assertThat(mPreferenceScreen.getInitialExpandedChildrenCount()).isEqualTo(
                EXPANDED_CHILDREN_COUNT + mPreferenceCategory.getPreferenceCount());
    }

    @Test
    public void onChildUpdated_subscriptionsBecameUnavailable_categoryIsNotVisible() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        mHeaderController.init(mLifecycle);
        mHeaderController.displayPreference(mPreferenceScreen);

        when(mSubscriptionsController.isAvailable()).thenReturn(false);
        mHeaderController.onChildrenUpdated();

        Assert.assertFalse(mPreferenceCategory.isVisible());
        assertThat(mPreferenceScreen.getInitialExpandedChildrenCount()).isEqualTo(
                EXPANDED_CHILDREN_COUNT);
    }

    @Test
    public void onChildUpdated_noExpandedChildCountAndAvailable_doesNotSetExpandedCount() {
        mPreferenceScreen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);

        when(mSubscriptionsController.isAvailable()).thenReturn(false);
        mHeaderController.init(mLifecycle);
        mHeaderController.displayPreference(mPreferenceScreen);

        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        mHeaderController.onChildrenUpdated();

        // Check that setInitialExpandedChildrenCount was never called.
        Assert.assertEquals(mPreferenceScreen.getInitialExpandedChildrenCount(), Integer.MAX_VALUE);
    }
}
