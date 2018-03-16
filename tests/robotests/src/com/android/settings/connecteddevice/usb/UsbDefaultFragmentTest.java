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
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.hardware.usb.UsbManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
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
    public void getDefaultKey_isNone_shouldReturnNone() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_NONE);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_NONE));
    }

    @Test
    public void getDefaultKey_isMtp_shouldReturnMtp() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_MTP);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
    }

    @Test
    public void getDefaultKey_isPtp_shouldReturnPtp() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_PTP);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_PTP));
    }

    @Test
    public void getDefaultKey_isRndis_shouldReturnRndis() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_RNDIS);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_RNDIS));
    }

    @Test
    public void getDefaultKey_isMidi_shouldReturnMidi() {
        when(mUsbBackend.getDefaultUsbFunctions()).thenReturn(UsbManager.FUNCTION_MIDI);
        assertThat(mFragment.getDefaultKey())
                .isEqualTo(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MIDI));
    }

    @Test
    public void setDefaultKey_isNone_shouldSetNone() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_NONE));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_NONE);
    }

    @Test
    public void setDefaultKey_isMtp_shouldSetMtp() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_MTP);
    }

    @Test
    public void setDefaultKey_isPtp_shouldSetPtp() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_PTP));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_PTP);
    }

    @Test
    public void setDefaultKey_isRndis_shouldSetRndis() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_RNDIS));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_RNDIS);
    }

    @Test
    public void setDefaultKey_isMidi_shouldSetMidi() {
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MIDI));
        verify(mUsbBackend).setDefaultUsbFunctions(UsbManager.FUNCTION_MIDI);
    }

    @Test
    @Config(shadows = ShadowUtils.class)
    public void setDefaultKey_isMonkey_shouldDoNothing() {
        ShadowUtils.setIsUserAMonkey(true);
        mFragment.setDefaultKey(UsbBackend.usbFunctionsToString(UsbManager.FUNCTION_MTP));
        verifyZeroInteractions(mUsbBackend);
    }
}
