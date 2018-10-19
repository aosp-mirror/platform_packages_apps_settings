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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

@RunWith(SettingsRobolectricTestRunner.class)
public class PickupGesturePreferenceControllerTest {

    private static final String KEY_PICK_UP = "gesture_pick_up";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private AmbientDisplayConfiguration mAmbientDisplayConfiguration;

    private PickupGesturePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new PickupGesturePreferenceController(mContext, KEY_PICK_UP);
        mController.setConfig(mAmbientDisplayConfiguration);
    }

    @Test
    public void testIsChecked_configIsSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        when(mAmbientDisplayConfiguration.pulseOnPickupEnabled(anyInt())).thenReturn(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_configIsNotSet_shouldReturnFalse() {
        // Set the setting to be disabled.
        when(mAmbientDisplayConfiguration.pulseOnPickupEnabled(anyInt())).thenReturn(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void testCanHandleClicks_configIsSet_shouldReturnTrue() {
        mController = spy(mController);
        doReturn(true).when(mController).pulseOnPickupCanBeModified();

        assertThat(mController.canHandleClicks()).isTrue();
    }

    @Test
    public void testCanHandleClicks_configIsNotSet_shouldReturnFalse() {
        mController = spy(mController);
        doReturn(false).when(mController).pulseOnPickupCanBeModified();

        assertThat(mController.canHandleClicks()).isFalse();
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        final Context context = RuntimeEnvironment.application;
        PickupGesturePreferenceController controller =
                new PickupGesturePreferenceController(context, KEY_PICK_UP);
        controller.setConfig(mAmbientDisplayConfiguration);
        ResultPayload payload = controller.getResultPayload();
        assertThat(payload).isInstanceOf(InlineSwitchPayload.class);
    }

    @Test
    public void testSetValue_updatesCorrectly() {
        int newValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.DOZE_PULSE_ON_PICK_UP, 0);

        ((InlinePayload) mController.getResultPayload()).setValue(mContext, newValue);
        int updatedValue = Settings.Secure.getInt(resolver,
                Settings.Secure.DOZE_PULSE_ON_PICK_UP, -1);

        assertThat(updatedValue).isEqualTo(newValue);
    }

    @Test
    public void testGetValue_correctValueReturned() {
        int currentValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.DOZE_PULSE_ON_PICK_UP, currentValue);

        int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }

    @Test
    public void isSuggestionCompleted_ambientDisplayPickup_trueWhenVisited() {
        when(mContext.getResources().getBoolean(anyInt())).thenReturn(true);
        when(mContext.getResources().getString(anyInt())).thenReturn("foo");
        final Context context = RuntimeEnvironment.application;
        final SharedPreferences prefs =
            new SuggestionFeatureProviderImpl(context).getSharedPrefs(context);
        prefs.edit().putBoolean(PickupGestureSettings.PREF_KEY_SUGGESTION_COMPLETE, true).commit();

        assertThat(PickupGesturePreferenceController.isSuggestionComplete(mContext, prefs))
                .isTrue();
    }

    @Test
    public void getAvailabilityStatus_aodNotSupported_UNSUPPORTED_ON_DEVICE() {
        when(mAmbientDisplayConfiguration.dozePulsePickupSensorAvailable()).thenReturn(false);
        when(mAmbientDisplayConfiguration.ambientDisplayAvailable()).thenReturn(false);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_aodOn_DISABLED_DEPENDENT_SETTING() {
        when(mAmbientDisplayConfiguration.dozePulsePickupSensorAvailable()).thenReturn(true);
        when(mAmbientDisplayConfiguration.ambientDisplayAvailable()).thenReturn(false);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_aodSupported_aodOff_AVAILABLE() {
        when(mAmbientDisplayConfiguration.dozePulsePickupSensorAvailable()).thenReturn(true);
        when(mAmbientDisplayConfiguration.ambientDisplayAvailable()).thenReturn(true);
        final int availabilityStatus = mController.getAvailabilityStatus();

        assertThat(availabilityStatus).isEqualTo(AVAILABLE);
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final PickupGesturePreferenceController controller =
                new PickupGesturePreferenceController(mContext,"gesture_pick_up");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final PickupGesturePreferenceController controller =
                new PickupGesturePreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }
}
