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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.CardPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class BatteryDefenderTipTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private BatteryDefenderTip mBatteryDefenderTip;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private CardPreference mCardPreference;

    @Rule public MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private BatteryTip mBatteryTip;
    @Mock private Preference mPreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;
        mBatteryDefenderTip =
                new BatteryDefenderTip(BatteryTip.StateType.NEW, /* isPluggedIn= */ false);
        mCardPreference = new CardPreference(mContext);

        when(mPreference.getContext()).thenReturn(mContext);
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
                .isEqualTo(R.drawable.ic_battery_defender_tip_shield);
    }

    @Test
    public void log_logMetric() {
        mBatteryDefenderTip.updateState(mBatteryTip);
        mBatteryDefenderTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider)
                .action(mContext, SettingsEnums.ACTION_BATTERY_DEFENDER_TIP, mBatteryTip.mState);
    }

    @Test
    public void updatePreference_castFail_logErrorMessage() {
        mBatteryDefenderTip.updatePreference(mPreference);

        assertThat(getLastErrorLog()).isEqualTo("cast Preference to CardPreference failed");
    }

    @Test
    public void updatePreference_shouldSetPrimaryButtonText() {
        String expectedText = mContext.getString(R.string.learn_more);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        assertThat(mCardPreference.getPrimaryButtonText()).isEqualTo(expectedText);
    }

    @Test
    public void updatePreference_shouldSetSecondaryButtonText() {
        String expected = mContext.getString(R.string.battery_tip_charge_to_full_button);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        assertThat(mCardPreference.getSecondaryButtonText()).isEqualTo(expected);
    }

    @Test
    public void updatePreference_shouldSetPrimaryButtonVisible() {
        mBatteryDefenderTip.updatePreference(mCardPreference);

        assertThat(mCardPreference.getPrimaryButtonVisibility()).isTrue();
    }

    @Test
    public void updatePreference_whenCharging_setPrimaryButtonVisibleToBeTrue() {
        mBatteryDefenderTip =
                new BatteryDefenderTip(BatteryTip.StateType.NEW, /* isPluggedIn= */ true);

        mBatteryDefenderTip.updatePreference(mCardPreference);

        assertThat(mCardPreference.getPrimaryButtonVisibility()).isTrue();
    }

    @Test
    public void updatePreference_whenNotCharging_setSecondaryButtonVisibleToBeFalse() {
        mBatteryDefenderTip.updatePreference(mCardPreference);

        assertThat(mCardPreference.getSecondaryButtonVisibility()).isFalse();
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
