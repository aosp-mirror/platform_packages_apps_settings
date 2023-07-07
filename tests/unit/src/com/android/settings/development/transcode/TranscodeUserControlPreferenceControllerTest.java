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
public class TranscodeUserControlPreferenceControllerTest {
    private static final String TRANSCODE_USER_CONTROL_SYS_PROP_KEY =
            "persist.sys.fuse.transcode_user_control";

    private TranscodeUserControlPreferenceController mUnderTest;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mUnderTest = new TranscodeUserControlPreferenceController(mContext, "some_key");
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
    }

    @Test
    public void isChecked_whenSysPropSet_shouldReturnTrue() {
        SystemProperties.set(TRANSCODE_USER_CONTROL_SYS_PROP_KEY, "true");
        assertThat(mUnderTest.isChecked()).isTrue();
    }

    @Test
    public void isChecked_whenSysPropUnset_shouldReturnFalse() {
        SystemProperties.set(TRANSCODE_USER_CONTROL_SYS_PROP_KEY, "false");
        assertThat(mUnderTest.isChecked()).isFalse();
    }

    @Test
    public void setChecked_withTrue_shouldSetSysProp() {
        mUnderTest.setChecked(true);
        assertThat(
                SystemProperties.getBoolean(TRANSCODE_USER_CONTROL_SYS_PROP_KEY, false)).isTrue();
    }

    @Test
    public void setChecked_withFalse_shouldUnsetSysProp() {
        mUnderTest.setChecked(false);
        assertThat(
                SystemProperties.getBoolean(TRANSCODE_USER_CONTROL_SYS_PROP_KEY, true)).isFalse();
    }

    @Test
    public void getAvailabilityStatus_shouldReturnAVAILABLE() {
        assertThat(mUnderTest.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_developerOptionFalse_shouldReturnUNAVAILABLE() {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        assertThat(mUnderTest.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }
}
