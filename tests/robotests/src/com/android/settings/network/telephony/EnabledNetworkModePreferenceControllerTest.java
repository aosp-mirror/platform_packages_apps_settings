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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.ListPreference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class EnabledNetworkModePreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;

    private PersistableBundle mPersistableBundle;
    private EnabledNetworkModePreferenceController mController;
    private ListPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mContext).when(mContext).createPackageContext(anyString(), anyInt());
        mPersistableBundle = new PersistableBundle();
        doReturn(mPersistableBundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mPreference = new ListPreference(mContext);
        mPreference.setEntries(R.array.enabled_networks_choices);
        mPreference.setEntryValues(R.array.enabled_networks_values);
        mController = new EnabledNetworkModePreferenceController(mContext, "enabled_network");
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
    public void getAvailabilityStatus_notWorldPhone_returnAvailable() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                false);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL, false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void init_initShow4GForLTE() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
                true);

        mController.init(SUB_ID);

        assertThat(mController.mShow4GForLTE).isTrue();
    }

    @Test
    public void updateState_updateByNetworkMode() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID,
                TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA));
        assertThat(mPreference.getSummary()).isEqualTo("3G");
    }

    @Test
    public void updateState_updateByNetworkMode_useDefaultValue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID,
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA));
    }

    @Test
    public void onPreferenceChange_updateSuccess() {
        doReturn(true).when(mTelephonyManager).setPreferredNetworkType(SUB_ID,
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.onPreferenceChange(mPreference,
                String.valueOf(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA));

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID, 0)).isEqualTo(
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);
    }

    @Test
    public void onPreferenceChange_updateFail() {
        doReturn(false).when(mTelephonyManager).setPreferredNetworkType(SUB_ID,
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.onPreferenceChange(mPreference,
                String.valueOf(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA));

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID, 0)).isNotEqualTo(
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);
    }
}
