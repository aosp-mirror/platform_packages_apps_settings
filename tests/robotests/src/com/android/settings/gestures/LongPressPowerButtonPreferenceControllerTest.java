/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.res.Resources;
import android.provider.Settings;

import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LongPressPowerButtonPreferenceControllerTest {

    private static final String KEY_LONG_PRESS_POWER_BUTTON =
            "gesture_power_menu_long_press_for_assist";

    private Application mContext;
    private Resources mResources;
    private LongPressPowerButtonPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getString(anyInt())).thenAnswer((Answer<String>) invocation -> {
            int id = invocation.getArgument(0);
            return getString(id);
        });
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(true);
        mController = new LongPressPowerButtonPreferenceController(mContext,
                KEY_LONG_PRESS_POWER_BUTTON);
        mController.mAssistSwitch = mock(Preference.class);
        mController.mFooterHint = mock(Preference.class);
    }

    @Test
    public void isAvailable_configIsTrue_shouldReturnTrue() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configIsFalse_shouldReturnFalse() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_longPressOnPowerForAssistantSettingAvailable))
                .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void preferenceChecked_powerMenuHintTextShown() {
        mController.onPreferenceChange(null, true);

        verify(mController.mFooterHint).setSummary(
                getString(
                        R.string.power_menu_power_volume_up_hint));
        verify(mController.mFooterHint).setVisible(true);
    }


    @Test
    public void preferenceUnchecked_keyChordEnabled_powerMenuHintTextShown() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_keyChordPowerVolumeUp))
                .thenReturn(
                 LongPressPowerButtonPreferenceController.KEY_CHORD_POWER_VOLUME_UP_GLOBAL_ACTIONS);

        mController.onPreferenceChange(null, false);

        verify(mController.mFooterHint).setSummary(
                getString(
                        R.string.power_menu_power_volume_up_hint));
        verify(mController.mFooterHint).setVisible(true);
    }

    @Test
    public void preferenceChecked_hushGestureEnabled_powerMenuHintTextIncludesHushHint() {
        when(mResources.getBoolean(
                com.android.internal.R.bool.config_volumeHushGestureEnabled))
                .thenReturn(true);

        mController.onPreferenceChange(null, true);

        verify(mController.mFooterHint).setSummary(
                getString(
                        R.string.power_menu_power_volume_up_hint) + "\n\n"
                        + getString(
                        R.string.power_menu_power_prevent_ringing_hint));
        verify(mController.mFooterHint).setVisible(true);
    }


    @Test
    public void preferenceUnchecked_keyChordDisabled_powerMenuHintTextHidden() {
        mController.onPreferenceChange(null, false);
        when(mResources.getInteger(
                com.android.internal.R.integer.config_keyChordPowerVolumeUp))
                .thenReturn(
                      LongPressPowerButtonPreferenceController.KEY_CHORD_POWER_VOLUME_UP_NO_ACTION);

        verify(mController.mFooterHint).setVisible(false);
    }

    @Test
    public void preferenceChecked_longPressPowerSettingSetToAssistant() {
        mController.onPreferenceChange(null, true);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS, -1)).isEqualTo(
                LongPressPowerButtonPreferenceController.LONG_PRESS_POWER_ASSISTANT_VALUE);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.KEY_CHORD_POWER_VOLUME_UP, -1)).isEqualTo(
                LongPressPowerButtonPreferenceController.KEY_CHORD_POWER_VOLUME_UP_GLOBAL_ACTIONS);
        verify(mController.mAssistSwitch).setSummary(
                getString(
                        R.string.power_menu_summary_long_press_for_assist_enabled));
    }

    @Test
    public void preferenceUnchecked_longPressPowerSettingSetToDefaultValue() {
        when(mResources.getInteger(
                com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(
                        LongPressPowerButtonPreferenceController.LONG_PRESS_POWER_GLOBAL_ACTIONS);

        mController.onPreferenceChange(null, false);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS, -1)).isEqualTo(
                LongPressPowerButtonPreferenceController.LONG_PRESS_POWER_GLOBAL_ACTIONS);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.KEY_CHORD_POWER_VOLUME_UP, -1)).isEqualTo(
                LongPressPowerButtonPreferenceController.KEY_CHORD_POWER_VOLUME_UP_NO_ACTION);
        verify(mController.mAssistSwitch).setSummary(
                getString(
                       R.string.power_menu_summary_long_press_for_assist_disabled_with_power_menu));
    }

    @Test
    public void preferenceUnchecked_muteChordDefault_longPressPowerSettingSetToDefaultValue() {
        // Value out of range chosen deliberately.
        when(mResources.getInteger(
                com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(8);
        when(mResources.getInteger(
                com.android.internal.R.integer.config_keyChordPowerVolumeUp))
                .thenReturn(
                    LongPressPowerButtonPreferenceController.KEY_CHORD_POWER_VOLUME_UP_MUTE_TOGGLE);

        mController.onPreferenceChange(null, false);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS, -1)).isEqualTo(8);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.KEY_CHORD_POWER_VOLUME_UP, -1)).isEqualTo(
                LongPressPowerButtonPreferenceController.KEY_CHORD_POWER_VOLUME_UP_MUTE_TOGGLE);
    }

    @Test
    public void preferenceUnchecked_assistDefault_setNoAction() {
        // Ensure that the Assistant is the default behavior for LPP.
        when(mResources.getInteger(
                com.android.internal.R.integer.config_longPressOnPowerBehavior))
                .thenReturn(
                        LongPressPowerButtonPreferenceController.LONG_PRESS_POWER_ASSISTANT_VALUE);

        mController.onPreferenceChange(null, false);

        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.POWER_BUTTON_LONG_PRESS, -1)).isEqualTo(
                LongPressPowerButtonPreferenceController.LONG_PRESS_POWER_GLOBAL_ACTIONS);
        assertThat(Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.KEY_CHORD_POWER_VOLUME_UP, -1)).isEqualTo(
                LongPressPowerButtonPreferenceController.KEY_CHORD_POWER_VOLUME_UP_NO_ACTION);
        verify(mController.mAssistSwitch).setSummary(getString(
                    R.string.power_menu_summary_long_press_for_assist_disabled_with_power_menu));
    }

    private String getString(@StringRes int id) {
        return ApplicationProvider.getApplicationContext().getString(id);
    }
}
