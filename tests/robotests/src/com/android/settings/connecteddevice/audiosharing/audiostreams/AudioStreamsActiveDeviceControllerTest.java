/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.audiosharing.audiostreams;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AudioStreamsActiveDeviceControllerTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    private AudioStreamsActiveDeviceController mController;
    @Mock private PreferenceScreen mScreen;
    @Mock private Preference mPreference;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        mController =
                new AudioStreamsActiveDeviceController(
                        context, AudioStreamsActiveDeviceController.KEY);
        when(mScreen.findPreference(anyString())).thenReturn(mPreference);
    }

    @Test
    public void getAvailabilityStatus() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onSummaryChanged_shouldSetPreferenceSummary() {
        String summary = "summary";
        mController.displayPreference(mScreen);
        mController.onSummaryChanged(summary);

        verify(mPreference).setSummary(summary);
    }
}
