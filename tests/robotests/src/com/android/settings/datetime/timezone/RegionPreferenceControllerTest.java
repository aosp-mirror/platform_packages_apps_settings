/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.datetime.timezone;

import static com.google.common.truth.Truth.assertThat;

import android.app.Activity;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RegionPreferenceControllerTest {

    private Activity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(Activity.class);
    }

    @Test
    public void updateState_matchCountryName() {
        Preference preference = new Preference(mActivity);
        RegionPreferenceController controller = new RegionPreferenceController(mActivity);
        controller.setRegionId("US");
        controller.updateState(preference);
        assertThat(controller.getSummary()).isEqualTo("United States");
        assertThat(preference.getSummary()).isEqualTo("United States");
    }
}
