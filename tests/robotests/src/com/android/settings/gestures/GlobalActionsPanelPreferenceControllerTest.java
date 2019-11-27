/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.gestures;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.provider.Settings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class GlobalActionsPanelPreferenceControllerTest {

    private Context mContext;
    private GlobalActionsPanelPreferenceController mController;

    private static final String KEY_GESTURE_PANEL = "gesture_global_actions_panel";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new GlobalActionsPanelPreferenceController(mContext, KEY_GESTURE_PANEL);
    }

    @Test
    public void testIsChecked_panelEnabled() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), mController.ENABLED_SETTING, 1);
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void testIsChecked_panelDisabled() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), mController.ENABLED_SETTING, 0);
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void getAvailabilityStatus_panelAvailable() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), mController.AVAILABLE_SETTING, 1);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(mController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_panelUnavailable() {
        Settings.Secure.putInt(
                mContext.getContentResolver(), mController.AVAILABLE_SETTING, 0);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(mController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isSliceable_correctKey() {
        final GlobalActionsPanelPreferenceController controller =
                new GlobalActionsPanelPreferenceController(mContext, mController.TOGGLE_KEY);
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceable_incorrectKey() {
        final GlobalActionsPanelPreferenceController controller =
                new GlobalActionsPanelPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
