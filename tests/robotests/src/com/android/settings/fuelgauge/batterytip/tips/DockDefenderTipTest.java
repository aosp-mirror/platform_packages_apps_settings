/*
 * Copyright (C) 2022 The Android Open Source Project
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.util.Log;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.widget.CardPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
public class DockDefenderTipTest {
    private Context mContext;
    private DockDefenderTip mDockDefenderTipFutureBypass;
    private DockDefenderTip mDockDefenderTipActive;
    private DockDefenderTip mDockDefenderTipTemporarilyBypassed;
    private DockDefenderTip mDockDefenderTipDisabled;
    private FakeFeatureFactory mFeatureFactory;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Mock
    private Preference mPreference;
    @Mock
    private CardPreference mCardPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = ApplicationProvider.getApplicationContext();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mMetricsFeatureProvider = mFeatureFactory.metricsFeatureProvider;

        mDockDefenderTipFutureBypass = new DockDefenderTip(BatteryTip.StateType.NEW,
                BatteryUtils.DockDefenderMode.FUTURE_BYPASS);
        mDockDefenderTipActive = new DockDefenderTip(BatteryTip.StateType.NEW,
                BatteryUtils.DockDefenderMode.ACTIVE);
        mDockDefenderTipTemporarilyBypassed = new DockDefenderTip(BatteryTip.StateType.NEW,
                BatteryUtils.DockDefenderMode.TEMPORARILY_BYPASSED);
        mDockDefenderTipDisabled = new DockDefenderTip(BatteryTip.StateType.INVISIBLE,
                BatteryUtils.DockDefenderMode.DISABLED);

        doReturn(mContext).when(mPreference).getContext();
        doReturn(mContext).when(mCardPreference).getContext();
    }

    @Test
    public void testGetTitle() {
        assertThat(mDockDefenderTipFutureBypass.getTitle(mContext).toString()).isEqualTo(
                mContext.getString(R.string.battery_tip_dock_defender_future_bypass_title));
        assertThat(mDockDefenderTipActive.getTitle(mContext).toString()).isEqualTo(
                mContext.getString(R.string.battery_tip_dock_defender_active_title));
        assertThat(mDockDefenderTipTemporarilyBypassed.getTitle(mContext).toString()).isEqualTo(
                mContext.getString(R.string.battery_tip_dock_defender_temporarily_bypassed_title));
        assertThat(mDockDefenderTipDisabled.getTitle(mContext)).isNull();
    }

    @Test
    public void testGetSummary() {
        assertThat(mDockDefenderTipFutureBypass.getSummary(mContext).toString()).isEqualTo(
                mContext.getString(R.string.battery_tip_dock_defender_future_bypass_summary));
        assertThat(mDockDefenderTipActive.getSummary(mContext).toString()).isEqualTo(
                mContext.getString(R.string.battery_tip_dock_defender_active_summary));
        assertThat(mDockDefenderTipTemporarilyBypassed.getSummary(mContext).toString()).isEqualTo(
                mContext.getString(
                        R.string.battery_tip_dock_defender_temporarily_bypassed_summary));
        assertThat(mDockDefenderTipDisabled.getSummary(mContext)).isNull();
    }

    @Test
    public void testGetIconId_dockDefenderActive_getProtectedIcon() {
        assertThat(mDockDefenderTipActive.getIconId()).isEqualTo(
                R.drawable.ic_battery_status_protected_24dp);
    }

    @Test
    public void testGetIconId_dockDefenderNotActive_getUntriggeredIcon() {
        assertThat(mDockDefenderTipFutureBypass.getIconId()).isEqualTo(
                R.drawable.ic_battery_dock_defender_untriggered_24dp);
        assertThat(mDockDefenderTipTemporarilyBypassed.getIconId()).isEqualTo(
                R.drawable.ic_battery_dock_defender_untriggered_24dp);
        assertThat(mDockDefenderTipDisabled.getIconId()).isEqualTo(
                R.drawable.ic_battery_dock_defender_untriggered_24dp);
    }

    @Test
    public void testUpdateState() {
        mDockDefenderTipTemporarilyBypassed.updateState(mDockDefenderTipDisabled);

        assertThat(mDockDefenderTipTemporarilyBypassed.getState()).isEqualTo(
                BatteryTip.StateType.INVISIBLE);
        assertThat(mDockDefenderTipTemporarilyBypassed.getMode()).isEqualTo(
                BatteryUtils.DockDefenderMode.DISABLED);
    }

    @Test
    public void testLog() {
        mDockDefenderTipActive.log(mContext, mMetricsFeatureProvider);

        verify(mMetricsFeatureProvider).action(mContext, SettingsEnums.ACTION_DOCK_DEFENDER_TIP,
                mDockDefenderTipActive.getState());
    }


    @Test
    public void testUpdatePreference_dockDefenderTipFutureBypass() {
        mDockDefenderTipFutureBypass.updatePreference(mCardPreference);

        verify(mCardPreference).setPrimaryButtonVisible(true);
        verify(mCardPreference).setPrimaryButtonText(
                mContext.getString(R.string.battery_tip_charge_to_full_button));
        verifySecondaryButton();
    }

    @Test
    public void testUpdatePreference_dockDefenderTipActive() {
        mDockDefenderTipActive.updatePreference(mCardPreference);

        verify(mCardPreference).setPrimaryButtonVisible(true);
        verify(mCardPreference).setPrimaryButtonText(
                mContext.getString(R.string.battery_tip_charge_to_full_button));
        verifySecondaryButton();
    }

    @Test
    public void testUpdatePreference_dockDefenderTipTemporarilyBypassed() {
        mDockDefenderTipTemporarilyBypassed.updatePreference(mCardPreference);

        verify(mCardPreference).setPrimaryButtonVisible(false);
        verify(mCardPreference, never()).setPrimaryButtonText(any());
        verifySecondaryButton();
    }

    private void verifySecondaryButton() {
        verify(mCardPreference).setSecondaryButtonText(mContext.getString(R.string.learn_more));
        verify(mCardPreference).setSecondaryButtonVisible(true);
        verify(mCardPreference).setSecondaryButtonContentDescription(mContext.getString(
                R.string.battery_tip_limited_temporarily_sec_button_content_description));
    }

    @Test
    public void updatePreference_castFail_logErrorMessage() {
        mDockDefenderTipActive.updatePreference(mPreference);

        assertThat(getLastErrorLog()).isEqualTo("cast Preference to CardPreference failed");
    }

    private String getLastErrorLog() {
        return ShadowLog.getLogsForTag(DockDefenderTip.class.getSimpleName()).stream().filter(
                log -> log.type == Log.ERROR).reduce((first, second) -> second).orElse(
                createErrorLog("No Error Log")).msg;
    }

    private ShadowLog.LogItem createErrorLog(String msg) {
        return new ShadowLog.LogItem(Log.ERROR, "tag", msg, null);
    }
}
