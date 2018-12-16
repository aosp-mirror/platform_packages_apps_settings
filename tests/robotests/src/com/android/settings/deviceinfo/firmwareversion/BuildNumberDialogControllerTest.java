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

package com.android.settings.deviceinfo.firmwareversion;

import static com.android.settings.deviceinfo.firmwareversion.BuildNumberDialogController
        .BUILD_NUMBER_VALUE_ID;

import static org.mockito.Mockito.verify;

import android.os.Build;
import android.text.BidiFormatter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BuildNumberDialogControllerTest {

    @Mock
    private FirmwareVersionDialogFragment mDialog;

    private BuildNumberDialogController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mController = new BuildNumberDialogController(mDialog);
    }

    @Test
    public void initialize_shouldUpdateBuildNumberToDialog() {
        mController.initialize();

        verify(mDialog)
            .setText(BUILD_NUMBER_VALUE_ID, BidiFormatter.getInstance().unicodeWrap(Build.DISPLAY));
    }
}
