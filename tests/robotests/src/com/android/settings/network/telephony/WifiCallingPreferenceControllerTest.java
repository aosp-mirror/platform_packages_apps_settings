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

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class WifiCallingPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private ImsManager mImsManager;

    private WifiCallingPreferenceController mController;
    private Preference mPreference;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);

        mPreference = new Preference(mContext);
        mController = new WifiCallingPreferenceController(mContext, "wifi_calling");
        mController.init(SUB_ID);
        mController.mImsManager = mImsManager;
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void updateState_noSimCallManager_setCorrectSummary() {
        mController.mSimCallManager = null;
        doReturn(true).when(mImsManager).isWfcEnabledByUser();
        doReturn(ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY).when(mImsManager).getWfcMode(
                anyBoolean());

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(
                mContext.getString(com.android.internal.R.string.wfc_mode_wifi_only_summary));
    }

    @Test
    public void updateState_notCallIdle_disable() {
        doReturn(TelephonyManager.CALL_STATE_RINGING).when(mTelephonyManager).getCallState(SUB_ID);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

}
