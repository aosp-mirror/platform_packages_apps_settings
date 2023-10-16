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
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Looper;
import android.provider.DeviceConfig;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;


@RunWith(AndroidJUnit4.class)
public final class NullAlgorithmsPreferenceControllerTest {
    private static final String LOG_TAG = "NullAlgosControllerTest";
    private static final int SUB_ID = 2;
    private static final String PREFERENCE_KEY = "TEST_NULL_CIPHER_PREFERENCE";

    @Mock
    private TelephonyManager mTelephonyManager;
    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private NullAlgorithmsPreferenceController mController;
    private Context mContext;
    // Ideally we would use TestableDeviceConfig, but that's not doable because the Settings
    // app is not currently debuggable. For now, we use the real device config and ensure that
    // we reset the cellular_security namespace property to its pre-test value after every test.
    private DeviceConfig.Properties mPreTestProperties;

    @Before
    public void setUp() {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        mPreTestProperties = DeviceConfig.getProperties(
                TelephonyManager.PROPERTY_ENABLE_NULL_CIPHER_TOGGLE);

        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getSystemService(TelephonyManager.class)).thenReturn(mTelephonyManager);

        doReturn(mTelephonyManager).when(mTelephonyManager).createForSubscriptionId(SUB_ID);

        mController = new NullAlgorithmsPreferenceController(mContext, PREFERENCE_KEY);

        mPreference = spy(new Preference(mContext));
        mPreference.setKey(PREFERENCE_KEY);
        mPreferenceScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
    }

    @After
    public void tearDown() {
        try {
            DeviceConfig.setProperties(mPreTestProperties);
        } catch (DeviceConfig.BadConfigException e) {
            Log.e(LOG_TAG,
                    "Failed to reset DeviceConfig to pre-test state. Test results may be impacted. "
                            + e.getMessage());
        }
    }

    @Test
    public void getAvailabilityStatus_unsupportedHardware_unsupportedOnDevice() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CELLULAR_SECURITY,
                TelephonyManager.PROPERTY_ENABLE_NULL_CIPHER_TOGGLE, Boolean.TRUE.toString(),
                false);
        doThrow(UnsupportedOperationException.class).when(
                mTelephonyManager).isNullCipherAndIntegrityPreferenceEnabled();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_featureFlagOff_conditionallyUnavailable() {
        doReturn(true).when(mTelephonyManager).isNullCipherAndIntegrityPreferenceEnabled();
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CELLULAR_SECURITY,
                TelephonyManager.PROPERTY_ENABLE_NULL_CIPHER_TOGGLE, Boolean.FALSE.toString(),
                false);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_telephonyManagerException_conditionallyUnavailable() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CELLULAR_SECURITY,
                TelephonyManager.PROPERTY_ENABLE_NULL_CIPHER_TOGGLE, Boolean.TRUE.toString(),
                false);
        doThrow(IllegalStateException.class).when(
                mTelephonyManager).isNullCipherAndIntegrityPreferenceEnabled();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_returnAvailable() {
        DeviceConfig.setProperty(DeviceConfig.NAMESPACE_CELLULAR_SECURITY,
                TelephonyManager.PROPERTY_ENABLE_NULL_CIPHER_TOGGLE, Boolean.TRUE.toString(),
                false);
        // The value returned here shouldn't matter. The fact that this call is successful
        // indicates that the device supports this operation
        doReturn(true).when(mTelephonyManager).isNullCipherAndIntegrityPreferenceEnabled();
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void setChecked_true_nullCiphersDisabled() {
        mController.setChecked(true);
        verify(mTelephonyManager, times(1)).setNullCipherAndIntegrityEnabled(false);
    }

    @Test
    public void setChecked_false_nullCiphersEnabled() {
        mController.setChecked(false);
        verify(mTelephonyManager, times(1)).setNullCipherAndIntegrityEnabled(true);
    }

    @Test
    public void setChecked_exceptionThrown() {
        doThrow(IllegalStateException.class).when(
                mTelephonyManager).setNullCipherAndIntegrityEnabled(true);
        assertFalse(mController.setChecked(false));
        verify(mTelephonyManager, times(1)).setNullCipherAndIntegrityEnabled(true);
    }
}
