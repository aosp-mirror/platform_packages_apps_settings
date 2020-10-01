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
import android.telephony.TelephonyManager;
import android.telephony.ims.ProvisioningManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.network.ims.MockVolteQueryImsState;
import com.android.settings.network.ims.MockVtQueryImsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class VideoCallingPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private ProvisioningManager mProvisioningManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private MockVtQueryImsState mQueryImsState;
    private MockVolteQueryImsState mQueryVoLteState;

    private VideoCallingPreferenceController mController;
    private PersistableBundle mCarrierConfig;
    private SwitchPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        doReturn(mCarrierConfigManager).when(mContext)
                .getSystemService(CarrierConfigManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);

        mCarrierConfig = new PersistableBundle();
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS, true);
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mQueryImsState = new MockVtQueryImsState(mContext, SUB_ID);
        mQueryImsState.setIsEnabledByUser(true);

        mQueryVoLteState = new MockVolteQueryImsState(mContext, SUB_ID);
        mQueryVoLteState.setIsEnabledByUser(true);

        mPreference = new SwitchPreference(mContext);
        mController = spy(new VideoCallingPreferenceController(mContext, "wifi_calling"));
        mController.init(SUB_ID);
        doReturn(mQueryImsState).when(mController).queryImsState(anyInt());
        doReturn(mQueryVoLteState).when(mController).queryVoLteState(anyInt());
        mPreference.setKey(mController.getPreferenceKey());

        mQueryImsState.setIsEnabledByPlatform(true);
        mQueryImsState.setIsProvisionedOnDevice(true);
        mQueryImsState.setServiceStateReady(true);
        doReturn(true).when(mTelephonyManager).isDataEnabled();

        mController.mCallState = TelephonyManager.CALL_STATE_IDLE;
    }

    @Test
    public void isVideoCallEnabled_allFlagsOn_returnTrue() {
        assertThat(mController.isVideoCallEnabled(SUB_ID)).isTrue();
    }

    @Test
    public void isVideoCallEnabled_disabledByPlatform_returnFalse() {
        mQueryImsState.setIsProvisionedOnDevice(false);
        mQueryImsState.setIsEnabledByPlatform(false);

        assertThat(mController.isVideoCallEnabled(SUB_ID)).isFalse();
    }

    @Test
    public void isVideoCallEnabled_dataDisabled_returnFalse() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS, false);
        doReturn(false).when(mTelephonyManager).isDataEnabled();

        assertThat(mController.isVideoCallEnabled(SUB_ID)).isFalse();
    }

    @Test
    public void updateState_4gLteOff_disabled() {
        mQueryImsState.setIsEnabledByUser(false);
        mQueryVoLteState.setIsEnabledByUser(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_4gLteOnWithoutCall_checked() {
        mQueryImsState.setIsEnabledByUser(true);
        mQueryVoLteState.setIsEnabledByUser(true);
        mQueryImsState.setIsTtyOnVolteEnabled(true);
        mController.mCallState = TelephonyManager.CALL_STATE_IDLE;

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }


    @Test
    public void displayPreference_notAvailable_setPreferenceInvisible() {
        mQueryImsState.setIsEnabledByPlatform(false);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.isVisible()).isFalse();
    }

}
