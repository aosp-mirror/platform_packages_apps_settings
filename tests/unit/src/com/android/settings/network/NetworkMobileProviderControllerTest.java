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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.network.NetworkMobileProviderController.PREF_KEY_PROVIDER_MOBILE_NETWORK;

import static com.google.common.truth.Truth.assertThat;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit test for NetworkMobileProviderController.
 *
 * {@link NetworkMobileProviderController} is used to show subscription status on internet page for
 * provider model. This original class can refer to {@link MultiNetworkHeaderController}, and
 * NetworkMobileProviderControllerTest can also refer to {@link MultiNetworkHeaderControllerTest}.
 */
@RunWith(AndroidJUnit4.class)
public class NetworkMobileProviderControllerTest {

    private static final int EXPANDED_CHILDREN_COUNT = 3;

    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private SubscriptionsPreferenceController mSubscriptionsController;

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private NetworkMobileProviderController mNetworkMobileProviderController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        when(mPreferenceCategory.getKey()).thenReturn(PREF_KEY_PROVIDER_MOBILE_NETWORK);
        when(mPreferenceCategory.getPreferenceCount()).thenReturn(3);

        mContext = ApplicationProvider.getApplicationContext();
        mNetworkMobileProviderController =
                new NetworkMobileProviderController(mContext, PREF_KEY_PROVIDER_MOBILE_NETWORK) {
            @Override
            SubscriptionsPreferenceController createSubscriptionsController(
                    Lifecycle lifecycle) {
                return mSubscriptionsController;
            }
        };

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.setInitialExpandedChildrenCount(EXPANDED_CHILDREN_COUNT);
        mPreferenceScreen.addPreference(mPreferenceCategory);
    }

    @Test
    public void testDisplayPreference_subscriptionsControllerAvailable() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        setupNetworkMobileProviderController();

        assertTrue(mPreferenceCategory.isVisible());
    }

    @Test
    public void testDisplayPreference_subscriptionsControllerUnAvailable() {
        when(mSubscriptionsController.isAvailable()).thenReturn(false);
        setupNetworkMobileProviderController();

        assertFalse(mPreferenceCategory.isVisible());
    }

    @Test
    public void testGetAvailabilityStatus_subscriptionsControllerIsNull() {
        when(mSubscriptionsController.isAvailable()).thenReturn(false);
        mNetworkMobileProviderController = new NetworkMobileProviderController(mContext,
                PREF_KEY_PROVIDER_MOBILE_NETWORK) {
            @Override
            SubscriptionsPreferenceController createSubscriptionsController(Lifecycle lifecycle) {
                return null;
            }
        };
        setupNetworkMobileProviderController();

        final int result = mNetworkMobileProviderController.getAvailabilityStatus();

        assertEquals(result, CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void testGetAvailabilityStatus_subscriptionsControllerAvailable() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        setupNetworkMobileProviderController();

        final int result = mNetworkMobileProviderController.getAvailabilityStatus();

        assertEquals(result, AVAILABLE);
    }

    @Test
    public void testOnChildUpdated_subscriptionsControllerAvailable_categoryIsVisible() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        setupNetworkMobileProviderController();

        mNetworkMobileProviderController.onChildrenUpdated();

        assertTrue(mPreferenceCategory.isVisible());
        assertThat(mPreferenceScreen.getInitialExpandedChildrenCount()).isEqualTo(
                EXPANDED_CHILDREN_COUNT + mPreferenceCategory.getPreferenceCount());
    }

    @Test
    public void testOnChildUpdated_subscriptionsControllerUnavailable_categoryIsInvisible() {
        when(mSubscriptionsController.isAvailable()).thenReturn(false);
        setupNetworkMobileProviderController();

        mNetworkMobileProviderController.onChildrenUpdated();

        assertFalse(mPreferenceCategory.isVisible());
        assertThat(mPreferenceScreen.getInitialExpandedChildrenCount()).isEqualTo(
                EXPANDED_CHILDREN_COUNT);
    }

    @Test
    public void testOnChildUpdated_noExpandedChildCountAndAvailable_doesNotSetExpandedCount() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        mPreferenceScreen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
        setupNetworkMobileProviderController();

        mNetworkMobileProviderController.onChildrenUpdated();

        assertEquals(mPreferenceScreen.getInitialExpandedChildrenCount(), Integer.MAX_VALUE);
    }

    @Test
    public void hidePreference_hidePreferenceTrue_preferenceIsNotVisible() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        setupNetworkMobileProviderController();
        mPreferenceCategory.setVisible(true);

        mNetworkMobileProviderController.hidePreference(true /* hide */, true /* immediately*/);

        assertThat(mPreferenceCategory.isVisible()).isFalse();
    }

    @Test
    public void hidePreference_hidePreferenceFalse_preferenceIsVisible() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        setupNetworkMobileProviderController();

        mNetworkMobileProviderController.hidePreference(false /* hide */, true /* immediately*/);

        assertThat(mPreferenceCategory.isVisible()).isTrue();
    }

    @Test
    public void hidePreference_hidePreferenceFalse_preferenceIsNotVisibleImmediately() {
        when(mSubscriptionsController.isAvailable()).thenReturn(true);
        setupNetworkMobileProviderController();
        mPreferenceCategory.setVisible(false);

        mNetworkMobileProviderController.hidePreference(false /* hide */, false /* immediately*/);

        // The preference is not visible immediately.
        assertThat(mPreferenceCategory.isVisible()).isFalse();

        mNetworkMobileProviderController.displayPreference(mPreferenceScreen);

        // The preference is visible after displayPreference() updated.
        assertThat(mPreferenceCategory.isVisible()).isTrue();
    }

    private void setupNetworkMobileProviderController() {
        mNetworkMobileProviderController.init(mLifecycle);
        mNetworkMobileProviderController.displayPreference(mPreferenceScreen);
    }
}
