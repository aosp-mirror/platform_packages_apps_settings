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
package com.android.settings.fuelgauge.batterytip.detectors;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;

import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class IncompatibleChargerDetectorTest {

    @Mock private UsbPort mUsbPort;
    @Mock private UsbManager mUsbManager;
    @Mock private UsbPortStatus mUsbPortStatus;

    private Context mContext;
    private IncompatibleChargerDetector mIncompatibleChargerDetector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(UsbManager.class)).thenReturn(mUsbManager);
        mIncompatibleChargerDetector = new IncompatibleChargerDetector(mContext);
    }

    @Test
    public void detect_withoutIncompatibleCharger_shouldNotShowTip() {
        BatteryTip batteryTip = mIncompatibleChargerDetector.detect();

        assertThat(batteryTip.isVisible()).isFalse();
        assertThat(batteryTip.getState()).isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void detect_withIncompatibleCharger_showTip() {
        setupIncompatibleCharging();

        BatteryTip batteryTip = mIncompatibleChargerDetector.detect();

        assertThat(batteryTip.isVisible()).isTrue();
        assertThat(batteryTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
    }

    private void setupIncompatibleCharging() {
        final List<UsbPort> usbPorts = new ArrayList<>();
        usbPorts.add(mUsbPort);
        when(mUsbManager.getPorts()).thenReturn(usbPorts);
        when(mUsbPort.getStatus()).thenReturn(mUsbPortStatus);
        when(mUsbPort.supportsComplianceWarnings()).thenReturn(true);
        when(mUsbPortStatus.isConnected()).thenReturn(true);
        when(mUsbPortStatus.getComplianceWarnings())
                .thenReturn(new int[] {UsbPortStatus.COMPLIANCE_WARNING_DEBUG_ACCESSORY});
    }
}
