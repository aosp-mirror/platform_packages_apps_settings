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

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.Uri;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.telephony.CarrierConfigManager;
import android.telephony.ServiceState;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class EnabledNetworkModePreferenceControllerTest {
    private static final int SUB_ID = 2;
    public static final String KEY = "enabled_network";

    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;
    @Mock
    private CarrierConfigManager mCarrierConfigManager;
    @Mock
    private ServiceState mServiceState;

    private PersistableBundle mPersistableBundle;
    private EnabledNetworkModePreferenceController mController;
    private ListPreference mPreference;
    private Context mContext;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mTelephonyManager).when(mContext).getSystemService(Context.TELEPHONY_SERVICE);
        doReturn(mTelephonyManager).when(mContext).getSystemService(TelephonyManager.class);
        doReturn(mCarrierConfigManager).when(mContext).getSystemService(CarrierConfigManager.class);
        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        doReturn(mContext).when(mContext).createPackageContext(anyString(), anyInt());
        doReturn(mServiceState).when(mTelephonyManager).getServiceState();
        mPersistableBundle = new PersistableBundle();
        doReturn(mPersistableBundle).when(mCarrierConfigManager).getConfigForSubId(SUB_ID);

        mPreference = new ListPreference(mContext);
        mPreference.setEntries(R.array.enabled_networks_choices);
        mPreference.setEntryValues(R.array.enabled_networks_values);
        mController = new EnabledNetworkModePreferenceController(mContext, KEY);
        mController.init(mLifecycle, SUB_ID);
        mPreference.setKey(mController.getPreferenceKey());
    }

    @Test
    public void getAvailabilityStatus_hideCarrierNetworkSettings_returnUnavailable() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hidePreferredNetworkType_returnUnavailable() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_PREFERRED_NETWORK_TYPE_BOOL,
                true);

        when(mServiceState.getState()).thenReturn(ServiceState.STATE_OUT_OF_SERVICE);
        when(mServiceState.getDataRegState()).thenReturn(ServiceState.STATE_OUT_OF_SERVICE);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);

        when(mServiceState.getState()).thenReturn(ServiceState.STATE_IN_SERVICE);
        when(mServiceState.getDataRegState()).thenReturn(ServiceState.STATE_IN_SERVICE);

        when(mServiceState.getRoaming()).thenReturn(false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);

        when(mServiceState.getRoaming()).thenReturn(true);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_notWorldPhone_returnAvailable() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL,
                false);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_WORLD_PHONE_BOOL, false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void init_initShow4GForLTE() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_SHOW_4G_FOR_LTE_DATA_ICON_BOOL,
                true);

        mController.init(mLifecycle, SUB_ID);

        assertThat(mController.mShow4GForLTE).isTrue();
    }

    @Test
    public void updateState_updateByNetworkMode() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID,
                TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA));
        assertThat(mPreference.getSummary()).isEqualTo("3G");
    }

    @Test
    public void updateState_updateByNetworkMode_useDefaultValue() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID,
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.updateState(mPreference);

        assertThat(mPreference.getValue()).isEqualTo(
                String.valueOf(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA));
    }

    @Test
    public void onPreferenceChange_updateSuccess() {
        doReturn(true).when(mTelephonyManager).setPreferredNetworkType(SUB_ID,
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.onPreferenceChange(mPreference,
                String.valueOf(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA));

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID, 0)).isEqualTo(
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);
    }

    @Test
    public void onPreferenceChange_updateFail() {
        doReturn(false).when(mTelephonyManager).setPreferredNetworkType(SUB_ID,
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);

        mController.onPreferenceChange(mPreference,
                String.valueOf(TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA));

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID, 0)).isNotEqualTo(
                TelephonyManager.NETWORK_MODE_LTE_GSM_WCDMA);
    }

    @Test
    public void preferredNetworkModeNotification_preferenceUpdates() {
        PreferenceScreen screen = mock(PreferenceScreen.class);
        doReturn(mPreference).when(screen).findPreference(KEY);
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID,
                TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA);
        mController.displayPreference(screen);
        mController.updateState(mPreference);
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(Integer.parseInt(mPreference.getValue())).isEqualTo(
                TelephonyManager.NETWORK_MODE_TDSCDMA_GSM_WCDMA);
        assertThat(mPreference.getSummary()).isEqualTo("3G");


        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID,
                TelephonyManager.NETWORK_MODE_GSM_ONLY);
        final Uri uri = Settings.Global.getUriFor(Settings.Global.PREFERRED_NETWORK_MODE + SUB_ID);
        mContext.getContentResolver().notifyChange(uri, null);

        assertThat(Integer.parseInt(mPreference.getValue())).isEqualTo(
                TelephonyManager.NETWORK_MODE_GSM_ONLY);
        assertThat(mPreference.getSummary()).isEqualTo("2G");
    }
}
