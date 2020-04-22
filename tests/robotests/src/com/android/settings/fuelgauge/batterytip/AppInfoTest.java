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

package com.android.settings.fuelgauge.batterytip;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;
import android.text.format.DateUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AppInfoTest {

    private static final String PACKAGE_NAME = "com.android.app";
    private static final int TYPE_WAKELOCK =
            StatsManagerConfig.AnomalyType.EXCESSIVE_WAKELOCK_ALL_SCREEN_OFF;
    private static final int TYPE_WAKEUP =
            StatsManagerConfig.AnomalyType.EXCESSIVE_WAKEUPS_IN_BACKGROUND;
    private static final long SCREEN_TIME_MS = DateUtils.HOUR_IN_MILLIS;
    private static final int UID = 3452;

    private AppInfo mAppInfo;

    @Before
    public void setUp() {
        mAppInfo = new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME)
                .addAnomalyType(TYPE_WAKELOCK)
                .addAnomalyType(TYPE_WAKEUP)
                .setScreenOnTimeMs(SCREEN_TIME_MS)
                .setUid(UID)
                .build();
    }

    @Test
    public void testParcel() {
        Parcel parcel = Parcel.obtain();
        mAppInfo.writeToParcel(parcel, mAppInfo.describeContents());
        parcel.setDataPosition(0);

        final AppInfo appInfo = new AppInfo(parcel);

        assertThat(appInfo.packageName).isEqualTo(PACKAGE_NAME);
        assertThat(appInfo.anomalyTypes).containsExactly(TYPE_WAKELOCK, TYPE_WAKEUP);
        assertThat(appInfo.screenOnTimeMs).isEqualTo(SCREEN_TIME_MS);
        assertThat(appInfo.uid).isEqualTo(UID);
    }

    @Test
    public void testCompareTo_hasCorrectOrder() {
        final AppInfo appInfo = new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME)
                .addAnomalyType(TYPE_WAKELOCK)
                .setScreenOnTimeMs(SCREEN_TIME_MS + 100)
                .build();

        List<AppInfo> appInfos = new ArrayList<>();
        appInfos.add(appInfo);
        appInfos.add(mAppInfo);

        Collections.sort(appInfos);
        assertThat(appInfos.get(0).screenOnTimeMs).isLessThan(appInfos.get(1).screenOnTimeMs);
    }

    @Test
    public void testBuilder() {
        assertThat(mAppInfo.packageName).isEqualTo(PACKAGE_NAME);
        assertThat(mAppInfo.anomalyTypes).containsExactly(TYPE_WAKELOCK, TYPE_WAKEUP);
        assertThat(mAppInfo.screenOnTimeMs).isEqualTo(SCREEN_TIME_MS);
        assertThat(mAppInfo.uid).isEqualTo(UID);
    }
}
