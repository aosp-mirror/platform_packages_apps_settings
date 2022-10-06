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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
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
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;


@RunWith(AndroidJUnit4.class)
public class NetworkProviderCallsSmsControllerTest {

    private static final String SUB_ID_1 = "1";
    private static final String SUB_ID_2 = "2";
    private static final String INVALID_SUB_ID = "-1";
    private static final String KEY_PREFERENCE_CALLS_SMS = "calls_and_sms";
    private static final String DISPLAY_NAME_1 = "Sub 1";
    private static final String DISPLAY_NAME_2 = "Sub 2";
    private static final String SUB_MCC_1 = "123";
    private static final String SUB_MNC_1 = "456";
    private static final String SUB_MCC_2 = "223";
    private static final String SUB_MNC_2 = "456";
    private static final String SUB_COUNTRY_ISO_1 = "Sub 1";
    private static final String SUB_COUNTRY_ISO_2 = "Sub 2";

    @Mock
    private SubscriptionInfoEntity mSubInfo1;
    @Mock
    private SubscriptionInfoEntity mSubInfo2;
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
    private List<SubscriptionInfoEntity> mSubscriptionInfoEntityList = new ArrayList<>();

    /**
     * Mock the NetworkProviderCallsSmsController that allows one to set a default voice
     * and SMS subscription ID.
     */
    private class MockNetworkProviderCallsSmsController extends
            com.android.settings.network.NetworkProviderCallsSmsController {
        public MockNetworkProviderCallsSmsController(Context context, Lifecycle lifecycle,
                LifecycleOwner lifecycleOwner) {
            super(context, lifecycle, lifecycleOwner);
        }

        private List<SubscriptionInfoEntity> mSubscriptionInfoEntity;
        private boolean mIsInService;

        @Override
        protected List<SubscriptionInfoEntity> getSubscriptionInfoList() {
            return mSubscriptionInfoEntity;
        }

        public void setSubscriptionInfoList(List<SubscriptionInfoEntity> list) {
            mSubscriptionInfoEntity = list;
        }

        @Override
        protected boolean isInService(int subId) {
            return mIsInService;
        }

        public void setInService(boolean inService) {
            mIsInService = inService;
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreference = new RestrictedPreference(mContext);
        mPreference.setKey(KEY_PREFERENCE_CALLS_SMS);
        mController = new MockNetworkProviderCallsSmsController(mContext, mLifecycle,
                mLifecycleOwner);
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

    private String setSummaryResId(String resName) {
        return ResourcesUtils.getResourcesString(mContext, resName);
    }

    @Test
    @UiThreadTest
    public void getSummary_noSim_returnNoSim() {
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
        displayPreferenceWithLifecycle();

        assertTrue(TextUtils.equals(mController.getSummary(),
                setSummaryResId("calls_sms_no_sim")));
    }

    private SubscriptionInfoEntity setupSubscriptionInfoEntity(String subId, int slotId,
            int carrierId, String displayName, String mcc, String mnc, String countryIso,
            int cardId, boolean isValid, boolean isActive, boolean isAvailable,
            boolean isDefaultCall, boolean isDefaultSms) {
        return new SubscriptionInfoEntity(subId, slotId, carrierId,
                displayName, displayName, 0, mcc, mnc, countryIso, false, cardId,
                TelephonyManager.DEFAULT_PORT_INDEX, false, null,
                SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM, displayName, false,
                "1234567890", true, "default", false, isValid,
                true, isActive, isAvailable, isDefaultCall, isDefaultSms, false, false,
                false);
    }

    @Test
    @UiThreadTest
    public void getSummary_invalidSubId_returnUnavailable() {

        mSubInfo1 = setupSubscriptionInfoEntity(INVALID_SUB_ID,
                SubscriptionManager.INVALID_SIM_SLOT_INDEX, TelephonyManager.UNKNOWN_CARRIER_ID,
                DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1, SUB_COUNTRY_ISO_1,
                TelephonyManager.UNINITIALIZED_CARD_ID, false, true, true, false, false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
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

        mSubInfo1 = setupSubscriptionInfoEntity(INVALID_SUB_ID,
                SubscriptionManager.INVALID_SIM_SLOT_INDEX, TelephonyManager.UNKNOWN_CARRIER_ID,
                DISPLAY_NAME_1, SUB_MCC_1, SUB_MNC_1, SUB_COUNTRY_ISO_1,
                TelephonyManager.UNINITIALIZED_CARD_ID, false, true, true, false, false);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, 1, 1, DISPLAY_NAME_2, SUB_MCC_2,
                SUB_MNC_2, SUB_COUNTRY_ISO_2, 1, true, true, true, false, false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
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

        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true, false, false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
        displayPreferenceWithLifecycle();

        assertThat(mPreference.getSummary()).isEqualTo(DISPLAY_NAME_1);
    }

    @Test
    @UiThreadTest
    public void getSummary_allSubscriptionsHaveNoPreferredStatus_returnDisplayName() {

        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true, false, false);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, 1, 1, DISPLAY_NAME_2, SUB_MCC_2,
                SUB_MNC_2, SUB_COUNTRY_ISO_2, 1, true, true, true, false, false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
        displayPreferenceWithLifecycle();

        final StringBuilder summary = new StringBuilder();
        summary.append(DISPLAY_NAME_1).append(", ").append(DISPLAY_NAME_2);

        assertTrue(TextUtils.equals(mController.getSummary(), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_oneSubscriptionsIsCallPreferredTwoIsSmsPreferred_returnStatus() {

        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true, true, false);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, 1, 1, DISPLAY_NAME_2, SUB_MCC_2,
                SUB_MNC_2, SUB_COUNTRY_ISO_2, 1, true, true, true, false, true);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
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

        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true, false, true);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, 1, 1, DISPLAY_NAME_2, SUB_MCC_2,
                SUB_MNC_2, SUB_COUNTRY_ISO_2, 1, true, true, true, true, false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
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

        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true, true, true);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, 1, 1, DISPLAY_NAME_2, SUB_MCC_2,
                SUB_MNC_2, SUB_COUNTRY_ISO_2, 1, true, true, true, false, false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
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
