/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.network.CarrierConfigCache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public final class Enable2gPreferenceControllerTest {
    private static final int SUB_ID = 2;

    @Mock
    private CarrierConfigCache mCarrierConfigCache;
    @Mock
    private TelephonyManager mTelephonyManager;
    @Mock
    private TelephonyManager mInvalidTelephonyManager;

    private PersistableBundle mPersistableBundle;
    private Enable2gPreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE)).thenReturn(mTelephonyManager);
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);
        CarrierConfigCache.setTestInstance(mContext, mCarrierConfigCache);

        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);
        doReturn(mInvalidTelephonyManager).when(mTelephonyManager).createForSubscriptionId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mPersistableBundle = new PersistableBundle();
        doReturn(mPersistableBundle).when(mCarrierConfigCache).getConfigForSubId(SUB_ID);
        doReturn(mPersistableBundle).when(mCarrierConfigCache).getConfigForSubId(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mController = new Enable2gPreferenceController(mContext, "mobile_data");
        mController.init(SUB_ID);
    }

    @Test
    public void getAvailabilityStatus_invalidSubId_returnUnavailable() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hideEnable2g_returnUnavailable() {
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENABLE_2G,
                true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_nullCarrierConfig_returnUnavailable() {
        doReturn(true).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENABLE_2G,
                false);
        doReturn(null).when(mCarrierConfigCache).getConfigForSubId(SUB_ID);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_capabilityNotSupported_returnUnavailable() {
        doReturn(false).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENABLE_2G,
                false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        doReturn(true).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENABLE_2G,
                false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setChecked_invalidSubIdAndIsCheckedTrue_returnFalse() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        assertThat(mController.setChecked(true)).isFalse();
    }

    @Test
    public void setChecked_invalidSubIdAndIsCheckedFalse_returnFalse() {
        mController.init(SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        assertThat(mController.setChecked(false)).isFalse();
    }

    @Test
    public void onPreferenceChange_update() {
        // Set "Enable 2G" flag to "on"
        when(mTelephonyManager.getAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G)).thenReturn(
                (long) (TelephonyManager.NETWORK_TYPE_BITMASK_GSM
                        | TelephonyManager.NETWORK_TYPE_BITMASK_LTE));

        // Setup state to allow disabling
        doReturn(true).when(mTelephonyManager).isRadioInterfaceCapabilitySupported(
                mTelephonyManager.CAPABILITY_USES_ALLOWED_NETWORK_TYPES_BITMASK);
        mPersistableBundle.putBoolean(CarrierConfigManager.KEY_HIDE_ENABLE_2G,
                false);

        // Disable 2G
        boolean changed = mController.setChecked(false);
        assertThat(changed).isEqualTo(true);

        verify(mTelephonyManager, times(1)).setAllowedNetworkTypesForReason(
                TelephonyManager.ALLOWED_NETWORK_TYPES_REASON_ENABLE_2G,
                TelephonyManager.NETWORK_TYPE_BITMASK_LTE);
    }
}
