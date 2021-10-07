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
package com.android.settings.deviceinfo.hardwareinfo;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.os.Build;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.deviceinfo.HardwareInfoPreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public class HardwareInfoPreferenceControllerTest {

    private final String KEY = "device_model";

    private Preference mPreference;
    private PreferenceScreen mPreferenceScreen;
    private Context mContext;
    private HardwareInfoPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mController = new HardwareInfoPreferenceController(mContext, KEY);
        mPreference = new Preference(mContext);
        mPreference.setKey(KEY);
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mPreferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        mPreferenceScreen.addPreference(mPreference);
    }

    @Test
    public void isAvailable_returnTrueIfVisible() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isAvailable_returnFalseIfNotVisible() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void updatePreference_summaryShouldContainBuildModel() {
        mController.updateState(mPreference);

        assertThat(containBuildModel(mPreference.getSummary())).isTrue();
    }

    private boolean containBuildModel(CharSequence result) {
        return result.toString().contains(Build.MODEL);
    }
}
