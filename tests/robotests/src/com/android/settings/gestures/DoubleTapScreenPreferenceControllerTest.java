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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.DISABLED_DEPENDENT_SETTING;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.dashboard.suggestions.SuggestionFeatureProviderImpl;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
public class DoubleTapScreenPreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private AmbientDisplayConfiguration mAmbientDisplayConfiguration;
    private DoubleTapScreenPreferenceController mController;

    private static final String KEY_DOUBLE_TAP_SCREEN = "gesture_double_tap_screen";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DoubleTapScreenPreferenceController(mContext, KEY_DOUBLE_TAP_SCREEN);
        mController.setConfig(mAmbientDisplayConfiguration);
    }

    @Test
    public void testIsChecked_configIsSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        when(mAmbientDisplayConfiguration.pulseOnDoubleTapEnabled(anyInt())).thenReturn(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_configIsNotSet_shouldReturnFalse() {
        when(mAmbientDisplayConfiguration.pulseOnDoubleTapEnabled(anyInt())).thenReturn(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        final Context context = RuntimeEnvironment.application;
        DoubleTapScreenPreferenceController controller =
                new DoubleTapScreenPreferenceController(context, KEY_DOUBLE_TAP_SCREEN);
        controller.setConfig(mAmbientDisplayConfiguration);
        ResultPayload payload = controller.getResultPayload();
        assertThat(payload).isInstanceOf(InlineSwitchPayload.class);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testSetValue_updatesCorrectly() {
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.DOZE_PULSE_ON_DOUBLE_TAP, 0);

        ((InlinePayload) mController.getResultPayload()).setValue(mContext, newValue);
        int updatedValue = Settings.Secure.getInt(resolver,
                Settings.Secure.DOZE_PULSE_ON_DOUBLE_TAP, -1);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
    public void testGetValue_correctValueReturned() {
        int currentValue = 1;
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.DOZE_PULSE_ON_DOUBLE_TAP, currentValue);

        int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }

    @Test
    public void isSuggestionCompleted_ambientDisplay_falseWhenNotVisited() {
        when(mAmbientDisplayConfiguration.pulseOnDoubleTapAvailable()).thenReturn(true);
        // No stored value in shared preferences if not visited yet.
        final Context context = RuntimeEnvironment.application;
        final SharedPreferences prefs =
            new SuggestionFeatureProviderImpl(context).getSharedPrefs(context);

        assertThat(DoubleTapScreenPreferenceController
            .isSuggestionComplete(mAmbientDisplayConfiguration, prefs)).isFalse();
    }

    @Test
    public void isSuggestionCompleted_ambientDisplay_trueWhenVisited() {
        when(mAmbientDisplayConfiguration.pulseOnDoubleTapAvailable()).thenReturn(false);
        final Context context = RuntimeEnvironment.application;
        final SharedPreferences prefs =
            new SuggestionFeatureProviderImpl(context).getSharedPrefs(context);

        prefs.edit().putBoolean(
                DoubleTapScreenSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(DoubleTapScreenPreferenceController
            .isSuggestionComplete(mAmbientDisplayConfiguration, prefs)).isTrue();
    }

    @Test
    public void canHandleClicks_falseWhenAlwaysOnEnabled() {
        when(mAmbientDisplayConfiguration.alwaysOnEnabled(anyInt())).thenReturn(true);
        assertThat(mController.canHandleClicks()).isFalse();
    }

    @Test
    public void canHandleClicks_trueWhenAlwaysOnDisabled() {
        when(mAmbientDisplayConfiguration.alwaysOnEnabled(anyInt())).thenReturn(false);
        assertThat(mController.canHandleClicks()).isTrue();
    }

    @Test
    public void getAvailabilityStatus_aodNotSupported_UNSUPPORTED_ON_DEVICE() {
        when(mAmbientDisplayConfiguration.doubleTapSensorAvailable()).thenReturn(false);
        when(mAmbientDisplayConfiguration.ambientDisplayAvailable()).thenReturn(false);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_aodOn_DISABLED_DEPENDENT_SETTING() {
        when(mAmbientDisplayConfiguration.doubleTapSensorAvailable()).thenReturn(true);
        when(mAmbientDisplayConfiguration.ambientDisplayAvailable()).thenReturn(false);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_aodSupported_aodOff_AVAILABLE() {
        when(mAmbientDisplayConfiguration.doubleTapSensorAvailable()).thenReturn(true);
        when(mAmbientDisplayConfiguration.ambientDisplayAvailable()).thenReturn(true);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(AVAILABLE);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final DoubleTapScreenPreferenceController controller =
                new DoubleTapScreenPreferenceController(mContext,"gesture_double_tap_screen");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final DoubleTapScreenPreferenceController controller =
                new DoubleTapScreenPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }
}
