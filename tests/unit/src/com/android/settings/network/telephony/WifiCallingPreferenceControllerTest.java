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

import static org.junit.Assert.assertNull;
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
import android.telephony.ims.ImsMmTelManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.network.ims.MockWifiCallingQueryImsState;
import com.android.settings.network.ims.WifiCallingQueryImsState;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class WifiCallingPreferenceControllerTest {
    private static final int SUB_ID = 2;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private ImsMmTelManager mImsMmTelManager;

    private PreferenceScreen mScreen;
    private PreferenceManager mPreferenceManager;

    private MockWifiCallingQueryImsState mQueryImsState;

    private TestWifiCallingPreferenceController mController;
    private Preference mPreference;
    private Context mContext;
    private PersistableBundle mCarrierConfig;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        mQueryImsState = new MockWifiCallingQueryImsState(mContext, SUB_ID);
        mQueryImsState.setIsEnabledByUser(true);
        mQueryImsState.setIsProvisionedOnDevice(true);

        mController = new TestWifiCallingPreferenceController(mContext, "wifi_calling");
        mController.mCarrierConfigManager = mCarrierConfigManager;
        mController.init(SUB_ID);
        mController.mCallState = TelephonyManager.CALL_STATE_IDLE;
        mCarrierConfig = new PersistableBundle();
        when(mCarrierConfigManager.getConfigForSubId(SUB_ID)).thenReturn(mCarrierConfig);

        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mPreference = new Preference(mContext);
        mPreference.setKey(mController.getPreferenceKey());
        mScreen.addPreference(mPreference);
    }

    @Test
    @UiThreadTest
    public void updateState_noSimCallManager_setCorrectSummary() {
        mController.mSimCallManager = null;
        mQueryImsState.setIsEnabledByUser(true);
        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_ONLY);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(com.android.internal.R.string.wfc_mode_wifi_only_summary));
    }

    @Test
    @UiThreadTest
    public void updateState_notCallIdle_disable() {
        mController.mCallState = TelephonyManager.CALL_STATE_RINGING;

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    @UiThreadTest
    public void updateState_invalidPhoneAccountHandle_shouldNotCrash() {
        mController.mSimCallManager = new PhoneAccountHandle(null /* invalid */, "");

        //Should not crash
        mController.updateState(mPreference);
    }

    @Test
    @UiThreadTest
    public void updateState_wfcNonRoamingByConfig() {
        assertNull(mController.mSimCallManager);
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_USE_WFC_HOME_NETWORK_MODE_IN_ROAMING_NETWORK_BOOL, true);
        mController.init(SUB_ID);

        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED);
        mQueryImsState.setIsEnabledByUser(true);
        when(mTelephonyManager.isNetworkRoaming()).thenReturn(true);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.wfc_mode_cellular_preferred_summary));
    }

    @Test
    @UiThreadTest
    public void updateState_wfcRoamingByConfig() {
        assertNull(mController.mSimCallManager);
        // useWfcHomeModeForRoaming is false by default. In order to check wfc in roaming mode. We
        // need the device roaming, and not using home mode in roaming network.
        when(mImsMmTelManager.getVoWiFiRoamingModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED);
        when(mImsMmTelManager.getVoWiFiModeSetting()).thenReturn(
                ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED);
        mQueryImsState.setIsEnabledByUser(true);
        when(mTelephonyManager.isNetworkRoaming()).thenReturn(true);

        mController.updateState(mPreference);
        assertThat(mPreference.getSummary())
                .isEqualTo(mContext.getString(R.string.wfc_mode_wifi_preferred_summary));
    }

    @Test
    @UiThreadTest
    public void displayPreference_notAvailable_setPreferenceInvisible() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(null);

        mController.displayPreference(mScreen);

        assertThat(mController.getPreferenceKey()).isEqualTo("wifi_calling");
        assertThat(mScreen.findPreference(mController.getPreferenceKey()).isVisible()).isFalse();
    }

    @Test
    @Ignore
    public void displayPreference_available_setsSubscriptionIdOnIntent() {
        final Intent intent = new Intent();
        mPreference.setIntent(intent);
        mController.displayPreference(mScreen);
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID)).isEqualTo(SUB_ID);
    }

    @Test
    @UiThreadTest
    public void getAvailabilityStatus_noWiFiCalling_shouldReturnUnsupported() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        when(mSubscriptionManager.getActiveSubscriptionInfoList()).thenReturn(null);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    private class TestWifiCallingPreferenceController extends WifiCallingPreferenceController {
        TestWifiCallingPreferenceController(Context context, String preferenceKey) {
            super(context, preferenceKey);
        }

        @Override
        protected ImsMmTelManager getImsMmTelManager(int subId) {
            return mImsMmTelManager;
        }

        @Override
        protected TelephonyManager getTelephonyManager(Context context, int subId) {
            return mTelephonyManager;
        }

        @Override
        protected WifiCallingQueryImsState queryImsState(int subId) {
            return mQueryImsState;
        }
    }
}
