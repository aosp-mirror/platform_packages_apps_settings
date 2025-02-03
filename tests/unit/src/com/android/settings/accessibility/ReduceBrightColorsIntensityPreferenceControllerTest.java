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
 * limitations under the License.
 */

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;
import com.android.server.display.feature.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Tests for {@link ReduceBrightColorsIntensityPreferenceController} */
@RunWith(AndroidJUnit4.class)
public class ReduceBrightColorsIntensityPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Context mContext;
    private Resources mResources;
    private ReduceBrightColorsIntensityPreferenceController mPreferenceController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        mPreferenceController = new ReduceBrightColorsIntensityPreferenceController(mContext,
                "rbc_intensity");
    }

    @Test
    @DisableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOffAndDisabled_RbcOnAndAvailable_returnTrue() {
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        doReturn(true).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOffAndDisabled_RbcOffAndAvailable_returnTrue() {
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 0);
        doReturn(true).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOffAndDisabled_RbcOnAndUnavailable_returnFalse() {
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        doReturn(false).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOnAndDisabled_RbcOnAndAvailable_returnTrue() {
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        doReturn(true).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOnAndDisabled_RbcOffAndAvailable_returnTrue() {
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 0);
        doReturn(true).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOnAndDisabled_RbcOnAndUnavailable_returnFalse() {
        doReturn(false).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        doReturn(false).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOnAndEnabled_RbcOnAndAvailable_returnFalse() {
        doReturn(true).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        doReturn(true).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOnAndEnabled_RbcOffAndAvailable_returnFalse() {
        doReturn(true).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 0);
        doReturn(true).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_EVEN_DIMMER)
    public void isAvailable_whenEvenDimmerOnAndEnabled_RbcOnAndUnavailable_returnFalse() {
        doReturn(true).when(mResources).getBoolean(
                com.android.internal.R.bool.config_evenDimmerEnabled);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        doReturn(false).when(mResources).getBoolean(
                R.bool.config_reduceBrightColorsAvailable);

        assertThat(mPreferenceController.isAvailable()).isFalse();
    }

    @Test
    public void onPreferenceChange_changesTemperature() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        mPreferenceController.onPreferenceChange(/* preference= */ null, 20);
        assertThat(
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL, 0))
                .isEqualTo(80);
    }

    // Slider range should represent percentage.
    @Test
    public void rangeOfSlider_isPercentage() {
        assertThat(mPreferenceController.getMax()).isEqualTo(100);
        assertThat(mPreferenceController.getMin()).isEqualTo(0);
        assertThat(mPreferenceController.getMax() - mPreferenceController.getMin())
                .isEqualTo(100);
    }

    // Slider should be of range 100 - 0.
    @Test
    public void rangeOfSlider_isInverted() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED, 1);
        mPreferenceController.onPreferenceChange(/* preference= */ null, 2);
        assertThat(
                Settings.Secure.getInt(mContext.getContentResolver(),
                        Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL, 0))
                .isEqualTo(98);
    }
}
