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
package com.android.settings.fuelgauge.batterytip.tips;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Parcel;
import android.text.format.DateUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.fuelgauge.batterytip.AppInfo;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class HighUsageTipTest {

    private static final String PACKAGE_NAME = "com.android.app";
    private static final long SCREEN_TIME = 30 * DateUtils.MINUTE_IN_MILLIS;
    private static final long LAST_FULL_CHARGE_TIME = 20 * DateUtils.MINUTE_IN_MILLIS;

    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private Context mContext;
    private HighUsageTip mBatteryTip;
    private List<AppInfo> mUsageAppList;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mUsageAppList = new ArrayList<>();
        mUsageAppList.add(new AppInfo.Builder()
                .setPackageName(PACKAGE_NAME)
                .setScreenOnTimeMs(SCREEN_TIME)
                .build());
        mBatteryTip = new HighUsageTip(LAST_FULL_CHARGE_TIME, mUsageAppList);
    }

    @Test
    public void testParcelable() {

        Parcel parcel = Parcel.obtain();
        mBatteryTip.writeToParcel(parcel, mBatteryTip.describeContents());
        parcel.setDataPosition(0);

        final HighUsageTip parcelTip = new HighUsageTip(parcel);

        assertThat(parcelTip.getTitle(mContext)).isEqualTo("Phone used more than usual");
        assertThat(parcelTip.getType()).isEqualTo(BatteryTip.TipType.HIGH_DEVICE_USAGE);
        assertThat(parcelTip.getState()).isEqualTo(BatteryTip.StateType.NEW);
        assertThat(parcelTip.getLastFullChargeTimeMs()).isEqualTo(LAST_FULL_CHARGE_TIME);
        assertThat(parcelTip.mHighUsageAppList).isNotNull();
        assertThat(parcelTip.mHighUsageAppList.size()).isEqualTo(1);
        final AppInfo app = parcelTip.mHighUsageAppList.get(0);
        assertThat(app.packageName).isEqualTo(PACKAGE_NAME);
        assertThat(app.screenOnTimeMs).isEqualTo(SCREEN_TIME);
    }

    @Test
    public void toString_containsAppData() {
        assertThat(mBatteryTip.toString()).isEqualTo(
                "type=2 state=0 { packageName=com.android.app,anomalyTypes={},screenTime=1800000 "
                        + "}");
    }

    @Test
    public void testLog_logAppInfo() {
        mBatteryTip.log(mContext, mMetricsFeatureProvider);
        verify(mMetricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_HIGH_USAGE_TIP, BatteryTip.StateType.NEW);

        verify(mMetricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_HIGH_USAGE_TIP_LIST,
                PACKAGE_NAME);
    }
}
