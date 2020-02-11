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
import android.telephony.TelephonyManager;
import android.telephony.ims.feature.ImsFeature;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.ims.ImsManager;

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
    private ImsManager mImsManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private PreferenceScreen mPreferenceScreen;

    private VideoCallingPreferenceController mController;
    private PersistableBundle mCarrierConfig;
    private SwitchPreference mPreference;
    private Context mContext;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);

        mCarrierConfig = new PersistableBundle();
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS, true);
        doReturn(mCarrierConfig).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mPreference = new SwitchPreference(mContext);
        mController = new VideoCallingPreferenceController(mContext, "wifi_calling");
        mController.init(SUB_ID);
        mController.mImsManager = mImsManager;
        mPreference.setKey(mController.getPreferenceKey());

        doReturn(true).when(mImsManager).isVtEnabledByPlatform();
        doReturn(true).when(mImsManager).isVtProvisionedOnDevice();
        doReturn(ImsFeature.STATE_READY).when(mImsManager).getImsServiceState();
        doReturn(true).when(mTelephonyManager).isDataEnabled();
    }

    @Test
    public void isVideoCallEnabled_allFlagsOn_returnTrue() {
        assertThat(mController.isVideoCallEnabled(SUB_ID, mImsManager)).isTrue();
    }

    @Test
    public void isVideoCallEnabled_disabledByPlatform_returnFalse() {
        doReturn(false).when(mImsManager).isVtEnabledByPlatform();

        assertThat(mController.isVideoCallEnabled(SUB_ID, mImsManager)).isFalse();
    }

    @Test
    public void isVideoCallEnabled_dataDisabled_returnFalse() {
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_IGNORE_DATA_ENABLED_CHANGED_FOR_VIDEO_CALLS, false);
        doReturn(false).when(mTelephonyManager).isDataEnabled();

        assertThat(mController.isVideoCallEnabled(SUB_ID, mImsManager)).isFalse();
    }

    @Test
    public void updateState_4gLteOff_disabled() {
        doReturn(false).when(mImsManager).isEnhanced4gLteModeSettingEnabledByUser();

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void updateState_4gLteOnWithoutCall_checked() {
        doReturn(true).when(mImsManager).isVtEnabledByUser();
        doReturn(true).when(mImsManager).isEnhanced4gLteModeSettingEnabledByUser();
        doReturn(true).when(mImsManager).isNonTtyOrTtyOnVolteEnabled();
        doReturn(TelephonyManager.CALL_STATE_IDLE).when(mTelephonyManager).getCallState(SUB_ID);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }


    @Test
    public void displayPreference_notAvailable_setPreferenceInvisible() {
        doReturn(false).when(mImsManager).isVtEnabledByPlatform();

        mController.displayPreference(mPreferenceScreen);

        assertThat(mPreferenceScreen.isVisible()).isFalse();
    }

}
