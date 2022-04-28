/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.view.Display;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ScreenResolutionControllerTest {

    private static final int FHD_WIDTH = 1080;
    private static final int QHD_WIDTH = 1440;

    private ScreenResolutionController mController;

    @Before
    public void setUp() {
        Context context = spy(ApplicationProvider.getApplicationContext());
        mController = spy(new ScreenResolutionController(context, "test"));
    }

    @Test
    public void getAvailabilityStatus_hasFhdAndQhdModes_returnAvailable() {
        Display.Mode modeA = new Display.Mode(0, FHD_WIDTH, 0, 0);
        Display.Mode modeB = new Display.Mode(0, QHD_WIDTH, 0, 0);
        Display.Mode[] modes = {modeA, modeB};
        doReturn(modes).when(mController).getSupportedModes();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_hasOneMode_returnUnsupported() {
        Display.Mode modeA = new Display.Mode(0, FHD_WIDTH, 0, 0);
        Display.Mode[] modes = {modeA};
        doReturn(modes).when(mController).getSupportedModes();

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updateState_screenResolutionFHD_shouldSetSummaryToFHD() {
        int width = FHD_WIDTH;
        doReturn(width).when(mController).getDisplayWidth();

        assertThat(mController.getSummary().toString()).isEqualTo("1080p FHD+");
    }

    @Test
    public void updateState_screenResolutionQHD_shouldSetSummaryToQHD() {
        int width = QHD_WIDTH;
        doReturn(width).when(mController).getDisplayWidth();

        assertThat(mController.getSummary().toString()).isEqualTo("1440p QHD+");
    }
}
