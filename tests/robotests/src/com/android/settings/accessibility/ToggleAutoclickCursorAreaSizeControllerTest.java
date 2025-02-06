/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.accessibility;

import static android.view.accessibility.AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_MAX;
import static android.view.accessibility.AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_MIN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.kotlin.VerificationKt.verify;

import android.content.Context;
import android.content.SharedPreferences;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SliderPreference;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ToggleAutoclickCursorAreaSizeController}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleAutoclickCursorAreaSizeControllerTest {

    private static final String PREFERENCE_KEY = "accessibility_control_autoclick_cursor_area_size";
    private static final String PACKAGE = "package";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ToggleAutoclickCursorAreaSizeController mController;

    @Before
    public void setUp() {
        mController = new ToggleAutoclickCursorAreaSizeController(mContext, PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getAvailabilityStatus_availableWhenFlagOn() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getAvailabilityStatus_conditionallyUnavailableWhenFlagOn() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void onStart_registerOnSharedPreferenceChangeListener() {
        final SharedPreferences prefs = mock(SharedPreferences.class);
        final Context context = mock(Context.class);
        doReturn(PACKAGE).when(context).getPackageName();
        doReturn(prefs).when(context).getSharedPreferences(anyString(), anyInt());
        final ToggleAutoclickCursorAreaSizeController controller =
                new ToggleAutoclickCursorAreaSizeController(context, PREFERENCE_KEY);

        controller.onStart();

        verify(prefs).registerOnSharedPreferenceChangeListener(controller);
    }

    @Test
    public void onStop_unregisterOnSharedPreferenceChangeListener() {
        final SharedPreferences prefs = mock(SharedPreferences.class);
        final Context context = mock(Context.class);
        doReturn(PACKAGE).when(context).getPackageName();
        doReturn(prefs).when(context).getSharedPreferences(anyString(), anyInt());
        final ToggleAutoclickCursorAreaSizeController controller =
                new ToggleAutoclickCursorAreaSizeController(context, PREFERENCE_KEY);

        controller.onStop();

        verify(prefs).unregisterOnSharedPreferenceChangeListener(controller);
    }

    @Test
    public void getProgress_matchesSetting_inRangeValue() {
        // TODO(388844952): Use parameter testing.
        for (int size : ImmutableList.of(20, 40, 60, 80, 100)) {
            updateSetting(size);

            assertThat(mController.getSliderPosition()).isEqualTo(size);
        }
    }

    @Test
    public void getProgress_matchesSetting_aboveMaxValue() {
        updateSetting(120);

        assertThat(mController.getSliderPosition()).isEqualTo(AUTOCLICK_CURSOR_AREA_SIZE_MAX);
    }

    @Test
    public void getProgress_matchesSetting_belowMinValue() {
        updateSetting(0);

        assertThat(mController.getSliderPosition()).isEqualTo(AUTOCLICK_CURSOR_AREA_SIZE_MIN);
    }

    @Test
    public void setProgress_updatesSetting_inRangeValue() {
        // TODO(388844952): Use parameter testing.
        for (int position : ImmutableList.of(20, 40, 60, 80, 100)) {
            mController.setSliderPosition(position);

            assertThat(readSetting()).isEqualTo(position);
        }
    }

    @Test
    public void setProgress_updatesSetting_aboveMaxValue() {
        mController.setSliderPosition(120);

        assertThat(readSetting()).isEqualTo(AUTOCLICK_CURSOR_AREA_SIZE_MAX);
    }

    @Test
    public void setProgress_updatesSetting_belowMinValue() {
        mController.setSliderPosition(0);

        assertThat(readSetting()).isEqualTo(AUTOCLICK_CURSOR_AREA_SIZE_MIN);
    }

    @Test
    public void sliderPreference_setCorrectInitialValue() {
        SliderPreference preference = mock(SliderPreference.class);
        PreferenceScreen screen = mock(PreferenceScreen.class);
        doReturn(preference).when(screen).findPreference(anyString());

        mController.displayPreference(screen);

        verify(preference).setValue(mController.getSliderPosition());
    }

    private int readSetting() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT);
    }

    private void updateSetting(int value) {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                value);
    }
}
