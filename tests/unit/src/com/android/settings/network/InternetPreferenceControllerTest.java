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
import static org.mockito.Mockito.mock;
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
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settings.wifi.WifiSummaryUpdater;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.wifitrackerlib.HotspotNetworkEntry;
import com.android.wifitrackerlib.StandardWifiEntry;
import com.android.wifitrackerlib.WifiPickerTracker;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class InternetPreferenceControllerTest {

    private static final String TEST_SUMMARY = "test summary";
    private static final String TEST_ALTERNATE_SUMMARY = "test alternate summary";
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
    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private SubscriptionInfoEntity mActiveSubInfo;
    @Mock
    private SubscriptionInfoEntity mDefaultDataSubInfo;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private WifiSummaryUpdater mSummaryHelper;
    @Mock
    private WifiPickerTrackerHelper mWifiPickerTrackerHelper;
    @Mock
    private WifiPickerTracker mWifiPickerTracker;
    @Mock
    private HotspotNetworkEntry mHotspotNetworkEntry;

    private LifecycleRegistry mLifecycleRegistry;

    private MockInternetPreferenceController mController;
    private PreferenceScreen mScreen;
    private Preference mPreference;
    private List<SubscriptionInfoEntity> mSubscriptionInfoEntityList = new ArrayList<>();

    @Before
    public void setUp() {
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mContext.getSystemService(NetworkScoreManager.class))
                .thenReturn(mock(NetworkScoreManager.class));
        when(mContext.getSystemService(Context.WIFI_SERVICE)).thenReturn(mWifiManager);
        when(mWifiManager.getWifiState()).thenReturn(WifiManager.WIFI_STATE_DISABLED);
        when(mWifiPickerTrackerHelper.getWifiPickerTracker()).thenReturn(mWifiPickerTracker);
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(null /* WifiEntry */);
        when(mHotspotNetworkEntry.getAlternateSummary()).thenReturn(TEST_ALTERNATE_SUMMARY);

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);
        mController = new MockInternetPreferenceController(mContext, mock(Lifecycle.class),
                mLifecycleOwner);
        mController.sIconMap.put(INTERNET_WIFI, 0);
        mController.mWifiPickerTrackerHelper = mWifiPickerTrackerHelper;

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(InternetPreferenceController.KEY);
        mScreen.addPreference(mPreference);
    }

    private class MockInternetPreferenceController extends
            com.android.settings.network.InternetPreferenceController {

        private int mDefaultDataSubscriptionId;
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

        @Override
        protected int getDefaultDataSubscriptionId() {
            return mDefaultDataSubscriptionId;
        }

        public void setDefaultDataSubscriptionId(int subscriptionId) {
            mDefaultDataSubscriptionId = subscriptionId;
        }

    }

    private SubscriptionInfoEntity setupSubscriptionInfoEntity(String subId, int slotId,
            int carrierId, String displayName, String mcc, String mnc, String countryIso,
            int cardId, boolean isVisible, boolean isValid, boolean isActive, boolean isAvailable,
            boolean isActiveData) {
        return new SubscriptionInfoEntity(subId, slotId, carrierId,
                displayName, displayName, 0, mcc, mnc, countryIso, false, cardId,
                TelephonyManager.DEFAULT_PORT_INDEX, false, null,
                SubscriptionManager.SUBSCRIPTION_TYPE_LOCAL_SIM, displayName, isVisible,
                "1234567890", true, false, isValid, true, isActive, isAvailable, isActiveData);
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

        verify(mContext, times(2)).unregisterReceiver(any(BroadcastReceiver.class));
        verify(mConnectivityManager, times(2)).unregisterNetworkCallback(
                any(ConnectivityManager.NetworkCallback.class));
    }

    @Test
    public void onSummaryChanged_internetWifi_updateSummary() {
        when(mSummaryHelper.getSummary()).thenReturn(TEST_SUMMARY);
        mController.mSummaryHelper = mSummaryHelper;
        mController.onInternetTypeChanged(INTERNET_WIFI);
        mController.displayPreference(mScreen);

        mController.onSummaryChanged(TEST_SUMMARY);

        assertThat(mPreference.getSummary()).isEqualTo(TEST_SUMMARY);
    }

    @Test
    public void onSummaryChanged_internetNetworksAvailable_notUpdateSummary() {
        when(mSummaryHelper.getSummary()).thenReturn(TEST_SUMMARY);
        mController.mSummaryHelper = mSummaryHelper;
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
        mController.setDefaultDataSubscriptionId(Integer.parseInt(SUB_ID_2));
        mActiveSubInfo = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, false, true, true, true, true);
        mDefaultDataSubInfo = setupSubscriptionInfoEntity(SUB_ID_2, 1, 1, DISPLAY_NAME_2, SUB_MCC_2,
                SUB_MNC_2, SUB_COUNTRY_ISO_2, 1, false, true, true, true, false);
        mSubscriptionInfoEntityList.add(mActiveSubInfo);
        mSubscriptionInfoEntityList.add(mDefaultDataSubInfo);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
        mController.displayPreference(mScreen);

        mController.updateCellularSummary();
        assertThat(mPreference.getSummary()).isEqualTo(DISPLAY_NAME_2);

        mActiveSubInfo = setupSubscriptionInfoEntity(SUB_ID_1, 1, 1, DISPLAY_NAME_1, SUB_MCC_1,
                SUB_MNC_1, SUB_COUNTRY_ISO_1, 1, true, true, true, true, true);
        mSubscriptionInfoEntityList.add(mActiveSubInfo);
        mController.setSubscriptionInfoList(mSubscriptionInfoEntityList);
        mController.onAvailableSubInfoChanged(mSubscriptionInfoEntityList);
        final String expectedSummary =
                ResourcesUtils.getResourcesString(mContext, "mobile_data_temp_using",
                        DISPLAY_NAME_1);
        mController.updateCellularSummary();
        assertThat(mPreference.getSummary()).isEqualTo(expectedSummary);
    }

    @Test
    public void updateHotspotNetwork_isHotspotNetworkEntry_updateAlternateSummary() {
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(mHotspotNetworkEntry);
        mController.onInternetTypeChanged(INTERNET_WIFI);
        mController.displayPreference(mScreen);
        mPreference.setSummary(TEST_SUMMARY);

        mController.updateHotspotNetwork();

        assertThat(mPreference.getSummary().toString()).isEqualTo(TEST_ALTERNATE_SUMMARY);
    }

    @Test
    public void updateHotspotNetwork_notHotspotNetworkEntry_notChangeSummary() {
        when(mWifiPickerTracker.getConnectedWifiEntry()).thenReturn(mock(StandardWifiEntry.class));
        mController.onInternetTypeChanged(INTERNET_WIFI);
        mController.displayPreference(mScreen);
        mPreference.setSummary(TEST_SUMMARY);

        mController.updateHotspotNetwork();

        assertThat(mPreference.getSummary().toString()).isEqualTo(TEST_SUMMARY);
    }

    @Test
    public void updateHotspotNetwork_hotspotNetworkNotEnabled_returnFalse() {
        mController.mWifiPickerTrackerHelper = null;

        assertThat(mController.updateHotspotNetwork()).isFalse();
    }
}
