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

import android.content.Context;
import android.provider.Settings;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.TimeoutListPreference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;
import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class TimeoutPreferenceControllerTest {
    private static final int TIMEOUT = 30;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    @Mock
    private TimeoutListPreference mPreference;
    private TimeoutPreferenceController mController;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mController = new TimeoutPreferenceController(mContext, KEY_SCREEN_TIMEOUT);
    }

    @Test
    public void testOnPreferenceChange_SetTimeout_ReturnCorrectTimeout() {
        mController.onPreferenceChange(mPreference, Integer.toString(TIMEOUT));

        final int mode = Settings.System.getInt(mContext.getContentResolver(),
                SCREEN_OFF_TIMEOUT, 0);
        assertThat(mode).isEqualTo(TIMEOUT);
    }
}
