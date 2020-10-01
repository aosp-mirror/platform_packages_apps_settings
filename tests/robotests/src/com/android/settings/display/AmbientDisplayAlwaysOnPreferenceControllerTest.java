/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.display;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.hardware.display.AmbientDisplayConfiguration;
import android.provider.Settings;

import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowSecureSettings.class)
public class AmbientDisplayAlwaysOnPreferenceControllerTest {

    @Mock
    private AmbientDisplayConfiguration mConfig;

    private Context mContext;

    private ContentResolver mContentResolver;

    private AmbientDisplayAlwaysOnPreferenceController mController;
    private boolean mCallbackInvoked;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mContentResolver = mContext.getContentResolver();
        mController = new AmbientDisplayAlwaysOnPreferenceController(mContext, "key");
        mController.setConfig(mConfig);
        mController.setCallback(() -> mCallbackInvoked = true);
    }

    @Test
    public void getAvailabilityStatus_available() {
        when(mConfig.alwaysOnAvailableForUser(anyInt())).thenReturn(true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                AmbientDisplayAlwaysOnPreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_disabled_unsupported() {
        when(mConfig.alwaysOnAvailableForUser(anyInt())).thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                AmbientDisplayAlwaysOnPreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void isChecked_enabled() {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(true);

        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void isChecked_disabled() {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(false);

        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void setChecked_enabled() {
        mController.setChecked(true);

        assertThat(Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, -1))
                .isEqualTo(1);
    }

    @Test
    public void setChecked_disabled() {
        mController.setChecked(false);

        assertThat(Settings.Secure.getInt(mContentResolver, Settings.Secure.DOZE_ALWAYS_ON, -1))
                .isEqualTo(0);
    }

    @Test
    public void onPreferenceChange_callback() {
        assertThat(mCallbackInvoked).isFalse();
        mController.setChecked(true);
        assertThat(mCallbackInvoked).isTrue();
    }

    @Test
    public void isSliceableCorrectKey_returnsTrue() {
        final AmbientDisplayAlwaysOnPreferenceController controller =
                new AmbientDisplayAlwaysOnPreferenceController(mContext,
                        "ambient_display_always_on");
        assertThat(controller.isSliceable()).isTrue();
    }

    @Test
    public void isSliceableIncorrectKey_returnsFalse() {
        final AmbientDisplayAlwaysOnPreferenceController controller =
                new AmbientDisplayAlwaysOnPreferenceController(mContext, "bad_key");
        assertThat(controller.isSliceable()).isFalse();
    }

    @Test
    public void isPublicSlice_returnTrue() {
        assertThat(mController.isPublicSlice()).isTrue();
    }
}
