/*
 * Copyright (C) 2021 The Android Open Source Project
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.TwoStatePreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.CarrierConfigCache;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class NrAdvancedCallingPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigCache mCarrierConfigCache;

    private NrAdvancedCallingPreferenceController mController;
    private TwoStatePreference mPreference;
    private PersistableBundle mCarrierConfig;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);

        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(TelephonyManager.NETWORK_TYPE_BITMASK_NR).when(
                mTelephonyManager).getSupportedRadioAccessFamily();
        doReturn(false).when(mTelephonyManager).isVoNrEnabled();
        doReturn(TelephonyManager.ENABLE_VONR_REQUEST_NOT_SUPPORTED).when(
                mTelephonyManager).setVoNrEnabled(anyBoolean());
        mCarrierConfig = new PersistableBundle();
        doReturn(mCarrierConfig).when(mCarrierConfigCache).getConfigForSubId(SUB_ID);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true);
        mCarrierConfig.putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                new int[]{1, 2});

        mPreference = new RestrictedSwitchPreference(mContext);
        mController = spy(new NrAdvancedCallingPreferenceController(mContext, "VoNr"));
        mController.init(SUB_ID);
        doReturn(true).when(mController).isCallStateIdle();
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_vonrEnabledAndVisibleDisable_returnUnavailable() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, false);

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_vonrDisabledAndVisibleDisable_returnUnavailable() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, false);

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_vonrDisabledAndVisibleEnable_returnUnavailable() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, false);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true);

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_vonrEnabledAndVisibleEnable_returnAvailable() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_ENABLED_BOOL, true);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_VONR_SETTING_VISIBILITY_BOOL, true);

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_deviceNoNr_returnUnavailable() {
        doReturn(TelephonyManager.NETWORK_TYPE_BITMASK_LTE).when(
                mTelephonyManager).getSupportedRadioAccessFamily();

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_carrierNoNr_returnUnavailable() {
        mCarrierConfig.putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                new int[0]);

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_carrierConfigNrIsNull_returnUnavailable() {
        mCarrierConfig.putIntArray(CarrierConfigManager.KEY_CARRIER_NR_AVAILABILITIES_INT_ARRAY,
                null);

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState_callStateNotIdle_prefDisabled() {
        doReturn(false).when(mController).isCallStateIdle();
        mPreference.setEnabled(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_configOn_prefChecked() {
        doReturn(TelephonyManager.ENABLE_VONR_SUCCESS).when(
                mTelephonyManager).setVoNrEnabled(anyBoolean());
        doReturn(true).when(mTelephonyManager).isVoNrEnabled();
        mPreference.setChecked(false);

        mController.init(SUB_ID);
        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }
}
