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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.res.Resources;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LongPressPowerForAssistantPreferenceControllerTest {

    private Application mContext;
    private Resources mResources;
    private SelectorWithWidgetPreference mPreference;
    private LongPressPowerForAssistantPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        mPreference = new SelectorWithWidgetPreference(mContext);
        mController = new LongPressPowerForAssistantPreferenceController(mContext, "test_key");

        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getBoolean(
                com.android.internal.R.bool
                        .config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(true);
        when(mResources.getInteger(com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(5); // Default to Assistant

        PreferenceScreen mScreen = mock(PreferenceScreen.class);
        when(mScreen.findPreference("test_key")).thenReturn(mPreference);
        mController.displayPreference(mScreen);
        mController.onStart();
    }

    @Test
    public void initialState_longPressPowerForPowerMenu_preferenceNotChecked() {
        PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isFalse();
    }

    @Test
    public void initialState_longPressPowerForAssistant_preferenceChecked() {
        PowerMenuSettingsUtils.setLongPressPowerForAssistant(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isChecked()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_longPressPowerSettingAvailable_returnsAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_longPressPowerSettingNotAvailable_returnsNotAvailable() {
        when(mResources.getBoolean(
                com.android.internal.R.bool
                        .config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void onClick_updatesSettingsValue_checksPreference() {
        // Initial state: preference not checked
        PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(mContext);
        mController.updateState(mPreference);
        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext)).isFalse();
        assertThat(mPreference.isChecked()).isFalse();

        mPreference.performClick();

        assertThat(PowerMenuSettingsUtils.isLongPressPowerForAssistantEnabled(mContext)).isTrue();
        assertThat(mPreference.isChecked()).isTrue();
    }
}
