/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
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
    private PhoneAccountHandle mPhoneAccountHandle;
    @Mock
    private ComponentName mComponentName;
    @Mock
    private ResolveInfo mResolveInfo;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;

    private Context mContext;
    private PersistableBundle mCarrierConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID_1)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID_2)).thenReturn(mTelephonyManager2);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mPhoneAccountHandle.getComponentName()).thenReturn(mComponentName);
        when(mComponentName.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mContext.getSystemService(CarrierConfigManager.class)).thenReturn(
                mCarrierConfigManager);

        mCarrierConfig = new PersistableBundle();
        when(mCarrierConfigManager.getConfigForSubId(SUB_ID_1)).thenReturn(mCarrierConfig);

        when(mSubscriptionInfo1.getSubscriptionId()).thenReturn(SUB_ID_1);
        when(mSubscriptionInfo1.getCarrierName()).thenReturn(PLMN_FROM_SUB_ID_1);
        when(mSubscriptionInfo2.getSubscriptionId()).thenReturn(SUB_ID_2);
        when(mSubscriptionInfo2.getCarrierName()).thenReturn(PLMN_FROM_SUB_ID_2);

        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));
        when(mSubscriptionManager.getAccessibleSubscriptionInfoList()).thenReturn(
                Arrays.asList(mSubscriptionInfo1, mSubscriptionInfo2));

        when(mTelephonyManager.getNetworkOperatorName()).thenReturn(
                PLMN_FROM_TELEPHONY_MANAGER_API);
    }

    @Test
    public void setMobileDataEnabled_setEnabled_enabled() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_1, true, false);

        verify(mTelephonyManager).setDataEnabled(true);
        verify(mTelephonyManager2, never()).setDataEnabled(anyBoolean());
    }

    @Test
    public void setMobileDataEnabled_setDisabled_disabled() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_2, true, false);

        verify(mTelephonyManager2).setDataEnabled(true);
        verify(mTelephonyManager, never()).setDataEnabled(anyBoolean());
    }

    @Test
    public void setMobileDataEnabled_disableOtherSubscriptions() {
        MobileNetworkUtils.setMobileDataEnabled(mContext, SUB_ID_1, true, true);

        verify(mTelephonyManager).setDataEnabled(true);
        verify(mTelephonyManager2).setDataEnabled(false);
    }

    @Test
    public void buildConfigureIntent_nullHandle_returnNull() {
        assertThat(MobileNetworkUtils.buildPhoneAccountConfigureIntent(mContext, null)).isNull();
    }

    @Test
    public void buildConfigureIntent_noActivityHandleIntent_returnNull() {
        when(mPackageManager.queryIntentActivities(nullable(Intent.class), anyInt()))
                .thenReturn(new ArrayList<>());

        assertThat(MobileNetworkUtils.buildPhoneAccountConfigureIntent(mContext,
                mPhoneAccountHandle)).isNull();
    }

    @Test
    public void buildConfigureIntent_hasActivityHandleIntent_returnIntent() {
        when(mPackageManager.queryIntentActivities(nullable(Intent.class), anyInt()))
                .thenReturn(Arrays.asList(mResolveInfo));

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

        Settings.Global.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID_1,
                TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA);

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
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, true);

        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isFalse();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdmaGsm_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);

        Settings.Global.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID_1,
                TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM);
        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdmaGsmWcdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);

        Settings.Global.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID_1,
                TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_GSM_WCDMA);
        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);

        Settings.Global.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID_1,
                TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA);
        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdmaWcdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);

        Settings.Global.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID_1,
                TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_WCDMA);
        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteTdscdmaCdmaEvdoGsmWcdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);

        Settings.Global.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID_1,
                TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA);
        assertThat(MobileNetworkUtils.shouldSpeciallyUpdateGsmCdma(mContext, SUB_ID_1)).isTrue();
    }

    @Test
    public void shouldSpeciallyUpdateGsmCdma_ModeLteCdmaEvdoGsmWcdma_returnTrue() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, false);

        Settings.Global.putInt(mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID_1,
                TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA);
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
}
