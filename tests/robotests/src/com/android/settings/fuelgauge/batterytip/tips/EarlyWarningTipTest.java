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
public class EarlyWarningTipTest {

    @Mock
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private Context mContext;
    private EarlyWarningTip mEarlyWarningTip;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mEarlyWarningTip =
                new EarlyWarningTip(BatteryTip.StateType.NEW, false /* powerSaveModeOn */);
    }

    @Test
    public void testParcelable() {
        Parcel parcel = Parcel.obtain();
        mEarlyWarningTip.writeToParcel(parcel, mEarlyWarningTip.describeContents());
        parcel.setDataPosition(0);

        final EarlyWarningTip parcelTip = new EarlyWarningTip(parcel);

        assertThat(parcelTip.isPowerSaveModeOn()).isFalse();
    }

    @Test
    public void testInfo_stateNew_displayPowerModeInfo() {
        final EarlyWarningTip tip =
                new EarlyWarningTip(BatteryTip.StateType.NEW, false /* powerModeOn */);

        assertThat(tip.getTitle(mContext)).isEqualTo("Turn on Battery Saver");
        assertThat(tip.getSummary(mContext)).isEqualTo("Battery may run out earlier than usual");
        assertThat(tip.getIconId()).isEqualTo(R.drawable.ic_battery_status_bad_24dp);
        assertThat(tip.getIconTintColorId()).isEqualTo(R.color.battery_bad_color_light);
    }

    @Test
    public void testInfo_stateHandled_displayPowerModeHandledInfo() {
        final EarlyWarningTip tip =
                new EarlyWarningTip(BatteryTip.StateType.HANDLED, false /* powerModeOn */);

        assertThat(tip.getTitle(mContext)).isEqualTo("Battery Saver is on");
        assertThat(tip.getSummary(mContext)).isEqualTo("Some features may be limited");
        assertThat(tip.getIconId()).isEqualTo(R.drawable.ic_battery_status_maybe_24dp);
        assertThat(tip.getIconTintColorId()).isEqualTo(R.color.battery_maybe_color_light);
    }

    @Test
    public void testUpdate_powerModeTurnedOn_typeBecomeHandled() {
        final EarlyWarningTip nextTip =
                new EarlyWarningTip(BatteryTip.StateType.INVISIBLE, true /* powerModeOn */);

        mEarlyWarningTip.updateState(nextTip);

        assertThat(mEarlyWarningTip.getState()).isEqualTo(BatteryTip.StateType.HANDLED);
    }

    @Test
    public void testUpdate_devicePluggedIn_typeBecomeInvisible() {
        final EarlyWarningTip nextTip = new EarlyWarningTip(BatteryTip.StateType.INVISIBLE,
                false /* powerModeOn */);

        mEarlyWarningTip.updateState(nextTip);

        assertThat(mEarlyWarningTip.getState()).isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void testUpdate_turnOnLowPowerModeExplicitly_typeStillInvisible() {
        final EarlyWarningTip earlyWarningTip = new EarlyWarningTip(BatteryTip.StateType.INVISIBLE,
                false /* powerModeOn */);
        final EarlyWarningTip nextTip = new EarlyWarningTip(BatteryTip.StateType.INVISIBLE,
                true /* powerModeOn */);

        earlyWarningTip.updateState(nextTip);

        assertThat(earlyWarningTip.getState()).isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void testUpdate_turnOffLowPowerModeExplicitly_typeBecomeInvisible() {
        final EarlyWarningTip earlyWarningTip = new EarlyWarningTip(BatteryTip.StateType.HANDLED,
                true /* powerModeOn */);
        final EarlyWarningTip nextTip = new EarlyWarningTip(BatteryTip.StateType.INVISIBLE,
                false /* powerModeOn */);

        earlyWarningTip.updateState(nextTip);

        assertThat(earlyWarningTip.getState()).isEqualTo(BatteryTip.StateType.INVISIBLE);
    }

    @Test
    public void testLog() {
        mEarlyWarningTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider).action(mContext,
                MetricsProto.MetricsEvent.ACTION_EARLY_WARNING_TIP, BatteryTip.StateType.NEW);
    }
}
