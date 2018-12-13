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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.android.settings.datetime.timezone.model.TimeZoneData;
import com.android.settingslib.core.AbstractPreferenceController;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TimeZoneSettingsTest {

    @Test
    public void findRegionIdForTzId_matchExpectedCountry() {
        String tzId = "Unknown/Secret_City";
        TimeZoneData timeZoneData = mock(TimeZoneData.class);
        when(timeZoneData.lookupCountryCodesForZoneId(tzId))
                .thenReturn(new HashSet<>(Arrays.asList("US", "GB")));

        TimeZoneSettings settings = new TimeZoneSettings();
        settings.setTimeZoneData(timeZoneData);
        assertThat(settings.findRegionIdForTzId(tzId, null, "")).matches("US|GB");
        assertThat(settings.findRegionIdForTzId(tzId, "GB", "")).isEqualTo("GB");
        assertThat(settings.findRegionIdForTzId(tzId, null, "GB")).isEqualTo("GB");
    }

    @Test
    public void createPreferenceControllers_matchExpectedControllers() {
        TimeZoneSettings settings = new TimeZoneSettings();
        List<AbstractPreferenceController> controllers =
                settings.createPreferenceControllers(RuntimeEnvironment.application);
        assertThat(controllers).hasSize(3);
        assertThat(controllers.get(0)).isInstanceOf(RegionPreferenceController.class);
        assertThat(controllers.get(1)).isInstanceOf(RegionZonePreferenceController.class);
        assertThat(controllers.get(2)).isInstanceOf(FixedOffsetPreferenceController.class);
    }
}
