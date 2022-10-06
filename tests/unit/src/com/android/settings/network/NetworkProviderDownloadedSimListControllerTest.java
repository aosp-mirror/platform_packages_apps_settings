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
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.testutils.ResourcesUtils;
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
public class NetworkProviderDownloadedSimListControllerTest {

    private static final String SUB_ID_1 = "1";
    private static final String DISPLAY_NAME_1 = "Sub 1";
    private static final String SUB_MCC_1 = "123";
    private static final String SUB_MNC_1 = "456";
    private static final String SUB_COUNTRY_ISO_1 = "Sub 1";
    private static final String KEY_PREFERENCE_DOWNLOADED_SIM =
            "provider_model_downloaded_sim_list";
    private static final String KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM =
            "provider_model_downloaded_sim_category";
    private static final String KEY_ADD_MORE = "add_more";

    @Mock
    private SubscriptionInfoEntity mSubInfo1;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LifecycleOwner mLifecycleOwner;

    private LifecycleRegistry mLifecycleRegistry;
    private MockNetworkProviderDownloadedSimListController mController;
    private PreferenceManager mPreferenceManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private Preference mPreference;
    private Preference mAddMorePreference;
    private Context mContext;
    private List<SubscriptionInfoEntity> mSubscriptionInfoEntityList = new ArrayList<>();

    /**
     * Mock the MockNetworkProviderDownloadedSimListController that allows one to set a
     * default voice, SMS and mobile data subscription ID.
     */
    @SuppressWarnings("ClassCanBeStatic")
    private class MockNetworkProviderDownloadedSimListController extends
            com.android.settings.network.NetworkProviderDownloadedSimListController {
        public MockNetworkProviderDownloadedSimListController(Context context,
                Lifecycle lifecycle, LifecycleOwner lifecycleOwner) {
            super(context, lifecycle, lifecycleOwner);
        }

        private List<SubscriptionInfoEntity> mSubscriptionInfoEntity;

        @Override
        protected List<SubscriptionInfoEntity> getAvailableDownloadedSubscriptions() {
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

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(KEY_PREFERENCE_DOWNLOADED_SIM);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(KEY_PREFERENCE_CATEGORY_DOWNLOADED_SIM);
        mController = new MockNetworkProviderDownloadedSimListController(mContext, mLifecycle,
                mLifecycleOwner);
        mAddMorePreference = new Preference(mContext);
        mAddMorePreference.setKey(KEY_ADD_MORE);
        mAddMorePreference.setVisible(true);
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
    }

    private void displayPreferenceWithLifecycle() {
        mLifecycleRegistry.addObserver(mController);
        mPreferenceScreen.addPreference(mPreference);
        mPreferenceScreen.addPreference(mPreferenceCategory);
        mPreferenceScreen.addPreference(mAddMorePreference);
        mController.displayPreference(mPreferenceScreen);
        mLifecycleRegistry.handleLifecycleEvent(Event.ON_RESUME);
    }

    private SubscriptionInfoEntity setupSubscriptionInfoEntity(String subId, int slotId,
            int carrierId, String displayName, String mcc, String mnc, String countryIso,
            int cardId, CharSequence defaultSimConfig, boolean isValid, boolean isActive,
            boolean isAvailable, boolean isDefaultCall, boolean isDefaultData,
            boolean isDefaultSms) {
        return new SubscriptionInfoEntity(subId, slotId, carrierId, displayName, displayName, 0,
                mcc, mnc, countryIso, true, cardId, TelephonyManager.DEFAULT_PORT_INDEX, false,
                null, SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM, displayName, false,
                "1234567890", true, defaultSimConfig.toString(), false, isValid, true, isActive,
                isAvailable, isDefaultCall, isDefaultSms, isDefaultData, false, false);
    }

    private String setSummaryResId(String resName) {
        return ResourcesUtils.getResourcesString(mContext, resName);
    }

    @Test
    @UiThreadTest
    public void getSummary_inactiveESim() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, "", true, false, false, false, false, false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);

        displayPreferenceWithLifecycle();
        String summary = setSummaryResId("sim_category_inactive_sim");

        assertTrue(TextUtils.equals(mController.getSummary(mSubInfo1), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_defaultCalls() {
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1,
                mContext.getString(R.string.sim_category_default_active_sim,
                        setSummaryResId("default_active_sim_calls")), true,
                true, true, true, false, false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);

        displayPreferenceWithLifecycle();
        CharSequence defaultCall = mSubInfo1.defaultSimConfig;
        final StringBuilder summary = new StringBuilder();
        summary.append(setSummaryResId("sim_category_active_sim"))
                .append(defaultCall);

        assertTrue(TextUtils.equals(mController.getSummary(mSubInfo1), summary));
    }

    @Test
    @UiThreadTest
    public void getSummary_defaultCallsAndMobileData() {
        final StringBuilder defaultConfig = new StringBuilder();
        defaultConfig.append(setSummaryResId("default_active_sim_mobile_data"))
                .append(", ")
                .append(setSummaryResId("default_active_sim_calls"));
        mSubInfo1 = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1,
                mContext.getString(R.string.sim_category_default_active_sim, defaultConfig), true,
                true, true, true, true,
                false);
        mSubscriptionInfoEntityList.add(mSubInfo1);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);

        displayPreferenceWithLifecycle();
        CharSequence defaultCall = mSubInfo1.defaultSimConfig;
        final StringBuilder summary = new StringBuilder();
        summary.append(setSummaryResId("sim_category_active_sim"))
                .append(defaultCall);
        assertTrue(TextUtils.equals(mController.getSummary(mSubInfo1), summary));
    }
}
