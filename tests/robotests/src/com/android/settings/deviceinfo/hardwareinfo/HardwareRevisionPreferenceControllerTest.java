/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.hardwareinfo;

import static android.content.Context.CLIPBOARD_SERVICE;

import static com.google.common.truth.Truth.assertThat;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.SystemProperties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HardwareRevisionPreferenceControllerTest {

    private Context mContext;
    private HardwareRevisionPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new HardwareRevisionPreferenceController(mContext,
                "hardware_info_device_revision");
    }

    @Test
    public void copy_shouldCopyHardwareRevisionToClipboard() {
        final String fakeHardwareVer = "FakeVer1.0";
        SystemProperties.set("ro.boot.hardware.revision", fakeHardwareVer);

        mController.copy();

        final ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(
                CLIPBOARD_SERVICE);
        final CharSequence data = clipboard.getPrimaryClip().getItemAt(0).getText();

        assertThat(data.toString()).isEqualTo(fakeHardwareVer);
    }
}