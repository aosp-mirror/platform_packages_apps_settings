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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
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
import com.android.settings.network.ims.MockVolteQueryImsState;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class Enhanced4gBasePreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigCache mCarrierConfigCache;

    private MockVolteQueryImsState mQueryImsState;

    private Enhanced4gLtePreferenceController mController;
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

        mCarrierConfig = new PersistableBundle();
        doReturn(mCarrierConfig).when(mCarrierConfigCache).getConfigForSubId(SUB_ID);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_HIDE_ENHANCED_4G_LTE_BOOL, false);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);
        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 1);

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
        doReturn(true).when(mController).isCallStateIdle();
        doReturn(1).when(mController).getMode();
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
    public void getAvailabilityStatus_modeMismatch_returnUnavailable() {
        doReturn(2).when(mController).getMode();

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
        mPreference.setEnabled(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
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
        mQueryImsState.setIsEnabledByUser(true);
        mPreference.setChecked(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }
}
