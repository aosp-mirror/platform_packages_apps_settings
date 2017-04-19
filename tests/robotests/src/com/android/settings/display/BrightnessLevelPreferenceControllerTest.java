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

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;

import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.testutils.shadow.SettingsShadowSystemProperties;

import java.text.NumberFormat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class BrightnessLevelPreferenceControllerTest {
    @Mock
    private Context mContext;
    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private Preference mPreference;

    private BrightnessLevelPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new BrightnessLevelPreferenceController(mContext);
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_shouldSetSummary() {
        final NumberFormat formatter = NumberFormat.getPercentInstance();
        when(mContext.getContentResolver()).thenReturn(mContentResolver);
        Settings.System.putInt(mContentResolver, SCREEN_BRIGHTNESS, 45);

        mController.updateState(mPreference);

        verify(mPreference).setSummary(formatter.format(45.0 / 255));
    }

}
