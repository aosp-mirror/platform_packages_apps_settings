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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.media.RingtoneManager;
import android.provider.Settings;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class RingtonePreferenceControllerBaseTest {

    private Context mContext;

    private RingtonePreferenceControllerBase mController;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mController = new RingtonePreferenceControllerBaseTestable(mContext);
    }

    @Test
    public void isAlwaysAvailable() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_shouldSetSummary() {
        Preference preference = mock(Preference.class);
        Settings.System.putString(mContext.getContentResolver(), Settings.System.RINGTONE,
                "content://test/ringtone");

        mController.updateState(preference);

        verify(preference).setSummary(anyString());
    }

    private class RingtonePreferenceControllerBaseTestable
            extends RingtonePreferenceControllerBase {
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
