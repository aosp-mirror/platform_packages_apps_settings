/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.deviceinfo.firmwareversion;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Build;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class FirmwareVersionPreferenceControllerTest {

    private static final String KEY = "firmware_version";

    private Preference mPreference;
    private PreferenceScreen mScreen;
    private FirmwareVersionPreferenceController mController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final Context context = RuntimeEnvironment.application;
        final PreferenceManager preferenceManager = new PreferenceManager(context);
        mController = new FirmwareVersionPreferenceController(context, KEY);
        mPreference = new Preference(context);
        mPreference.setKey(KEY);
        mScreen = preferenceManager.createPreferenceScreen(context);
        mScreen.addPreference(mPreference);
    }

    @Test
    public void firmwareVersion_shouldAlwaysBeShown() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void updatePreference_shouldSetSummaryToBuildNumber() {
        mController.updateState(mPreference);

        assertThat(mPreference.getSummary()).isEqualTo(Build.VERSION.RELEASE_OR_CODENAME);
    }
}
