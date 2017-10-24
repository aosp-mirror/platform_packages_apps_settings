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

import android.content.ContentResolver;
import android.content.Context;

import android.provider.Settings;
import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.search.InlinePayload;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import com.android.settings.testutils.shadow.ShadowSecureSettings;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PIckupGesturePreferenceControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private AmbientDisplayConfiguration mAmbientDisplayConfiguration;

    private PickupGesturePreferenceController mController;

    private static final String KEY_PICK_UP = "gesture_pick_up";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new PickupGesturePreferenceController(
                mContext, null, mAmbientDisplayConfiguration, 0, KEY_PICK_UP);
    }

    @Test
    public void isAvailable_configIsTrue_shouldReturnTrue() {
        when(mAmbientDisplayConfiguration.pulseOnPickupAvailable()).thenReturn(true);

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_configIsFalse_shouldReturnFalse() {
        when(mAmbientDisplayConfiguration.pulseOnPickupAvailable()).thenReturn(false);

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testSwitchEnabled_configIsSet_shouldReturnTrue() {
        // Set the setting to be enabled.
        when(mAmbientDisplayConfiguration.pulseOnPickupEnabled(anyInt())).thenReturn(true);

        assertThat(mController.isSwitchPrefEnabled()).isTrue();
    }

    @Test
    public void testSwitchEnabled_configIsNotSet_shouldReturnFalse() {
        // Set the setting to be disabled.
        when(mAmbientDisplayConfiguration.pulseOnPickupEnabled(anyInt())).thenReturn(false);

        assertThat(mController.isSwitchPrefEnabled()).isFalse();
    }

    @Test
    public void testCanHandleClicks_configIsSet_shouldReturnTrue() {
        when(mAmbientDisplayConfiguration.pulseOnPickupCanBeModified(anyInt())).thenReturn(true);

        assertThat(mController.canHandleClicks()).isTrue();
    }

    @Test
    public void testCanHandleClicks_configIsNotSet_shouldReturnFalse() {
        when(mAmbientDisplayConfiguration.pulseOnPickupCanBeModified(anyInt())).thenReturn(false);

        assertThat(mController.canHandleClicks()).isFalse();
    }

    @Test
    public void testPreferenceController_ProperResultPayloadType() {
        final Context context = RuntimeEnvironment.application;
        PickupGesturePreferenceController controller =
                new PickupGesturePreferenceController(
                        context, null, mAmbientDisplayConfiguration, 0, KEY_PICK_UP);
        ResultPayload payload = controller.getResultPayload();
        assertThat(payload).isInstanceOf(InlineSwitchPayload.class);
    }

    @Test
    @Config(shadows = ShadowSecureSettings.class)
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
    @Config(shadows = ShadowSecureSettings.class)
    public void testGetValue_correctValueReturned() {
        int currentValue = 1;
        ContentResolver resolver = mContext.getContentResolver();
        Settings.Secure.putInt(resolver, Settings.Secure.DOZE_PULSE_ON_PICK_UP, currentValue);

        int newValue = ((InlinePayload) mController.getResultPayload()).getValue(mContext);

        assertThat(newValue).isEqualTo(currentValue);
    }
}
