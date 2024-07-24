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

package com.android.settings.development;

import static android.hardware.display.DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED;

import static com.android.internal.display.RefreshRateSettingsUtils.DEFAULT_REFRESH_RATE;
import static com.android.settings.development.ForcePeakRefreshRatePreferenceController.NO_CONFIG;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.hardware.display.DisplayManager;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;
import android.testing.TestableContext;
import android.view.Display;

import androidx.preference.PreferenceScreen;
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
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class ForcePeakRefreshRatePreferenceControllerTest {

    @Mock
    private SwitchPreference mPreference;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private DisplayManager mDisplayManagerMock;
    @Mock
    private Display mDisplayMock;
    @Mock
    private Display mDisplayMock2;

    private ForcePeakRefreshRatePreferenceController mController;

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

        mController = new ForcePeakRefreshRatePreferenceController(mContext);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        when(mPreference.getKey()).thenReturn(mController.getPreferenceKey());
        mController.displayPreference(mScreen);
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_BACK_UP_SMOOTH_DISPLAY_AND_FORCE_PEAK_REFRESH_RATE)
    public void onPreferenceChange_preferenceChecked_shouldEnableForcePeak_featureFlagOff() {
        mController.mPeakRefreshRate = 88f;

        mController.onPreferenceChange(mPreference, true);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, NO_CONFIG)).isEqualTo(88f);
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_BACK_UP_SMOOTH_DISPLAY_AND_FORCE_PEAK_REFRESH_RATE)
    public void onPreferenceChange_preferenceChecked_shouldEnableForcePeak_featureFlagOn() {
        mController.mPeakRefreshRate = 88f;

        mController.onPreferenceChange(mPreference, true);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, NO_CONFIG)).isPositiveInfinity();
    }

    @Test
    public void onPreferenceChange_preferenceUnchecked_shouldDisableForcePeak() {
        mController.mPeakRefreshRate = 88f;

        mController.onPreferenceChange(mPreference, false);

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, NO_CONFIG)).isEqualTo(NO_CONFIG);
    }

    @Test
    public void updateState_enableForcePeak_shouldCheckedToggle() {
        mController.mPeakRefreshRate = 88f;
        mController.forcePeakRefreshRate(true);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
        assertThat(mController.isForcePeakRefreshRateEnabled()).isTrue();
    }

    @Test
    public void updateState_disableForcePeak_shouldUncheckedToggle() {
        mController.mPeakRefreshRate = 88f;
        mController.forcePeakRefreshRate(false);

        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
        assertThat(mController.isForcePeakRefreshRateEnabled()).isFalse();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_withConfigNoShow_returnUnsupported() {
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_refreshRateLargerThanDefault_returnTrue() {
        mController.mPeakRefreshRate = DEFAULT_REFRESH_RATE + 1;

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_refreshRateEqualToDefault_returnFalse() {
        mController.mPeakRefreshRate = DEFAULT_REFRESH_RATE;

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void onDeveloperOptionsDisabled_shouldDisablePreference() {
        mController.onDeveloperOptionsSwitchDisabled();

        assertThat(Settings.System.getFloat(mContext.getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, -1f)).isEqualTo(NO_CONFIG);
        assertThat(mPreference.isChecked()).isFalse();
        assertThat(mPreference.isEnabled()).isFalse();
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
