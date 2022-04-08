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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.android.settings.network.ApnSettings;
import com.android.settingslib.RestrictedPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class ApnPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;

    private ApnPreferenceController mController;
    private RestrictedPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);

        mPreference = new RestrictedPreference(mContext);
        mController = new ApnPreferenceController(mContext, "mobile_data");
        mController.init(SUB_ID);
        mController.setPreference(mPreference);
        mController.mCarrierConfigManager = mCarrierConfigManager;
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_apnSettingsNotSupported_returnUnavailable() {
        doReturn(TelephonyManager.PHONE_TYPE_CDMA).when(mTelephonyManager).getPhoneType();
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_SHOW_APN_SETTING_CDMA_BOOL, false);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_apnSettingsSupportedWithCDMA_returnAvailable() {
        doReturn(TelephonyManager.PHONE_TYPE_CDMA).when(mTelephonyManager).getPhoneType();
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_SHOW_APN_SETTING_CDMA_BOOL, true);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_apnSettingsSupportedWithGsm_returnAvailable() {
        doReturn(TelephonyManager.PHONE_TYPE_GSM).when(mTelephonyManager).getPhoneType();
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL, true);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_carrierConfigNull_returnUnavailable() {
        doReturn(TelephonyManager.PHONE_TYPE_GSM).when(mTelephonyManager).getPhoneType();
        when(mCarrierConfigManager.getConfigForSubId(SUB_ID)).thenReturn(null);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }


    @Test
    public void getAvailabilityStatus_hideCarrierNetworkSettings_returnUnavailable() {
        doReturn(TelephonyManager.PHONE_TYPE_GSM).when(mTelephonyManager).getPhoneType();
        final PersistableBundle bundle = new PersistableBundle();
        bundle.putBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL, true);
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL, true);
        doReturn(bundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void handPreferenceTreeClick_fireIntent() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(captor.capture());

        mController.handlePreferenceTreeClick(mPreference);

        final Intent intent = captor.getValue();
        assertThat(intent.getAction()).isEqualTo(Settings.ACTION_APN_SETTINGS);
        assertThat(intent.getIntExtra(ApnSettings.SUB_ID, 0)).isEqualTo(SUB_ID);
    }
}
