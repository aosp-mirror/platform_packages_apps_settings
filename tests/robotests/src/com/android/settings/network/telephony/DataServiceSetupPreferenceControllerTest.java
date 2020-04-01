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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import androidx.preference.Preference;

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
public class DataServiceSetupPreferenceControllerTest {
    private static final int SUB_ID = 2;

    private static final String SETUP_URL = "url://tmp_url:^1";

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;

    private PersistableBundle mCarrierConfig;
    private DataServiceSetupPreferenceController mController;
    private Preference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL, SETUP_URL);

        mCarrierConfig = new PersistableBundle();
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mPreference = new RestrictedPreference(mContext);
        mController = new DataServiceSetupPreferenceController(mContext, "data_service_setup");
        mController.init(SUB_ID);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_allConfigOn_returnAvailable() {
        doReturn(true).when(mTelephonyManager).isLteCdmaEvdoGsmWcdmaEnabled();
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_missUrl_returnUnavailable() {
        Settings.Global.putString(mContext.getContentResolver(),
                Settings.Global.SETUP_PREPAID_DATA_SERVICE_URL, "");
        doReturn(true).when(mTelephonyManager).isLteCdmaEvdoGsmWcdmaEnabled();
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                false);

        mController = new DataServiceSetupPreferenceController(mContext, "data_service_setup");
        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notCdma_returnUnavailable() {
        doReturn(false).when(mTelephonyManager).isLteCdmaEvdoGsmWcdmaEnabled();
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_startActivity() {
        ArgumentCaptor<Intent> captor = ArgumentCaptor.forClass(Intent.class);
        doNothing().when(mContext).startActivity(captor.capture());

        mController.handlePreferenceTreeClick(mPreference);

        final Intent intent = captor.getValue();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isEqualTo(
                Uri.parse(TextUtils.expandTemplate(SETUP_URL, "").toString()));
    }
}
