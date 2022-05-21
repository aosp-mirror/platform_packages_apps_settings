/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.google.common.truth.Truth.assertThat;

import static androidx.lifecycle.Lifecycle.Event;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.PreferenceManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class NetworkProviderSimListControllerTest {

    private static final int SUB_ID_1 = 1;
    private static final String KEY_PREFERENCE_SIM_LIST = "provider_model_sim_list";
    private static final String DISPLAY_NAME_1 = "Sub 1";

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    private LifecycleRegistry mLifecycleRegistry;

    private MockNetworkProviderSimListController mController;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private Preference mPreference;

    private Context mContext;

    /**
     * Mock the NetworkProviderSimListController that allows one to set a default voice,
     * SMS and mobile data subscription ID.
     */
    @SuppressWarnings("ClassCanBeStatic")
    private class MockNetworkProviderSimListController extends
            com.android.settings.network.NetworkProviderSimListController {
        public MockNetworkProviderSimListController(Context context, Lifecycle lifecycle) {
            super(context, lifecycle);
        }

        private int mDefaultVoiceSubscriptionId;
        private int mDefaultSmsSubscriptionId;
        private int mDefaultDataSubscriptionId;

        @Override
        protected int getDefaultVoiceSubscriptionId() {
            return mDefaultVoiceSubscriptionId;
        }

        @Override
        protected int getDefaultSmsSubscriptionId() {
            return mDefaultSmsSubscriptionId;
        }

        @Override
        protected int getDefaultDataSubscriptionId() {
            return mDefaultDataSubscriptionId;
        }

        public void setDefaultVoiceSubscriptionId(int subscriptionId) {
            mDefaultVoiceSubscriptionId = subscriptionId;
        }

        public void setDefaultSmsSubscriptionId(int subscriptionId) {
            mDefaultSmsSubscriptionId = subscriptionId;
        }

        public void setDefaultDataSubscriptionId(int subscriptionId) {
            mDefaultDataSubscriptionId = subscriptionId;
        }

    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(KEY_PREFERENCE_SIM_LIST);
        mController = new MockNetworkProviderSimListController(mContext, mLifecycle);
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
    }

    private void displayPreferenceWithLifecycle() {
        mLifecycleRegistry.addObserver(mController);
        mPreferenceScreen.addPreference(mPreference);
        mController.displayPreference(mPreferenceScreen);
        mLifecycleRegistry.handleLifecycleEvent(Event.ON_RESUME);
    }

    private void setupSubscriptionInfoList(int subId, String displayName,
            SubscriptionInfo subscriptionInfo) {
        when(subscriptionInfo.getSubscriptionId()).thenReturn(subId);
        doReturn(subscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(subId);
        when(subscriptionInfo.getDisplayName()).thenReturn(displayName);
        when(subscriptionInfo.isEmbedded()).thenReturn(false);
    }

    private String setSummaryResId(String resName, String value) {
        return ResourcesUtils.getResourcesString(mContext, resName, value);
    }

    @Test
    @UiThreadTest
    public void getSummary_tapToActivePSim() {
        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(new ArrayList<>());
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(new ArrayList<>());
        doReturn(false).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID_1);

        displayPreferenceWithLifecycle();
        String summary = setSummaryResId("mobile_network_tap_to_activate", DISPLAY_NAME_1);

        assertTrue(TextUtils.equals(mController.getSummary(SUB_ID_1, DISPLAY_NAME_1), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_inactivePSim() {
        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(new ArrayList<>());
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(new ArrayList<>());
        doReturn(false).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID_1);
        doReturn(true).when(mSubscriptionManager).canDisablePhysicalSubscription();

        displayPreferenceWithLifecycle();
        String summary = setSummaryResId("sim_category_inactive_sim", null);

        assertTrue(TextUtils.equals(mController.getSummary(SUB_ID_1, DISPLAY_NAME_1), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_defaultCalls() {
        mController.setDefaultVoiceSubscriptionId(SUB_ID_1);
        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID_1);

        displayPreferenceWithLifecycle();
        CharSequence defaultCall = SubscriptionUtil.getDefaultSimConfig(mContext, SUB_ID_1);
        final StringBuilder summary = new StringBuilder();
        summary.append(setSummaryResId("sim_category_active_sim", null))
                .append(defaultCall);

        assertTrue(TextUtils.equals(mController.getSummary(SUB_ID_1, DISPLAY_NAME_1), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_defaultCallsAndSms() {
        mController.setDefaultVoiceSubscriptionId(SUB_ID_1);
        mController.setDefaultSmsSubscriptionId(SUB_ID_1);
        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID_1);

        displayPreferenceWithLifecycle();
        CharSequence defaultCall = SubscriptionUtil.getDefaultSimConfig(mContext, SUB_ID_1);
        final StringBuilder summary = new StringBuilder();
        summary.append(setSummaryResId("sim_category_active_sim", null))
                .append(defaultCall);

        assertTrue(TextUtils.equals(mController.getSummary(SUB_ID_1, DISPLAY_NAME_1), summary));
    }

    @Ignore
    @Test
    @UiThreadTest
    public void getAvailablePhysicalSubscription_withTwoPhysicalSims_returnTwo() {
        final SubscriptionInfo info1 = mock(SubscriptionInfo.class);
        when(info1.isEmbedded()).thenReturn(false);
        final SubscriptionInfo info2 = mock(SubscriptionInfo.class);
        when(info2.isEmbedded()).thenReturn(false);
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(info1,  info2));
        displayPreferenceWithLifecycle();

        assertThat(mController.getAvailablePhysicalSubscription().size()).isEqualTo(2);
    }

}
