/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_DATA;
import static android.telephony.NetworkRegistrationInfo.SERVICE_TYPE_SMS;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.platform.test.annotations.EnableFlags;
import android.telephony.CarrierConfigManager;
import android.telephony.TelephonyManager;
import android.telephony.satellite.SatelliteManager;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.R;
import com.android.settings.network.CarrierConfigCache;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
@Ignore("b/382664790")
public class SatelliteSettingsPreferenceControllerTest {
    private static final String KEY = "key";
    private static final int TEST_SUB_ID = 0;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private CarrierConfigCache mCarrierConfigCache;
    @Mock
    private TelephonyManager mTelephonyManager;

    private Context mContext = null;
    private SatelliteManager mSatelliteManager;
    private SatelliteSettingPreferenceController mController = null;
    private PersistableBundle mCarrierConfig = new PersistableBundle();

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mContext = spy(ApplicationProvider.getApplicationContext());
        mSatelliteManager = new SatelliteManager(mContext);
        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(mSatelliteManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        when(mTelephonyManager.createForSubscriptionId(TEST_SUB_ID)).thenReturn(mTelephonyManager);
        mController = spy(new SatelliteSettingPreferenceController(mContext, KEY));
    }

    @Test
    @EnableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public void getAvailabilityStatus_noSatellite_returnUnsupport() {
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController = new SatelliteSettingPreferenceController(mContext, KEY);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public void getAvailabilityStatus_carrierIsNotSupport_returnUnavailable() {
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                false);
        when(mCarrierConfigCache.getConfigForSubId(TEST_SUB_ID)).thenReturn(mCarrierConfig);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public void getAvailabilityStatus_carrierIsSupport_returnAvailable() {
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mCarrierConfig.putBoolean(
                CarrierConfigManager.KEY_SATELLITE_ATTACH_SUPPORTED_BOOL,
                true);
        when(mCarrierConfigCache.getConfigForSubId(TEST_SUB_ID)).thenReturn(mCarrierConfig);

        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(AVAILABLE);
    }

    @Test
    @Ignore("avoid post submit failed")
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void getAvailabilityStatus_registerTelephonyCallback_success() {
        mController.init(TEST_SUB_ID);
        mController.onResume(null);

        verify(mTelephonyManager).registerTelephonyCallback(any(), any());
    }

    @Test
    @Ignore("avoid post submit failed")
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void getAvailabilityStatus_unregisterTelephonyCallback_success() {
        mController.init(TEST_SUB_ID);
        mController.onPause(null);

        verify(mTelephonyManager).unregisterTelephonyCallback(any());
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void getAvailabilityStatus_hasServiceDataType_showDataUi() {
        mController.init(TEST_SUB_ID);
        Preference preference = new Preference(mContext);
        preference.setKey(KEY);
        preference.setTitle("test title");
        mController.updateState(preference);

        mController.mCarrierRoamingNtnModeCallback.onCarrierRoamingNtnAvailableServicesChanged(
                new int[]{SERVICE_TYPE_SMS, SERVICE_TYPE_DATA});

        assertThat(preference.getTitle()).isEqualTo(
                mContext.getString(R.string.title_satellite_setting_connectivity));
    }

    @Test
    @EnableFlags(com.android.settings.flags.Flags.FLAG_SATELLITE_OEM_SETTINGS_UX_MIGRATION)
    public void getAvailabilityStatus_onlyHasServiceSmsType_showSmsUi() {
        mController.init(TEST_SUB_ID);
        Preference preference = new Preference(mContext);
        preference.setKey(KEY);
        preference.setTitle("test title");
        mController.updateState(preference);

        mController.mCarrierRoamingNtnModeCallback.onCarrierRoamingNtnAvailableServicesChanged(
                new int[]{SERVICE_TYPE_SMS});

        assertThat(preference.getTitle()).isEqualTo(
                mContext.getString(R.string.satellite_setting_title));
    }
}
