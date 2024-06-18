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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrength;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NetworkSelectSettingsTest {
    private static final int SUB_ID = 2;
    private static final String CARRIER_NAME1 = "CarrierName1";
    private static final String CARRIER_NAME2 = "CarrierName2";

    @Mock
    public Resources mResources;
    @Mock
    public TelephonyManager mTelephonyManager;
    @Mock
    public CarrierConfigManager mCarrierConfigManager;
    @Mock
    public MetricsFeatureProvider mMetricsFeatureProvider;
    @Mock
    private CellInfo mCellInfo1;
    @Mock
    private CellIdentity mCellId1;
    @Mock
    private CellInfo mCellInfo2;
    @Mock
    private CellIdentity mCellId2;

    @Mock
    public PreferenceManager mPreferenceManager;

    public Context mContext;
    public PreferenceCategory mPreferenceCategory;
    public boolean mIsAggregationEnabled = true;

    private TargetClass mNetworkSelectSettings;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        doReturn(mResources).when(mContext).getResources();
        doReturn(mContext).when(mPreferenceManager).getContext();

        mPreferenceCategory = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceCategory).getPreferenceManager();
        doReturn(mCellId1).when(mCellInfo1).getCellIdentity();
        doReturn(mock(CellSignalStrength.class)).when(mCellInfo1).getCellSignalStrength();
        doReturn(CARRIER_NAME1).when(mCellId1).getOperatorAlphaLong();
        doReturn(mCellId2).when(mCellInfo2).getCellIdentity();
        doReturn(mock(CellSignalStrength.class)).when(mCellInfo2).getCellSignalStrength();
        doReturn(CARRIER_NAME2).when(mCellId2).getOperatorAlphaLong();
        mIsAggregationEnabled = true;
        mNetworkSelectSettings = spy(new TargetClass(this));

        PersistableBundle config = new PersistableBundle();
        config.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, true);
        doReturn(config).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        doReturn(TelephonyManager.DATA_CONNECTED).when(mTelephonyManager).getDataState();
    }

    public static class TargetClass extends NetworkSelectSettings {
        private final NetworkSelectSettingsTest mTestEnv;
        private boolean mIsPreferenceScreenEnabled;

        public TargetClass(NetworkSelectSettingsTest env) {
            mTestEnv = env;
        }

        @Override
        public Context getContext() {
            return mTestEnv.mContext;
        }

        @Override
        public PreferenceManager getPreferenceManager() {
            return mTestEnv.mPreferenceManager;
        }

        @Override
        protected PreferenceCategory getPreferenceCategory(String preferenceKey) {
            return mTestEnv.mPreferenceCategory;
        }

        @Override
        protected TelephonyManager getTelephonyManager(Context context, int subscriptionId) {
            return mTestEnv.mTelephonyManager;
        }

        @Override
        protected CarrierConfigManager getCarrierConfigManager(Context context) {
            return mTestEnv.mCarrierConfigManager;
        }

        @Override
        protected MetricsFeatureProvider getMetricsFeatureProvider(Context context) {
            return mTestEnv.mMetricsFeatureProvider;
        }

        @Override
        protected boolean isPreferenceScreenEnabled() {
            return mIsPreferenceScreenEnabled;
        }

        @Override
        protected void enablePreferenceScreen(boolean enable) {
            mIsPreferenceScreenEnabled = enable;
        }

        @Override
        protected NetworkOperatorPreference
                createNetworkOperatorPreference(CellInfo cellInfo) {
            NetworkOperatorPreference pref = super.createNetworkOperatorPreference(cellInfo);
            if (cellInfo == mTestEnv.mCellInfo1) {
                pref.updateCell(cellInfo, mTestEnv.mCellId1);
            } else if (cellInfo == mTestEnv.mCellInfo2) {
                pref.updateCell(cellInfo, mTestEnv.mCellId2);
            }
            return pref;
        }

        @Override
        protected boolean enableAggregation(Context context) {
            return mTestEnv.mIsAggregationEnabled;
        }

        @Override
        protected int getSubId() {
            return SUB_ID;
        }
    }

    @Test
    @UiThreadTest
    public void updateAllPreferenceCategory_correctOrderingPreference() {
        mNetworkSelectSettings.onCreateInitialization();
        mNetworkSelectSettings.enablePreferenceScreen(true);
        mNetworkSelectSettings.scanResultHandler(Arrays.asList(mCellInfo1, mCellInfo2));
        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(2);
        final NetworkOperatorPreference preference =
                (NetworkOperatorPreference) mPreferenceCategory.getPreference(1);
        assertThat(preference.getOperatorName()).isEqualTo(mCellId2.getOperatorAlphaLong());
    }

    @Test
    @UiThreadTest
    public void updateForbiddenPlmns_forbiddenPlmnsNull_shouldNotCrash() {
        when(mTelephonyManager.getForbiddenPlmns()).thenReturn(null);

        mNetworkSelectSettings.onCreateInitialization();
        mNetworkSelectSettings.enablePreferenceScreen(true);

        // Should not Crash
        mNetworkSelectSettings.updateForbiddenPlmns();
    }

    @Test
    public void doAggregation_hasDuplicateItemsDiffCellIdCase1_removeSamePlmnRatItem() {
        mNetworkSelectSettings.onCreateInitialization();
        List<CellInfo> testList = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createLteCellInfo(true, 1234, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "232", "CarrierB"));
        List<CellInfo> expected = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "232", "CarrierB"));
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);
    }

    @Test
    public void doAggregation_hasDuplicateItemsDiffCellIdCase2_removeSamePlmnRatItem() {
        mNetworkSelectSettings.onCreateInitialization();
        List<CellInfo> testList = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "232", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "232", "CarrierB"),
                createGsmCellInfo(false, 1234, "123", "232", "CarrierB"));
        List<CellInfo> expected = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "232", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "232", "CarrierB"));
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);
    }

    @Test
    public void doAggregation_hasDuplicateItemsDiffMccMncCase1_removeSamePlmnRatItem() {
        mNetworkSelectSettings.onCreateInitialization();
        List<CellInfo> testList = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createLteCellInfo(true, 123, "456", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "232", "CarrierB"));
        List<CellInfo> expected = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "232", "CarrierB"));
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);
    }

    @Test
    public void doAggregation_hasDuplicateItemsDiffMccMncCase2_removeSamePlmnRatItem() {
        mNetworkSelectSettings.onCreateInitialization();
        List<CellInfo> testList = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "232", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "232", "CarrierB"),
                createGsmCellInfo(false, 123, "456", "232", "CarrierB"));
        List<CellInfo> expected = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "232", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "232", "CarrierB"));
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);
    }

    @Test
    public void doAggregation_hasDuplicateItemsDiffMccMncCase3_removeSamePlmnRatItem() {
        mNetworkSelectSettings.onCreateInitialization();
        List<CellInfo> testList = Arrays.asList(
                createLteCellInfo(false, 123, "123", "232", "CarrierA"),
                createLteCellInfo(false, 124, "123", "233", "CarrierA"),
                createLteCellInfo(true, 125, "123", "234", "CarrierA"),
                createGsmCellInfo(false, 126, "456", "232", "CarrierA"));
        List<CellInfo> expected = Arrays.asList(
                createLteCellInfo(true, 125, "123", "234", "CarrierA"),
                createGsmCellInfo(false, 126, "456", "232", "CarrierA"));
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);
    }

    @Test
    public void doAggregation_filterOutSatellitePlmn_whenKeyIsTrue() {
        PersistableBundle config = new PersistableBundle();
        config.putBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL, true);
        doReturn(config).when(mCarrierConfigManager).getConfigForSubId(eq(SUB_ID),
                eq(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL),
                eq(CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL));

        List<String> testSatellitePlmn = new ArrayList<>(Arrays.asList("123232", "123235"));
        doReturn(testSatellitePlmn).when(
                mNetworkSelectSettings).getSatellitePlmnsForCarrierWrapper();

        /* Expect filter out satellite plmns when
           KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL is true, and there is available
           satellite plmns. */
        mNetworkSelectSettings.onCreateInitialization();
        List<CellInfo> testList = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "233", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "234", "CarrierC"),
                createGsmCellInfo(false, 1234, "123", "235", "CarrierD"));
        List<CellInfo> expected = Arrays.asList(
                createGsmCellInfo(false, 123, "123", "233", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "234", "CarrierC"));
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);
    }

    @Test
    public void doAggregation_filterOutSatellitePlmn_whenNoSatellitePlmnIsAvailable() {
        PersistableBundle config = new PersistableBundle();
        config.putBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL, true);
        doReturn(config).when(mCarrierConfigManager).getConfigForSubId(eq(SUB_ID),
                eq(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL),
                eq(CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL));

        List<String> testSatellitePlmn = new ArrayList<>(Arrays.asList("123232", "123235"));
        doReturn(testSatellitePlmn).when(
                mNetworkSelectSettings).getSatellitePlmnsForCarrierWrapper();

        // Expect no filter out when there is no available satellite plmns.
        mNetworkSelectSettings.onCreateInitialization();
        testSatellitePlmn = new ArrayList<>();
        doReturn(testSatellitePlmn).when(
                mNetworkSelectSettings).getSatellitePlmnsForCarrierWrapper();
        mNetworkSelectSettings.onCreateInitialization();
        List<CellInfo> testList = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "233", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "234", "CarrierC"),
                createGsmCellInfo(false, 12345, "123", "235", "CarrierD"));
        List<CellInfo> expected = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "233", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "234", "CarrierC"),
                createGsmCellInfo(false, 12345, "123", "235", "CarrierD"));
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);

        // Expect no filter out when KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL is false.
        config.putBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL, false);
        mNetworkSelectSettings.onCreateInitialization();
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);
    }

    @Test
    public void doAggregation_filterOutSatellitePlmn_whenKeyIsFalse() {
        PersistableBundle config = new PersistableBundle();
        config.putBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL, true);
        doReturn(config).when(mCarrierConfigManager).getConfigForSubId(eq(SUB_ID),
                eq(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL),
                eq(CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL));

        List<String> testSatellitePlmn = new ArrayList<>(Arrays.asList("123232", "123235"));
        doReturn(testSatellitePlmn).when(
                mNetworkSelectSettings).getSatellitePlmnsForCarrierWrapper();

        // Expect no filter out when KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL is false.
        config.putBoolean(
                CarrierConfigManager.KEY_REMOVE_SATELLITE_PLMN_IN_MANUAL_NETWORK_SCAN_BOOL, false);
        mNetworkSelectSettings.onCreateInitialization();
        List<CellInfo> testList = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "233", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "234", "CarrierC"),
                createGsmCellInfo(false, 12345, "123", "235", "CarrierD"));
        List<CellInfo> expected = Arrays.asList(
                createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                createGsmCellInfo(false, 123, "123", "233", "CarrierB"),
                createLteCellInfo(false, 1234, "123", "234", "CarrierC"),
                createGsmCellInfo(false, 12345, "123", "235", "CarrierD"));
        assertThat(mNetworkSelectSettings.doAggregation(testList)).isEqualTo(expected);
    }

    private CellInfoLte createLteCellInfo(boolean registered, int cellId, String mcc, String mnc,
            String plmnName) {
        CellIdentityLte cil = new CellIdentityLte(
                cellId, 5, 200, 2000, new int[]{1, 2}, 10000, mcc, mnc, plmnName, plmnName,
                Collections.emptyList(), null);
        CellSignalStrengthLte cssl = new CellSignalStrengthLte(15, 16, 17, 18, 19, 20);

        CellInfoLte cellInfoLte = new CellInfoLte();
        cellInfoLte.setRegistered(registered);
        cellInfoLte.setTimeStamp(22);
        cellInfoLte.setCellIdentity(cil);
        cellInfoLte.setCellSignalStrength(cssl);
        return cellInfoLte;
    }

    private CellInfoGsm createGsmCellInfo(boolean registered, int cellId, String mcc, String mnc,
            String plmnName) {
        CellIdentityGsm cig = new CellIdentityGsm(1, cellId, 40, 5, mcc, mnc, plmnName, plmnName,
                Collections.emptyList());
        CellSignalStrengthGsm cssg = new CellSignalStrengthGsm(5, 6, 7);
        CellInfoGsm cellInfoGsm = new CellInfoGsm();
        cellInfoGsm.setRegistered(registered);
        cellInfoGsm.setTimeStamp(9);
        cellInfoGsm.setCellIdentity(cig);
        cellInfoGsm.setCellSignalStrength(cssg);
        return cellInfoGsm;
    }

    @Test
    @UiThreadTest
    public void onPreferenceTreeClick_notNetworkOperatorPreference_noCrash() {
        mNetworkSelectSettings.onCreateInitialization();
        mNetworkSelectSettings.enablePreferenceScreen(true);

        mNetworkSelectSettings.onPreferenceTreeClick(new Preference(mContext));
    }
}
