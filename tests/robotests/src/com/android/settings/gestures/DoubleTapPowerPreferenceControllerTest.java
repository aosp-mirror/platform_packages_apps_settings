/*
 * Copyright (C) 2016 The Android Open Source Project
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

import static android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED;
import static android.provider.Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED;

import static com.android.settings.gestures.DoubleTapPowerPreferenceController.isSuggestionComplete;
import static com.android.settings.gestures.DoubleTapPowerToOpenCameraPreferenceController.OFF;
import static com.android.settings.gestures.DoubleTapPowerToOpenCameraPreferenceController.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.service.quickaccesswallet.Flags;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class DoubleTapPowerPreferenceControllerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext;
    private ContentResolver mContentResolver;
    private DoubleTapPowerPreferenceController mController;
    private Preference mPreference;
    private PreferenceScreen mScreen;
    private static final String KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.getApplication();
        mContentResolver = mContext.getContentResolver();
        mController = new DoubleTapPowerPreferenceController(mContext, KEY_DOUBLE_TAP_POWER);
        mPreference = new Preference(mContext);
        mScreen = mock(PreferenceScreen.class);
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isAvailable_flagEnabled_configIsTrue_returnsTrue() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_doubleTapPowerGestureEnabled, Boolean.TRUE);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isAvailable_flagEnabled_configIsFalse_returnsFalse() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_doubleTapPowerGestureEnabled, Boolean.FALSE);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isAvailable_flagDisabled_configIsTrue_returnsTrue() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled,
                Boolean.TRUE);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isAvailable_flagDisabled_configIsFalse_returnsFalse() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled,
                Boolean.FALSE);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isSuggestionCompleted_enableFlag_doubleTapPower_trueWhenNotAvailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_doubleTapPowerGestureEnabled, false);

        assertThat(isSuggestionComplete(mContext, null /* prefs */)).isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isSuggestionCompleted_enableFlag_doubleTapPower_falseWhenNotVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_doubleTapPowerGestureEnabled, true);
        // No stored value in shared preferences if not visited yet.
        final SharedPreferences prefs =
                new SuggestionFeatureProviderImpl().getSharedPrefs(mContext);

        assertThat(isSuggestionComplete(mContext, prefs)).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isSuggestionCompleted_enableFlag_doubleTapPower_trueWhenVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_doubleTapPowerGestureEnabled, true);
        // No stored value in shared preferences if not visited yet.
        final SharedPreferences prefs =
                new SuggestionFeatureProviderImpl().getSharedPrefs(mContext);
        prefs.edit().putBoolean(DoubleTapPowerSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(isSuggestionComplete(mContext, prefs)).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isSuggestionCompleted_disableFlag_doubleTapPower_trueWhenNotAvailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, false);

        assertThat(isSuggestionComplete(mContext, null /* prefs */)).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isSuggestionCompleted_disableFlag_doubleTapPower_falseWhenNotVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, true);
        // No stored value in shared preferences if not visited yet.
        final SharedPreferences prefs =
                new SuggestionFeatureProviderImpl().getSharedPrefs(mContext);

        assertThat(isSuggestionComplete(mContext, prefs)).isFalse();
    }

    @Test
    @DisableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void isSuggestionCompleted_disableFlag_doubleTapPower_trueWhenVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, true);
        // No stored value in shared preferences if not visited yet.
        final SharedPreferences prefs =
                new SuggestionFeatureProviderImpl().getSharedPrefs(mContext);
        prefs.edit().putBoolean(DoubleTapPowerSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(isSuggestionComplete(mContext, prefs)).isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void displayPreference_flagDisabled_doubleTapPowerLegacyTitleIsDisplayed() {
        mController.displayPreference(mScreen);

        assertThat(
                        TextUtils.equals(
                                mPreference.getTitle(),
                                mContext.getText(R.string.double_tap_power_for_camera_title)))
                .isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void getSummary_flagDisabled_doubleTapPowerEnabled_returnsOn() {
        // Set the setting to be enabled.
        Settings.Secure.putInt(mContentResolver, CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, ON);

        assertThat(
                        TextUtils.equals(
                                mController.getSummary(),
                                mContext.getText(R.string.gesture_setting_on)))
                .isTrue();
    }

    @Test
    @DisableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void getSummary_flagDisabled_doubleTapPowerDisabled_returnsOff() {
        // Set the setting to be disabled.
        Settings.Secure.putInt(mContentResolver, CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, OFF);

        assertThat(
                        TextUtils.equals(
                                mController.getSummary(),
                                mContext.getText(R.string.gesture_setting_off)))
                .isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void getSummary_flagEnabled_doubleTapPowerDisabled_returnsOff() {
        // Set the setting to be disabled.
        Settings.Secure.putInt(
                mContentResolver, DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, 0 /* OFF */);

        assertThat(
                        TextUtils.equals(
                                mController.getSummary(),
                                mContext.getText(R.string.gesture_setting_off)))
                .isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void getSummary_flagEnabled_doubleTapPowerEnabled_cameraTargetAction_returnsSummary() {
        // Set the setting to be enabled.
        Settings.Secure.putInt(
                mContentResolver, DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, 1 /* ON */);
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForCameraLaunch(mContext);

        assertThat(
                        TextUtils.equals(
                                mController.getSummary(),
                                mContext.getString(
                                        R.string.double_tap_power_summary,
                                        mContext.getText(R.string.gesture_setting_on),
                                        mContext.getText(
                                                R.string.double_tap_power_camera_action_summary))))
                .isTrue();
    }

    @Test
    @EnableFlags(Flags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    public void getSummary_flagEnabled_doubleTapPowerEnabled_walletTargetAction_returnsSummary() {
        // Set the setting to be enabled.
        Settings.Secure.putInt(
                mContentResolver, DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED, 1 /* ON */);
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForWalletLaunch(mContext);

        assertThat(
                        TextUtils.equals(
                                mController.getSummary(),
                                mContext.getString(
                                        R.string.double_tap_power_summary,
                                        mContext.getText(R.string.gesture_setting_on),
                                        mContext.getText(
                                                R.string.double_tap_power_wallet_action_summary))))
                .isTrue();
    }
}
