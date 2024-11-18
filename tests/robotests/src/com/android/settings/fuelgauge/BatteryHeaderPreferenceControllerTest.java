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
 */
package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.fuelgauge.BatteryUtils;
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

// LINT.IfChange
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowEntityHeaderController.class, ShadowUtils.class})
public class BatteryHeaderPreferenceControllerTest {

    private static final String PREF_KEY = "battery_header";
    private static final int BATTERY_LEVEL = 60;
    private static final int BATTERY_MAX_LEVEL = 100;

    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private BatteryBroadcastReceiver mBatteryBroadcastReceiver;
    @Mock private EntityHeaderController mEntityHeaderController;
    @Mock private UsageProgressBarPreference mBatteryUsageProgressBarPreference;
    @Mock private UsbManager mUsbManager;
    @Mock private LifecycleOwner mLifecycleOwner;

    private BatteryHeaderPreferenceController mController;
    private Context mContext;
    private Intent mBatteryIntent;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(UsbManager.class)).thenReturn(mUsbManager);
        ShadowEntityHeaderController.setUseMock(mEntityHeaderController);

        mBatteryIntent = new Intent();
        mBatteryIntent.putExtra(BatteryManager.EXTRA_LEVEL, BATTERY_LEVEL);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_SCALE, 100);
        mBatteryIntent.putExtra(BatteryManager.EXTRA_PLUGGED, 1);
        doReturn(mBatteryIntent).when(mContext).registerReceiver(any(), any());

        doReturn(mBatteryUsageProgressBarPreference)
                .when(mPreferenceScreen)
                .findPreference(PREF_KEY);

        mController = spy(new BatteryHeaderPreferenceController(mContext, PREF_KEY));
        mController.mBatteryUsageProgressBarPreference = mBatteryUsageProgressBarPreference;

        BatteryUtils.setChargingStringV2Enabled(null);
    }

    @After
    public void tearDown() {
        ShadowEntityHeaderController.reset();
        ShadowUtils.reset();
    }

    @Test
    public void onStateChanged_onCreate_receiverCreated() {
        mController.onStateChanged(mLifecycleOwner,  Lifecycle.Event.ON_CREATE);

        assertThat(mController.mBatteryBroadcastReceiver).isNotNull();
    }

    @Test
    public void onStateChanged_onStart_receiverRegistered() {
        mController.mBatteryBroadcastReceiver = mBatteryBroadcastReceiver;

        mController.onStateChanged(mLifecycleOwner,  Lifecycle.Event.ON_START);

        verify(mBatteryBroadcastReceiver).register();
    }

    @Test
    public void onStateChanged_onStop_receiverUnregistered() {
        mController.mBatteryBroadcastReceiver = mBatteryBroadcastReceiver;

        mController.onStateChanged(mLifecycleOwner,  Lifecycle.Event.ON_STOP);

        verify(mBatteryBroadcastReceiver).unRegister();
    }

    @Test
    public void displayPreference_displayBatteryLevel() {
        mController.displayPreference(mPreferenceScreen);

        verify(mBatteryUsageProgressBarPreference).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPreference).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
    }

    @Test
    public void quickUpdateHeaderPreference_onlyUpdateBatteryLevelAndChargingState() {
        mController.quickUpdateHeaderPreference();

        verify(mBatteryUsageProgressBarPreference).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPreference).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
    }

    @Test
    public void getAvailabilityStatus_returnAvailableUnsearchable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void displayPreference_batteryNotPresent_isInvisible() {
        ShadowUtils.setIsBatteryPresent(false);

        mController.displayPreference(mPreferenceScreen);

        assertThat(mBatteryUsageProgressBarPreference.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_init_setEmptyBottomSummary() {
        mController.displayPreference(mPreferenceScreen);

        verify(mBatteryUsageProgressBarPreference).setBottomSummary("");
    }

    private CharSequence formatBatteryPercentageText() {
        return com.android.settings.Utils.formatPercentage(BATTERY_LEVEL);
    }
}
// LINT.ThenChange(BatteryHeaderPreferenceTest.java)
