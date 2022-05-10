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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.GSM;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.RAF_TD_SCDMA;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.WCDMA;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.ListPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants;
import com.android.settings.testutils.ResourcesUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PreferredNetworkModePreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private CarrierConfigCache mCarrierConfigCache;
    @Mock
    private ServiceState mServiceState;

    private PersistableBundle mPersistableBundle;
    private PreferredNetworkModePreferenceController mController;
    private ListPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);

        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mServiceState).when(mTelephonyManager).getServiceState();
        mPersistableBundle = new PersistableBundle();
        doReturn(mPersistableBundle).when(mCarrierConfigCache).getConfigForSubId(SUB_ID);

        mPreference = new ListPreference(mContext);
        mController = new PreferredNetworkModePreferenceController(mContext, "mobile_data");
        mController.init(SUB_ID);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_hideCarrierNetworkSettings_returnUnavailable() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_worldPhone_returnAvailable() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                false);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL, true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hidePreferredNetworkType_returnUnavailable() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL,
                true);

        when(mServiceState.getState()).thenReturn(ServiceState.STATE_OUT_OF_SERVICE);
        when(mServiceState.getDataRegistrationState()).thenReturn(
                ServiceState.STATE_OUT_OF_SERVICE);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);

        when(mServiceState.getState()).thenReturn(ServiceState.STATE_IN_SERVICE);
        when(mServiceState.getDataRegistrationState()).thenReturn(ServiceState.STATE_IN_SERVICE);

        when(mServiceState.getRoaming()).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);

        when(mServiceState.getRoaming()).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState_updateByNetworkMode() {
        // NETWORK_MODE_TDSCDMA_GSM_WCDMA = RAF_TD_SCDMA | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (RAF_TD_SCDMA | GSM | WCDMA));

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA));
        assertThat(mPreference.getSummary()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext,
                        "preferred_network_mode_tdscdma_gsm_wcdma_summary"));
    }

    @Test
    public void onPreferenceChange_updateNetworkMode() {
        mController.onPreferenceChange(mPreference,
                String.valueOf(TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA));

        verify(mTelephonyManager, times(1)).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER,
                MobileNetworkUtils.getRafFromNetworkType(
                        TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA));
    }
}
