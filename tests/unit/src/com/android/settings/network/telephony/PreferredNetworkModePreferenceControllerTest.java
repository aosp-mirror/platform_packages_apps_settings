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

import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_CONNECTED;
import static android.telephony.satellite.SatelliteManager.SATELLITE_MODEM_STATE_OFF;

import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.GSM;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.RAF_TD_SCDMA;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.WCDMA;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.platform.test.annotations.EnableFlags;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.ListPreference;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.flags.Flags;
import com.android.settings.network.CarrierConfigCache;
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
    public void updateState_updateByNetworkMode() {
        // NETWORK_MODE_TDSCDMA_GSM_WCDMA = RAF_TD_SCDMA | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (RAF_TD_SCDMA | GSM | WCDMA));

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA));
        assertThat(mPreference.getSummary()).isEqualTo(
                ResourcesUtils.getResourcesString(mContext,
                        "preferred_network_mode_tdscdma_gsm_wcdma_summary"));
    }

    @Test
    @UiThreadTest
    @EnableFlags(Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void updateState_satelliteIsStartedAndSelectedSubForSatellite_disablePreference() {
        mController.mSatelliteModemStateCallback
                .onSatelliteModemStateChanged(SATELLITE_MODEM_STATE_CONNECTED);
        mController.mSelectedNbIotSatelliteSubscriptionCallback
                .onSelectedNbIotSatelliteSubscriptionChanged(SUB_ID);

        mController.updateState(mPreference);

        assertFalse(mPreference.isEnabled());
    }

    @Test
    @UiThreadTest
    @EnableFlags(Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void updateState_satelliteIsIdle_enablePreference() {
        mController.mSatelliteModemStateCallback
                .onSatelliteModemStateChanged(SATELLITE_MODEM_STATE_OFF);
        mController.mSelectedNbIotSatelliteSubscriptionCallback
                .onSelectedNbIotSatelliteSubscriptionChanged(SUB_ID);

        mController.updateState(mPreference);

        assertTrue(mPreference.isEnabled());
    }

    @Test
    @UiThreadTest
    @EnableFlags(Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void updateState_notSelectedSubForSatellite_enablePreference() {
        mController.mSatelliteModemStateCallback
                .onSatelliteModemStateChanged(SATELLITE_MODEM_STATE_CONNECTED);
        mController.mSelectedNbIotSatelliteSubscriptionCallback
                .onSelectedNbIotSatelliteSubscriptionChanged(0);

        mController.updateState(mPreference);

        assertTrue(mPreference.isEnabled());
    }

    @Test
    public void onPreferenceChange_updateNetworkMode() {
        mController.onPreferenceChange(mPreference,
                String.valueOf(TelephonyManager.NETWORK_MODE_LTE_TDSCDMA));

        verify(mTelephonyManager, times(1)).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER,
                RadioAccessFamily.getRafFromNetworkType(TelephonyManager.NETWORK_MODE_LTE_TDSCDMA));
    }
}
