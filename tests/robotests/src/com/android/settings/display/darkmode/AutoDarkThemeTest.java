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

package com.android.settings.display.darkmode;

import static android.app.UiModeManager.MODE_ATTENTION_THEME_OVERLAY_NIGHT;
import static android.app.UiModeManager.MODE_NIGHT_AUTO;
import static android.app.UiModeManager.MODE_NIGHT_CUSTOM;
import static android.app.UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.UiModeManager;
import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenDeviceEffects;

import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowApplication;

import java.time.LocalTime;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AutoDarkThemeTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final ZenDeviceEffects DEVICE_EFFECTS_WITH_DARK_THEME =
            new ZenDeviceEffects.Builder().setShouldUseNightMode(true).build();

    private static final ZenMode MODE_WITH_DARK_THEME = new TestModeBuilder()
            .setName("Sechseläuten")
            .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
            .build();

    private Context mContext;
    @Mock private UiModeManager mUiModeManager;
    @Mock private ZenModesBackend mZenModesBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();

        ShadowApplication shadowApp = ShadowApplication.getInstance();
        shadowApp.setSystemService(Context.UI_MODE_SERVICE, mUiModeManager);

        ZenModesBackend.setInstance(mZenModesBackend);
        when(mZenModesBackend.getModes()).thenReturn(List.of());
    }

    @Test
    public void getStatus_inactiveButAuto() {
        when(mUiModeManager.getNightMode()).thenReturn(MODE_NIGHT_AUTO);
        assertThat(getStatus(false)).isEqualTo("Will turn on automatically at sunset");
    }

    @Test
    public void getStatus_activeDueToAuto() {
        when(mUiModeManager.getNightMode()).thenReturn(MODE_NIGHT_AUTO);
        assertThat(getStatus(true)).isEqualTo("Will turn off automatically at sunrise");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getStatus_inactiveButUsedInModes() {
        when(mUiModeManager.getNightMode()).thenReturn(MODE_NIGHT_CUSTOM);
        when(mZenModesBackend.getModes()).thenReturn(List.of(MODE_WITH_DARK_THEME));

        assertThat(getStatus(false)).isEqualTo("Will turn on when Sechseläuten starts");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getStatus_activeDueToModes() {
        when(mUiModeManager.getNightMode()).thenReturn(MODE_NIGHT_CUSTOM);
        when(mUiModeManager.getAttentionModeThemeOverlay()).thenReturn(
                MODE_ATTENTION_THEME_OVERLAY_NIGHT);
        when(mZenModesBackend.getModes()).thenReturn(
                List.of(new TestModeBuilder(MODE_WITH_DARK_THEME).setActive(true).build()));

        assertThat(getStatus(true)).isEqualTo("Will turn off when Sechseläuten ends");
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void getStatus_inactiveButUsingBedtime() {
        when(mUiModeManager.getNightMode()).thenReturn(MODE_NIGHT_CUSTOM);
        when(mUiModeManager.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

        assertThat(getStatus(false)).isEqualTo("Will turn on automatically at bedtime");
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void getStatus_activeDueToBedtime() {
        when(mUiModeManager.getNightMode()).thenReturn(MODE_NIGHT_CUSTOM);
        when(mUiModeManager.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

        assertThat(getStatus(true)).isEqualTo("Will turn off automatically after bedtime");
    }

    @Test
    public void getStatus_inactiveButHasSchedule() {
        when(mUiModeManager.getNightMode()).thenReturn(MODE_NIGHT_CUSTOM);
        when(mUiModeManager.getCustomNightModeStart()).thenReturn(LocalTime.of(22, 0, 0, 0));
        when(mUiModeManager.getCustomNightModeEnd()).thenReturn(LocalTime.of(8, 0, 0, 0));

        assertThat(getStatus(false)).isEqualTo("Will turn on automatically at 10:00 PM");
    }

    @Test
    public void getStatus_activeDueToSchedule() {
        when(mUiModeManager.getNightMode()).thenReturn(MODE_NIGHT_CUSTOM);
        when(mUiModeManager.getCustomNightModeStart()).thenReturn(LocalTime.of(22, 0, 0, 0));
        when(mUiModeManager.getCustomNightModeEnd()).thenReturn(LocalTime.of(8, 0, 0, 0));

        assertThat(getStatus(true)).isEqualTo("Will turn off automatically at 8:00 AM");
    }

    private String getStatus(boolean active) {
        return AutoDarkTheme.getStatus(mContext, active);
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getModesThatChangeDarkTheme_returnsModeNames() {
        ZenMode modeThatChanges1 = new TestModeBuilder()
                .setName("Inactive")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(false)
                .build();
        ZenMode modeThatDoesNotChange = new TestModeBuilder()
                .setName("Unrelated")
                .build();
        ZenMode modeThatChanges2 = new TestModeBuilder()
                .setName("Active")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(true)
                .build();
        when(mZenModesBackend.getModes()).thenReturn(
                List.of(modeThatChanges1, modeThatDoesNotChange, modeThatChanges2));

        assertThat(AutoDarkTheme.getModesThatChangeDarkTheme(mContext))
                .containsExactly("Inactive", "Active")
                .inOrder();
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void getActiveModesThatChangeDarkTheme_returnsModeNames() {
        ZenMode inactiveModeThatUsesDarkTheme = new TestModeBuilder()
                .setName("Inactive")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(false)
                .build();
        ZenMode otherInactiveMode = new TestModeBuilder()
                .setName("Unrelated, inactive")
                .setActive(false)
                .build();
        ZenMode otherActiveMode = new TestModeBuilder()
                .setName("Unrelated, active")
                .setActive(true)
                .build();
        ZenMode activeModeThatUsesDarkTheme = new TestModeBuilder()
                .setName("Active")
                .setDeviceEffects(DEVICE_EFFECTS_WITH_DARK_THEME)
                .setActive(true)
                .build();
        when(mZenModesBackend.getModes()).thenReturn(
                List.of(inactiveModeThatUsesDarkTheme, otherInactiveMode, otherActiveMode,
                        activeModeThatUsesDarkTheme));

        assertThat(AutoDarkTheme.getActiveModesThatChangeDarkTheme(mContext))
                .containsExactly("Active");
    }
}
