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

package com.android.settings.notification;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.media.RingtoneManager;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class RingtonePreferenceControllerBaseTest {

    @Mock
    private Context mContext;

    private RingtonePreferenceControllerBase mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new RingtonePreferenceControllerBaseTestable(mContext);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }


    @Test
    public void updateState_shouldSetSummary() {
        Preference preference = mock(Preference.class);

        mController.updateState(preference);

        verify(preference).setSummary(anyString());
    }

    private class RingtonePreferenceControllerBaseTestable extends
        RingtonePreferenceControllerBase {
        RingtonePreferenceControllerBaseTestable(Context context) {
            super(context);
        }

        @Override
        public String getPreferenceKey() {
            return null;
        }

        @Override
        public int getRingtoneType() {
            return RingtoneManager.TYPE_RINGTONE;
        }
    }

}
