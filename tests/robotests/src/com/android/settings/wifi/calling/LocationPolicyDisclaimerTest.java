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

package com.android.settings.wifi.calling;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LocationPolicyDisclaimerTest {
    private static final String TEST_SHARED_PREFERENCE = "test_wfc_disclaimer_prefs";
    private static final int TEST_SUB_ID = 0;

    @Mock
    private CarrierConfigManager mCarrierConfigManager;

    private final PersistableBundle mBundle = new PersistableBundle();
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);

        doReturn(mCarrierConfigManager).when(mContext).getSystemService(
                Context.CARRIER_CONFIG_SERVICE);
        when(mCarrierConfigManager.getConfigForSubId(anyInt())).thenReturn(mBundle);
        doReturn(getSharedPreferences()).when(mContext).getSharedPreferences(anyString(), anyInt());
    }

    @Test
    public void sholdShow_configTrue_shouldShowLocationPolicyDisclaimer() {
        LocationPolicyDisclaimer disclaimerItem
                = spy(new LocationPolicyDisclaimer(mContext, TEST_SUB_ID));
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL, false);
        getSharedPreferences().edit().putBoolean(
                LocationPolicyDisclaimer.KEY_HAS_AGREED_LOCATION_DISCLAIMER + TEST_SUB_ID,
                false).commit();

        // Check the WFC disclaimer item is should be shown.
        assertThat(disclaimerItem.shouldShow()).isTrue();
    }

    @Test
    public void sholdShow_configFalse_shouldNotShowLocationPolicyDisclaimer() {
        LocationPolicyDisclaimer disclaimerItem = new LocationPolicyDisclaimer(mContext,
                TEST_SUB_ID);
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL, false);

        // Check the WFC disclaimer item is should not be shown due to the
        // KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL on carrier config is false.
        assertThat(disclaimerItem.shouldShow()).isFalse();
    }

    @Test
    public void sholdShow_defaultWfcEnabled_shouldNotShowLocationPolicyDisclaimer() {
        LocationPolicyDisclaimer disclaimerItem
                = new LocationPolicyDisclaimer(mContext, TEST_SUB_ID);
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL, true);

        // Check the WFC disclaimer item is should not be shown due to the
        // KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL on carrier config is true.
        assertThat(disclaimerItem.shouldShow()).isFalse();
    }

    @Test
    public void sholdShow_alreadyAgreed_shouldNotShowLocationPolicyDisclaimer() {
        LocationPolicyDisclaimer disclaimerItem =
                spy(new LocationPolicyDisclaimer(mContext, TEST_SUB_ID));
        mBundle.putBoolean(CarrierConfigManager.KEY_SHOW_WFC_LOCATION_PRIVACY_POLICY_BOOL, true);
        mBundle.putBoolean(CarrierConfigManager.KEY_CARRIER_DEFAULT_WFC_IMS_ENABLED_BOOL, false);
        getSharedPreferences().edit().putBoolean(
                LocationPolicyDisclaimer.KEY_HAS_AGREED_LOCATION_DISCLAIMER + TEST_SUB_ID, true)
                .commit();

        // Check the WFC disclaimer item is should not be shown due to an item is already agreed.
        assertThat(disclaimerItem.shouldShow()).isFalse();
    }

    @Test
    public void onAgreed_shouldSetSharedPreferencesToAgreed() {
        LocationPolicyDisclaimer disclaimerItem
                =spy(new LocationPolicyDisclaimer(mContext, TEST_SUB_ID));

        disclaimerItem.onAgreed();

        // Check the SharedPreferences key is changed to agreed.
        assertThat(getSharedPreferences().getBoolean(
                LocationPolicyDisclaimer.KEY_HAS_AGREED_LOCATION_DISCLAIMER + TEST_SUB_ID,
                false)).isTrue();
    }

    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(TEST_SHARED_PREFERENCE, Context.MODE_PRIVATE);
    }
}
