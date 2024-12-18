/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.Flags;
import android.content.Context;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenDeviceEffects;

import androidx.preference.TwoStatePreference;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
@EnableFlags(Flags.FLAG_MODES_UI)
public final class ZenModeDisplayEffectPreferenceControllerTest {

    private Context mContext;
    @Mock
    private ZenModesBackend mBackend;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void testUpdateState_grayscale() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDisplayGrayscale(true)
                        .build())
                .build();

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(
                        mContext, "effect_greyscale", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_grayscale() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDisplayGrayscale(true)
                        .build())
                .build();

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_greyscale", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getDeviceEffects().shouldDisplayGrayscale())
                .isFalse();
    }

    @Test
    public void testUpdateState_aod() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldSuppressAmbientDisplay(true)
                        .build())
                .build();

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_aod", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_aod() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldSuppressAmbientDisplay(true)
                        .build())
                .build();

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_aod", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getDeviceEffects().shouldSuppressAmbientDisplay())
                .isFalse();
    }

    @Test
    public void testUpdateState_wallpaper() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDimWallpaper(true)
                        .build())
                .build();

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(
                        mContext, "effect_wallpaper", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_wallpaper() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldDimWallpaper(true)
                        .build())
                .build();

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(
                        mContext, "effect_wallpaper", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getDeviceEffects().shouldDimWallpaper()).isFalse();
    }

    @Test
    public void testUpdateState_darkTheme() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldUseNightMode(true)
                        .build())
                .build();

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_dark_theme",
                        mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_darkTheme() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new TestModeBuilder()
                .setDeviceEffects(new ZenDeviceEffects.Builder()
                        .setShouldUseNightMode(true)
                        .build())
                .build();

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_dark_theme",
                        mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getDeviceEffects().shouldUseNightMode()).isFalse();
    }
}