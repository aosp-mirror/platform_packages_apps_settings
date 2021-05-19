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
import android.content.res.Resources;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

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
    public NetworkOperatorPreference mNetworkOperatorPreference1;
    @Mock
    public NetworkOperatorPreference mNetworkOperatorPreference2;
    @Mock
    private CellInfo mCellInfo1;
    @Mock
    private CellIdentity mCellId1;
    @Mock
    private CellInfo mCellInfo2;
    @Mock
    private CellIdentity mCellId2;

    private PreferenceScreen mPreferenceScreen;
    @Mock
    public PreferenceManager mPreferenceManager;

    public Context mContext;
    public PreferenceCategory mPreferenceCategory;

    private Bundle mInitArguments;
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

        doReturn(CARRIER_NAME1).when(mCellId1).getOperatorAlphaLong();
        doReturn(CARRIER_NAME2).when(mCellId2).getOperatorAlphaLong();

        mNetworkSelectSettings = spy(new TargetClass(this));

        PersistableBundle config = new PersistableBundle();
        config.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL, true);
        doReturn(config).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        doReturn(TelephonyManager.DATA_CONNECTED).when(mTelephonyManager).getDataState();
    }

    public class TargetClass extends NetworkSelectSettings {
        private NetworkSelectSettingsTest mTestEnv;
        private boolean mIsPreferenceScreenEnabled;

        public TargetClass(NetworkSelectSettingsTest env) {
            mTestEnv = env;

            Bundle bundle = new Bundle();
            bundle.putInt(Settings.EXTRA_SUB_ID, SUB_ID);
            setArguments(bundle);
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
}
