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
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LongPressPowerFooterPreferenceControllerTest {

    private Application mContext;
    private Resources mResources;
    private Preference mPreference;
    private LongPressPowerFooterPreferenceController mController;

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(mResources);
        mPreference = new FooterPreference(mContext);
        mController = new LongPressPowerFooterPreferenceController(mContext, "test_key");

        PreferenceScreen mScreen = mock(PreferenceScreen.class);
        when(mScreen.findPreference("test_key")).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void updateState_longPressPowerForPowerMenu_hidesPreference() {
        PowerMenuSettingsUtils.setLongPressPowerForPowerMenu(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_longPressPowerForAssistant_showsPreference() {
        PowerMenuSettingsUtils.setLongPressPowerForAssistant(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_notEligible_showsPreference() {
        PowerMenuSettingsUtils.setLongPressPowerForAssistant(mContext);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_hushGestureEnabled_includesPreventRingingHint() {
        when(mResources.getBoolean(com.android.internal.R.bool.config_volumeHushGestureEnabled))
                .thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString())
                .isEqualTo(
                        TextUtils.concat(
                                mContext.getString(R.string.power_menu_power_volume_up_hint),
                                "\n\n",
                                mContext.getString(
                                        R.string.power_menu_power_prevent_ringing_hint)));
    }

    @Test
    public void updateState_hushGestureDisabled_doesNotIncludePreventRingingHint() {
        when(mResources.getBoolean(com.android.internal.R.bool.config_volumeHushGestureEnabled))
                .thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.power_menu_power_volume_up_hint));
    }
}
