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

package com.android.settings.network.telephony;

import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.CDMA;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.EVDO;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.GSM;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.LTE;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.RAF_TD_SCDMA;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.WCDMA;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.PersistableBundle;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.ims.MockWfcQueryImsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(AndroidJUnit4.class)
public class MobileNetworkUtilsTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int SUB_ID_1 = 1;
    private static final int SUB_ID_2 = 2;
    private static final int SUB_ID_INVALID = -1;
    private static final String PLMN_FROM_TELEPHONY_MANAGER_API = "testPlmn";
    private static final String PLMN_FROM_SUB_ID_1 = "testPlmnSub1";
    private static final String PLMN_FROM_SUB_ID_2 = "testPlmnSub2";

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mTelephonyManager2;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo1;
    @Mock
    private SubscriptionInfo mSubscriptionInfo2;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ResolveInfo mResolveInfo;
    @Mock
    private CarrierConfigCache mCarrierConfigCache;
    @Mock
    private ConnectivityManager mConnectivityManager;
    @Mock
    private TelecomManager mTelecomManager;

    private Context mContext;
    private PersistableBundle mCarrierConfig;
    private PhoneAccountHandle mPhoneAccountHandle;
    private ComponentName mComponentName;
    private NetworkCapabilities mNetworkCapabilities;
    private Network mNetwork;
    private MockWfcQueryImsState mMockQueryWfcState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mSubscriptionManager.createForAllUserProfiles()).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID_1)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID_2)).thenReturn(mTelephonyManager2);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);
        mCarrierConfig = new PersistableBundle();
        when(mCarrierConfigCache.getConfigForSubId(SUB_ID_1)).thenReturn(mCarrierConfig);

        mNetwork = mock(Network.class, CALLS_REAL_METHODS);
        when(mContext.getSystemService(ConnectivityManager.class)).thenReturn(mConnectivityManager);
        when(mConnectivityManager.getActiveNetwork()).thenReturn(mNetwork);

        when(mSubscriptionInfo1.getSubscriptionId()).thenReturn(SUB_ID_1);
        when(mSubscriptionInfo1.getCarrierName()).thenReturn(PLMN_FROM_SUB_ID_1);
        when(mSubscriptionInfo2.getSubscriptionId()).thenReturn(SUB_ID_2);
        when(mSubscriptionInfo2.getCarrierName()).thenReturn(PLMN_FROM_SUB_ID_2);

        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));

        when(mTelephonyManager.getNetworkOperatorName()).thenReturn(
                PLMN_FROM_TELEPHONY_MANAGER_API);

        when(mContext.getSystemService(TelecomManager.class)).thenReturn(mTelecomManager);
        when(mTelecomManager.getSimCallManagerForSubscription(SUB_ID_1))
                .thenReturn(mPhoneAccountHandle);
        mMockQueryWfcState = new MockWfcQueryImsState(mContext, SUB_ID_1);
    }

    @Test
    public void setMobileDataEnabled_setEnabled_enabled() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_1, true, false);

        verify(mTelephonyManager)
                .setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, true);
        verify(mTelephonyManager2, never())
                .setDataEnabledForReason(anyInt(), anyBoolean());
    }

    @Test
    public void setMobileDataEnabled_setDisabled_disabled() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_2, true, false);

        verify(mTelephonyManager2)
                .setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, true);
        verify(mTelephonyManager, never())
                .setDataEnabledForReason(anyInt(), anyBoolean());
    }

    @Test
    public void setMobileDataEnabled_disableOtherSubscriptions() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_1, true, true);

        verify(mTelephonyManager)
                .setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, true);
        verify(mTelephonyManager2)
                .setDataEnabledForReason(TelephonyManager.DATA_ENABLED_REASON_USER, false);
    }

    @Test
    public void buildConfigureIntent_nullHandle_returnNull() {
        assertThat(MobileNetworkUtils.buildPhoneAccountConfigureIntent(mContext, null)).isNull();
    }

    @Test
    public void buildConfigureIntent_noActivityHandleIntent_returnNull() {
        buildPhoneAccountConfigureIntent(false);

        assertThat(MobileNetworkUtils.buildPhoneAccountConfigureIntent(mContext,
                mPhoneAccountHandle)).isNull();
    }

    @Test
    public void buildConfigureIntent_hasActivityHandleIntent_returnIntent() {
        buildPhoneAccountConfigureIntent(true);

        assertThat(MobileNetworkUtils.buildPhoneAccountConfigureIntent(mContext,
                mPhoneAccountHandle)).isNotNull();
    }

    @Test
    public void isCdmaOptions_phoneTypeCdma_returnTrue() {
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);

        assertThat(MobileNetworkUtils.isCdmaOptions(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void isCdmaOptions_worldModeWithGsmWcdma_returnTrue() {
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_GSM);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        // NETWORK_MODE_LTE_GSM_WCDMA = LTE | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | GSM | WCDMA));

        assertThat(MobileNetworkUtils.isCdmaOptions(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void isCdmaOptions_carrierWorldModeWithoutHideCarrier_returnTrue() {
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_GSM);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                false);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL, true);

        assertThat(MobileNetworkUtils.isCdmaOptions(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void getSearchableSubscriptionId_oneActive_returnValid() {
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1));
        assertThat(MobileNetworkUtils.getSearchableSubscriptionId(mContext)).isEqualTo(SUB_ID_1);
    }

    @Test
    public void getSearchableSubscriptionId_nonActive_returnInvalid() {
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(new ArrayList<>());

        assertThat(MobileNetworkUtils.getSearchableSubscriptionId(mContext))
                .isEqualTo(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    @Test
    public void shouldDisplayNetworkSelectOptions_HideCarrierNetwork_returnFalse() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                true);

        assertThat(MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, SUB_ID_1))
                .isFalse();
    }

    @Test
    public void shouldDisplayNetworkSelectOptions_allCheckPass_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                false);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_OPERATOR_SELECTION_EXPAND_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_CSP_ENABLED_BOOL, false);
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_GSM);

        assertThat(MobileNetworkUtils.shouldDisplayNetworkSelectOptions(mContext, SUB_ID_1))
                .isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_notWorldMode_returnFalse() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, false);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isFalse();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_supportTdscdma_returnFalse() {
        when(mCarrierConfigCache.getConfig()).thenReturn(mCarrierConfig);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, true);

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isFalse();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdmaGsm_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);
        // NETWORK_MODE_LTE_TDSCDMA_GSM = LTE | RAF_TD_SCDMA | GSM
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | RAF_TD_SCDMA | GSM));

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdmaGsmWcdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);
        // NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA = LTE | RAF_TD_SCDMA | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | RAF_TD_SCDMA | GSM | WCDMA));

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);
        // NETWORK_MODE_LTE_TDSCDMA = LTE | RAF_TD_SCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | RAF_TD_SCDMA));

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdmaWcdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);
        // NETWORK_MODE_LTE_TDSCDMA_WCDMA = LTE | RAF_TD_SCDMA | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | RAF_TD_SCDMA | WCDMA));

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdmaCdmaEvdoGsmWcdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);
        // NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA
        //     = LTE | RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | RAF_TD_SCDMA | CDMA | EVDO | GSM | WCDMA));

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteCdmaEvdoGsmWcdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);
        // NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA = LTE | CDMA | EVDO | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | CDMA | EVDO | GSM | WCDMA));

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void getCurrentCarrierNameForDisplay_withoutValidSubId_returnNetworkOperatorName() {
        assertThat(MobileNetworkUtils.getCurrentCarrierNameForDisplay(
                mContext, SUB_ID_INVALID)).isEqualTo(PLMN_FROM_TELEPHONY_MANAGER_API);
    }

    @Test
    public void getCurrentCarrierNameForDisplay_withValidSubId_returnCurrentCarrierName() {
        assertThat(MobileNetworkUtils.getCurrentCarrierNameForDisplay(
                mContext, SUB_ID_1)).isEqualTo(PLMN_FROM_SUB_ID_1);
        assertThat(MobileNetworkUtils.getCurrentCarrierNameForDisplay(
                mContext, SUB_ID_2)).isEqualTo(PLMN_FROM_SUB_ID_2);
    }

    @Test
    public void getCurrentCarrierNameForDisplay_withoutSubId_returnNotNull() {
        assertThat(MobileNetworkUtils.getCurrentCarrierNameForDisplay(
                mContext)).isNotNull();
    }

    @Test
    public void isCellularNetwork_withCellularNetwork_returnTrue() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);

        assertTrue(MobileNetworkUtils.activeNetworkIsCellular(mContext));
    }

    @Test
    public void isCellularNetwork_withWifiNetwork_returnFalse() {
        addNetworkTransportType(NetworkCapabilities.TRANSPORT_WIFI);

        assertFalse(MobileNetworkUtils.activeNetworkIsCellular(mContext));
    }

    private void addNetworkTransportType (int networkType) {
        mNetworkCapabilities = new NetworkCapabilities.Builder()
                .addTransportType(networkType).build();
        when(mConnectivityManager.getNetworkCapabilities(mNetwork)).thenReturn(
                mNetworkCapabilities);
    }

    @Test
    public void isWifiCallingEnabled_wifiCallingIsReady_returnTrue() {
        setWifiCallingEnabled(true);

        assertTrue(MobileNetworkUtils.isWifiCallingEnabled(mContext, SUB_ID_1,
                mMockQueryWfcState));
    }

    @Test
    public void isWifiCallingEnabled_wifiCallingNotReady_returnFalse() {
        setWifiCallingEnabled(false);

        assertFalse(MobileNetworkUtils.isWifiCallingEnabled(mContext, SUB_ID_1,
                mMockQueryWfcState));
    }

    private void setWifiCallingEnabled(boolean enabled){
        mMockQueryWfcState.setIsEnabledByUser(enabled);
        mMockQueryWfcState.setServiceStateReady(enabled);
        mMockQueryWfcState.setIsEnabledByPlatform(enabled);
        mMockQueryWfcState.setIsProvisionedOnDevice(enabled);
    }

    private void buildPhoneAccountConfigureIntent(boolean hasActivityHandleIntent) {
        mComponentName = new ComponentName(PACKAGE_NAME, "testClass");
        mPhoneAccountHandle = new PhoneAccountHandle(mComponentName, "");
        when(mPackageManager.queryIntentActivities(nullable(Intent.class), anyInt()))
                .thenReturn(
                        hasActivityHandleIntent ? Arrays.asList(mResolveInfo) : new ArrayList<>());
    }
}
