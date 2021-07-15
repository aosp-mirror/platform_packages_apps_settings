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

import static androidx.lifecycle.Lifecycle.Event;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class NetworkProviderDownloadedSimListControllerTest {

    private static final int SUB_ID = 1;
    private static final String KEY_PREFERENCE_DOWNLOADED_SIM =
            "provider_model_downloaded_sim_list";
    private static final String KEY_ADD_MORE = "add_more";
    private static final String DISPLAY_NAME = "Sub 1";

    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    private LifecycleRegistry mLifecycleRegistry;

    private MockNetworkProviderDownloadedSimListController mController;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mPreferenceScreen;
    private Preference mPreference;
    private Preference mAddMorePreference;

    private Context mContext;

    /**
     * Mock the MockNetworkProviderDownloadedSimListController that allows one to set a
     * default voice, SMS and mobile data subscription ID.
     */
    @SuppressWarnings("ClassCanBeStatic")
    private class MockNetworkProviderDownloadedSimListController extends
            com.android.settings.network.NetworkProviderDownloadedSimListController {
        public MockNetworkProviderDownloadedSimListController(Context context,
                Lifecycle lifecycle) {
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
        mPreference.setKey(KEY_PREFERENCE_DOWNLOADED_SIM);
        mController = new MockNetworkProviderDownloadedSimListController(mContext, mLifecycle);
        mAddMorePreference = new Preference(mContext);
        mAddMorePreference.setKey(KEY_ADD_MORE);
        mAddMorePreference.setVisible(true);
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
    }

    private void displayPreferenceWithLifecycle() {
        mLifecycleRegistry.addObserver(mController);
        mPreferenceScreen.addPreference(mPreference);
        mPreferenceScreen.addPreference(mAddMorePreference);
        mController.displayPreference(mPreferenceScreen);
        mLifecycleRegistry.handleLifecycleEvent(Event.ON_RESUME);
    }

    private void setupSubscriptionInfoList(int subId, String displayName,
            SubscriptionInfo subscriptionInfo) {
        when(subscriptionInfo.getSubscriptionId()).thenReturn(subId);
        doReturn(subscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(subId);
        when(subscriptionInfo.getDisplayName()).thenReturn(displayName);
        when(subscriptionInfo.isEmbedded()).thenReturn(true);
    }

    private String setSummaryResId(String resName) {
        return ResourcesUtils.getResourcesString(mContext, resName);
    }

    @Test
    @UiThreadTest
    public void getSummary_inactiveESim() {
        setupSubscriptionInfoList(SUB_ID, DISPLAY_NAME, mSubscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(new ArrayList<>());
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(new ArrayList<>());
        doReturn(false).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID);

        displayPreferenceWithLifecycle();
        String summary = setSummaryResId("sim_category_inactive_sim");

        assertTrue(TextUtils.equals(mController.getSummary(SUB_ID), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_defaultCalls() {
        mController.setDefaultVoiceSubscriptionId(SUB_ID);
        setupSubscriptionInfoList(SUB_ID, DISPLAY_NAME, mSubscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID);

        displayPreferenceWithLifecycle();
        CharSequence defaultCall = SubscriptionUtil.getDefaultSimConfig(mContext, SUB_ID);
        final StringBuilder summary = new StringBuilder();
        summary.append(setSummaryResId("sim_category_active_sim"))
                .append(defaultCall);

        assertTrue(TextUtils.equals(mController.getSummary(SUB_ID), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_defaultCallsAndMobileData() {
        mController.setDefaultVoiceSubscriptionId(SUB_ID);
        mController.setDefaultDataSubscriptionId(SUB_ID);
        setupSubscriptionInfoList(SUB_ID, DISPLAY_NAME, mSubscriptionInfo);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        when(mSubscriptionManager.getAvailableSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo));
        doReturn(true).when(mSubscriptionManager).isActiveSubscriptionId(SUB_ID);

        displayPreferenceWithLifecycle();
        CharSequence defaultCall = SubscriptionUtil.getDefaultSimConfig(mContext, SUB_ID);
        final StringBuilder summary = new StringBuilder();
        summary.append(setSummaryResId("sim_category_active_sim"))
                .append(defaultCall);

        assertTrue(TextUtils.equals(mController.getSummary(SUB_ID), summary));
    }

}
