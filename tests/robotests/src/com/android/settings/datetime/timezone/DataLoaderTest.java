/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.android.settings.TestConfig;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DataLoaderTest {

    @Test
    public void testHasData() {
        List<RegionInfo> regions = new DataLoader(Locale.US).loadRegionInfos();
        // Sanity check. Real size is closer to 200.
        assertNotNull(regions);
        assertTrue(regions.size() > 100);
        assertEquals("Afghanistan", regions.get(0).getName());
        assertEquals("Zimbabwe", regions.get(regions.size() - 1).getName());
    }

    @Test
    public void testRegionsWithTimeZone() {
        List<RegionInfo> regions = new DataLoader(Locale.US).loadRegionInfos();
        checkRegionHasTimeZone(regions, "AT", "Europe/Vienna");
        checkRegionHasTimeZone(regions, "US", "America/Los_Angeles");
        checkRegionHasTimeZone(regions, "CN", "Asia/Shanghai");
        checkRegionHasTimeZone(regions, "AU", "Australia/Sydney");
    }

    @Test
    public void testFixedOffsetTimeZones() {
        List<TimeZoneInfo> timeZones = new DataLoader(Locale.US).loadFixedOffsets();
        // Etc/GMT would be equivalent to Etc/UTC, except for how it is labelled. Users have
        // explicitly asked for UTC to be supported, so make sure we label it as such.
        checkHasTimeZone(timeZones, "Etc/UTC");
        checkHasTimeZone(timeZones, "Etc/GMT-1");
        checkHasTimeZone(timeZones, "Etc/GMT-14");
        checkHasTimeZone(timeZones, "Etc/GMT+1");
        checkHasTimeZone(timeZones, "Etc/GMT+12");
    }

    private void checkRegionHasTimeZone(List<RegionInfo> regions, String regionId, String tzId) {
        RegionInfo ri = findRegion(regions, regionId);
        assertTrue("Region " + regionId + " does not have time zone " + tzId,
                ri.getTimeZoneIds().contains(tzId));
    }

    private void checkHasTimeZone(List<TimeZoneInfo> timeZoneInfos, String tzId) {
        for (TimeZoneInfo tz : timeZoneInfos) {
            if (tz.getId().equals(tzId)) {
                return;
            }
        }
        fail("Fixed offset time zones do not contain " + tzId);
    }

    private RegionInfo findRegion(List<RegionInfo> regions, String regionId) {
        for (RegionInfo region : regions) {
            if (region.getId().equals(regionId)) {
                assertNotNull(region.getName());
                return region;
            }

        }
        fail("No region with id " + regionId + " found.");
        return null; // can't reach.
    }
}
