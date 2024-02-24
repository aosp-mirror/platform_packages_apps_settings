/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.display;

import static android.hardware.display.DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED;

import static com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE;
import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.hardware.display.DisplayManager;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;
import android.testing.TestableContext;
import android.view.Display;

import androidx.preference.SwitchPreference;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.display.feature.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class PeakRefreshRatePreferenceControllerTest {

    private PeakRefreshRatePreferenceController mController;
    private SwitchPreference mPreference;

    @Mock
    private PeakRefreshRatePreferenceController.DeviceConfigDisplaySettings
            mDeviceConfigDisplaySettings;
    @Mock
    private DisplayManager mDisplayManagerMock;
    @Mock
    private Display mDisplayMock;
    @Mock
    private Display mDisplayMock2;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();
    @Rule
    public final TestableContext mContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getContext());

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext.addMockSystemService(DisplayManager.class, mDisplayManagerMock);

        Display.Mode[] modes = new Display.Mode[]{
                new Display.Mode(/* modeId= */ 0, /* width= */ 800, /* height= */ 600,
                        /* refreshRate= */ 60),
                new Display.Mode(/* modeId= */ 0, /* width= */ 800, /* height= */ 600,
                        /* refreshRate= */ 120),
                new Display.Mode(/* modeId= */ 0, /* width= */ 800, /* height= */ 600,
                        /* refreshRate= */ 90)
        };
        when(mDisplayManagerMock.getDisplay(Display.DEFAULT_DISPLAY)).thenReturn(mDisplayMock);
        when(mDisplayMock.getSupportedModes()).thenReturn(modes);

        Display.Mode[] modes2 = new Display.Mode[]{
                new Display.Mode(/* modeId= */ 0, /* width= */ 800, /* height= */ 600,
                        /* refreshRate= */ 70),
                new Display.Mode(/* modeId= */ 0, /* width= */ 800, /* height= */ 600,
                        /* refreshRate= */ 130),
                new Display.Mode(/* modeId= */ 0, /* width= */ 800, /* height= */ 600,
                        /* refreshRate= */ 80)
        };
        when(mDisplayManagerMock.getDisplay(Display.DEFAULT_DISPLAY + 1)).thenReturn(mDisplayMock2);
        when(mDisplayMock2.getSupportedModes()).thenReturn(modes2);

        when(mDisplayManagerMock.getDisplays(DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED))
                .thenReturn(new Display[]{ mDisplayMock, mDisplayMock2 });

        mController = new PeakRefreshRatePreferenceController(mContext, "key");
        mController.injectDeviceConfigDisplaySettings(mDeviceConfigDisplaySettings);
        mPreference = new SwitchPreference(RuntimeEnvironment.application);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void getAvailabilityStatus_withConfigNoShow_returnUnsupported() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_refreshRateLargerThanDefault_returnAvailable() {
        mController.mPeakRefreshRate = DEFAULT_REFRESH_RATE + 1;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_refreshRateEqualToDefault_returnUnsupported() {
        mController.mPeakRefreshRate = DEFAULT_REFRESH_RATE;

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_BACK_UP_SMOOTH_DISPLAY_AND_FORCE_PEAK_REFRESH_RATE)
    public void setChecked_enableSmoothDisplay_featureFlagOff() {
        mController.mPeakRefreshRate = 88f;
        mController.setChecked(true);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, DEFAULT_REFRESH_RATE))
                .isEqualTo(88f);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BACK_UP_SMOOTH_DISPLAY_AND_FORCE_PEAK_REFRESH_RATE)
    public void setChecked_enableSmoothDisplay_featureFlagOn() {
        mController.mPeakRefreshRate = 88f;
        mController.setChecked(true);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, DEFAULT_REFRESH_RATE))
                .isPositiveInfinity();
    }

    @Test
    public void setChecked_disableSmoothDisplay_setDefaultRefreshRate() {
        mController.mPeakRefreshRate = 88f;
        mController.setChecked(false);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, DEFAULT_REFRESH_RATE))
                .isEqualTo(DEFAULT_REFRESH_RATE);
    }

    @Test
    public void isChecked_enableSmoothDisplay_returnTrue() {
        mController.mPeakRefreshRate = 88f;
        mController.setChecked(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disableSmoothDisplay_returnFalse() {
        mController.mPeakRefreshRate = 88f;
        mController.setChecked(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_default_returnTrue() {
        mController.mPeakRefreshRate = 88f;
        when(mDeviceConfigDisplaySettings.getDefaultPeakRefreshRate())
                .thenReturn(mController.mPeakRefreshRate);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_default_returnFalse() {
        mController.mPeakRefreshRate = 88f;
        when(mDeviceConfigDisplaySettings.getDefaultPeakRefreshRate()).thenReturn(60f);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_BACK_UP_SMOOTH_DISPLAY_AND_FORCE_PEAK_REFRESH_RATE)
    public void peakRefreshRate_highestOfDefaultDisplay_featureFlagOff() {
        assertThat(mController.mPeakRefreshRate).isEqualTo(120);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BACK_UP_SMOOTH_DISPLAY_AND_FORCE_PEAK_REFRESH_RATE)
    public void peakRefreshRate_highestOfAllDisplays_featureFlagOn() {
        assertThat(mController.mPeakRefreshRate).isEqualTo(130);
    }
}
