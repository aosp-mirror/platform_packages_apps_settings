/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.deviceinfo.imei;

import static com.android.settings.deviceinfo.imei.ImeiInfoDialogController.ID_CDMA_SETTINGS;
import static com.android.settings.deviceinfo.imei.ImeiInfoDialogController.ID_GSM_SETTINGS;
import static com.android.settings.deviceinfo.imei.ImeiInfoDialogController.ID_IMEI_SV_VALUE;
import static com.android.settings.deviceinfo.imei.ImeiInfoDialogController.ID_IMEI_VALUE;
import static com.android.settings.deviceinfo.imei.ImeiInfoDialogController.ID_MEID_NUMBER_VALUE;
import static com.android.settings.deviceinfo.imei.ImeiInfoDialogController.ID_MIN_NUMBER_VALUE;
import static com.android.settings.deviceinfo.imei.ImeiInfoDialogController.ID_PRL_VERSION_VALUE;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowSubscriptionManager;
import org.robolectric.shadows.ShadowTelephonyManager;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
public class ImeiInfoDialogControllerTest {

    private static final String PRL_VERSION = "some_prl_version";
    private static final String MEID_NUMBER = "12871234124";
    private static final String IMEI_NUMBER = "2341982751254";
    private static final String MIN_NUMBER = "123417851315";
    private static final String IMEI_SV_NUMBER = "12";
    private static final int SLOT_ID = 0;
    private static final int SUB_ID = 0;

    @Mock
    private ImeiInfoDialogFragment mDialog;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;

    private Context mContext;
    private ImeiInfoDialogController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        final ShadowSubscriptionManager ssm = Shadow.extract(mContext.getSystemService(
                SubscriptionManager.class));
        ssm.setActiveSubscriptionInfos(mSubscriptionInfo);
        when(mSubscriptionInfo.getSubscriptionId()).thenReturn(SUB_ID);
        final ShadowTelephonyManager stm = Shadow.extract(mContext.getSystemService(
                TelephonyManager.class));
        stm.setTelephonyManagerForSubscriptionId(SUB_ID, mTelephonyManager);
        when(mDialog.getContext()).thenReturn(mContext);

        mController = spy(new ImeiInfoDialogController(mDialog, SLOT_ID));

