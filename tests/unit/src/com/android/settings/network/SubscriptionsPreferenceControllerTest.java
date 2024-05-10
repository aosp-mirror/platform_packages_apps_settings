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

import static android.telephony.SignalStrength.SIGNAL_STRENGTH_GOOD;
import static android.telephony.SignalStrength.SIGNAL_STRENGTH_GREAT;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static com.android.wifitrackerlib.WifiEntry.WIFI_LEVEL_MAX;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.telephony.AccessNetworkConstants;
import android.telephony.NetworkRegistrationInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyDisplayInfo;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.Utils;
import com.android.settings.network.SubscriptionsPreferenceController.SubsPrefCtrlInjector;
import com.android.settings.testutils.ResourcesUtils;
import com.android.settings.wifi.WifiPickerTrackerHelper;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.mobile.MobileMappings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class SubscriptionsPreferenceControllerTest {
    private static final String KEY = "preference_group";
    private static final int SUB_ID = 1;

    @Mock
    private UserManager mUserManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mTelephonyManagerForSub;
    @Mock
    private Network mActiveNetwork;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    @Mock
    private WifiPickerTrackerHelper mWifiPickerTrackerHelper;
    @Mock
    private WifiManager mWifiManager;
    @Mock
    private SignalStrength mSignalStrength;
    @Mock
    private ServiceState mServiceState;

    private LifecycleRegistry mLifecycleRegistry;
    private int mOnChildUpdatedCount;
    private Context mContext;
    private SubscriptionsPreferenceController.UpdateListener mUpdateListener;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceScreen mPreferenceScreen;
    private PreferenceManager mPreferenceManager;
    private NetworkCapabilities mNetworkCapabilities;
    private FakeSubscriptionsPreferenceController mController;
    private static SubsPrefCtrlInjector sInjector;
    private NetworkRegistrationInfo mNetworkRegistrationInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mLifecycleRegistry = new LifecycleRegistry(mLifecycleOwner);

        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mTelephonyManager.createForSubscriptionId(anyInt())).thenReturn(mTelephonyManager);
        when(mSignalStrength.getLevel()).thenReturn(SIGNAL_STRENGTH_GREAT);
        when(mTelephonyManager.getServiceState()).thenReturn(mServiceState);
        mNetworkRegistrationInfo = createNetworkRegistrationInfo(false /* dateState */);
        when(mServiceState.getNetworkRegistrationInfo(anyInt(), anyInt()))
                .thenReturn(mNetworkRegistrationInfo);
        when(mTelephonyManager.getSignalStrength()).thenReturn(mSignalStrength);
        when(mConnectivityManager.getActiveNetwork()).thenReturn(mActiveNetwork);
        when(mConnectivityManager.getNetworkCapabilities(mActiveNetwork))
                .thenReturn(mNetworkCapabilities);
        when(mUserManager.isAdminUser()).thenReturn(true);
        when(mContext.getSystemService(WifiManager.class)).thenReturn(mWifiManager);
        when(mLifecycleOwner.getLifecycle()).thenReturn(mLifecycleRegistry);

        mPreferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.setInitialExpandedChildrenCount(3);
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(KEY);
        mPreferenceCategory.setOrderingAsAdded(true);
        mPreferenceScreen.addPreference(mPreferenceCategory);

        mOnChildUpdatedCount = 0;
        mUpdateListener = () -> mOnChildUpdatedCount++;

        sInjector = spy(new SubsPrefCtrlInjector());
        mController =  new FakeSubscriptionsPreferenceController(mContext, mLifecycle,
                mUpdateListener, KEY, 5);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        mController.setWifiPickerTrackerHelper(mWifiPickerTrackerHelper);
    }

    @After
    public void tearDown() {
        SubscriptionUtil.setActiveSubscriptionsForTesting(null);
    }

    @Test
    public void isAvailable_oneSubAndProviderOn_availableTrue() {
        setupMockSubscriptions(1);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_fiveSubscriptions_availableTrue() {
        setupMockSubscriptions(5);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_airplaneModeOnWifiOff_availableFalse() {
        setupMockSubscriptions(2);

        assertThat(mController.isAvailable()).isTrue();
        when(mWifiManager.isWifiEnabled()).thenReturn(false);

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_airplaneModeOnWifiOnWithNoCarrierNetwork_availableFalse() {
        setupMockSubscriptions(2);

        assertThat(mController.isAvailable()).isTrue();
        when(mWifiManager.isWifiEnabled()).thenReturn(true);
        doReturn(false).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_airplaneModeOnWifiOffWithCarrierNetwork_availableFalse() {
        setupMockSubscriptions(1);

        when(mWifiManager.isWifiEnabled()).thenReturn(false);
        doReturn(true).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_airplaneModeOff_availableTrue() {
        setupMockSubscriptions(2);

        assertThat(mController.isAvailable()).isTrue();
        when(mWifiManager.isWifiEnabled()).thenReturn(true);
        doReturn(true).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();

        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @UiThreadTest
    public void displayPreference_providerAndHasSim_showPreference() {
        final List<SubscriptionInfo> sub = setupMockSubscriptions(1);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        doReturn(sub).when(mSubscriptionManager).getAvailableSubscriptionInfoList();

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void displayPreference_providerAndHasMultiSim_showOnePreference() {
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        doReturn(sub).when(mSubscriptionManager).getAvailableSubscriptionInfoList();

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void displayPreference_providerAndHasMultiSimAndActive_connectedAndRat() {
        final String networkType = "5G";
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        setupGetIconConditions(sub.get(0).getSubscriptionId(), true, true,
                true, ServiceState.STATE_IN_SERVICE);
        doReturn(mock(MobileMappings.Config.class)).when(sInjector).getConfig(mContext);
        doReturn(networkType)
                .when(sInjector).getNetworkType(any(), any(), any(), anyInt(), eq(false),
                        eq(false));

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceCategory.getPreference(0).getSummary()).isEqualTo("Connected / 5G");
    }

    @Test
    @UiThreadTest
    public void displayPreference_providerAndHasMultiSimAndActiveCarrierWifi_connectedAndWPlus() {
        final String networkType = "W+";
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        setupGetIconConditions(sub.get(0).getSubscriptionId(), false, true,
                true, ServiceState.STATE_IN_SERVICE);
        doReturn(mock(MobileMappings.Config.class)).when(sInjector).getConfig(mContext);
        doReturn(networkType)
                .when(sInjector).getNetworkType(any(), any(), any(), anyInt(), eq(true), eq(false));
        doReturn(true).when(mWifiPickerTrackerHelper).isCarrierNetworkActive();

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceCategory.getPreference(0).getSummary()).isEqualTo("Connected / W+");
    }

    @Test
    @UiThreadTest
    public void displayPreference_providerAndHasMultiSimButMobileDataOff_notAutoConnect() {
        final String dataOffSummary =
                ResourcesUtils.getResourcesString(mContext, "mobile_data_off_summary");
        final String networkType = "5G";
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        setupGetIconConditions(sub.get(0).getSubscriptionId(), false, false,
                true, ServiceState.STATE_IN_SERVICE);
        doReturn(networkType)
                .when(sInjector).getNetworkType(any(), any(), any(), anyInt(), eq(false),
                        eq(false));

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceCategory.getPreference(0).getSummary()).isEqualTo(dataOffSummary);
    }

    @Test
    @UiThreadTest
    public void displayPreference_providerAndHasMultiSimAndNotActive_showRatOnly() {
        final String networkType = "5G";
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        setupGetIconConditions(sub.get(0).getSubscriptionId(), false, true,
                true, ServiceState.STATE_IN_SERVICE);
        doReturn(networkType)
                .when(sInjector).getNetworkType(any(), any(), any(), anyInt(), eq(false),
                        eq(false));
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceCategory.getPreference(0).getSummary()).isEqualTo(networkType);
    }

    @Test
    @UiThreadTest
    public void displayPreference_providerAndNoSim_noPreference() {
        doReturn(null).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void onTelephonyDisplayInfoChanged_providerAndHasMultiSimAndActive_connectedAndRat() {
        final String networkType = "LTE";
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        final TelephonyDisplayInfo telephonyDisplayInfo =
                new TelephonyDisplayInfo(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        setupGetIconConditions(sub.get(0).getSubscriptionId(), true, true,
                true, ServiceState.STATE_IN_SERVICE);
        doReturn(mock(MobileMappings.Config.class)).when(sInjector).getConfig(mContext);
        doReturn(networkType)
                .when(sInjector).getNetworkType(any(), any(), any(), anyInt(), eq(false),
                        eq(false));
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);
        mController.onTelephonyDisplayInfoChanged(sub.get(0).getSubscriptionId(),
                telephonyDisplayInfo);

        assertThat(mPreferenceCategory.getPreference(0).getSummary()).isEqualTo("Connected / LTE");
    }

    @Test
    @UiThreadTest
    public void onTelephonyDisplayInfoChanged_providerAndHasMultiSimAndNotActive_showRat() {
        final String networkType = "LTE";
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        final TelephonyDisplayInfo telephonyDisplayInfo =
                new TelephonyDisplayInfo(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        setupGetIconConditions(sub.get(0).getSubscriptionId(), false, true,
                true, ServiceState.STATE_IN_SERVICE);
        doReturn(mock(MobileMappings.Config.class)).when(sInjector).getConfig(mContext);
        doReturn(networkType)
                .when(sInjector).getNetworkType(any(), any(), any(), anyInt(), eq(false),
                        eq(false));

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);
        mController.onTelephonyDisplayInfoChanged(sub.get(0).getSubscriptionId(),
                telephonyDisplayInfo);

        assertThat(mPreferenceCategory.getPreference(0).getSummary()).isEqualTo(networkType);
    }

    @Test
    @UiThreadTest
    public void onTelephonyDisplayInfoChanged_providerAndHasMultiSimAndOutOfService_noConnection() {
        final String noConnectionSummary =
                ResourcesUtils.getResourcesString(mContext, "mobile_data_no_connection");
        final String networkType = "LTE";
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        final TelephonyDisplayInfo telephonyDisplayInfo =
                new TelephonyDisplayInfo(TelephonyManager.NETWORK_TYPE_UNKNOWN,
                        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        setupGetIconConditions(sub.get(0).getSubscriptionId(), false, true,
                false, ServiceState.STATE_OUT_OF_SERVICE);
        doReturn(mock(MobileMappings.Config.class)).when(sInjector).getConfig(mContext);
        doReturn(networkType)
                .when(sInjector).getNetworkType(any(), any(), any(), anyInt(), eq(false),
                        eq(false));

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);
        mController.onTelephonyDisplayInfoChanged(sub.get(0).getSubscriptionId(),
                telephonyDisplayInfo);

        assertThat(mPreferenceCategory.getPreference(0).getSummary())
                .isEqualTo(noConnectionSummary);
    }

    @Test
    @UiThreadTest
    public void onAirplaneModeChanged_providerAndHasSim_noPreference() {
        setupMockSubscriptions(1);
        mController.onResume();
        mController.displayPreference(mPreferenceScreen);
        Settings.Global.putInt(mContext.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);

        mController.onAirplaneModeChanged(true);

        assertThat(mController.isAvailable()).isFalse();
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void dataSubscriptionChanged_providerAndHasMultiSim_showOnePreference() {
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        doReturn(sub).when(mSubscriptionManager).getAvailableSubscriptionInfoList();
        Intent intent = new Intent(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);
        mController.mConnectionChangeReceiver.onReceive(mContext, intent);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void dataSubscriptionChanged_providerAndHasMultiSim_showOnlyOnePreference() {
        final List<SubscriptionInfo> sub = setupMockSubscriptions(2);
        final int subId = sub.get(0).getSubscriptionId();
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        doReturn(sub).when(mSubscriptionManager).getAvailableSubscriptionInfoList();
        Intent intent = new Intent(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        doReturn(sub.get(1)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();

        mController.mConnectionChangeReceiver.onReceive(mContext, intent);

        assertThat(mController.isAvailable()).isTrue();
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    @UiThreadTest
    public void getIcon_cellularIsActive_iconColorIsAccentDefaultColor() {
        final List<SubscriptionInfo> sub = setupMockSubscriptions(1);
        doReturn(sub.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        Drawable icon = mock(Drawable.class);
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);
        doReturn(icon).when(sInjector).getIcon(any(), anyInt(), anyInt(), eq(false), eq(false));
        setupGetIconConditions(sub.get(0).getSubscriptionId(), true, true,
                true, ServiceState.STATE_IN_SERVICE);

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);

        verify(icon).setTint(Utils.getColorAccentDefaultColor(mContext));
    }

    @Test
    @UiThreadTest
    public void getIcon_dataStateConnectedAndMobileDataOn_iconIsSignalIcon() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(1);
        final int subId = subs.get(0).getSubscriptionId();
        doReturn(subs.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        Drawable icon = mock(Drawable.class);
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);
        doReturn(icon).when(sInjector).getIcon(any(), anyInt(), anyInt(), eq(false), eq(false));
        setupGetIconConditions(subId, false, true,
                true, ServiceState.STATE_IN_SERVICE);
        mController.onResume();
        mController.displayPreference(mPreferenceScreen);
        Drawable actualIcon = mPreferenceCategory.getPreference(0).getIcon();

        assertThat(icon).isEqualTo(actualIcon);
    }

    @Test
    @UiThreadTest
    public void getIcon_voiceInServiceAndMobileDataOff_iconIsSignalIcon() {
        final List<SubscriptionInfo> subs = setupMockSubscriptions(1);
        final int subId = subs.get(0).getSubscriptionId();
        doReturn(subs.get(0)).when(mSubscriptionManager).getDefaultDataSubscriptionInfo();
        Drawable icon = mock(Drawable.class);
        when(mTelephonyManager.isDataEnabled()).thenReturn(false);
        doReturn(icon).when(sInjector).getIcon(any(), anyInt(), anyInt(), eq(true), eq(false));

        setupGetIconConditions(subId, false, false,
                false, ServiceState.STATE_IN_SERVICE);

        mController.onResume();
        mController.displayPreference(mPreferenceScreen);
        Drawable actualIcon = mPreferenceCategory.getPreference(0).getIcon();
        ServiceState ss = mock(ServiceState.class);
        NetworkRegistrationInfo regInfo = createNetworkRegistrationInfo(true /* dataState */);
        doReturn(ss).when(mTelephonyManagerForSub).getServiceState();
        doReturn(regInfo).when(ss).getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN);

        assertThat(icon).isEqualTo(actualIcon);
    }

    @Test
    @UiThreadTest
    public void getIcon_carrierNetworkIsNotActive_useMobileDataLevel() {
        // Fake mobile data active and level is SIGNAL_STRENGTH_GOOD(3)
        mNetworkRegistrationInfo = createNetworkRegistrationInfo(true /* dateState */);
        when(mServiceState.getNetworkRegistrationInfo(anyInt(), anyInt()))
                .thenReturn(mNetworkRegistrationInfo);
        when(mSignalStrength.getLevel()).thenReturn(SIGNAL_STRENGTH_GOOD);
        // Fake carrier network not active and level is WIFI_LEVEL_MAX(4)
        when(mWifiPickerTrackerHelper.isCarrierNetworkActive()).thenReturn(false);
        when(mWifiPickerTrackerHelper.getCarrierNetworkLevel()).thenReturn(WIFI_LEVEL_MAX);

        mController.getIcon(SUB_ID);

        verify(sInjector).getIcon(any(), eq(SIGNAL_STRENGTH_GOOD), anyInt(), anyBoolean(),
                anyBoolean());
    }

    @Test
    @UiThreadTest
    public void getIcon_carrierNetworkIsActive_useCarrierNetworkLevel() {
        // Fake mobile data not active and level is SIGNAL_STRENGTH_GOOD(3)
        mNetworkRegistrationInfo = createNetworkRegistrationInfo(false /* dateState */);
        when(mServiceState.getNetworkRegistrationInfo(anyInt(), anyInt()))
                .thenReturn(mNetworkRegistrationInfo);
        when(mSignalStrength.getLevel()).thenReturn(SIGNAL_STRENGTH_GOOD);
        // Fake carrier network active and level is WIFI_LEVEL_MAX(4)
        when(mWifiPickerTrackerHelper.isCarrierNetworkActive()).thenReturn(true);
        when(mWifiPickerTrackerHelper.getCarrierNetworkLevel()).thenReturn(WIFI_LEVEL_MAX);

        mController.getIcon(SUB_ID);

        verify(sInjector).getIcon(any(), eq(WIFI_LEVEL_MAX), anyInt(), anyBoolean(), anyBoolean());
    }

    @Test
    public void connectCarrierNetwork_isDataEnabled_helperConnect() {
        when(mTelephonyManager.isDataEnabled()).thenReturn(true);

        mController.connectCarrierNetwork();

        verify(mWifiPickerTrackerHelper).connectCarrierNetwork(any());
    }

    @Test
    public void connectCarrierNetwork_isNotDataEnabled_helperNeverConnect() {
        when(mTelephonyManager.isDataEnabled()).thenReturn(false);

        mController.connectCarrierNetwork();

        verify(mWifiPickerTrackerHelper, never()).connectCarrierNetwork(any());
    }

    private void setupGetIconConditions(int subId, boolean isActiveCellularNetwork,
            boolean isDataEnable, boolean dataState, int servicestate) {
        doReturn(mTelephonyManagerForSub).when(mTelephonyManager).createForSubscriptionId(subId);
        doReturn(isActiveCellularNetwork).when(sInjector).isActiveCellularNetwork(mContext);
        doReturn(isDataEnable).when(mTelephonyManagerForSub).isDataEnabled();
        ServiceState ss = mock(ServiceState.class);
        NetworkRegistrationInfo regInfo = createNetworkRegistrationInfo(dataState);
        doReturn(ss).when(mTelephonyManagerForSub).getServiceState();
        doReturn(servicestate).when(ss).getState();
        doReturn(regInfo).when(ss).getNetworkRegistrationInfo(
                NetworkRegistrationInfo.DOMAIN_PS,
                AccessNetworkConstants.TRANSPORT_TYPE_WWAN);
    }

    private NetworkRegistrationInfo createNetworkRegistrationInfo(boolean dataState) {
        return new NetworkRegistrationInfo.Builder()
                .setDomain(NetworkRegistrationInfo.DOMAIN_PS)
                .setTransportType(AccessNetworkConstants.TRANSPORT_TYPE_WWAN)
                .setRegistrationState(dataState ? NetworkRegistrationInfo.REGISTRATION_STATE_HOME
                        : NetworkRegistrationInfo.REGISTRATION_STATE_NOT_REGISTERED_SEARCHING)
                .setAccessNetworkTechnology(TelephonyManager.NETWORK_TYPE_LTE)
                .build();
    }

    private List<SubscriptionInfo> setupMockSubscriptions(int count) {
        return setupMockSubscriptions(count, 0, true);
    }

    /** Helper method to setup several mock active subscriptions. The generated subscription id's
     * start at 1.
     *
     * @param count How many subscriptions to create
     * @param defaultDataSubId The subscription id of the default data subscription - pass
     *                         INVALID_SUBSCRIPTION_ID if there should not be one
     * @param mobileDataEnabled Whether mobile data should be considered enabled for the default
     *                          data subscription
     */
    private List<SubscriptionInfo> setupMockSubscriptions(int count, int defaultDataSubId,
            boolean mobileDataEnabled) {
        if (defaultDataSubId != INVALID_SUBSCRIPTION_ID) {
            when(sInjector.getDefaultDataSubscriptionId()).thenReturn(defaultDataSubId);
        }
        final ArrayList<SubscriptionInfo> infos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int subscriptionId = i + 1;
            final SubscriptionInfo info = mock(SubscriptionInfo.class);
            final TelephonyManager mgrForSub = mock(TelephonyManager.class);
            final SignalStrength signalStrength = mock(SignalStrength.class);

            if (subscriptionId == defaultDataSubId) {
                when(mgrForSub.isDataEnabled()).thenReturn(mobileDataEnabled);
            }
            when(info.getSubscriptionId()).thenReturn(subscriptionId);
            when(info.getDisplayName()).thenReturn("sub" + (subscriptionId));
            doReturn(mgrForSub).when(mTelephonyManager).createForSubscriptionId(eq(subscriptionId));
            when(mgrForSub.getSignalStrength()).thenReturn(signalStrength);
            when(signalStrength.getLevel()).thenReturn(SIGNAL_STRENGTH_GOOD);
            doReturn(true).when(sInjector).canSubscriptionBeDisplayed(mContext, subscriptionId);
            infos.add(info);
        }
        SubscriptionUtil.setActiveSubscriptionsForTesting(infos);
        return infos;
    }

    private static class FakeSubscriptionsPreferenceController
            extends SubscriptionsPreferenceController {

        /**
         * @param context            the context for the UI where we're placing these preferences
         * @param lifecycle          for listening to lifecycle events for the UI
         * @param updateListener     called to let our parent controller know that our
         *                           availability has
         *                           changed, or that one or more of the preferences we've placed
         *                           in the
         *                           PreferenceGroup has changed
         * @param preferenceGroupKey the key used to lookup the PreferenceGroup where Preferences
         *                          will
         *                           be placed
         * @param startOrder         the order that should be given to the first Preference
         *                           placed into
         *                           the PreferenceGroup; the second will use startOrder+1, third
         *                           will
         *                           use startOrder+2, etc. - this is useful for when the parent
         *                           wants
         *                           to have other preferences in the same PreferenceGroup and wants
         */
        FakeSubscriptionsPreferenceController(Context context, Lifecycle lifecycle,
                UpdateListener updateListener, String preferenceGroupKey, int startOrder) {
            super(context, lifecycle, updateListener, preferenceGroupKey, startOrder);
        }

        @Override
        protected SubsPrefCtrlInjector createSubsPrefCtrlInjector() {
            return sInjector;
        }
    }
}
