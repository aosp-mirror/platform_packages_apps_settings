/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.CardPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class BatteryDefenderTipTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryDefenderTip mBatteryDefenderTip;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Mock private BatteryTip mBatteryTip;
    @Mock private Preference mPreference;
    @Mock private CardPreference mCardPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mContext = RuntimeEnvironment.application;
        mBatteryDefenderTip = new BatteryDefenderTip(BatteryTip.StateType.NEW);

        when(mPreference.getContext()).thenReturn(mContext);
        when(mCardPreference.getContext()).thenReturn(mContext);
    }

    @Test
    public void getTitle_showTitle() {
        assertThat(mBatteryDefenderTip.getTitle(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_limited_temporarily_title));
    }

    @Test
    public void getSummary_showSummary() {
        assertThat(mBatteryDefenderTip.getSummary(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_limited_temporarily_summary));
    }

    @Test
    public void getIcon_showIcon() {
        assertThat(mBatteryDefenderTip.getIconId())
                .isEqualTo(R.drawable.ic_battery_status_good_24dp);
    }

    @Test
    public void testLog_logMetric() {
        mBatteryDefenderTip.updateState(mBatteryTip);
        mBatteryDefenderTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider).action(mContext,
                SettingsEnums.ACTION_BATTERY_DEFENDER_TIP, mBatteryTip.mState);
    }

    @Test
    public void updatePreference_castFail_logErrorMessage() {
        mBatteryDefenderTip.updatePreference(mPreference);

        assertThat(getLastErrorLog()).isEqualTo("cast Preference to CardPreference failed");
    }

    @Test
    public void updatePreference_shouldSetPrimaryButtonText() {
        String expectedText = mContext.getString(R.string.battery_tip_charge_to_full_button);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setPrimaryButtonText(expectedText);
    }

    @Test
    public void updatePreference_shouldSetSecondaryButtonText() {
        String expected = mContext.getString(R.string.learn_more);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setSecondaryButtonText(expected);
    }

    @Test
    public void updatePreference_shouldSetSecondaryButtonVisible() {
        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setSecondaryButtonVisible(true);
    }

    @Test
    public void updatePreference_whenCharging_setPrimaryButtonVisibleToBeTrue() {
        fakeDeviceIsCharging(true);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setPrimaryButtonVisible(true);
    }

    @Test
    public void updatePreference_whenNotCharging_setPrimaryButtonVisibleToBeFalse() {
        fakeDeviceIsCharging(false);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setPrimaryButtonVisible(false);
    }

    @Test
    public void updatePreference_whenGetChargingStatusFailed_setPrimaryButtonVisibleToBeFalse() {
        fakeGetChargingStatusFailed();

        mBatteryDefenderTip.updatePreference(mCardPreference);

        verify(mCardPreference).setPrimaryButtonVisible(false);
    }

    private void fakeDeviceIsCharging(boolean charging) {
        int charged = charging ? 1 : 0; // 1 means charging, 0:not charging
        Intent batteryChangedIntent = new Intent(Intent.ACTION_BATTERY_CHANGED);
        batteryChangedIntent.putExtra(BatteryManager.EXTRA_PLUGGED, charged);

        Context mockContext = mock(Context.class);
        when(mockContext.getString(anyInt())).thenReturn("fake_string");
        when(mCardPreference.getContext()).thenReturn(mockContext);
        when(mockContext.registerReceiver(eq(null), any(IntentFilter.class)))
                .thenReturn(batteryChangedIntent);
    }

    private void fakeGetChargingStatusFailed() {
        Context mockContext = mock(Context.class);
        when(mockContext.getString(anyInt())).thenReturn("fake_string");
        when(mCardPreference.getContext()).thenReturn(mockContext);
        when(mockContext.registerReceiver(eq(null), any(IntentFilter.class))).thenReturn(null);
    }

    private String getLastErrorLog() {
        return ShadowLog.getLogsForTag(BatteryDefenderTip.class.getSimpleName()).stream()
                .filter(log -> log.type == Log.ERROR)
                .reduce((first, second) -> second)
                .orElse(createErrorLog("No Error Log"))
                .msg;
    }

    private ShadowLog.LogItem createErrorLog(String msg) {
        return new ShadowLog.LogItem(Log.ERROR, "tag", msg, null);
    }
}
