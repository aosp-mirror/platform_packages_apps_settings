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

import android.annotation.StringRes;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.provider.Settings;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.Arrays;
import java.util.Collection;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class PowerMenuPreferenceControllerSummaryTest {

    private static final String KEY_GESTURE_POWER_MENU = "gesture_power_menu";
    private static final String CONTROLS_ENABLED = Settings.Secure.CONTROLS_ENABLED;
    private static final String CONTROLS_FEATURE = PackageManager.FEATURE_CONTROLS;
    private static final String CARDS_ENABLED = Settings.Secure.GLOBAL_ACTIONS_PANEL_ENABLED;
    private static final String CARDS_AVAILABLE = Settings.Secure.GLOBAL_ACTIONS_PANEL_AVAILABLE;

    @ParameterizedRobolectricTestRunner.Parameters(
            name = "ctrls available={0}, ctrls enabled={1}, cards available={2}, cards enabled={3}")
    public static Collection data() {
        return Arrays.asList(new Object[][]{
                // controls available, controls enabled, cards available, cards enabled, summary
                {false, false, false, false, R.string.power_menu_none},
                {false, false, false, true, R.string.power_menu_none},
                {false, false, true, false, R.string.power_menu_none},
                {false, false, true, true, R.string.power_menu_cards_passes},
                {false, true, false, false, R.string.power_menu_none},
                {false, true, false, true, R.string.power_menu_none},
                {false, true, true, false, R.string.power_menu_none},
                {false, true, true, true, R.string.power_menu_cards_passes},
                {true, false, false, false, R.string.power_menu_none},
                {true, false, false, true, R.string.power_menu_none},
                {true, false, true, false, R.string.power_menu_none},
                {true, false, true, true, R.string.power_menu_cards_passes},
                {true, true, false, false, R.string.power_menu_device_controls},
                {true, true, false, true, R.string.power_menu_device_controls},
                {true, true, true, false, R.string.power_menu_device_controls},
                {true, true, true, true, R.string.power_menu_cards_passes_device_controls}
        });
    }

    private Context mContext;
    private PowerMenuPreferenceController mController;
    private ShadowPackageManager mShadowPackageManager;

    private boolean mControlsAvailable;
    private boolean mControlsEnabled;
    private boolean mCardsAvailable;
    private boolean mCardsEnabled;
    private @StringRes int mSummaryRes;

    public PowerMenuPreferenceControllerSummaryTest(
            boolean controlsAvailable,
            boolean controlsEnabled,
            boolean cardsAvailable,
            boolean cardsEnabled,
            @StringRes int summaryRes) {
        mControlsAvailable = controlsAvailable;
        mControlsEnabled = controlsEnabled;
        mCardsAvailable = cardsAvailable;
        mCardsEnabled = cardsEnabled;
        mSummaryRes = summaryRes;
    }

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mShadowPackageManager = Shadows.shadowOf(mContext.getPackageManager());
        mController = new PowerMenuPreferenceController(mContext, KEY_GESTURE_POWER_MENU);
    }

    @Test
    public void getSummary_possiblyAvailableAndEnabled() {
        mShadowPackageManager.setSystemFeature(CONTROLS_FEATURE, mControlsAvailable);
        ContentResolver cr = mContext.getContentResolver();
        Settings.Secure.putInt(cr, CONTROLS_ENABLED, mControlsEnabled ? 1 : 0);
        Settings.Secure.putInt(cr, CARDS_AVAILABLE, mCardsAvailable ? 1 : 0);
        Settings.Secure.putInt(cr, CARDS_ENABLED, mCardsEnabled ? 1 : 0);

        assertThat(mController.getSummary()).isEqualTo(mContext.getText(mSummaryRes));
    }
}