        when(mTelephonyManager.getCdmaPrlVersion()).thenReturn(PRL_VERSION);
        when(mTelephonyManager.getMeid(anyInt())).thenReturn(MEID_NUMBER);
        when(mTelephonyManager.getCdmaMin(anyInt())).thenReturn(MIN_NUMBER);
        when(mTelephonyManager.getDeviceSoftwareVersion(anyInt())).thenReturn(IMEI_SV_NUMBER);
        when(mTelephonyManager.getImei(anyInt())).thenReturn(IMEI_NUMBER);
    }

    @Test
    public void populateImeiInfo_invalidSlot_shouldSetNothing() {
        mController = spy(new ImeiInfoDialogController(mDialog, SLOT_ID + 1));

        mController.populateImeiInfo();

        verify(mDialog, never()).setText(anyInt(), any());
    }

    @Test
    public void populateImeiInfo_cdmaLteEnabled_shouldSetMeidAndImeiAndMin() {
        doReturn(true).when(mController).isCdmaLteEnabled();
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);

        mController.populateImeiInfo();

        verify(mDialog).setText(ID_MEID_NUMBER_VALUE, MEID_NUMBER);
        verify(mDialog).setText(ID_MIN_NUMBER_VALUE, MIN_NUMBER);
        verify(mDialog).setText(ID_PRL_VERSION_VALUE, PRL_VERSION);
        verify(mDialog).setText(eq(ID_IMEI_VALUE), any());
        verify(mDialog).setText(eq(ID_IMEI_SV_VALUE), any());
    }

    @Test
    public void populateImeiInfo_cdmaLteDisabled_shouldSetMeidAndMinAndRemoveGsmSettings() {
        doReturn(false).when(mController).isCdmaLteEnabled();
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);

        mController.populateImeiInfo();

        verify(mDialog).setText(ID_MEID_NUMBER_VALUE, MEID_NUMBER);
        verify(mDialog).setText(ID_MIN_NUMBER_VALUE, MIN_NUMBER);
        verify(mDialog).setText(ID_PRL_VERSION_VALUE, PRL_VERSION);
        verify(mDialog).removeViewFromScreen(ID_GSM_SETTINGS);
    }

    @Test
    public void populateImeiInfo_cdmaSimDisabled_shouldRemoveImeiInfoAndSetMinPrlToEmpty() {
        ReflectionHelpers.setField(mController, "mSubscriptionInfo", null);
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);

        mController.populateImeiInfo();

        verify(mDialog).setText(ID_MEID_NUMBER_VALUE, MEID_NUMBER);
        verify(mDialog).setText(ID_MIN_NUMBER_VALUE, "");
        verify(mDialog).setText(ID_PRL_VERSION_VALUE, "");
        verify(mDialog).removeViewFromScreen(ID_GSM_SETTINGS);
    }


    @Test
    public void populateImeiInfo_cdmaSimPresent_shouldSetImeiInfoAndSetAllCdmaSetting() {
        ReflectionHelpers.setField(mController, "mSubscriptionInfo", null);
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);
        when(mTelephonyManager.getSimState(anyInt())).thenReturn(
                TelephonyManager.SIM_STATE_PRESENT);

        mController.populateImeiInfo();

        verify(mDialog).setText(ID_MEID_NUMBER_VALUE, MEID_NUMBER);
        verify(mDialog).setText(ID_MIN_NUMBER_VALUE, "");
        verify(mDialog).setText(ID_PRL_VERSION_VALUE, "");
        verify(mDialog).setText(eq(ID_IMEI_VALUE), any());
        verify(mDialog).setText(eq(ID_IMEI_SV_VALUE), any());
    }

    @Test
    public void populateImeiInfo_cdmaSimABSENT_shouldSetImeiInfoAndSetAllCdmaSetting() {
        ReflectionHelpers.setField(mController, "mSubscriptionInfo", null);
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);
        when(mTelephonyManager.getSimState(anyInt())).thenReturn(TelephonyManager.SIM_STATE_ABSENT);

        mController.populateImeiInfo();

        verify(mDialog).setText(ID_MEID_NUMBER_VALUE, MEID_NUMBER);
        verify(mDialog).setText(ID_MIN_NUMBER_VALUE, "");
        verify(mDialog).setText(ID_PRL_VERSION_VALUE, "");
        verify(mDialog).removeViewFromScreen(ID_GSM_SETTINGS);
    }

    @Test
    public void populateImeiInfo_gsmSimDisabled_shouldSetImeiAndRemoveCdmaSettings() {
        ReflectionHelpers.setField(mController, "mSubscriptionInfo", null);
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_GSM);

        mController.populateImeiInfo();

        verify(mDialog).setText(eq(ID_IMEI_VALUE), any());
        verify(mDialog).setText(eq(ID_IMEI_SV_VALUE), any());
        verify(mDialog).removeViewFromScreen(ID_CDMA_SETTINGS);
    }

    @Test
    public void populateImeinfo_gsm_shouldSetImeiAndRemoveCdmaSettings() {
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_GSM);

        mController.populateImeiInfo();

        verify(mDialog).setText(eq(ID_IMEI_VALUE), any());
        verify(mDialog).setText(eq(ID_IMEI_SV_VALUE), any());
        verify(mDialog).removeViewFromScreen(ID_CDMA_SETTINGS);
    }

    @Test
    public void populateImeiInfo_emptyImei_shouldSetMeid_imeiSetToEmptyString() {
        doReturn(true).when(mController).isCdmaLteEnabled();
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);
        when(mTelephonyManager.getImei(anyInt())).thenReturn(null);

        mController.populateImeiInfo();

        verify(mDialog).setText(ID_MEID_NUMBER_VALUE, MEID_NUMBER);
        verify(mDialog).setText(ID_MIN_NUMBER_VALUE, MIN_NUMBER);
        verify(mDialog).setText(ID_PRL_VERSION_VALUE, PRL_VERSION);
        verify(mDialog).setText(eq(ID_IMEI_VALUE), eq(""));
        verify(mDialog).setText(eq(ID_IMEI_SV_VALUE), any());
    }
}
