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

import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppInfoTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int ANOMALY_TYPE = Anomaly.AnomalyType.WAKE_LOCK;
    private static final long SCREEN_TIME_MS = DateUtils.HOUR_IN_MILLIS;

    private AppInfo mAppInfo;

    @Before
    public void setUp() {
        mAppInfo = new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME)
                .setAnomalyType(ANOMALY_TYPE)
                .setScreenOnTimeMs(SCREEN_TIME_MS)
                .build();
    }

    @Test
    public void testParcel() {
        Parcel parcel = Parcel.obtain();
        mAppInfo.writeToParcel(parcel, mAppInfo.describeContents());
        parcel.setDataPosition(0);

        final AppInfo appInfo = new AppInfo(parcel);

        assertThat(appInfo.packageName).isEqualTo(PACKAGE_NAME);
        assertThat(appInfo.anomalyType).isEqualTo(ANOMALY_TYPE);
        assertThat(appInfo.screenOnTimeMs).isEqualTo(SCREEN_TIME_MS);
    }

    @Test
    public void testCompareTo_hasCorrectOrder() {
        final AppInfo appInfo = new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME)
                .setAnomalyType(ANOMALY_TYPE)
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
        assertThat(mAppInfo.anomalyType).isEqualTo(ANOMALY_TYPE);
        assertThat(mAppInfo.screenOnTimeMs).isEqualTo(SCREEN_TIME_MS);
    }
}
