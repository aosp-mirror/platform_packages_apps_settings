/*
 * Copyright (C) 2023 The Android Open Source Project
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
public final class IncompatibleChargerTipTest {

    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private IncompatibleChargerTip mIncompatibleChargerTip;
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
        mIncompatibleChargerTip = new IncompatibleChargerTip(BatteryTip.StateType.NEW);

        when(mPreference.getContext()).thenReturn(mContext);
        when(mCardPreference.getContext()).thenReturn(mContext);
    }

    @Test
    public void getTitle_showTitle() {
        assertThat(mIncompatibleChargerTip.getTitle(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_incompatible_charging_title));
    }

    @Test
    public void getSummary_showSummary() {
        assertThat(mIncompatibleChargerTip.getSummary(mContext))
                .isEqualTo(mContext.getString(R.string.battery_tip_incompatible_charging_message));
    }

    @Test
    public void getIcon_showIcon() {
        assertThat(mIncompatibleChargerTip.getIconId()).isEqualTo(R.drawable.ic_battery_charger);
    }

    @Test
    public void testLog_logMetric() {
        mIncompatibleChargerTip.updateState(mBatteryTip);
        mIncompatibleChargerTip.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider)
                .action(
                        mContext,
                        SettingsEnums.ACTION_INCOMPATIBLE_CHARGING_TIP,
                        mBatteryTip.mState);
    }

    @Test
    public void updatePreference_castFail_logErrorMessage() {
        mIncompatibleChargerTip.updatePreference(mPreference);
        assertThat(getLastErrorLog()).isEqualTo("cast Preference to CardPreference failed");
    }

    @Test
    public void updatePreference_shouldSetSecondaryButtonText() {
        String expected = mContext.getString(R.string.learn_more);

        mIncompatibleChargerTip.updatePreference(mCardPreference);

        verify(mCardPreference).setPrimaryButtonText(expected);
    }

    @Test
    public void updatePreference_shouldSetSecondaryButtonVisible() {
        mIncompatibleChargerTip.updatePreference(mCardPreference);
        verify(mCardPreference).setPrimaryButtonVisible(true);
    }

    private String getLastErrorLog() {
        return ShadowLog.getLogsForTag(IncompatibleChargerTip.class.getSimpleName()).stream()
                .filter(log -> log.type == Log.ERROR)
                .reduce((first, second) -> second)
                .orElse(createErrorLog("No Error Log"))
                .msg;
    }

    private ShadowLog.LogItem createErrorLog(String msg) {
        return new ShadowLog.LogItem(Log.ERROR, "tag", msg, null);
    }
}
