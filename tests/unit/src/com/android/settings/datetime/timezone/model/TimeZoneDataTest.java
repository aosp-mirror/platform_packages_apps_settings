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
package com.android.settings.datetime.timezone.model;

import static com.google.common.truth.Truth.assertThat;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TimeZoneDataTest {

    private TimeZoneData mTimeZoneData;
    @Before
    public void setUp() {
        mTimeZoneData = TimeZoneData.getInstance();
    }

    @Test
    public void lookupCountryTimeZones_shouldReturnAtLeastOneTimeZoneInEveryRegion() {
        Set<String> regionIds = mTimeZoneData.getRegionIds();
        for (String regionId : regionIds) {
            FilteredCountryTimeZones countryTimeZones =
                    mTimeZoneData.lookupCountryTimeZones(regionId);
            assertThat(countryTimeZones).isNotNull();
            assertThat(countryTimeZones.getTimeZoneIds().size()).isGreaterThan(0);
        }
    }

    @Test
    public void lookupCountryCodesForZoneId_shouldNotReturnHiddenZone() {
        /*
        Simferopol is filtered out for two reasons:
        1) because we specifically exclude it with the picker attribute, and
        2) because it's the same as Moscow after Oct 2014.
        */
        assertThat(mTimeZoneData.lookupCountryCodesForZoneId("Europe/Simferopol").isEmpty())
                .isTrue();
        assertThat(mTimeZoneData.lookupCountryCodesForZoneId("Europe/London").isEmpty())
                .isFalse();
        assertThat(mTimeZoneData.lookupCountryCodesForZoneId("America/Los_Angeles").isEmpty())
                .isFalse();
    }
}
