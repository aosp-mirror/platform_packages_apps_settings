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

package com.android.settings.deviceinfo.simstatus;

import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.CELL_DATA_NETWORK_TYPE_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.CELL_VOICE_NETWORK_TYPE_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.ICCID_INFO_LABEL_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.ICCID_INFO_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.IMS_REGISTRATION_STATE_LABEL_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.IMS_REGISTRATION_STATE_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.MAX_PHONE_COUNT_SINGLE_SIM;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.NETWORK_PROVIDER_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.OPERATOR_INFO_LABEL_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.OPERATOR_INFO_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.ROAMING_INFO_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.SERVICE_STATE_VALUE_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.SIGNAL_STRENGTH_LABEL_ID;
import static com.android.settings.deviceinfo.simstatus.SimStatusDialogController.SIGNAL_STRENGTH_VALUE_ID;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.CellSignalStrength;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.euicc.EuiccManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.testutils.ResourcesUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(AndroidJUnit4.class)
public class SimStatusDialogControllerTest {

    @Mock
    private SimStatusDialogFragment mDialog;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionInfo mSubscriptionInfo;
    @Mock
    private ServiceState mServiceState;
    @Mock
    private SignalStrength mSignalStrength;
    @Mock
    private CellSignalStrength mCellSignalStrengthCdma;
    @Mock
    private CellSignalStrength mCellSignalStrengthLte;
    @Mock
    private CellSignalStrength mCellSignalStrengthWcdma;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    private PersistableBundle mPersistableBundle;
    @Mock
    private EuiccManager mEuiccManager;
    private SubscriptionManager mSubscriptionManager;

    private SimStatusDialogController mController;
    private Context mContext;
    @Mock
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;
    private AtomicBoolean mEuiccEnabled;
    private AtomicInteger mUpdatePhoneNumberCount;

    private static final int MAX_PHONE_COUNT_DUAL_SIM = 2;

