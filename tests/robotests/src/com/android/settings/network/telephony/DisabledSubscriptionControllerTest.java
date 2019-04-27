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

package com.android.settings.network.telephony;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.telephony.SubscriptionManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DisabledSubscriptionControllerTest {

    private static final String KEY = "disabled_subscription_category";
    private static final int SUB_ID = 111;

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private PreferenceScreen mScreen;

    private PreferenceCategory mCategory;
    private Context mContext;
    private Lifecycle mLifecycle;
    private DisabledSubscriptionController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        LifecycleOwner lifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(lifecycleOwner);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        mCategory = new PreferenceCategory(mContext);
        doReturn(mCategory).when(mScreen).findPreference(KEY);
        mController = new DisabledSubscriptionController(mContext, KEY);
        mController.init(mLifecycle, SUB_ID);
    }

    @Test
    public void displayPreference_subscriptionEnabled_categoryIsVisible() {
        doReturn(true).when(mSubscriptionManager).isSubscriptionEnabled(SUB_ID);
        mController.displayPreference(mScreen);
        assertThat(mCategory.isVisible()).isTrue();
    }

    @Test
    public void displayPreference_subscriptionDisabled_categoryIsNotVisible() {
        doReturn(false).when(mSubscriptionManager).isSubscriptionEnabled(SUB_ID);
        mController.displayPreference(mScreen);
        assertThat(mCategory.isVisible()).isFalse();
    }

    @Test
    public void onSubscriptionsChanged_subscriptionBecomesDisabled_categoryIsNotVisible() {
        doReturn(true).when(mSubscriptionManager).isSubscriptionEnabled(SUB_ID);
        mController.displayPreference(mScreen);
        doReturn(false).when(mSubscriptionManager).isSubscriptionEnabled(SUB_ID);
        mController.onSubscriptionsChanged();
        assertThat(mCategory.isVisible()).isFalse();
    }

    @Test
    public void onSubscriptionsChanged_subscriptionBecomesEnabled_categoryIsVisible() {
        doReturn(false).when(mSubscriptionManager).isSubscriptionEnabled(SUB_ID);
        mController.displayPreference(mScreen);
        doReturn(true).when(mSubscriptionManager).isSubscriptionEnabled(SUB_ID);
        mController.onSubscriptionsChanged();
        assertThat(mCategory.isVisible()).isTrue();
    }
}
