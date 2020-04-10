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

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class PowerMenuPreferenceControllerTest {
    private Context mContext;
    private PowerMenuPreferenceController mController;

    private static final String KEY_GESTURE_POWER_MENU = "gesture_power_menu";
    private static final String CONTROLS_ENABLED = Settings.Secure.CONTROLS_ENABLED;
    private static final String CARDS_ENABLED = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    private static final String CARDS_AVAILABLE = Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new PowerMenuPreferenceController(mContext, KEY_GESTURE_POWER_MENU);
    }

    @Test
    public void getAvailabilityStatus_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary_allDisabled() {
        Settings.Secure.putInt(mContext.getContentResolver(), CONTROLS_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 0);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.power_menu_none));
    }

    @Test
    public void getSummary_onlyControlsEnabled() {
        Settings.Secure.putInt(mContext.getContentResolver(), CONTROLS_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 0);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.power_menu_device_controls));
    }

    @Test
    public void getSummary_onlyCardsEnabled_notAvailable() {
        Settings.Secure.putInt(mContext.getContentResolver(), CONTROLS_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 0);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.power_menu_none));
    }

    @Test
    public void getSummary_cardsAvailable_notEnabled() {
        Settings.Secure.putInt(mContext.getContentResolver(), CONTROLS_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 1);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.power_menu_none));
    }

    @Test
    public void getSummary_allEnabled_cardsNotAvailable() {
        Settings.Secure.putInt(mContext.getContentResolver(), CONTROLS_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 0);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.power_menu_device_controls));
    }

    @Test
    public void getSummary_controlsEnabled_cardsDisabledAvailable() {
        Settings.Secure.putInt(mContext.getContentResolver(), CONTROLS_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 1);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.power_menu_device_controls));
    }

    @Test
    public void getSummary_controlsDisabled() {
        Settings.Secure.putInt(mContext.getContentResolver(), CONTROLS_ENABLED, 0);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 1);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.power_menu_cards_passes));
    }

    @Test
    public void getSummary_allEnabled() {
        Settings.Secure.putInt(mContext.getContentResolver(), CONTROLS_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_ENABLED, 1);
        Settings.Secure.putInt(mContext.getContentResolver(), CARDS_AVAILABLE, 1);

        assertThat(mController.getSummary())
                .isEqualTo(mContext.getText(R.string.power_menu_cards_passes_device_controls));
    }
}
