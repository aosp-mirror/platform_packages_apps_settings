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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NetworkProviderSimListControllerTest {

    private static final String SUB_ID_1 = "1";
    private static final String SUB_ID_2 = "2";
    private static final String KEY_PREFERENCE_SIM_LIST = "provider_model_sim_list";
    private static final String KEY_PREFERENCE_CATEGORY_SIM = "provider_model_sim_category";
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
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    private LifecycleRegistry mLifecycleRegistry;

    private MockNetworkProviderSimListController mController;
    private PreferenceManager mPreferenceManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private RestrictedPreference mPreference;
    private Context mContext;
    private List<SubscriptionInfoEntity> mSubscriptionInfoEntityList = new ArrayList<>();

    /**
     * Mock the NetworkProviderSimListController that allows one to set a default voice,
     * SMS and mobile data subscription ID.
     */
    private static class MockNetworkProviderSimListController
            extends NetworkProviderSimListController {
        MockNetworkProviderSimListController(Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        private List<SubscriptionInfoEntity> mSubscriptionInfoEntity;

        @Override
        protected List<SubscriptionInfoEntity> getAvailablePhysicalSubscriptions() {
            return mSubscriptionInfoEntity;
        }

        public void setSubscriptionInfoList(List<SubscriptionInfoEntity> list) {
            mSubscriptionInfoEntity = list;
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
        mPreference.setKey(KEY_PREFERENCE_SIM_LIST);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(KEY_PREFERENCE_CATEGORY_SIM);
        mController = new MockNetworkProviderSimListController(mContext, "test_key");
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
    }

    private void displayPreferenceWithLifecycle() {
        mLifecycleRegistry.addObserver(mController);
        mPreferenceScreen.addPreference(mPreference);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mController.displayPreference(mPreferenceScreen);
        mLifecycleRegistry.handleLifecycleEvent(Event.ON_RESUME);
    }

    private SubscriptionInfoEntity setupSubscriptionInfoEntity(String subId, int slotId,
            int carrierId, String displayName, String mcc, String mnc, String countryIso,
            int cardId, boolean isValid, boolean isActive, boolean isAvailable) {
        return new SubscriptionInfoEntity(subId, slotId, carrierId, displayName, displayName, 0,
                mcc, mnc, countryIso, false, cardId, TelephonyManager.DEFAULT_PORT_INDEX, false,
                null, SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM, displayName, false,
                "1234567890", true, false, isValid, true, isActive, isAvailable, false);
    }

    private String setSummaryResId(String resName, String value) {
        return ResourcesUtils.getResourcesString(mContext, resName, value);
    }

    private String setSummaryResId(String resName) {
        return ResourcesUtils.getResourcesString(mContext, resName);
    }

    @Test
    @UiThreadTest
    public void getSummary_tapToActivePSim() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, false, true);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
        displayPreferenceWithLifecycle();
        String summary = setSummaryResId("mobile_network_tap_to_activate", DISPLAY_NAME_1);

        assertTrue(TextUtils.equals(mController.getSummary(mSubInfo1, DISPLAY_NAME_1), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_inactivePSim() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, false, true);
        doReturn(true).when(mSubscriptionManager).canDisablePhysicalSubscription();
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);

        displayPreferenceWithLifecycle();
        String summary = setSummaryResId("sim_category_inactive_sim", null);

        assertTrue(TextUtils.equals(mController.getSummary(mSubInfo1, DISPLAY_NAME_1), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_defaultCalls() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);

        displayPreferenceWithLifecycle();
        CharSequence defaultCall = SubscriptionUtil.getDefaultSimConfig(mContext,
                Integer.parseInt(SUB_ID_1));
        final StringBuilder summary = new StringBuilder();
        summary.append(setSummaryResId("sim_category_active_sim", null))
                .append(defaultCall);

        assertTrue(TextUtils.equals(mController.getSummary(mSubInfo1, DISPLAY_NAME_1), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_defaultCallsAndSms() {
        final StringBuilder defaultConfig = new StringBuilder();
        defaultConfig.append(setSummaryResId("default_active_sim_calls"))
                .append(", ")
                .append(setSummaryResId("default_active_sim_sms"));
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);

        displayPreferenceWithLifecycle();
        CharSequence defaultCall = SubscriptionUtil.getDefaultSimConfig(mContext,
                Integer.parseInt(SUB_ID_1));
        final StringBuilder summary = new StringBuilder();
        summary.append(setSummaryResId("sim_category_active_sim", null))
                .append(defaultCall);

        assertTrue(TextUtils.equals(mController.getSummary(mSubInfo1, DISPLAY_NAME_1), summary));
    }

    @Ignore
    @Test
    @UiThreadTest
    public void getAvailablePhysicalSubscription_withTwoPhysicalSims_returnTwo() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true);
        mSubInfo2 = setupSubscriptionInfoEntity(SUB_ID_2, 1, 1, DISPLAY_NAME_2, SUB_MCC_2,
                SUB_MNC_2, SUB_COUNTRY_ISO_2, 1, true, true, true);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mSubscriptionInfoEntityList.add(mSubInfo2);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);

        displayPreferenceWithLifecycle();

        assertThat(mController.getAvailablePhysicalSubscriptions().size()).isEqualTo(2);
    }

}
