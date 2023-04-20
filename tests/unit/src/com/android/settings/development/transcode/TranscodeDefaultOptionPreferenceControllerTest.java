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
public class TranscodeDefaultOptionPreferenceControllerTest {
    private static final String TRANSCODE_DEFAULT_SYS_PROP_KEY =
            "persist.sys.fuse.transcode_default";

    private TranscodeDefaultOptionPreferenceController mUnderTest;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mUnderTest = new TranscodeDefaultOptionPreferenceController(mContext, "some_key");
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 1);
    }

    @Test
    public void isChecked_whenSysPropSet_shouldReturnFalse() {
        SystemProperties.set(TRANSCODE_DEFAULT_SYS_PROP_KEY, "true");
        assertThat(mUnderTest.isChecked()).isFalse();
    }

    @Test
    public void isChecked_whenSysPropUnset_shouldReturnTrue() {
        SystemProperties.set(TRANSCODE_DEFAULT_SYS_PROP_KEY, "false");
        assertThat(mUnderTest.isChecked()).isTrue();
    }

    @Test
    public void setChecked_withTrue_shouldUnsetSysProp() {
        mUnderTest.setChecked(true);
        assertThat(
                SystemProperties.getBoolean(TRANSCODE_DEFAULT_SYS_PROP_KEY, true)).isFalse();
    }

    @Test
    public void setChecked_withFalse_shouldSetSysProp() {
        mUnderTest.setChecked(false);
        assertThat(
                SystemProperties.getBoolean(TRANSCODE_DEFAULT_SYS_PROP_KEY, false)).isTrue();
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
