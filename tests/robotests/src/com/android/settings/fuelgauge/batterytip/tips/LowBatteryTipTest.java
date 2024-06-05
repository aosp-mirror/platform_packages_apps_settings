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

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class LowBatteryTipTest {

    @Mock private MetricsFeatureProvider mMetricsFeatureProvider;
    private Context mContext;
    private LowBatteryTip mLowBatteryTip;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mLowBatteryTip = new LowBatteryTip(BatteryTip.StateType.NEW, false /* powerSaveModeOn */);
    }

    @Test
    public void testParcelable() {
        Parcel parcel = Parcel.obtain();
        mLowBatteryTip.writeToParcel(parcel, mLowBatteryTip.describeContents());
        parcel.setDataPosition(0);

        final LowBatteryTip parcelTip = new LowBatteryTip(parcel);

        assertThat(parcelTip.isPowerSaveModeOn()).isFalse();
        assertThat(parcelTip.getSummary(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_low_battery_summary));
    }

    @Test
    public void updateState_stateNew_showExpectedInformation() {
        mLowBatteryTip.mState = BatteryTip.StateType.NEW;

        assertThat(mLowBatteryTip.getTitle(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_low_battery_title));
        assertThat(mLowBatteryTip.getSummary(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_low_battery_summary));
        assertThat(mLowBatteryTip.getIconId()).isEqualTo(R.drawable.ic_battery_saver_accent_24dp);
    }

    @Test
    public void updateState_powerSaveModeOn_notShowTipItem() {
        final LowBatteryTip tip =
                new LowBatteryTip(BatteryTip.StateType.NEW, true /* powerSaveModeOn */);

        tip.updateState(tip);

        assertThat(tip.mState).isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void log_lowBatteryActionWithCorrectState() {
        mLowBatteryTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider)
                .action(
                        mContext,
                        MetricsProto.MetricsEvent.ACTION_LOW_BATTERY_TIP,
                        BatteryTip.StateType.NEW);
    }
}
