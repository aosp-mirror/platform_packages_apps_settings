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

import com.android.settings.datetime.timezone.model.TimeZoneData;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import libcore.util.CountryZonesFinder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
public class FixedOffsetPickerTest {

    @Test
    public void getAllTimeZoneInfos_containsUtcAndGmtZones() {
        List regionList = Collections.emptyList();
        CountryZonesFinder finder = mock(CountryZonesFinder.class);
        when(finder.lookupAllCountryIsoCodes()).thenReturn(regionList);

        FixedOffsetPicker picker = new FixedOffsetPicker() {
            @Override
            protected Locale getLocale() {
                return Locale.US;
            }
        };
        List<TimeZoneInfo> infos = picker.getAllTimeZoneInfos(new TimeZoneData(finder));
        List<String> tzIds = infos.stream().map(info -> info.getId()).collect(Collectors.toList());
        tzIds.contains("Etc/Utc");
        tzIds.contains("Etc/GMT-12");
        tzIds.contains("Etc/GMT+14");
    }
}
