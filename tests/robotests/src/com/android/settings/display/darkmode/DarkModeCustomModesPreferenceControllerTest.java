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

package com.android.settings.display.darkmode;

import static android.app.UiModeManager.MODE_NIGHT_AUTO;
import static android.app.UiModeManager.MODE_NIGHT_CUSTOM_TYPE_BEDTIME;
import static android.app.UiModeManager.MODE_NIGHT_CUSTOM_TYPE_SCHEDULE;
import static android.app.UiModeManager.MODE_NIGHT_NO;
import static android.app.UiModeManager.MODE_NIGHT_YES;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Flags;
import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Resources;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenDeviceEffects;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.BedtimeSettingsUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DarkModeCustomModesPreferenceControllerTest {

    private static final ZenMode MODE_WITH_DARK_THEME = new TestModeBuilder()
            .setDeviceEffects(new ZenDeviceEffects.Builder().setShouldUseNightMode(true).build())
            .build();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private UiModeManager mService;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FooterPreference mFooterPreference;
    @Mock
    private ZenModesBackend mZenModesBackend;

    private DarkModeCustomModesPreferenceController mController;
    private Context mContext;
    private BedtimeSettingsUtils mBedtimeSettingsUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();

        mContext = spy(ApplicationProvider.getApplicationContext());
        mBedtimeSettingsUtils = new BedtimeSettingsUtils(mContext);

        when(mContext.getSystemService(UiModeManager.class)).thenReturn(mService);
        Resources res = spy(mContext.getResources());
        when(res.getString(com.android.internal.R.string.config_systemWellbeing))
                .thenReturn("wellbeing");
        when(mContext.getResources()).thenReturn(res);

        when(mScreen.findPreference(anyString())).thenReturn(mFooterPreference);

        mController = new DarkModeCustomModesPreferenceController(mContext, "key");

        ZenModesBackend.setInstance(mZenModesBackend);
        when(mZenModesBackend.getModes()).thenReturn(List.of());
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void displayPreference_withOneModeTogglingDarkTheme() {
        when(mZenModesBackend.getModes()).thenReturn(List.of(
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("A").build()));

        mController.displayPreference(mScreen);

        verify(mFooterPreference).setTitle("A also activates dark theme");
        verify(mFooterPreference).setLearnMoreAction(any());
        verify(mFooterPreference).setLearnMoreText("Modes settings");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void displayPreference_withTwoModesTogglingDarkTheme() {
        when(mZenModesBackend.getModes()).thenReturn(List.of(
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("A").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("B").build()));

        mController.displayPreference(mScreen);

        verify(mFooterPreference).setTitle("A and B also activate dark theme");
        verify(mFooterPreference).setLearnMoreAction(any());
        verify(mFooterPreference).setLearnMoreText("Modes settings");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void displayPreference_withManyModesTogglingDarkTheme() {
        when(mZenModesBackend.getModes()).thenReturn(List.of(
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("A").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("B").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("C").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("D").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("E").build()
        ));

        mController.displayPreference(mScreen);

        verify(mFooterPreference).setTitle("A, B, and 3 more also activate dark theme");
        verify(mFooterPreference).setLearnMoreAction(any());
        verify(mFooterPreference).setLearnMoreText("Modes settings");
    }

    @Test
    @EnableFlags(Flags.FLAG_MODES_UI)
    public void displayPreference_withZeroModesTogglingDarkTheme() {
        when(mZenModesBackend.getModes()).thenReturn(List.of());

        mController.displayPreference(mScreen);

        verify(mFooterPreference).setTitle("Modes can also activate dark theme");
        verify(mFooterPreference).setLearnMoreAction(any());
        verify(mFooterPreference).setLearnMoreText("Modes settings");
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void getAvailabilityStatus_bedtimeSettingsExist_shouldBeAvailableUnsearchable() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void getAvailabilityStatus_bedtimeSettingsDisabled_shouldBeUnsupportedOnDevice() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                false /* enabled */);
        when(mService.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void nightModeCustomModeBedtime_bedtimeSettingsExist_shouldShowFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(true));
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void nightModeCustomModeSchedule_bedtimeSettingsExist_shouldHideFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_SCHEDULE);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(false));
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void nightModeNo_bedtimeSettingsExist_shouldHideFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightMode()).thenReturn(MODE_NIGHT_NO);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(false));
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void nightModeYes_bedtimeSettingsExist_shouldHideFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightMode()).thenReturn(MODE_NIGHT_YES);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(false));
    }

    @Test
    @DisableFlags(Flags.FLAG_MODES_UI)
    public void nightModeAuto_bedtimeSettingsExist_shouldHideFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightMode()).thenReturn(MODE_NIGHT_AUTO);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(false));
    }
}
