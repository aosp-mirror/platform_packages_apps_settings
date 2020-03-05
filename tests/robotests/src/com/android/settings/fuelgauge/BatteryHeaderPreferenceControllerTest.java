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
import android.os.BatteryManager;
import android.os.PowerManager;
import android.widget.TextView;

import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.LayoutPreference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPowerManager;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowEntityHeaderController.class)
public class BatteryHeaderPreferenceControllerTest {

    private static final String PREF_KEY = "battery_header";
    private static final int BATTERY_LEVEL = 60;
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
    private BatteryHeaderPreferenceController mController;
    private Context mContext;
    private PowerManager mPowerManager;
    private BatteryMeterView mBatteryMeterView;
    private TextView mBatteryPercentText;
    private TextView mSummary;
    private LayoutPreference mBatteryLayoutPref;
    private Intent mBatteryIntent;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mContext = spy(RuntimeEnvironment.application);
        mBatteryMeterView = new BatteryMeterView(mContext);
        mBatteryPercentText = new TextView(mContext);
        mSummary = new TextView(mContext);
        ShadowEntityHeaderController.setUseMock(mEntityHeaderController);

        mBatteryIntent = new Intent();
        mBatteryIntent.putExtra(BatteryManager.EXTRA_LEVEL, BATTERY_LEVEL);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_PLUGGED, 1);
        doReturn(mBatteryIntent).when(mContext).registerReceiver(any(), any());

        mBatteryLayoutPref = new LayoutPreference(mContext, R.layout.battery_header);
        doReturn(mBatteryLayoutPref).when(mPreferenceScreen)
            .findPreference(BatteryHeaderPreferenceController.KEY_BATTERY_HEADER);

        mBatteryInfo.batteryLevel = BATTERY_LEVEL;

        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        mController = new BatteryHeaderPreferenceController(mContext, PREF_KEY);
        mLifecycle.addObserver(mController);
        mController.setActivity(mActivity);
        mController.setFragment(mPreferenceFragment);
        mController.setLifecycle(mLifecycle);
        mController.mBatteryMeterView = mBatteryMeterView;
        mController.mBatteryPercentText = mBatteryPercentText;
        mController.mSummary1 = mSummary;
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
    }

    @Test
    public void displayPreference_displayBatteryLevel() {
        mController.displayPreference(mPreferenceScreen);

        assertThat(((BatteryMeterView) mBatteryLayoutPref.findViewById(
                R.id.battery_header_icon)).getBatteryLevel()).isEqualTo(BATTERY_LEVEL);
        assertThat(((TextView) mBatteryLayoutPref.findViewById(R.id.battery_percent))
                .getText().toString())
                .isEqualTo("60 %");
    }

    @Test
    public void updatePreference_hasRemainingTime_showRemainingLabel() {
        mBatteryInfo.remainingLabel = TIME_LEFT;

        mController.updateHeaderPreference(mBatteryInfo);

        assertThat(mSummary.getText()).isEqualTo(mBatteryInfo.remainingLabel);
    }

    @Test
    public void updatePreference_updateBatteryInfo() {
        mBatteryInfo.remainingLabel = TIME_LEFT;
        mBatteryInfo.batteryLevel = BATTERY_LEVEL;
        mBatteryInfo.discharging = true;

        mController.updateHeaderPreference(mBatteryInfo);

        assertThat(mBatteryMeterView.mDrawable.getBatteryLevel()).isEqualTo(BATTERY_LEVEL);
        assertThat(mBatteryMeterView.mDrawable.getCharging()).isEqualTo(false);
    }

    @Test
    public void updatePreference_noRemainingTime_showStatusLabel() {
        mBatteryInfo.remainingLabel = null;
        mBatteryInfo.statusLabel = BATTERY_STATUS;

        mController.updateHeaderPreference(mBatteryInfo);

        assertThat(mSummary.getText()).isEqualTo(BATTERY_STATUS);
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
        mSummary.setText(BATTERY_STATUS);

        mController.quickUpdateHeaderPreference();

        assertThat(mBatteryMeterView.getBatteryLevel()).isEqualTo(BATTERY_LEVEL);
        assertThat(mBatteryMeterView.getCharging()).isTrue();
        assertThat(mBatteryPercentText.getText().toString()).isEqualTo("60 %");
        assertThat(mSummary.getText()).isEqualTo(BATTERY_STATUS);
    }

    @Test
    public void quickUpdateHeaderPreference_showPowerSave() {
        boolean testValues[] = {false, true};

        ShadowPowerManager shadowPowerManager = Shadows.shadowOf(mPowerManager);
        for (boolean value : testValues) {
            shadowPowerManager.setIsPowerSaveMode(value);
            mController.quickUpdateHeaderPreference();

            assertThat(mBatteryMeterView.getPowerSave()).isEqualTo(value);
        }
    }

    @Test
    public void getAvailabilityStatus_returnAvailableUnsearchable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }
}
