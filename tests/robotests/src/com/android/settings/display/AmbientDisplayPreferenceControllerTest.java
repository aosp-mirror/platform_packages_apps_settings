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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.v7.preference.Preference;

import com.android.internal.hardware.AmbientDisplayConfiguration;
import com.android.settings.R;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.ShadowSecureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION,
        shadows = {ShadowSecureSettings.class})
public class AmbientDisplayPreferenceControllerTest {

    @Mock Context mContext;
    @Mock AmbientDisplayConfiguration mConfig;
    @Mock Preference mPreference;

    AmbientDisplayPreferenceController mController;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mController = new AmbientDisplayPreferenceController(mContext, mConfig, "key");
    }

    @Test
    public void isAvailable_available() throws Exception {
        when(mConfig.available()).thenReturn(true);
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isAvailable_unavailable() throws Exception {
        when(mConfig.available()).thenReturn(false);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void updateState_alwaysOn() throws Exception {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(true);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.ambient_display_screen_summary_always_on);
    }

    @Test
    public void updateState_notifications() throws Exception {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(false);
        when(mConfig.pulseOnNotificationEnabled(anyInt())).thenReturn(true);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.ambient_display_screen_summary_notifications);
    }

    @Test
    public void updateState_gestures() throws Exception {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(false);
        when(mConfig.pulseOnNotificationEnabled(anyInt())).thenReturn(false);
        when(mConfig.enabled(anyInt())).thenReturn(true);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.switch_on_text);
    }

    @Test
    public void updateState_off() throws Exception {
        when(mConfig.alwaysOnEnabled(anyInt())).thenReturn(false);
        when(mConfig.pulseOnNotificationEnabled(anyInt())).thenReturn(false);
        when(mConfig.pulseOnDoubleTapEnabled(anyInt())).thenReturn(false);
        when(mConfig.pulseOnPickupEnabled(anyInt())).thenReturn(false);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(R.string.switch_off_text);
    }

    @Test
    public void getPreferenceKey() throws Exception {
        assertThat(mController.getPreferenceKey()).isEqualTo("key");
    }

}