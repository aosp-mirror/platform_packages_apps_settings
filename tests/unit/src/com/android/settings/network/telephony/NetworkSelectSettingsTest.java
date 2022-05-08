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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.CellIdentityGsm;
import android.telephony.CellIdentityLte;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NetworkSelectSettingsTest {
    private static final int SUB_ID = 2;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private SubscriptionManager mSubscriptionManager;
    @Mock
    private PreferenceManager mPreferenceManager;
    @Mock
    private SharedPreferences mSharedPreferences;

    private Context mContext;
    private PreferenceCategory mPreferenceCategory;
    private NetworkSelectSettings mNetworkSelectSettings;

    @Before
    @UiThreadTest
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());

        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(SubscriptionManager.class)).thenReturn(mSubscriptionManager);
        when(mTelephonyManager.createForSubscriptionId(SUB_ID)).thenReturn(mTelephonyManager);

        doReturn(mSharedPreferences).when(mPreferenceManager).getSharedPreferences();
        mPreferenceCategory = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceCategory).getPreferenceManager();

        mNetworkSelectSettings = spy(new NetworkSelectSettings());
        doReturn(mContext).when(mNetworkSelectSettings).getContext();
        doReturn(mPreferenceManager).when(mNetworkSelectSettings).getPreferenceManager();
        doReturn(mContext).when(mPreferenceManager).getContext();

        mNetworkSelectSettings.mTelephonyManager = mTelephonyManager;
        mNetworkSelectSettings.mPreferenceCategory = mPreferenceCategory;
        mNetworkSelectSettings.mCellInfoList =
                Arrays.asList(createLteCellInfo(true, 123, "123", "232", "CarrierA"),
                        createGsmCellInfo(false, 123, "123", "232", "CarrierB"));
        mNetworkSelectSettings.mIsAggregationEnabled = true;
    }

    @Test
    @UiThreadTest
    public void updateAllPreferenceCategory_correctOrderingPreference() {
        mNetworkSelectSettings.updateAllPreferenceCategory();

        assertThat(mPreferenceCategory.getPreferenceCount()).isEqualTo(2);
        final NetworkOperatorPreference preference =
                (NetworkOperatorPreference) mPreferenceCategory.getPreference(1);
        assertThat(preference.getOperatorName()).isEqualTo("CarrierB");
    }

    @Test
    @UiThreadTest
    public void updateForbiddenPlmns_forbiddenPlmnsNull_shouldNotCrash() {
        when(mTelephonyManager.getForbiddenPlmns()).thenReturn(null);

        // Should not Crash
        mNetworkSelectSettings.updateForbiddenPlmns();
    }

    @Test
    public void doAggregation_hasDuplicateItemsDiffCellIdCase1_removeSamePlmnRatItem() {
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

    private CellInfoLte createLteCellInfo(boolean registered, int cellId, String mcc, String mnc,
            String plmnName) {
        CellIdentityLte cil = new CellIdentityLte(
                cellId, 5, 200, 2000, new int[]{1, 2}, 10000, new String(mcc),
                new String(mnc), new String(plmnName), new String(plmnName),
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
        CellIdentityGsm cig = new CellIdentityGsm(1, cellId, 40, 5, new String(mcc),
                new String(mnc), new String(plmnName), new String(plmnName),
                Collections.emptyList());
        CellSignalStrengthGsm cssg = new CellSignalStrengthGsm(5, 6, 7);
        CellInfoGsm cellInfoGsm = new CellInfoGsm();
        cellInfoGsm.setRegistered(registered);
        cellInfoGsm.setTimeStamp(9);
        cellInfoGsm.setCellIdentity(cig);
        cellInfoGsm.setCellSignalStrength(cssg);
        return cellInfoGsm;
    }
}
