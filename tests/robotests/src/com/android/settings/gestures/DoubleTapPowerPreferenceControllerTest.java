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
import static com.android.settings.gestures.DoubleTapPowerPreferenceController.OFF;
import static com.android.settings.gestures.DoubleTapPowerPreferenceController.ON;
import static com.android.settings.gestures.DoubleTapPowerPreferenceController.isSuggestionComplete;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.android.settings.TestConfig;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION_O, shadows = {
        SettingsShadowResources.class
})
public class DoubleTapPowerPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private DoubleTapPowerPreferenceController mController;
    private static final String KEY_DOUBLE_TAP_POWER = "gesture_double_tap_power";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DoubleTapPowerPreferenceController(mContext, null, KEY_DOUBLE_TAP_POWER);
    }

    @After
    public void tearDown() {
        SettingsShadowResources.reset();
    }

    @Test
    public void isAvailable_configIsTrue_shouldReturnTrue() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled))
                .thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configIsTrue_shouldReturnFalse() {
        when(mContext.getResources().
                getBoolean(com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled))
                .thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testSwitchEnabled_configIsNotSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        final Context context = RuntimeEnvironment.application;
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, ON);
        mController = new DoubleTapPowerPreferenceController(context, null, KEY_DOUBLE_TAP_POWER);

        assertThat(mController.isSwitchPrefEnabled()).isTrue();
    }

    @Test
    public void testSwitchEnabled_configIsSet_shouldReturnFalse() {
        // Set the setting to be disabled.
        final Context context = RuntimeEnvironment.application;
        Settings.System.putInt(context.getContentResolver(),
                CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, OFF);
        mController = new DoubleTapPowerPreferenceController(context, null, KEY_DOUBLE_TAP_POWER);

        assertThat(mController.isSwitchPrefEnabled()).isFalse();
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        final Context context = RuntimeEnvironment.application;
        DoubleTapPowerPreferenceController controller =
                new DoubleTapPowerPreferenceController(context, null /* lifecycle */,
                        KEY_DOUBLE_TAP_POWER);
        ResultPayload payload = controller.getResultPayload();
        assertThat(payload).isInstanceOf(InlineSwitchPayload.class);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testSetValue_updatesCorrectly() {
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
                0);

        InlinePayload payload = ((InlineSwitchPayload) mController.getResultPayload());
        payload.setValue(mContext, newValue);
        int updatedValue = Settings.Secure.getInt(resolver,
                Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, -1);
        updatedValue = 1 - updatedValue; // DoubleTapPower is a non-standard switch

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testGetValue_correctValueReturned() {
        int currentValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver,
                Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED, currentValue);

        int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);
        newValue = 1 - newValue; // DoubleTapPower is a non-standard switch
        assertThat(newValue).isEqualTo(currentValue);
    }

    @Test
    public void isSuggestionCompleted_doubleTapPower_trueWhenNotAvailable() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, false);

        assertThat(
                isSuggestionComplete(RuntimeEnvironment.application, null/* prefs */))
                .isTrue();
    }

    @Test
    public void isSuggestionCompleted_doubleTapPower_falseWhenNotVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, true);
        // No stored value in shared preferences if not visited yet.
        final Context context = RuntimeEnvironment.application;
        final SharedPreferences prefs = new SuggestionFeatureProviderImpl(context)
                .getSharedPrefs(context);
        assertThat(
                isSuggestionComplete(RuntimeEnvironment.application, prefs))
                .isFalse();
    }

    @Test
    public void isSuggestionCompleted_doubleTapPower_trueWhenVisited() {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_cameraDoubleTapPowerGestureEnabled, true);
        // No stored value in shared preferences if not visited yet.
        final Context context = RuntimeEnvironment.application;
        final SharedPreferences prefs = new SuggestionFeatureProviderImpl(context)
                .getSharedPrefs(context);
        prefs.edit().putBoolean(
                DoubleTapPowerSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(
                isSuggestionComplete(RuntimeEnvironment.application, prefs))
                .isTrue();
    }
}
