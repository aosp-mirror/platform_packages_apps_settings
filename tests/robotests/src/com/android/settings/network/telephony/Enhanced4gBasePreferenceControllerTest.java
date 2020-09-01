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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.ims.ProvisioningManager;

import androidx.preference.SwitchPreference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.ims.MockVolteQueryImsState;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class Enhanced4gBasePreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private ProvisioningManager mProvisioningManager;

    private MockVolteQueryImsState mQueryImsState;

    private Enhanced4gLtePreferenceController mController;
    private SwitchPreference mPreference;
    private PersistableBundle mCarrierConfig;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mCarrierConfig = new PersistableBundle();
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mQueryImsState = spy(new MockVolteQueryImsState(mContext, SUB_ID));
        mQueryImsState.setEnabledByPlatform(true);
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        mQueryImsState.setServiceStateReady(true);
        mQueryImsState.setIsEnabledByUser(true);

        mPreference = new RestrictedSwitchPreference(mContext);
        mController = spy(new Enhanced4gLtePreferenceController(mContext, "VoLTE"));
        mController.init(SUB_ID);
        doReturn(mQueryImsState).when(mController).queryImsState(anyInt());
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_default_returnUnavailable() {
        mQueryImsState.setEnabledByPlatform(false);
        mQueryImsState.setIsProvisionedOnDevice(false);

        mController.init(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_volteDisabled_returnUnavailable() {
        mQueryImsState.setEnabledByPlatform(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState_configEnabled_prefEnabled() {
        mQueryImsState.setIsEnabledByUser(true);
        mPreference.setEnabled(false);
        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 1);
        mController.mCallState = TelephonyManager.CALL_STATE_IDLE;
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_configOn_prefChecked() {
        mQueryImsState.setIsEnabledByUser(true);
        mPreference.setChecked(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }
}