    @Before
    @UiThreadTest
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mDialog.getContext()).thenReturn(mContext);
        mLifecycle = new Lifecycle(mLifecycleOwner);

        mTelephonyManager = spy(mContext.getSystemService(TelephonyManager.class));
        mSubscriptionManager = spy(mContext.getSystemService(SubscriptionManager.class));
        doReturn(mSubscriptionInfo).when(mSubscriptionManager)
                .getActiveSubscriptionInfoForSimSlotIndex(anyInt());

        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(CarrierConfigManager.class)).thenReturn(
                    mCarrierConfigManager);
        when(mContext.getSystemService(EuiccManager.class)).thenReturn(mEuiccManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);

        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                anyInt());
        doReturn(2).when(mTelephonyManager).getCardIdForDefaultEuicc();
        doReturn(TelephonyManager.NETWORK_TYPE_LTE).when(mTelephonyManager).getDataNetworkType();

        mUpdatePhoneNumberCount = new AtomicInteger();
        mEuiccEnabled = new AtomicBoolean(false);
        mController = new SimStatusDialogController(mDialog, mLifecycle, 0 /* phone id */) {
            @Override
            public TelephonyManager getTelephonyManager() {
                return mTelephonyManager;
            }

            @Override
            public void updatePhoneNumber() {
                super.updatePhoneNumber();
                mUpdatePhoneNumberCount.incrementAndGet();
            }
        };
        // CellSignalStrength setup
        doReturn(0).when(mCellSignalStrengthCdma).getDbm();
        doReturn(0).when(mCellSignalStrengthCdma).getAsuLevel();
        doReturn(0).when(mCellSignalStrengthLte).getDbm();
        doReturn(0).when(mCellSignalStrengthLte).getAsuLevel();
        doReturn(0).when(mCellSignalStrengthWcdma).getDbm();
        doReturn(0).when(mCellSignalStrengthWcdma).getAsuLevel();

        doReturn(null).when(mSignalStrength).getCellSignalStrengths();
        doReturn(mSubscriptionInfo).when(mSubscriptionManager).getActiveSubscriptionInfo(anyInt());

        when(mTelephonyManager.getActiveModemCount()).thenReturn(MAX_PHONE_COUNT_SINGLE_SIM);
        doReturn(new ArrayList<UiccCardInfo>()).when(mTelephonyManager).getUiccCardsInfo();
        doReturn(new HashMap<Integer, Integer>()).when(mTelephonyManager)
                .getLogicalToPhysicalSlotMapping();

        when(mEuiccManager.isEnabled()).thenReturn(false);
        mEuiccEnabled.set(false);
        when(mEuiccManager.createForCardId(anyInt())).thenReturn(mEuiccManager);

        mPersistableBundle = new PersistableBundle();
        when(mCarrierConfigManager.getConfigForSubId(anyInt())).thenReturn(mPersistableBundle);

        mPersistableBundle.putBoolean(
                CarrierConfigManager.KEY_SHOW_SIGNAL_STRENGTH_IN_SIM_STATUS_BOOL, true);
        doReturn(mServiceState).when(mTelephonyManager).getServiceState();
        doReturn(mSignalStrength).when(mTelephonyManager).getSignalStrength();
    }

    @Test
    public void initialize_updateNetworkProviderWithFoobarCarrier_shouldUpdateCarrierWithFoobar() {
        final CharSequence carrierName = "foobar";
        doReturn(carrierName).when(mSubscriptionInfo).getCarrierName();

        mController.initialize();

        verify(mDialog).setText(NETWORK_PROVIDER_VALUE_ID, carrierName);
    }

    @Test
    public void initialize_shouldUpdatePhoneNumber() {
        mController.initialize();

        assertTrue(mUpdatePhoneNumberCount.get() > 0);
    }

    @Test
    public void initialize_updateLatestAreaInfoWithCdmaPhone_shouldRemoveOperatorInfoSetting() {
        when(mTelephonyManager.getPhoneType()).thenReturn(TelephonyManager.PHONE_TYPE_CDMA);

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(OPERATOR_INFO_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(OPERATOR_INFO_VALUE_ID);
    }

    @Test
    public void initialize_updateServiceStateWithInService_shouldUpdateTextToBeCInService() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_IN_SERVICE);

        mController.initialize();

        final String inServiceText = ResourcesUtils.getResourcesString(
                mContext, "radioInfo_service_in");
        verify(mDialog).setText(SERVICE_STATE_VALUE_ID, inServiceText);
    }

    @Test
    public void initialize_updateServiceStateWithPowerOff_shouldUpdateTextAndResetSignalStrength() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_POWER_OFF);

        mController.initialize();

        final String offServiceText = ResourcesUtils.getResourcesString(
                mContext, "radioInfo_service_off");
        verify(mDialog).setText(SERVICE_STATE_VALUE_ID, offServiceText);
        verify(mDialog).setText(SIGNAL_STRENGTH_VALUE_ID, "0");
    }

    @Test
    public void initialize_updateVoiceDataOutOfService_shouldUpdateSettingAndResetSignalStrength() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_OUT_OF_SERVICE);
        when(mServiceState.getDataRegistrationState()).thenReturn(
                ServiceState.STATE_OUT_OF_SERVICE);

        mController.initialize();

        final String offServiceText = ResourcesUtils.getResourcesString(
                mContext, "radioInfo_service_out");
        verify(mDialog).setText(SERVICE_STATE_VALUE_ID, offServiceText);
        verify(mDialog).setText(SIGNAL_STRENGTH_VALUE_ID, "0");
    }

    @Test
    public void initialize_updateVoiceOutOfServiceDataInService_shouldUpdateTextToBeInService() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_OUT_OF_SERVICE);
        when(mServiceState.getDataRegistrationState()).thenReturn(ServiceState.STATE_IN_SERVICE);

        mController.initialize();

        final String inServiceText = ResourcesUtils.getResourcesString(
                mContext, "radioInfo_service_in");
        verify(mDialog).setText(SERVICE_STATE_VALUE_ID, inServiceText);
    }

    @Test
    public void initialize_updateSignalStrengthWithLte50Wcdma40_shouldUpdateSignalStrengthTo50() {
        final int lteDbm = 50;
        final int lteAsu = 50;
        final int wcdmaDbm = 40;
        final int wcdmaAsu = 40;
        setupCellSignalStrength_lteWcdma(lteDbm, lteAsu, wcdmaDbm, wcdmaAsu);

        mController.initialize();

        final String signalStrengthString = ResourcesUtils.getResourcesString(
                mContext, "sim_signal_strength", lteDbm, lteAsu);
        verify(mDialog, times(2)).setText(SIGNAL_STRENGTH_VALUE_ID, signalStrengthString);
    }

    @Test
    public void initialize_updateSignalStrengthWithLte50Cdma30_shouldUpdateSignalStrengthTo50() {
        final int lteDbm = 50;
        final int lteAsu = 50;
        final int cdmaDbm = 30;
        final int cdmaAsu = 30;
        setupCellSignalStrength_lteCdma(lteDbm, lteAsu, cdmaDbm, cdmaAsu);

        mController.initialize();

        final String signalStrengthString = ResourcesUtils.getResourcesString(
                mContext, "sim_signal_strength", lteDbm, lteAsu);
        verify(mDialog, times(2)).setText(SIGNAL_STRENGTH_VALUE_ID, signalStrengthString);
    }

    @Test
    public void initialize_updateVoiceOutOfServiceDataInService_shouldUpdateSignalStrengthTo50() {
        when(mServiceState.getState()).thenReturn(ServiceState.STATE_OUT_OF_SERVICE);
        when(mServiceState.getDataRegistrationState()).thenReturn(ServiceState.STATE_IN_SERVICE);

        final int lteDbm = 50;
        final int lteAsu = 50;
        setupCellSignalStrength_lteOnly(lteDbm, lteAsu);

        mController.initialize();

        final String signalStrengthString = ResourcesUtils.getResourcesString(
                mContext, "sim_signal_strength", lteDbm, lteAsu);
        verify(mDialog, times(2)).setText(SIGNAL_STRENGTH_VALUE_ID, signalStrengthString);
    }

    @Test
    public void initialize_updateVoiceNetworkTypeWithEdge_shouldUpdateSettingToEdge() {
        when(mTelephonyManager.getVoiceNetworkType()).thenReturn(
                TelephonyManager.NETWORK_TYPE_EDGE);

        mController.initialize();

        verify(mDialog).setText(CELL_VOICE_NETWORK_TYPE_VALUE_ID,
                SimStatusDialogController.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EDGE));
    }

    @Test
    public void initialize_updateDataNetworkTypeWithEdge_shouldUpdateSettingToEdge() {
        when(mTelephonyManager.getDataNetworkType()).thenReturn(
                TelephonyManager.NETWORK_TYPE_EDGE);

        mController.initialize();

        verify(mDialog).setText(CELL_DATA_NETWORK_TYPE_VALUE_ID,
                SimStatusDialogController.getNetworkTypeName(TelephonyManager.NETWORK_TYPE_EDGE));
    }

    @Test
    public void initialize_updateRoamingStatusIsRoaming_shouldSetSettingToRoaming() {
        when(mServiceState.getRoaming()).thenReturn(true);

        mController.initialize();

        final String roamingOnString = ResourcesUtils.getResourcesString(
                mContext, "radioInfo_roaming_in");
        verify(mDialog).setText(ROAMING_INFO_VALUE_ID, roamingOnString);
    }

    @Test
    public void initialize_updateRoamingStatusNotRoaming_shouldSetSettingToRoamingOff() {
        when(mServiceState.getRoaming()).thenReturn(false);

        mController.initialize();

        final String roamingOffString = ResourcesUtils.getResourcesString(
                mContext, "radioInfo_roaming_not");
        verify(mDialog).setText(ROAMING_INFO_VALUE_ID, roamingOffString);
    }

    @Test
    public void initialize_doNotShowIccid_shouldRemoveIccidSetting() {
        mPersistableBundle.putBoolean(
                CarrierConfigManager.KEY_SHOW_ICCID_IN_SIM_STATUS_BOOL, false);

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(ICCID_INFO_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(ICCID_INFO_VALUE_ID);
    }

    @Test
    public void initialize_doNotShowSignalStrength_shouldRemoveSignalStrengthSetting() {
        mPersistableBundle.putBoolean(
                CarrierConfigManager.KEY_SHOW_SIGNAL_STRENGTH_IN_SIM_STATUS_BOOL, false);

        mController.initialize();

        verify(mDialog, times(2)).removeSettingFromScreen(SIGNAL_STRENGTH_LABEL_ID);
        verify(mDialog, times(2)).removeSettingFromScreen(SIGNAL_STRENGTH_VALUE_ID);
    }

    @Test
    public void initialize_showSignalStrengthAndIccId_shouldShowSignalStrengthAndIccIdSetting() {
        // getConfigForSubId is nullable, so make sure the default behavior is correct
        when(mCarrierConfigManager.getConfigForSubId(anyInt())).thenReturn(null);

        mController.initialize();

        verify(mDialog, times(2)).setText(eq(SIGNAL_STRENGTH_VALUE_ID), any());
        verify(mDialog).removeSettingFromScreen(ICCID_INFO_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(ICCID_INFO_VALUE_ID);
    }

    @Test
    public void initialize_showIccid_shouldSetIccidToSetting() {
        final String iccid = "12351351231241";
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_SHOW_ICCID_IN_SIM_STATUS_BOOL, true);
        doReturn(iccid).when(mTelephonyManager).getSimSerialNumber();

        mController.initialize();

        verify(mDialog).setText(ICCID_INFO_VALUE_ID, iccid);
    }

    @Test
    @Ignore
    public void initialize_imsRegistered_shouldSetImsRegistrationStateSummaryToRegisterd() {
        mPersistableBundle.putBoolean(
                CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true);
        when(mTelephonyManager.isImsRegistered(anyInt())).thenReturn(true);

        mController.initialize();

        verify(mDialog).setText(IMS_REGISTRATION_STATE_VALUE_ID,
                mContext.getString(com.android.settingslib.R.string.ims_reg_status_registered));
    }

    @Test
    @Ignore
    public void initialize_imsNotRegistered_shouldSetImsRegistrationStateSummaryToNotRegisterd() {
        mPersistableBundle.putBoolean(
                CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true);
        when(mTelephonyManager.isImsRegistered(anyInt())).thenReturn(false);

        mController.initialize();

        verify(mDialog).setText(IMS_REGISTRATION_STATE_VALUE_ID,
                mContext.getString(com.android.settingslib.R.string.ims_reg_status_not_registered));
    }

    @Test
    @Ignore
    public void initialize_showImsRegistration_shouldNotRemoveImsRegistrationStateSetting() {
        mPersistableBundle.putBoolean(
                CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, true);

        mController.initialize();

        verify(mDialog, never()).removeSettingFromScreen(IMS_REGISTRATION_STATE_VALUE_ID);
    }

    @Test
    @Ignore
    public void initialize_doNotShowImsRegistration_shouldRemoveImsRegistrationStateSetting() {
        mPersistableBundle.putBoolean(
                CarrierConfigManager.KEY_SHOW_IMS_REGISTRATION_STATUS_BOOL, false);

        mController.initialize();

        verify(mDialog).removeSettingFromScreen(IMS_REGISTRATION_STATE_LABEL_ID);
        verify(mDialog).removeSettingFromScreen(IMS_REGISTRATION_STATE_VALUE_ID);
    }

    @Test
    public void initialize_nullSignalStrength_noCrash() {
        doReturn(null).when(mTelephonyManager).getSignalStrength();
        // we should not crash when running the following line
        mController.initialize();
    }

    private void setupCellSignalStrength_lteWcdma(int lteDbm, int lteAsu, int wcdmaDbm,
            int wcdmaAsu) {
        doReturn(lteDbm).when(mCellSignalStrengthLte).getDbm();
        doReturn(lteAsu).when(mCellSignalStrengthLte).getAsuLevel();
        doReturn(wcdmaDbm).when(mCellSignalStrengthWcdma).getDbm();
        doReturn(wcdmaAsu).when(mCellSignalStrengthWcdma).getAsuLevel();

        List<CellSignalStrength> cellSignalStrengthList = new ArrayList<>(2);
        cellSignalStrengthList.add(mCellSignalStrengthLte);
        cellSignalStrengthList.add(mCellSignalStrengthWcdma);

        doReturn(cellSignalStrengthList).when(mSignalStrength).getCellSignalStrengths();
    }

    private void setupCellSignalStrength_lteCdma(int lteDbm, int lteAsu, int cdmaDbm, int cdmaAsu) {
        doReturn(lteDbm).when(mCellSignalStrengthLte).getDbm();
        doReturn(lteAsu).when(mCellSignalStrengthLte).getAsuLevel();
        doReturn(cdmaDbm).when(mCellSignalStrengthCdma).getDbm();
        doReturn(cdmaAsu).when(mCellSignalStrengthCdma).getAsuLevel();

        List<CellSignalStrength> cellSignalStrengthList = new ArrayList<>(2);
        cellSignalStrengthList.add(mCellSignalStrengthLte);
        cellSignalStrengthList.add(mCellSignalStrengthCdma);

        doReturn(cellSignalStrengthList).when(mSignalStrength).getCellSignalStrengths();
    }

    private void setupCellSignalStrength_lteOnly(int lteDbm, int lteAsu) {
        doReturn(lteDbm).when(mCellSignalStrengthLte).getDbm();
        doReturn(lteAsu).when(mCellSignalStrengthLte).getAsuLevel();

        List<CellSignalStrength> cellSignalStrengthList = new ArrayList<>(2);
        cellSignalStrengthList.add(mCellSignalStrengthLte);

        doReturn(cellSignalStrengthList).when(mSignalStrength).getCellSignalStrengths();
    }
}
