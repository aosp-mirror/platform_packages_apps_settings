/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.provider.Settings;

import android.text.TextUtils;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.utils.AnnotationSpan;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class BluetoothSwitchPreferenceControllerTest {

    private static final String BLUETOOTH_INFO_STRING = "When Bluetooth is turned on, your device"
            + " can communicate with other nearby Bluetooth devices.";
    @Mock
    private RestrictionUtils mRestrictionUtils;
    @Mock
    private SwitchWidgetController mSwitchController;
    @Mock
    private AlwaysDiscoverable mAlwaysDiscoverable;

    private FooterPreference mFooterPreference;
    private Context mContext;
    private BluetoothSwitchPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application.getApplicationContext());
        mFooterPreference = new FooterPreference(mContext);
        FakeFeatureFactory.setupForTest();

        mController =
            new BluetoothSwitchPreferenceController(mContext, mRestrictionUtils,
                    mSwitchController, mFooterPreference);
        mController.mAlwaysDiscoverable = mAlwaysDiscoverable;
    }

    @Test
    public void updateText_bluetoothOffScanningOn() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 1);
        mController.updateText(false);
        AnnotationSpan.LinkInfo info = new AnnotationSpan.LinkInfo(
                AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, mController);
        CharSequence text = AnnotationSpan.linkify(
                mContext.getText(R.string.bluetooth_scanning_on_info_message), info);

        assertThat(TextUtils.equals(mFooterPreference.getTitle(), text)).isTrue();
    }

    @Test
    public void updateText_bluetoothOffScanningOff() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0);
        mController.updateText(false);

        assertThat(mFooterPreference.getTitle()).isEqualTo(BLUETOOTH_INFO_STRING);
    }

    @Test
    public void updateText_bluetoothOnScanningOff() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 0);
        mController.updateText(true);

        assertThat(mFooterPreference.getTitle()).isEqualTo(BLUETOOTH_INFO_STRING);
    }

    @Test
    public void updateText_bluetoothOnScanningOn() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.BLE_SCAN_ALWAYS_AVAILABLE, 1);
        mController.updateText(true);

        assertThat(mFooterPreference.getTitle()).isEqualTo(BLUETOOTH_INFO_STRING);
    }

    @Test
    public void onStart_shouldStartAlwaysDiscoverable() {
        mController.onStart();

        verify(mAlwaysDiscoverable).start();
    }

    @Test
    public void onStop_shouldStopAlwaysDiscoverable() {
        mController.onStop();

        verify(mAlwaysDiscoverable).stop();
    }
}
