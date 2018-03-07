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

import com.android.settings.datetime.timezone.BaseTimeZoneAdapter.AdapterItem;
import com.android.settings.datetime.timezone.model.TimeZoneData;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import libcore.util.CountryZonesFinder;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SettingsRobolectricTestRunner.class)
public class RegionSearchPickerTest {

    @Test
    public void createAdapter_matchRegionName() {
        List regionList = new ArrayList();
        regionList.add("US");
        CountryZonesFinder finder = mock(CountryZonesFinder.class);
        when(finder.lookupAllCountryIsoCodes()).thenReturn(regionList);

        RegionSearchPicker picker = new RegionSearchPicker() {
            @Override
            protected Locale getLocale() {
                return Locale.US;
            }
        };
        BaseTimeZoneAdapter adapter = picker.createAdapter(new TimeZoneData(finder));
        assertEquals(1, adapter.getItemCount());
        AdapterItem item = adapter.getItem(0);
        assertEquals("United States", item.getTitle().toString());
        assertThat(Arrays.asList(item.getSearchKeys())).contains("United States");
    }
}
