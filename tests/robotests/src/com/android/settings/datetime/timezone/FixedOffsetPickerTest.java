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

import android.content.Context;

import com.android.settings.datetime.timezone.BaseTimeZoneAdapter.AdapterItem;
import com.android.settings.datetime.timezone.model.TimeZoneData;

import libcore.timezone.CountryZonesFinder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RunWith(RobolectricTestRunner.class)
public class FixedOffsetPickerTest {

    private CountryZonesFinder mFinder;

    @Before
    public void setUp() {
        List<String> regionList = Collections.emptyList();
        mFinder = mock(CountryZonesFinder.class);
        when(mFinder.lookupAllCountryIsoCodes()).thenReturn(regionList);
    }

    @Test
    public void getAllTimeZoneInfos_containsUtcAndGmtZones() {
        TestFixedOffsetPicker picker = new TestFixedOffsetPicker();
        List<TimeZoneInfo> infos = picker.getAllTimeZoneInfos(new TimeZoneData(mFinder));
        List<String> tzIds = infos.stream().map(info -> info.getId()).collect(Collectors.toList());
        assertThat(tzIds).contains("Etc/UTC");
        assertThat(tzIds).contains("Etc/GMT-14"); // Etc/GMT-14 means GMT+14:00
        assertThat(tzIds).contains("Etc/GMT+12"); // Etc/GMT+14 means GMT-12:00
    }

    @Test
    public void createAdapter_verifyTitleAndOffset() {
        TestFixedOffsetPicker picker = new TestFixedOffsetPicker();
        BaseTimeZoneAdapter adapter = picker.createAdapter(new TimeZoneData(mFinder));
        assertThat(adapter.getItemCount()).isEqualTo(12 + 1 + 14); // 27 GMT offsets from -12 to +14
        AdapterItem utc = adapter.getDataItem(0);
        assertThat(utc.getTitle().toString()).isEqualTo("Coordinated Universal Time");
        assertThat(utc.getSummary().toString()).isEqualTo("GMT+00:00");
        AdapterItem gmtMinus12 = adapter.getDataItem(1);
        assertThat(gmtMinus12.getTitle().toString()).isEqualTo("GMT-12:00");
        assertThat(gmtMinus12.getSummary().toString()).isEmpty();
    }

    public static class TestFixedOffsetPicker extends FixedOffsetPicker {
        // Make the method public
        @Override
        public BaseTimeZoneAdapter createAdapter(TimeZoneData timeZoneData) {
            return super.createAdapter(timeZoneData);
        }

        @Override
        protected Locale getLocale() {
            return Locale.US;
        }

        @Override
        public Context getContext() {
            return RuntimeEnvironment.application;
        }
    }
}
