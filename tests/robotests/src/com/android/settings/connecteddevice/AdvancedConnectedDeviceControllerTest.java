/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.content.ContentResolver;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.nfc.NfcPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowNfcAdapter;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowNfcAdapter.class)
public class AdvancedConnectedDeviceControllerTest {

    private static final String KEY = "test_key";
    private static final String DRIVING_MODE_SETTINGS_ENABLED =
            "gearhead:driving_mode_settings_enabled";

    private Context mContext;
    private NfcPreferenceController mNfcController;
    private ShadowNfcAdapter mShadowNfcAdapter;
    private ContentResolver mContentResolver;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mContentResolver = mContext.getContentResolver();
        mNfcController = new NfcPreferenceController(mContext,
                NfcPreferenceController.KEY_TOGGLE_NFC);
        mShadowNfcAdapter = Shadows.shadowOf(NfcAdapter.getNfcAdapter(mContext));
    }

    @Test
    public void getAvailabilityStatus_returnStatusIsAvailable() {
        AdvancedConnectedDeviceController controller =
                new AdvancedConnectedDeviceController(mContext, KEY);

        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void isDrivingModeAvailable_returnTrue() {
        Settings.System.putInt(mContentResolver, DRIVING_MODE_SETTINGS_ENABLED, 1);

        assertThat(AdvancedConnectedDeviceController.isDrivingModeAvailable(mContext)).isTrue();
    }

    @Test
    public void isDrivingModeAvailable_returnFalse() {
        Settings.System.putInt(mContentResolver, DRIVING_MODE_SETTINGS_ENABLED, 0);

        assertThat(AdvancedConnectedDeviceController.isDrivingModeAvailable(mContext)).isFalse();
    }

    @Test
    public void getConnectedDevicesSummaryResourceId_NFCAndDrivingModeAvailable() {
        // NFC available, driving mode available
        mShadowNfcAdapter.setEnabled(true);
        assertThat(AdvancedConnectedDeviceController
                .getConnectedDevicesSummaryResourceId(mNfcController, true))
                .isEqualTo(R.string.connected_devices_dashboard_summary);
    }

    @Test
    public void getConnectedDevicesSummaryResourceId_NFCAvailableAndDrivingModeNotAvailable() {
        // NFC is available, driving mode not available
        mShadowNfcAdapter.setEnabled(true);
        assertThat(AdvancedConnectedDeviceController
                .getConnectedDevicesSummaryResourceId(mNfcController, false))
                .isEqualTo(R.string.connected_devices_dashboard_no_driving_mode_summary);
    }

    @Test
    public void getConnectedDevicesSummaryResourceId_NFCNotAvailableDrivingModeAvailable() {
        // NFC not available, driving mode available
        ReflectionHelpers.setField(mNfcController, "mNfcAdapter", null);
        assertThat(AdvancedConnectedDeviceController
                .getConnectedDevicesSummaryResourceId(mNfcController, true))
                .isEqualTo(R.string.connected_devices_dashboard_no_nfc_summary);
    }

    @Test
    public void getConnectedDevicesSummaryResourceId_NFCAndDrivingModeNotAvailable() {
        // NFC not available, driving mode not available
        ReflectionHelpers.setField(mNfcController, "mNfcAdapter", null);
        assertThat(AdvancedConnectedDeviceController
                .getConnectedDevicesSummaryResourceId(mNfcController, false))
                .isEqualTo(R.string.connected_devices_dashboard_no_driving_mode_no_nfc_summary);
    }
}
