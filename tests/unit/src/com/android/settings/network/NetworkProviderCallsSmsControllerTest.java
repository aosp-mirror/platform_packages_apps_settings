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

import static androidx.lifecycle.Lifecycle.Event;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
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
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;


@RunWith(AndroidJUnit4.class)
public class NetworkProviderCallsSmsControllerTest {

    private static final int SUB_ID_1 = 1;
    private static final int SUB_ID_2 = 2;
    private static final String KEY_PREFERENCE_CALLS_SMS = "calls_and_sms";
    private static final String DISPLAY_NAME_1 = "Sub 1";
    private static final String DISPLAY_NAME_2 = "Sub 2";

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo1;
    @Mock
    private SubscriptionInfo mSubscriptionInfo2;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    private LifecycleRegistry mLifecycleRegistry;

    private MockNetworkProviderCallsSmsController mController;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private RestrictedPreference mPreference;

    private Context mContext;

    /**
     * Mock the NetworkProviderCallsSmsController that allows allows one to set a default voice
     * and SMS subscription ID.
     */
    private class MockNetworkProviderCallsSmsController extends
            com.android.settings.network.NetworkProviderCallsSmsController {
        public MockNetworkProviderCallsSmsController(Context context, Lifecycle lifecycle) {
            super(context, lifecycle);
        }

        private int mDefaultVoiceSubscriptionId;
        private int mDefaultSmsSubscriptionId;
        private boolean mIsInService;
        @Override
        protected int getDefaultVoiceSubscriptionId() {
            return mDefaultVoiceSubscriptionId;
        }

        @Override
        protected int getDefaultSmsSubscriptionId() {
            return mDefaultSmsSubscriptionId;
        }

        @Override
        protected boolean isInService(int subId) {
            return mIsInService;
        }

        public void setDefaultVoiceSubscriptionId(int subscriptionId) {
            mDefaultVoiceSubscriptionId = subscriptionId;
        }

        public void setDefaultSmsSubscriptionId(int subscriptionId) {
            mDefaultSmsSubscriptionId = subscriptionId;
        }

        public void setInService(boolean inService) {
            mIsInService = inService;
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
        mPreference = new RestrictedPreference(mContext);
        mPreference.setKey(KEY_PREFERENCE_CALLS_SMS);
        mController = new MockNetworkProviderCallsSmsController(mContext, mLifecycle);
        mController.setInService(true);
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
    }

    private String setSummaryResId(String resName) {
        return ResourcesUtils.getResourcesString(mContext, resName);
    }

    @Test
    @UiThreadTest
    public void getSummary_noSim_returnNoSim() {
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(new ArrayList<>());
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(new ArrayList<>());
        displayPreferenceWithLifecycle();

        assertTrue(TextUtils.equals(mController.getSummary(),
                setSummaryResId("calls_sms_no_sim")));
    }

    @Test
    @UiThreadTest
    public void getSummary_invalidSubId_returnUnavailable() {
        setupSubscriptionInfoList(SubscriptionManager.INVALID_SUBSCRIPTION_ID, DISPLAY_NAME_1,
                mSubscriptionInfo1);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1));
        displayPreferenceWithLifecycle();

        final StringBuilder summary = new StringBuilder();
        summary.append(DISPLAY_NAME_1)
                .append(" (")
                .append(setSummaryResId("calls_sms_temp_unavailable"))
                .append(")");

        assertTrue(TextUtils.equals(mController.getSummary(), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_oneIsInvalidSubIdTwoIsValidSubId_returnOneIsUnavailable() {
        setupSubscriptionInfoList(SubscriptionManager.INVALID_SUBSCRIPTION_ID, DISPLAY_NAME_1,
                mSubscriptionInfo1);
        setupSubscriptionInfoList(SUB_ID_2, DISPLAY_NAME_2, mSubscriptionInfo2);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        displayPreferenceWithLifecycle();

        final StringBuilder summary = new StringBuilder();
        summary.append(DISPLAY_NAME_1)
                .append(" (")
                .append(setSummaryResId("calls_sms_unavailable"))
                .append(")")
                .append(", ")
                .append(DISPLAY_NAME_2);

        assertTrue(TextUtils.equals(mController.getSummary(), summary));
    }



    @Test
    @UiThreadTest
    public void getSummary_oneSubscription_returnDisplayName() {
        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo1);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1));
        displayPreferenceWithLifecycle();

        assertThat(mPreference.getSummary()).isEqualTo(DISPLAY_NAME_1);
    }

    @Test
    @UiThreadTest
    public void getSummary_allSubscriptionsHaveNoPreferredStatus_returnDisplayName() {
        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo1);
        setupSubscriptionInfoList(SUB_ID_2, DISPLAY_NAME_2, mSubscriptionInfo2);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        displayPreferenceWithLifecycle();

        final StringBuilder summary = new StringBuilder();
        summary.append(DISPLAY_NAME_1).append(", ").append(DISPLAY_NAME_2);

        assertTrue(TextUtils.equals(mController.getSummary(), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_oneSubscriptionsIsCallPreferredTwoIsSmsPreferred_returnStatus() {

        mController.setDefaultVoiceSubscriptionId(SUB_ID_1);
        mController.setDefaultSmsSubscriptionId(SUB_ID_2);

        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo1);
        setupSubscriptionInfoList(SUB_ID_2, DISPLAY_NAME_2, mSubscriptionInfo2);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        displayPreferenceWithLifecycle();

        final StringBuilder summary = new StringBuilder();
        summary.append(DISPLAY_NAME_1)
                .append(" (")
                .append(setSummaryResId("calls_sms_calls_preferred"))
                .append(")")
                .append(", ")
                .append(DISPLAY_NAME_2)
                .append(" (")
                .append(setSummaryResId("calls_sms_sms_preferred"))
                .append(")");

        assertTrue(TextUtils.equals(mController.getSummary(), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_oneSubscriptionsIsSmsPreferredTwoIsCallPreferred_returnStatus() {

        mController.setDefaultVoiceSubscriptionId(SUB_ID_2);
        mController.setDefaultSmsSubscriptionId(SUB_ID_1);

        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo1);
        setupSubscriptionInfoList(SUB_ID_2, DISPLAY_NAME_2, mSubscriptionInfo2);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        displayPreferenceWithLifecycle();

        final StringBuilder summary = new StringBuilder();
        summary.append(DISPLAY_NAME_1)
                .append(" (")
                .append(setSummaryResId("calls_sms_sms_preferred"))
                .append(")")
                .append(", ")
                .append(DISPLAY_NAME_2)
                .append(" (")
                .append(setSummaryResId("calls_sms_calls_preferred"))
                .append(")");

        assertTrue(TextUtils.equals(mController.getSummary(), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_oneSubscriptionsIsSmsPreferredAndIsCallPreferred_returnStatus() {

        mController.setDefaultVoiceSubscriptionId(SUB_ID_1);
        mController.setDefaultSmsSubscriptionId(SUB_ID_1);

        setupSubscriptionInfoList(SUB_ID_1, DISPLAY_NAME_1, mSubscriptionInfo1);
        setupSubscriptionInfoList(SUB_ID_2, DISPLAY_NAME_2, mSubscriptionInfo2);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        displayPreferenceWithLifecycle();

        final StringBuilder summary = new StringBuilder();
        summary.append(DISPLAY_NAME_1)
                .append(" (")
                .append(setSummaryResId("calls_sms_preferred"))
                .append(")")
                .append(", ")
                .append(DISPLAY_NAME_2);

        assertTrue(TextUtils.equals(mController.getSummary(), summary));
    }
}
