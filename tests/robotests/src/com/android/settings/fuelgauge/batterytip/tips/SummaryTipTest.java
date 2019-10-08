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
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SummaryTipTest {

    private static final long AVERAGE_TIME_MS = DateUtils.HOUR_IN_MILLIS;

    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private Context mContext;
    private SummaryTip mSummaryTip;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mSummaryTip = new SummaryTip(BatteryTip.StateType.NEW, AVERAGE_TIME_MS);
    }

    @Test
    public void testParcelable() {
        Parcel parcel = Parcel.obtain();
        mSummaryTip.writeToParcel(parcel, mSummaryTip.describeContents());
        parcel.setDataPosition(0);

        final SummaryTip parcelTip = new SummaryTip(parcel);

        assertThat(parcelTip.getAverageTimeMs()).isEqualTo(AVERAGE_TIME_MS);
    }

    @Test
    public void testLog() {
        mSummaryTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_SUMMARY_TIP, BatteryTip.StateType.NEW);
    }
}
