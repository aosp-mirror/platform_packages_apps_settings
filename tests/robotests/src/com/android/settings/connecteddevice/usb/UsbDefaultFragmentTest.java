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

package com.android.settings.connecteddevice.usb;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.usb.UsbManager;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UsbDefaultFragmentTest {

    @Mock
    private UsbBackend mUsbBackend;

    private UsbDefaultFragment mFragment;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mFragment = new UsbDefaultFragment();
        mFragment.mUsbBackend = mUsbBackend;
    }

    @Test
    public void testGetDefaultKey_isNone_shouldReturnNone() {
        when(mUsbBackend.getDefaultUsbMode()).thenReturn(UsbBackend.MODE_DATA_NONE);
        assertThat(mFragment.getDefaultKey()).isEqualTo(UsbManager.USB_FUNCTION_NONE);
    }

    @Test
    public void testGetDefaultKey_isMtp_shouldReturnMtp() {
        when(mUsbBackend.getDefaultUsbMode()).thenReturn(UsbBackend.MODE_DATA_MTP);
        assertThat(mFragment.getDefaultKey()).isEqualTo(UsbManager.USB_FUNCTION_MTP);
    }

    @Test
    public void testGetDefaultKey_isPtp_shouldReturnPtp() {
        when(mUsbBackend.getDefaultUsbMode()).thenReturn(UsbBackend.MODE_DATA_PTP);
        assertThat(mFragment.getDefaultKey()).isEqualTo(UsbManager.USB_FUNCTION_PTP);
    }

    @Test
    public void testGetDefaultKey_isRndis_shouldReturnRndis() {
        when(mUsbBackend.getDefaultUsbMode()).thenReturn(UsbBackend.MODE_DATA_TETHER);
        assertThat(mFragment.getDefaultKey()).isEqualTo(UsbManager.USB_FUNCTION_RNDIS);
    }

    @Test
    public void testGetDefaultKey_isMidi_shouldReturnMidi() {
        when(mUsbBackend.getDefaultUsbMode()).thenReturn(UsbBackend.MODE_DATA_MIDI);
        assertThat(mFragment.getDefaultKey()).isEqualTo(UsbManager.USB_FUNCTION_MIDI);
    }

    @Test
    public void testSetDefaultKey_isNone_shouldSetNone() {
        mFragment.setDefaultKey(UsbManager.USB_FUNCTION_NONE);
        verify(mUsbBackend).setDefaultUsbMode(UsbBackend.MODE_DATA_NONE);
    }

    @Test
    public void testSetDefaultKey_isMtp_shouldSetMtp() {
        mFragment.setDefaultKey(UsbManager.USB_FUNCTION_MTP);
        verify(mUsbBackend).setDefaultUsbMode(UsbBackend.MODE_DATA_MTP);
    }

    @Test
    public void testSetDefaultKey_isPtp_shouldSetPtp() {
        mFragment.setDefaultKey(UsbManager.USB_FUNCTION_PTP);
        verify(mUsbBackend).setDefaultUsbMode(UsbBackend.MODE_DATA_PTP);
    }

    @Test
    public void testSetDefaultKey_isRndis_shouldSetRndis() {
        mFragment.setDefaultKey(UsbManager.USB_FUNCTION_RNDIS);
        verify(mUsbBackend).setDefaultUsbMode(UsbBackend.MODE_DATA_TETHER);
    }

    @Test
    public void testSetDefaultKey_isMidi_shouldSetMidi() {
        mFragment.setDefaultKey(UsbManager.USB_FUNCTION_MIDI);
        verify(mUsbBackend).setDefaultUsbMode(UsbBackend.MODE_DATA_MIDI);
    }
}
