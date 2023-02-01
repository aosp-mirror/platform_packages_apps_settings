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

import static com.android.settings.network.InternetUpdater.INTERNET_NETWORKS_AVAILABLE;
import static com.android.settings.network.InternetUpdater.INTERNET_WIFI;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkRequest;
import android.net.NetworkScoreManager;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class InternetPreferenceControllerTest {

    private static final String TEST_SUMMARY = "test summary";
    private static final String NOT_CONNECTED = "Not connected";
    private static final String SUB_ID_1 = "1";
    private static final String SUB_ID_2 = "2";
    private static final String INVALID_SUB_ID = "-1";
    private static final String DISPLAY_NAME_1 = "Sub 1";
    private static final String DISPLAY_NAME_2 = "Sub 2";
    private static final String SUB_MCC_1 = "123";
    private static final String SUB_MNC_1 = "456";
    private static final String SUB_MCC_2 = "223";
    private static final String SUB_MNC_2 = "456";
    private static final String SUB_COUNTRY_ISO_1 = "Sub 1";
    private static final String SUB_COUNTRY_ISO_2 = "Sub 2";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private SubscriptionInfoEntity mActiveSubInfo;
    @Mock
    private SubscriptionInfoEntity mDefaultDataSubInfo;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private LifecycleOwner mLifecycleOwner;

    private LifecycleRegistry mLifecycleRegistry;

    private Context mContext;
    private MockInternetPreferenceController mController;
    private PreferenceScreen mScreen;
    private Preference mPreference;
    private List<SubscriptionInfoEntity> mSubscriptionInfoEntityList = new ArrayList<>();

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mContext.getSystemService(NetworkScoreManager.class))
                .thenReturn(mock(NetworkScoreManager.class));
        final WifiManager wifiManager = mock(WifiManager.class);
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(wifiManager);
        when(wifiManager.getWifiState()).thenReturn(WifiManager.WIFI_STATE_DISABLED);
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
        mController = new MockInternetPreferenceController(mContext, mock(Lifecycle.class),
                mLifecycleOwner);
        mController.sIconMap.put(INTERNET_WIFI, 0);

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(InternetPreferenceController.KEY);
        mScreen.addPreference(mPreference);
    }

    private class MockInternetPreferenceController extends
            com.android.settings.network.InternetPreferenceController {
        public MockInternetPreferenceController(Context context, Lifecycle lifecycle,
                LifecycleOwner lifecycleOwner) {
            super(context, lifecycle, lifecycleOwner);
        }

        private List<SubscriptionInfoEntity> mSubscriptionInfoEntity;

        @Override
        protected List<SubscriptionInfoEntity> getSubscriptionInfoList() {
            return mSubscriptionInfoEntity;
        }

        public void setSubscriptionInfoList(List<SubscriptionInfoEntity> list) {
            mSubscriptionInfoEntity = list;
        }

    }

    private SubscriptionInfoEntity setupSubscriptionInfoEntity(String subId, int slotId,
            int carrierId, String displayName, String mcc, String mnc, String countryIso,
            int cardId, boolean isVisible, boolean isValid, boolean isActive, boolean isAvailable,
            boolean isDefaultData, boolean isActiveData) {
        return new SubscriptionInfoEntity(subId, slotId, carrierId,
                displayName, displayName, 0, mcc, mnc, countryIso, false, cardId,
                TelephonyManager.DEFAULT_PORT_INDEX, false, null,
                SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM, displayName, isVisible,
                "1234567890", true, "default", false, isValid, true, isActive, isAvailable, false,
                false, isDefaultData, false, isActiveData);
    }

    @Test
    public void isAvailable_shouldBeTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @UiThreadTest
    public void onResume_shouldRegisterCallback() {
        mController.onResume();

        verify(mContext).registerReceiver(any(BroadcastReceiver.class), any(IntentFilter.class),
                any(int.class));
        verify(mConnectivityManager).registerNetworkCallback(
                any(NetworkRequest.class),
                any(ConnectivityManager.NetworkCallback.class),
                any(Handler.class));
    }

    @Test
    @UiThreadTest
    public void onPause_shouldUnregisterCallback() {
        mController.onResume();
        mController.onPause();

        verify(mContext).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mConnectivityManager, times(2)).unregisterNetworkCallback(
                any(ConnectivityManager.NetworkCallback.class));
    }

    @Test
    public void onSummaryChanged_internetWifi_updateSummary() {
        mController.onInternetTypeChanged(INTERNET_WIFI);
        mController.displayPreference(mScreen);

        mController.onSummaryChanged(TEST_SUMMARY);

        assertThat(mPreference.getSummary()).isEqualTo(TEST_SUMMARY);
    }

    @Test
    public void onSummaryChanged_internetNetworksAvailable_notUpdateSummary() {
        mController.onInternetTypeChanged(INTERNET_NETWORKS_AVAILABLE);
        mController.displayPreference(mScreen);
        mPreference.setSummary(NOT_CONNECTED);

        mController.onSummaryChanged(TEST_SUMMARY);

        assertThat(mPreference.getSummary()).isNotEqualTo(TEST_SUMMARY);
    }

    @Test
    public void updateCellularSummary_getNullSubscriptionInfo_shouldNotCrash() {
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);

        mController.updateCellularSummary();
    }

    @Test
    public void updateCellularSummary_getActiveSubscriptionInfo_cbrs() {
        mActiveSubInfo = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, false, true, true, true, false, true);
        mDefaultDataSubInfo = setupSubscriptionInfoEntity(SUB_ID_2, 1, 1, DISPLAY_NAME_2, SUB_MCC_2,
                SUB_MNC_2, SUB_COUNTRY_ISO_2, 1, false, true, true, true, true, false);
        mSubscriptionInfoEntityList.add(mActiveSubInfo);
        mSubscriptionInfoEntityList.add(mDefaultDataSubInfo);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
        mController.displayPreference(mScreen);

        mController.updateCellularSummary();
        assertThat(mPreference.getSummary()).isEqualTo(DISPLAY_NAME_2);

        mActiveSubInfo = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true, true, false, true);
        mSubscriptionInfoEntityList.add(mActiveSubInfo);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
        mController.onAvailableSubInfoChanged(mSubscriptionInfoEntityList);
        final String expectedSummary =
                ResourcesUtils.getResourcesString(mContext, "mobile_data_temp_using",
                        DISPLAY_NAME_1);
        mController.updateCellularSummary();
        assertThat(mPreference.getSummary()).isEqualTo(expectedSummary);
    }
}
