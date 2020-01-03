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

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telecom.PhoneAccountHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class WifiCallingPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private ImsManager mImsManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private WifiCallingPreferenceController mController;
    private Preference mPreference;
    private PreferenceCategory mPreferenceCategory;
    private Context mContext;
    private PersistableBundle mCarrierConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(mTelephonyManager);

        mPreference = new Preference(mContext);
        mController = new WifiCallingPreferenceController(mContext, "wifi_calling");
        mController.mCarrierConfigManager = mCarrierConfigManager;
        mController.init(SUB_ID);
        mController.mImsManager = mImsManager;
        mPreference.setKey(mController.getPreferenceKey());

        mCarrierConfig = new PersistableBundle();
        when(mCarrierConfigManager.getConfigForSubId(SUB_ID)).thenReturn(mCarrierConfig);

        mPreferenceCategory = new PreferenceCategory(mContext);
        when(mPreferenceScreen.findPreference(
                WifiCallingPreferenceController.KEY_PREFERENCE_CATEGORY)).thenReturn(
                mPreferenceCategory);
        when(mPreferenceScreen.findPreference(mController.getPreferenceKey())).thenReturn(
                mPreference);
    }

    @Test
    public void updateState_noSimCallManager_setCorrectSummary() {
        mController.mSimCallManager = null;
        when(mImsManager.isWfcEnabledByUser()).thenReturn(true);
        when(mImsManager.getWfcMode(anyBoolean())).thenReturn(
                ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(com.android.internal.R.string.wfc_mode_wifi_only_summary));
    }

    @Test
    public void updateState_notCallIdle_disable() {
        when(mTelephonyManager.getCallState(SUB_ID)).thenReturn(
                TelephonyManager.CALL_STATE_RINGING);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_invalidPhoneAccountHandle_shouldNotCrash() {
        mController.mSimCallManager = new PhoneAccountHandle(null /* invalid */, "");

        //Should not crash
        mController.updateState(mPreference);
    }

    @Test
    public void updateState_nonRoaming_wfcCellularPreferred() {
        assertNull(mController.mSimCallManager);
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);
        mController.init(SUB_ID);
        mController.mImsManager = mImsManager;

        when(mImsManager.getWfcMode(true)).thenReturn(
                ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED);
        when(mImsManager.getWfcMode(false)).thenReturn(
                ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED);
        when(mImsManager.isWfcEnabledByUser()).thenReturn(true);
        when(mTelephonyManager.isNetworkRoaming()).thenReturn(true);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.wfc_mode_cellular_preferred_summary));
    }

    @Test
    public void updateState_roaming_wfcWifiPreferred() {
        assertNull(mController.mSimCallManager);
        // useWfcHomeModeForRoaming is false by default. In order to check wfc in roaming mode. We
        // need the device roaming, and not using home mode in roaming network.
        when(mImsManager.getWfcMode(true)).thenReturn(
                ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED);
        when(mImsManager.getWfcMode(false)).thenReturn(
                ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED);
        when(mImsManager.isWfcEnabledByUser()).thenReturn(true);
        when(mTelephonyManager.isNetworkRoaming()).thenReturn(true);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.wfc_mode_wifi_preferred_summary));
    }

    @Test
    public void displayPreference_notAvailable_setCategoryInvisible() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceCategory.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_available_setsSubscriptionIdOnIntent() {
        Intent intent = new Intent();
        mPreference.setIntent(intent);
        mController.displayPreference(mPreferenceScreen);
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(SUB_ID);
    }

    @Test
    public void getAvailabilityStatus_noWiFiCalling_shouldReturnUnsupported() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }
}
