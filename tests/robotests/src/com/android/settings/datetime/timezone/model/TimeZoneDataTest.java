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

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import libcore.util.CountryTimeZones;
import libcore.util.CountryZonesFinder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class TimeZoneDataTest {

    private CountryZonesFinder mCountryZonesFinder;

    @Before
    public void setUp() throws Exception {
        mCountryZonesFinder = mock(CountryZonesFinder.class);
        when(mCountryZonesFinder.lookupAllCountryIsoCodes()).thenReturn(new ArrayList<>());
    }

    @Test
    public void testRegionsWithTimeZone() {
        TimeZoneData timeZoneData = new TimeZoneData(mCountryZonesFinder);
        CountryTimeZones countryTimeZones = mock(CountryTimeZones.class);
        when(countryTimeZones.getTimeZoneMappings()).thenReturn(Collections.emptyList());
        when(mCountryZonesFinder.lookupCountryTimeZones("US")).thenReturn(countryTimeZones);
        assertThat(timeZoneData.lookupCountryTimeZones("US").getCountryTimeZones())
                .isSameAs(countryTimeZones);
    }

    @Test
    public void testGetRegionIds() {
        when(mCountryZonesFinder.lookupAllCountryIsoCodes()).thenReturn(Arrays.asList());
        TimeZoneData timeZoneData = new TimeZoneData(mCountryZonesFinder);
        assertThat(timeZoneData.getRegionIds()).isEmpty();

        when(mCountryZonesFinder.lookupAllCountryIsoCodes()).thenReturn(Arrays.asList("us", "GB"));
        timeZoneData = new TimeZoneData(mCountryZonesFinder);
        assertThat(timeZoneData.getRegionIds()).containsExactly("US", "GB");
    }

    @Test
    public void testLookupCountryCodesForZoneId() {
        TimeZoneData timeZoneData = new TimeZoneData(mCountryZonesFinder);
        assertThat(timeZoneData.lookupCountryCodesForZoneId(null)).isEmpty();
        CountryTimeZones US = mock(CountryTimeZones.class);
        when(US.getCountryIso()).thenReturn("us");
        when(US.getTimeZoneMappings()).thenReturn(Arrays.asList(
           new CountryTimeZones.TimeZoneMapping("Unknown/Secret_City", true),
           new CountryTimeZones.TimeZoneMapping("Unknown/Secret_City2", false)
        ));
        CountryTimeZones GB = mock(CountryTimeZones.class);
        when(GB.getCountryIso()).thenReturn("gb");
        when(GB.getTimeZoneMappings()).thenReturn(Arrays.asList(
           new CountryTimeZones.TimeZoneMapping("Unknown/Secret_City", true)
        ));
        when(mCountryZonesFinder.lookupCountryTimeZonesForZoneId("Unknown/Secret_City"))
                .thenReturn(Arrays.asList(US, GB));
        assertThat(timeZoneData.lookupCountryCodesForZoneId("Unknown/Secret_City"))
                .containsExactly("US", "GB");
        assertThat(timeZoneData.lookupCountryCodesForZoneId("Unknown/Secret_City2")).isEmpty();
    }
}
