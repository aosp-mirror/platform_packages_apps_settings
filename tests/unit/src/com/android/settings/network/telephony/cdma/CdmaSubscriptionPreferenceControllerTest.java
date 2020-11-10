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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class CdmaSubscriptionPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private TelephonyManager mTelephonyManager;

    private CdmaSubscriptionPreferenceController mController;
    private ListPreference mPreference;
    private Context mContext;
    private int mCdmaMode;
    private String mSubscriptionsSupported;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);

        mPreference = new ListPreference(mContext);
        mController = spy(new CdmaSubscriptionPreferenceController(mContext, "mobile_data"));
        mController.init(mPreferenceManager, SUB_ID);
        mController.mPreference = mPreference;
        mPreference.setKey(mController.getPreferenceKey());

        mCdmaMode = Settings.Global.getInt(mContext.getContentResolver(),
            Settings.Global.CDMA_SUBSCRIPTION_MODE,
            TelephonyManager.CDMA_SUBSCRIPTION_RUIM_SIM);
    }

    @After
    public void tearDown() {
        Settings.Global.putInt(mContext.getContentResolver(),
            Settings.Global.CDMA_SUBSCRIPTION_MODE, mCdmaMode);
    }

    @Test
    public void onPreferenceChange_selectNV_returnNVMode() {
        mController.onPreferenceChange(mPreference, Integer.toString(
                TelephonyManager.CDMA_SUBSCRIPTION_NV));

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE,
                TelephonyManager.CDMA_SUBSCRIPTION_RUIM_SIM)).isEqualTo(
                        TelephonyManager.CDMA_SUBSCRIPTION_NV);
    }

    @Test
    public void updateState_stateRUIM_displayRUIM() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE, TelephonyManager.CDMA_SUBSCRIPTION_NV);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(Integer.toString(
                TelephonyManager.CDMA_SUBSCRIPTION_NV));
    }

    @Test
    public void updateState_stateUnknown_doNothing() {
        mPreference.setValue(Integer.toString(TelephonyManager.CDMA_SUBSCRIPTION_NV));
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.CDMA_SUBSCRIPTION_MODE, TelephonyManager.CDMA_SUBSCRIPTION_UNKNOWN);

        mController.updateState(mPreference);

        // Still NV mode
        assertThat(mPreference.getValue()).isEqualTo(Integer.toString(
                TelephonyManager.CDMA_SUBSCRIPTION_NV));
    }

    @Test
    public void deviceSupportsNvAndRuim() {
        doReturn("NV,RUIM").when(mController).getRilSubscriptionTypes();
        assertThat(mController.deviceSupportsNvAndRuim()).isTrue();
        doReturn("").when(mController).getRilSubscriptionTypes();
        assertThat(mController.deviceSupportsNvAndRuim()).isFalse();
    }
}
