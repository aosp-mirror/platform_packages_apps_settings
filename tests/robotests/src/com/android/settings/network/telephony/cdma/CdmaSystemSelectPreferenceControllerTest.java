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

package com.android.settings.network.telephony.cdma;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;

import com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class CdmaSystemSelectPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;

    private CdmaSystemSelectPreferenceController mController;
    private ListPreference mPreference;
    private PersistableBundle mCarrierConfig;
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

        mCarrierConfig = new PersistableBundle();
        when(mCarrierConfigManager.getConfigForSubId(SUB_ID)).thenReturn(mCarrierConfig);


        mPreference = new ListPreference(mContext);
        mController = new CdmaSystemSelectPreferenceController(mContext, "mobile_data");
        mController.init(mPreferenceManager, SUB_ID);
        mController.mPreference = mPreference;
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void onPreferenceChange_selectHome_returnHomeMode() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.CDMA_ROAMING_MODE,
                TelephonyManager.CDMA_ROAMING_MODE_ANY);
        doReturn(true).when(mTelephonyManager).setCdmaRoamingMode(anyInt());

        mController.onPreferenceChange(mPreference,
                Integer.toString(TelephonyManager.CDMA_ROAMING_MODE_HOME));

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CDMA_ROAMING_MODE,
                TelephonyManager.CDMA_ROAMING_MODE_ANY)).isEqualTo(
                TelephonyManager.CDMA_ROAMING_MODE_HOME);
    }

    @Test
    public void updateState_stateHome_displayHome() {
        doReturn(TelephonyManager.CDMA_ROAMING_MODE_HOME).when(
                mTelephonyManager).getCdmaRoamingMode();

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                Integer.toString(TelephonyManager.CDMA_ROAMING_MODE_HOME));
    }

    @Test
    public void updateState_LteGSMWcdma_disabled() {
        doReturn(TelephonyManager.CDMA_ROAMING_MODE_HOME).when(
                mTelephonyManager).getCdmaRoamingMode();
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID,
                TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_stateOther_resetToDefault() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.CDMA_ROAMING_MODE,
                TelephonyManager.CDMA_ROAMING_MODE_HOME);
        doReturn(TelephonyManager.CDMA_ROAMING_MODE_AFFILIATED).when(
                mTelephonyManager).getCdmaRoamingMode();

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                Integer.toString(TelephonyManager.CDMA_ROAMING_MODE_ANY));
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CDMA_ROAMING_MODE,
                TelephonyManager.CDMA_ROAMING_MODE_HOME)).isEqualTo(
                TelephonyManager.CDMA_ROAMING_MODE_ANY);
    }
}
