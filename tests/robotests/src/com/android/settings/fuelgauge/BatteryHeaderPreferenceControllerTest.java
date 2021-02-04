/*
 * Copyright (C) 2017 The Android Open Source Project
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
 *
 *
 */

package com.android.settings.fuelgauge;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.icu.text.NumberFormat;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.UsageProgressBarPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowEntityHeaderController.class, ShadowUtils.class})
public class BatteryHeaderPreferenceControllerTest {

    private static final String PREF_KEY = "battery_header";
    private static final int BATTERY_LEVEL = 60;
    private static final int BATTERY_MAX_LEVEL = 100;
    private static final String TIME_LEFT = "2h30min";
    private static final String BATTERY_STATUS = "Charging";

    @Mock
    private Activity mActivity;
    @Mock
    private PreferenceFragmentCompat mPreferenceFragment;
    @Mock
    private PreferenceScreen mPreferenceScreen;
    @Mock
    private BatteryInfo mBatteryInfo;
    @Mock
    private EntityHeaderController mEntityHeaderController;
    @Mock
    private UsageProgressBarPreference mBatteryUsageProgressBarPref;
    private BatteryHeaderPreferenceController mController;
    private Context mContext;
    private PowerManager mPowerManager;
    private Intent mBatteryIntent;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = spy(RuntimeEnvironment.application);
        ShadowEntityHeaderController.setUseMock(mEntityHeaderController);

        mBatteryIntent = new Intent();
        mBatteryIntent.putExtra(BatteryManager.EXTRA_LEVEL, BATTERY_LEVEL);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_PLUGGED, 1);
        doReturn(mBatteryIntent).when(mContext).registerReceiver(any(), any());

        doReturn(mBatteryUsageProgressBarPref).when(mPreferenceScreen)
            .findPreference(BatteryHeaderPreferenceController.KEY_BATTERY_HEADER);

        mBatteryInfo.batteryLevel = BATTERY_LEVEL;

        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        mController = spy(new BatteryHeaderPreferenceController(mContext, PREF_KEY));
        mLifecycle.addObserver(mController);
        mController.setActivity(mActivity);
        mController.setFragment(mPreferenceFragment);
        mController.setLifecycle(mLifecycle);
        mController.mBatteryUsageProgressBarPref = mBatteryUsageProgressBarPref;
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
        ShadowUtils.reset();
    }

    @Test
    public void displayPreference_displayBatteryLevel() {
        mController.displayPreference(mPreferenceScreen);

        verify(mBatteryUsageProgressBarPref).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPref).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
    }

    @Test
    public void updatePreference_hasRemainingTime_showRemainingLabel() {
        mBatteryInfo.remainingLabel = TIME_LEFT;

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setTotalSummary(mBatteryInfo.remainingLabel);
    }

    @Test
    public void updatePreference_updateBatteryInfo() {
        mBatteryInfo.remainingLabel = TIME_LEFT;
        mBatteryInfo.batteryLevel = BATTERY_LEVEL;
        mBatteryInfo.discharging = true;

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPref).setTotalSummary(mBatteryInfo.remainingLabel);
        verify(mBatteryUsageProgressBarPref).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
    }

    @Test
    public void updatePreference_noRemainingTime_showStatusLabel() {
        mBatteryInfo.remainingLabel = null;
        mBatteryInfo.statusLabel = BATTERY_STATUS;

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setTotalSummary(BATTERY_STATUS);
    }

    @Test
    public void updatePreference_isOverheat_showEmptyText() {
        mBatteryInfo.isOverheated = true;

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setTotalSummary(null);
    }

    @Test
    public void onStart_shouldStyleActionBar() {
        when(mEntityHeaderController.setRecyclerView(nullable(RecyclerView.class), eq(mLifecycle)))
                .thenReturn(mEntityHeaderController);

        mController.displayPreference(mPreferenceScreen);
        mLifecycle.handleLifecycleEvent(ON_START);

        verify(mEntityHeaderController).styleActionBar(mActivity);
    }

    @Test
    public void quickUpdateHeaderPreference_onlyUpdateBatteryLevelAndChargingState() {
        mController.quickUpdateHeaderPreference();

        verify(mBatteryUsageProgressBarPref).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPref).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
    }

    @Test
    public void getAvailabilityStatus_returnAvailableUnsearchable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    private CharSequence formatBatteryPercentageText() {
        return TextUtils.expandTemplate(mContext.getText(R.string.battery_header_title_alternate),
                NumberFormat.getIntegerInstance().format(BATTERY_LEVEL));
    }
}
