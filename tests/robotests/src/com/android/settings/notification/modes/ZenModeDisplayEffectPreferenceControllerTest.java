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

import static android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY;
import static android.service.notification.ZenPolicy.STATE_ALLOW;
import static android.service.notification.ZenPolicy.STATE_UNSET;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.app.AutomaticZenRule;
import android.app.Flags;
import android.content.Context;
import android.net.Uri;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenDeviceEffects;
import android.service.notification.ZenPolicy;
import androidx.preference.TwoStatePreference;
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
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAlarms(true).build())
                        .setDeviceEffects(new ZenDeviceEffects.Builder()
                                .setShouldDisplayGrayscale(true)
                                .build())
                        .build(), true);

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(
                        mContext, "effect_greyscale", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_grayscale() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowAlarms(false).build())
                        .setDeviceEffects(new ZenDeviceEffects.Builder()
                                .setShouldDisplayGrayscale(true)
                                .build())
                        .build(), true);

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_greyscale", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getRule().getDeviceEffects().shouldDisplayGrayscale())
                .isFalse();
    }

    @Test
    public void testUpdateState_aod() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowMedia(true).build())
                        .setDeviceEffects(new ZenDeviceEffects.Builder()
                                .setShouldSuppressAmbientDisplay(true)
                                .build())
                        .build(), true);

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_aod", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_aod() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowMedia(false).build())
                        .setDeviceEffects(new ZenDeviceEffects.Builder()
                                .setShouldSuppressAmbientDisplay(true)
                                .build())
                        .build(), true);

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_aod", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getRule().getDeviceEffects().shouldSuppressAmbientDisplay())
                .isFalse();
    }

    @Test
    public void testUpdateState_wallpaper() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowSystem(true).build())
                        .setDeviceEffects(new ZenDeviceEffects.Builder()
                                .setShouldDimWallpaper(true)
                                .build())
                        .build(), true);

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(
                        mContext, "effect_wallpaper", mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_wallpaper() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowSystem(false).build())
                        .setDeviceEffects(new ZenDeviceEffects.Builder()
                                .setShouldDimWallpaper(true)
                                .build())
                        .build(), true);

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(
                        mContext, "effect_wallpaper", mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getRule().getDeviceEffects().shouldDimWallpaper()).isFalse();
    }

    @Test
    public void testUpdateState_darkTheme() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowReminders(true).build())
                        .setDeviceEffects(new ZenDeviceEffects.Builder()
                                .setShouldUseNightMode(true)
                                .build())
                        .build(), true);

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_dark_theme",
                        mBackend);

        controller.updateZenMode(preference, zenMode);

        verify(preference).setChecked(true);
    }

    @Test
    public void testOnPreferenceChange_darkTheme() {
        TwoStatePreference preference = mock(TwoStatePreference.class);
        ZenMode zenMode = new ZenMode("id",
                new AutomaticZenRule.Builder("Driving", Uri.parse("drive"))
                        .setType(AutomaticZenRule.TYPE_DRIVING)
                        .setInterruptionFilter(INTERRUPTION_FILTER_PRIORITY)
                        .setZenPolicy(new ZenPolicy.Builder().allowReminders(false).build())
                        .setDeviceEffects(new ZenDeviceEffects.Builder()
                                .setShouldUseNightMode(true)
                                .build())
                        .build(), true);

        ZenModeDisplayEffectPreferenceController controller =
                new ZenModeDisplayEffectPreferenceController(mContext, "effect_dark_theme",
                        mBackend);

        controller.updateZenMode(preference, zenMode);

        controller.onPreferenceChange(preference, false);

        ArgumentCaptor<ZenMode> captor = ArgumentCaptor.forClass(ZenMode.class);
        verify(mBackend).updateMode(captor.capture());
        assertThat(captor.getValue().getRule().getDeviceEffects().shouldUseNightMode()).isFalse();
    }
}