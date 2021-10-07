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

package com.android.settings.network.telephony.cdma;

import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.GSM;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.LTE;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.WCDMA;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CdmaSystemSelectPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private TelephonyManager mTelephonyManager;

    private CdmaSystemSelectPreferenceController mController;
    private ListPreference mPreference;
    private Context mContext;
    private int mCdmaRoamingMode;

    @UiThreadTest
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);

        mPreference = new ListPreference(mContext);
        mController = new CdmaSystemSelectPreferenceController(mContext, "mobile_data");
        mController.init(mPreferenceManager, SUB_ID);
        mController.mPreference = mPreference;
        mPreference.setKey(mController.getPreferenceKey());

        mCdmaRoamingMode = Settings.Global.getInt(mContext.getContentResolver(),
            Settings.Global.CDMA_ROAMING_MODE,
            TelephonyManager.CDMA_ROAMING_MODE_ANY);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(),
            Settings.Global.CDMA_ROAMING_MODE, mCdmaRoamingMode);
    }

    @Test
    public void onPreferenceChange_selectHome_returnHomeMode() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.CDMA_ROAMING_MODE,
                TelephonyManager.CDMA_ROAMING_MODE_ANY);

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
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | GSM | WCDMA));

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
