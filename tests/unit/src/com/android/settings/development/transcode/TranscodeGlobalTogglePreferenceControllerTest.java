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

package com.android.settings.development.transcode;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.SystemProperties;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TranscodeGlobalTogglePreferenceControllerTest {

    private static final String TRANSCODE_ENABLED_PROP_KEY = "persist.sys.fuse.transcode_enabled";

    private TranscodeGlobalTogglePreferenceController mController;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mController = new TranscodeGlobalTogglePreferenceController(mContext, "test_key");
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
    }

    @Test
    public void isAvailable_shouldReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void isChecked_whenDisabled_shouldReturnFalse() {
        SystemProperties.set(TRANSCODE_ENABLED_PROP_KEY, "false");
        assertThat(mController.isChecked()).isFalse();
    }

    @Test
    public void isChecked_whenEnabled_shouldReturnTrue() {
        SystemProperties.set(TRANSCODE_ENABLED_PROP_KEY, "true");
        assertThat(mController.isChecked()).isTrue();
    }

    @Test
    public void setChecked_withTrue_shouldUpdateSystemProperty() {
        // Simulate the UI being clicked.
        mController.setChecked(true);

        // Verify the system property was updated.
        assertThat(SystemProperties.getBoolean(TRANSCODE_ENABLED_PROP_KEY, false)).isTrue();
    }

    @Test
    public void setChecked_withFalse_shouldUpdateSystemProperty() {
        // Simulate the UI being clicked.
        mController.setChecked(false);

        // Verify the system property was updated.
        assertThat(SystemProperties.getBoolean(TRANSCODE_ENABLED_PROP_KEY, true)).isFalse();
    }

    @Test
    public void getAvailabilityStatus_developerOptionFalse_shouldReturnUNAVAILABLE() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }
}
