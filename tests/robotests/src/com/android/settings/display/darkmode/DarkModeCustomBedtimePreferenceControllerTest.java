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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Resources;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.BedtimeSettingsUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DarkModeCustomBedtimePreferenceControllerTest {
    @Mock
    private UiModeManager mService;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private Resources mResources;
    @Mock
    private FooterPreference mFooterPreference;

    private DarkModeCustomBedtimePreferenceController mController;
    private Context mContext;
    private BedtimeSettingsUtils mBedtimeSettingsUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();

        mContext = spy(ApplicationProvider.getApplicationContext());
        mBedtimeSettingsUtils = new BedtimeSettingsUtils(mContext);

        when(mContext.getSystemService(UiModeManager.class)).thenReturn(mService);

        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getString(com.android.internal.R.string.config_defaultWellbeingPackage))
                .thenReturn("wellbeing");

        when(mScreen.findPreference(anyString())).thenReturn(mFooterPreference);

        mController = new DarkModeCustomBedtimePreferenceController(mContext, "key");
    }

    @Test
    public void getAvailabilityStatus_bedtimeSettingsExist_shouldBeAvailableUnsearchable() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void getAvailabilityStatus_bedtimeSettingsDisabled_shouldBeUnsupportedOnDevice() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                false /* enabled */);
        when(mService.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void nightModeCustomModeBedtime_bedtimeSettingsExist_shouldShowFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_BEDTIME);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(true));
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void nightModeCustomModeSchedule_bedtimeSettingsExist_shouldHideFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightModeCustomType()).thenReturn(MODE_NIGHT_CUSTOM_TYPE_SCHEDULE);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(false));
    }

    @Test
    public void nightModeNo_bedtimeSettingsExist_shouldHideFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightMode()).thenReturn(MODE_NIGHT_NO);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(false));
    }

    @Test
    public void nightModeYes_bedtimeSettingsExist_shouldHideFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightMode()).thenReturn(MODE_NIGHT_YES);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(false));
    }

    @Test
    public void nightModeAuto_bedtimeSettingsExist_shouldHideFooterPreference() {
        mBedtimeSettingsUtils.installBedtimeSettings("wellbeing" /* wellbeingPackage */,
                true /* enabled */);
        when(mService.getNightMode()).thenReturn(MODE_NIGHT_AUTO);

        mController.updateState(mFooterPreference);

        verify(mFooterPreference).setVisible(eq(false));
    }
}
