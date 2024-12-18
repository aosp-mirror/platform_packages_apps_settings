/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import static com.android.settings.network.telephony.MobileNetworkUtils.getRafFromNetworkType;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.CDMA;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.EVDO;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.GSM;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.LTE;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.NR;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.RAF_TD_SCDMA;
import static com.android.settings.network.telephony.TelephonyConstants.RadioAccessFamily.WCDMA;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.testing.TestLifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.CarrierConfigCache;
import com.android.settings.network.telephony.TelephonyConstants.TelephonyManagerConstants;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class EnabledNetworkModePreferenceControllerTest {
    private static final int SUB_ID = 2;
    public static final String KEY = "enabled_network";

    private static final long ALLOWED_ALL_NETWORK_TYPE = -1;
    private static final long DISABLED_5G_NETWORK_TYPE = ~TelephonyManager.NETWORK_TYPE_BITMASK_NR;

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private CarrierConfigCache mCarrierConfigCache;
    @Mock
    private ServiceState mServiceState;
    @Mock
    private FragmentManager mFragmentManager;

    private PersistableBundle mPersistableBundle;
    private EnabledNetworkModePreferenceController mController;
    private ListPreference mPreference;
    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @UiThreadTest
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = spy(ApplicationProvider.getApplicationContext());

        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mServiceState).when(mTelephonyManager).getServiceState();
        mPersistableBundle = new PersistableBundle();
        doReturn(mPersistableBundle).when(mCarrierConfigCache).getConfig();
        doReturn(mPersistableBundle).when(mCarrierConfigCache).getConfigForSubId(SUB_ID);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_CARRIER_CONFIG_APPLIED_BOOL, true);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_PREFER_3G_VISIBILITY_BOOL, true);
        mPreference = new ListPreference(mContext);
        mController = new EnabledNetworkModePreferenceController(mContext, KEY);
        mockAllowedNetworkTypes(ALLOWED_ALL_NETWORK_TYPE);
        mockAccessFamily(TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA);
        when(mFragmentManager.getFragments()).thenReturn(Collections.emptyList());
        mController.init(SUB_ID, mFragmentManager);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @UiThreadTest
    @Test
    public void updateState_LteWorldPhone_GlobalHasLte() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);

        mController.updateState(mPreference);

        assertThat(mPreference.getEntryValues())
                .asList()
                .contains(String.valueOf(TelephonyManager.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void updateState_5gWorldPhone_GlobalHasNr() {
        mockAllowedNetworkTypes(ALLOWED_ALL_NETWORK_TYPE);
        mockAccessFamily(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA);
        mController.init(SUB_ID, mFragmentManager);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);

        mController.updateState(mPreference);

        assertThat(mPreference.getEntryValues())
                .asList()
                .contains(String.valueOf(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void updateState_selectedOn5gItem() {
        mockAllowedNetworkTypes(ALLOWED_ALL_NETWORK_TYPE);
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA);
        mockAccessFamily(TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA);
        mController.init(SUB_ID, mFragmentManager);

        // NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA = NR | LTE | RAF_TD_SCDMA | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (NR | LTE | RAF_TD_SCDMA | GSM | WCDMA));

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(
                        TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void updateState_disAllowed5g_5gOptionHidden() {
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA);
        mockAccessFamily(TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA);
        mockAllowedNetworkTypes(DISABLED_5G_NETWORK_TYPE);
        mController.init(SUB_ID, mFragmentManager);

        // NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA = NR | LTE | RAF_TD_SCDMA | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (NR | LTE | RAF_TD_SCDMA | GSM | WCDMA));
        mController.updateState(mPreference);

        assertThat(mPreference.getEntryValues())
                .asList()
                .doesNotContain(
                        String.valueOf(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void updateState_disAllowed5g_selectOn4gOption() {
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA);
        mockAccessFamily(TelephonyManager.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA);
        mockAllowedNetworkTypes(DISABLED_5G_NETWORK_TYPE);
        mController.init(SUB_ID, mFragmentManager);

        // NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA = NR | LTE | RAF_TD_SCDMA | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (NR | LTE | RAF_TD_SCDMA | GSM | WCDMA));
        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(
                        TelephonyManagerConstants.NETWORK_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void updateState_NrEnableBoolFalse_5gOptionHidden() {
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA);
        mockAccessFamily(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA);
        mockAllowedNetworkTypes(DISABLED_5G_NETWORK_TYPE);

        mController.init(SUB_ID, mFragmentManager);

        // NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA = LTE | CDMA | EVDO | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | CDMA | EVDO | GSM | WCDMA));

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(
                        TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
        assertThat(mPreference.getEntryValues())
                .asList()
                .doesNotContain(
                        String.valueOf(TelephonyManager.NETWORK_MODE_NR_LTE_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void updateState_GlobalDisAllowed5g_GlobalWithoutNR() {
        mockAccessFamily(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA);
        mockAllowedNetworkTypes(DISABLED_5G_NETWORK_TYPE);
        mController.init(SUB_ID, mFragmentManager);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);

        // NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA = NR | LTE | CDMA | EVDO | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (NR | LTE | CDMA | EVDO | GSM | WCDMA));
        mController.updateState(mPreference);

        assertThat(mPreference.getEntryValues())
                .asList()
                .doesNotContain(
                        String.valueOf(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void updateState_GlobalDisAllowed5g_SelectOnGlobal() {
        mockAccessFamily(TelephonyManager.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA);
        mockAllowedNetworkTypes(DISABLED_5G_NETWORK_TYPE);
        mController.init(SUB_ID, mFragmentManager);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_WORLD_MODE_ENABLED_BOOL, true);

        // NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA = NR | LTE | CDMA | EVDO | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (NR | LTE | CDMA | EVDO | GSM | WCDMA));
        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(
                        TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void updateState_updateByNetworkMode() {
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA);

        // NETWORK_MODE_TDSCDMA_GSM_WCDMA = RAF_TD_SCDMA | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (RAF_TD_SCDMA | GSM | WCDMA));
        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA));
        assertThat(mPreference.getSummary()).isEqualTo("3G");
    }

    @UiThreadTest
    @Test
    public void updateState_updateByNetworkMode_useDefaultValue() {
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA);

        // NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA = LTE | CDMA | EVDO | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (LTE | CDMA | EVDO | GSM | WCDMA));
        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void onPreferenceChange_updateSuccess() {
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA);
        doReturn(true).when(mTelephonyManager).setPreferredNetworkTypeBitmask(
                getRafFromNetworkType(
                        TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));

        mController.updateState(mPreference);
        mController.onViewCreated(new TestLifecycleOwner());
        mController.onPreferenceChange(mPreference,
                String.valueOf(TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void onPreferenceChange_updateFail() {
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA);
        doReturn(false).when(mTelephonyManager).setPreferredNetworkTypeBitmask(
                getRafFromNetworkType(TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA));

        mController.updateState(mPreference);
        mController.onViewCreated(new TestLifecycleOwner());
        mController.onPreferenceChange(mPreference,
                String.valueOf(TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA));

        assertThat(mPreference.getValue()).isNotEqualTo(
                String.valueOf(TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA));
    }

    @UiThreadTest
    @Test
    public void preferredNetworkModeNotification_preferenceUpdates() {

        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen screen = preferenceManager.createPreferenceScreen(mContext);
        mPreference.setKey(KEY);
        screen.addPreference(mPreference);
        mockEnabledNetworkMode(TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA);

        // NETWORK_MODE_TDSCDMA_GSM_WCDMA = RAF_TD_SCDMA | GSM | WCDMA
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_USER)).thenReturn(
                (long) (RAF_TD_SCDMA | GSM | WCDMA));

        mController.displayPreference(screen);
        mController.updateState(mPreference);
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(Integer.parseInt(mPreference.getValue())).isEqualTo(
                TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA);
        assertThat(mPreference.getSummary()).isEqualTo("3G");
    }

    @UiThreadTest
    @Test
    public void checkResource_stringArrayLength() {
        int id = mController.getResourcesForSubId().getIdentifier("enabled_networks_cdma_values",
                "array", mContext.getPackageName());
        String[] entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(4, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier("enabled_networks_cdma_no_lte_values",
                "array", mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(2, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier(
                "enabled_networks_cdma_only_lte_values", "array", mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(2, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier("enabled_networks_tdscdma_values",
                "array", mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(3, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier(
                "enabled_networks_except_gsm_lte_values", "array", mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(1, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier("enabled_networks_except_gsm_values",
                "array", mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(2, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier("enabled_networks_except_lte_values",
                "array", mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(2, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier("enabled_networks_values", "array",
                mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(3, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier("enabled_networks_values", "array",
                mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(3, entryValues.length);

        id = mController.getResourcesForSubId().getIdentifier(
                "preferred_network_mode_values_world_mode", "array", mContext.getPackageName());
        entryValues = mController.getResourcesForSubId().getStringArray(id);
        assertEquals(3, entryValues.length);
    }

    private void mockEnabledNetworkMode(int networkMode) {
        if (networkMode == TelephonyManagerConstants.NETWORK_MODE_TDSCDMA_GSM_WCDMA) {
            mockPhoneType(TelephonyManager.PHONE_TYPE_GSM);
            mPersistableBundle.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, true);
        } else if (networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_GSM_WCDMA
                || networkMode == TelephonyManagerConstants.NETWORK_MODE_LTE_CDMA_EVDO_GSM_WCDMA) {
            mockPhoneType(TelephonyManager.PHONE_TYPE_GSM);
            mPersistableBundle.putBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL, true);
            mPersistableBundle.putBoolean(CarrierConfigManager.KEY_LTE_ENABLED_BOOL, true);
        } else if (networkMode == TelephonyManagerConstants.NETWORK_MODE_NR_LTE_TDSCDMA_GSM_WCDMA) {
            mockPhoneType(TelephonyManager.PHONE_TYPE_GSM);
            mPersistableBundle.putBoolean(CarrierConfigManager.KEY_SUPPORT_TDSCDMA_BOOL, true);
        } else if (networkMode == TelephonyManagerConstants.NETWORK_MODE_NR_LTE_GSM_WCDMA
                || networkMode
                == TelephonyManagerConstants.NETWORK_MODE_NR_LTE_CDMA_EVDO_GSM_WCDMA) {
            mockPhoneType(TelephonyManager.PHONE_TYPE_GSM);
            mPersistableBundle.putBoolean(CarrierConfigManager.KEY_PREFER_2G_BOOL, true);
            mPersistableBundle.putBoolean(CarrierConfigManager.KEY_LTE_ENABLED_BOOL, true);
        }
        mController.init(SUB_ID, mFragmentManager);
    }

    private void mockAllowedNetworkTypes(long allowedNetworkType) {
        doReturn(allowedNetworkType).when(mTelephonyManager).getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_CARRIER);
    }

    private void mockAccessFamily(int networkMode) {
        doReturn(MobileNetworkUtils.getRafFromNetworkType(networkMode))
                .when(mTelephonyManager)
                .getSupportedRadioAccessFamily();
    }

    private void mockPhoneType(int phoneType) {
        doReturn(phoneType).when(mTelephonyManager).getPhoneType();
    }
}
