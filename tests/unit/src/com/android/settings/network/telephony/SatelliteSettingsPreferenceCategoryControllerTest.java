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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.os.PersistableBundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.telephony.CarrierConfigManager;
import android.telephony.satellite.SatelliteManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.network.CarrierConfigCache;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class SatelliteSettingsPreferenceCategoryControllerTest {
    private static final String KEY = "key";
    private static final int TEST_SUB_ID = 0;

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private CarrierConfigCache mCarrierConfigCache;

    private Context mContext = null;
    private SatelliteManager mSatelliteManager = null;
    private SatelliteSettingsPreferenceCategoryController mController = null;
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
        mController = new SatelliteSettingsPreferenceCategoryController(mContext, KEY);
    }

    @Test
    @DisableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public void getAvailabilityStatus_featureDisabled_returnUnsupport() {
        int result = mController.getAvailabilityStatus(TEST_SUB_ID);

        assertThat(result).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(Flags.FLAG_CARRIER_ENABLED_SATELLITE_FLAG)
    public void getAvailabilityStatus_noSatellite_returnUnsupport() {
        when(mContext.getSystemService(SatelliteManager.class)).thenReturn(null);
        mController = new SatelliteSettingsPreferenceCategoryController(mContext, KEY);

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
}
