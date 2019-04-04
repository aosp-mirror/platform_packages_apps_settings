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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.SwitchPreference;

import com.android.ims.ImsManager;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.RestrictedSwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class Enhanced4gLtePreferenceControllerTest {
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
    private ImsManager mImsManager;

    private Enhanced4gLtePreferenceController mController;
    private SwitchPreference mPreference;
    private PersistableBundle mCarrierConfig;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mSubscriptionManager).when(mContext).getSystemService(SubscriptionManager.class);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mCarrierConfig = new PersistableBundle();
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mPreference = new RestrictedSwitchPreference(mContext);
        mController = new Enhanced4gLtePreferenceController(mContext, "roaming");
        mController.init(SUB_ID);
        mController.mImsManager = mImsManager;
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_invalidSubId_returnUnavailable() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_volteDisabled_returnUnavailable() {
        doReturn(false).when(mImsManager).isVolteEnabledByPlatform();
        doReturn(true).when(mImsManager).isVolteProvisionedOnDevice();

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void updateState_doNotShow4GForLTE_showVolteTitleAndSummary() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, false);

        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 0);
        mController.updateState(mPreference);
        assertThat(mPreference.getTitle()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_title));
        assertThat(mPreference.getSummary()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_summary));

        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 2);
        mController.updateState(mPreference);
        assertThat(mPreference.getTitle()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_title));
        assertThat(mPreference.getSummary()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_summary));
    }

    @Test
    public void updateState_show4GForLTE_show4GTitleAndSummary() {
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, true);

        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 0);
        mController.updateState(mPreference);
        assertThat(mPreference.getTitle()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_title_4g_calling));
        assertThat(mPreference.getSummary()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_summary_4g_calling));

        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 2);
        mController.updateState(mPreference);
        assertThat(mPreference.getTitle()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_title_4g_calling));
        assertThat(mPreference.getSummary()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_summary_4g_calling));
    }

    @Test
    public void updateState_variantAdvancedCalling_showAdvancedCallingTitleAndSummary() {
        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 1);

        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, false);
        mController.updateState(mPreference);
        assertThat(mPreference.getTitle()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_title_advanced_calling));
        assertThat(mPreference.getSummary()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_summary));

        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, true);
        mController.updateState(mPreference);
        assertThat(mPreference.getTitle()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_title_advanced_calling));
        assertThat(mPreference.getSummary()).isEqualTo(
            mContext.getString(R.string.enhanced_4g_lte_mode_summary));
    }

    @Test
    public void updateState_configEnabled_prefEnabled() {
        mPreference.setEnabled(false);
        mCarrierConfig.putInt(CarrierConfigManager.KEY_ENHANCED_4G_LTE_TITLE_VARIANT_INT, 1);
        doReturn(TelephonyManager.CALL_STATE_IDLE).when(mTelephonyManager).getCallState(SUB_ID);
        doReturn(true).when(mImsManager).isNonTtyOrTtyOnVolteEnabled();
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_EDITABLE_ENHANCED_4G_LTE_BOOL, true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }

    @Test
    public void updateState_configOn_prefChecked() {
        mPreference.setChecked(false);
        doReturn(true).when(mImsManager).isEnhanced4gLteModeSettingEnabledByUser();
        doReturn(true).when(mImsManager).isNonTtyOrTtyOnVolteEnabled();

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }
}
