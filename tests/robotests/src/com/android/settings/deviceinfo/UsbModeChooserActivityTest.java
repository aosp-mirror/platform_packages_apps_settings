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
package com.android.settings.deviceinfo;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UsbModeChooserActivityTest {

    @Mock
    private TextView mTextView;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void updateSummary_chargeDevice_shouldNotSetSummary() {
        UsbModeChooserActivity.updateSummary(mTextView, UsbModeChooserActivity.DEFAULT_MODES[0]);
        verify(mTextView, never()).setText(anyInt());
    }

    @Test
    public void updateSummary_supplyPower_shouldSetSummary() {
        UsbModeChooserActivity.updateSummary(mTextView, UsbModeChooserActivity.DEFAULT_MODES[1]);
        verify(mTextView).setText(R.string.usb_use_power_only_desc);
    }

    @Test
    public void updateSummary_transferFiles_shouldNotSetSummary() {
        UsbModeChooserActivity.updateSummary(mTextView, UsbModeChooserActivity.DEFAULT_MODES[2]);
        verify(mTextView, never()).setText(anyInt());
    }

    @Test
    public void updateSummary_transferPhoto_shouldNotSetSummary() {
        UsbModeChooserActivity.updateSummary(mTextView, UsbModeChooserActivity.DEFAULT_MODES[3]);
        verify(mTextView, never()).setText(anyInt());
    }

    @Test
    public void updateSummary_MIDI_shouldNotSetSummary() {
        UsbModeChooserActivity.updateSummary(mTextView, UsbModeChooserActivity.DEFAULT_MODES[4]);
        verify(mTextView, never()).setText(anyInt());
    }

    @Test
    public void getTitle_shouldReturnCorrectTitle() {
        assertThat(UsbModeChooserActivity.getTitle(UsbModeChooserActivity.DEFAULT_MODES[0]))
                .isEqualTo(R.string.usb_use_charging_only);

        assertThat(UsbModeChooserActivity.getTitle(UsbModeChooserActivity.DEFAULT_MODES[1]))
                .isEqualTo(R.string.usb_use_power_only);

        assertThat(UsbModeChooserActivity.getTitle(UsbModeChooserActivity.DEFAULT_MODES[2]))
                .isEqualTo(R.string.usb_use_file_transfers);

        assertThat(UsbModeChooserActivity.getTitle(UsbModeChooserActivity.DEFAULT_MODES[3]))
                .isEqualTo(R.string.usb_use_photo_transfers);

        assertThat(UsbModeChooserActivity.getTitle(UsbModeChooserActivity.DEFAULT_MODES[4]))
                .isEqualTo(R.string.usb_use_MIDI);
    }

}