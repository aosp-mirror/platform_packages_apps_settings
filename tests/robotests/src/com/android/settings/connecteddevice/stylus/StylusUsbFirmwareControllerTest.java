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

package com.android.settings.connecteddevice.stylus;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.HashMap;

@RunWith(RobolectricTestRunner.class)
public class StylusUsbFirmwareControllerTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private Lifecycle mLifecycle;
    private PreferenceScreen mScreen;

    private StylusUsbFirmwareController mController;
    @Mock
    private StylusUsiDetailsFragment mFragment;
    @Mock
    private UsbManager mUsbManager;
    private PreferenceCategory mPreferenceCategory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        mLifecycle = new Lifecycle(() -> mLifecycle);

        when(mFragment.getContext()).thenReturn(mContext);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new StylusUsbFirmwareController(mContext, "stylus_usb_firmware");

        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);

        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(mController.getPreferenceKey());
    }

    @Test
    public void displayPreference_featurePresentUsbStylusAttached_preferenceAdded() {
        attachUsbDevice();
        enableFullStylusFeature();

        mController.displayPreference(mScreen);

        assertNotNull(mScreen.findPreference("stylus_usb_firmware"));
    }

    @Test
    public void displayPreference_featureAbsentUsbStylusAttached_preferenceNotAdded() {
        attachUsbDevice();
        mController.mUsbConnectionListener.onUsbStylusConnectionChanged(
                mock(UsbDevice.class), true);

        mController.displayPreference(mScreen);

        assertNull(mScreen.findPreference(mController.getPreferenceKey()));
    }

    @Test
    public void onUsbStylusConnectionChanged_featurePresentUsbStylusAttached_preferenceAdded() {
        mController.displayPreference(mScreen);

        attachUsbDevice();
        enableFullStylusFeature();
        mController.mUsbConnectionListener.onUsbStylusConnectionChanged(
                mock(UsbDevice.class), true);

        assertNotNull(mScreen.findPreference(mController.getPreferenceKey()));
    }

    @Test
    public void onUsbStylusConnectionChanged_featureAbsentUsbStylusAttached_preferenceRemoved() {
        mController.displayPreference(mScreen);

        attachUsbDevice();
        mController.mUsbConnectionListener.onUsbStylusConnectionChanged(
                mock(UsbDevice.class), true);

        assertNull(mScreen.findPreference(mController.getPreferenceKey()));
    }

    @Test
    public void hasUsbStylusFirmwareUpdateFeature_featurePresent_true() {
        when(mFeatureFactory.getStylusFeatureProvider()
                .isUsbFirmwareUpdateEnabled(any())).thenReturn(true);
        attachUsbDevice();

        assertTrue(StylusUsbFirmwareController
                .hasUsbStylusFirmwareUpdateFeature(mock(UsbDevice.class)));
    }

    @Test
    public void hasUsbStylusFirmwareUpdateFeature_featureNotPresent_false() {
        when(mFeatureFactory.getStylusFeatureProvider()
                .isUsbFirmwareUpdateEnabled(any())).thenReturn(false);
        attachUsbDevice();

        assertFalse(StylusUsbFirmwareController
                .hasUsbStylusFirmwareUpdateFeature(mock(UsbDevice.class)));
    }

    private void attachUsbDevice() {
        when(mContext.getSystemService(UsbManager.class)).thenReturn(mUsbManager);
        HashMap<String, UsbDevice> deviceList = new HashMap<>();
        deviceList.put("0", mock(UsbDevice.class));
        when(mUsbManager.getDeviceList()).thenReturn(deviceList);
    }

    private void enableFullStylusFeature() {
        when(mFeatureFactory.getStylusFeatureProvider()
                .isUsbFirmwareUpdateEnabled(any())).thenReturn(true);
        when(mFeatureFactory.getStylusFeatureProvider()
                .getUsbFirmwareUpdatePreferences(any()))
                .thenReturn(Collections.singletonList(mock(Preference.class)));
    }
}
