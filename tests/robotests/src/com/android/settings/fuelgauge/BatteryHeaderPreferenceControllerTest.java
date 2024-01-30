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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.icu.text.NumberFormat;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.fuelgauge.batterytip.tips.BatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.LowBatteryTip;
import com.android.settings.fuelgauge.batterytip.tips.SmartBatteryTip;
import com.android.settings.testutils.BatteryTestUtils;
import com.android.settings.testutils.shadow.ShadowEntityHeaderController;
import com.android.settings.testutils.shadow.ShadowUtils;
import com.android.settings.widget.EntityHeaderController;
import com.android.settingslib.widget.UsageProgressBarPreference;

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
@Config(shadows = {ShadowEntityHeaderController.class, ShadowUtils.class})
public class BatteryHeaderPreferenceControllerTest {

    private static final String PREF_KEY = "battery_header";
    private static final int BATTERY_LEVEL = 60;
    private static final int BATTERY_MAX_LEVEL = 100;
    private static final String TIME_LEFT = "2h30min";
    private static final String BATTERY_STATUS = "Charging";

    @Mock private PreferenceScreen mPreferenceScreen;
    @Mock private BatteryInfo mBatteryInfo;
    @Mock private EntityHeaderController mEntityHeaderController;
    @Mock private UsageProgressBarPreference mBatteryUsageProgressBarPref;
    @Mock private BatteryStatusFeatureProvider mBatteryStatusFeatureProvider;
    @Mock private UsbPort mUsbPort;
    @Mock private UsbManager mUsbManager;
    @Mock private UsbPortStatus mUsbPortStatus;

    private BatteryHeaderPreferenceController mController;
    private Context mContext;
    private ShadowPowerManager mShadowPowerManager;
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

        doReturn(mBatteryUsageProgressBarPref)
                .when(mPreferenceScreen)
                .findPreference(BatteryHeaderPreferenceController.KEY_BATTERY_HEADER);

        mBatteryInfo.batteryLevel = BATTERY_LEVEL;

        mShadowPowerManager = Shadows.shadowOf(mContext.getSystemService(PowerManager.class));

        mController = spy(new BatteryHeaderPreferenceController(mContext, PREF_KEY));
        mController.mBatteryUsageProgressBarPref = mBatteryUsageProgressBarPref;
        mController.mBatteryStatusFeatureProvider = mBatteryStatusFeatureProvider;
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

        verify(mBatteryUsageProgressBarPref).setBottomSummary(mBatteryInfo.remainingLabel);
    }

    @Test
    public void updatePreference_updateBatteryInfo() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ false);

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPref).setBottomSummary(mBatteryInfo.remainingLabel);
        verify(mBatteryUsageProgressBarPref).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
    }

    @Test
    public void updatePreference_noRemainingTime_showStatusLabel() {
        mBatteryInfo.remainingLabel = null;
        mBatteryInfo.statusLabel = BATTERY_STATUS;

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setBottomSummary(BATTERY_STATUS);
    }

    @Test
    public void updatePreference_statusAnomalous_showStatusLabel() {
        mBatteryInfo.remainingLabel = TIME_LEFT;
        mBatteryInfo.statusLabel = BATTERY_STATUS;
        mBatteryInfo.batteryStatus = BatteryManager.BATTERY_STATUS_NOT_CHARGING;

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setBottomSummary(BATTERY_STATUS);
    }

    @Test
    public void updatePreference_charging_showFullText() {
        setChargingState(/* isDischarging */ false, /* updatedByStatusFeature */ false);

        mController.updateHeaderPreference(mBatteryInfo);

        final String expectedResult = BATTERY_STATUS + " • " + TIME_LEFT;
        verify(mBatteryUsageProgressBarPref).setBottomSummary(expectedResult);
    }

    @Test
    public void updatePreference_powerSaverOn_showPowerSaverOn() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ false);
        mShadowPowerManager.setIsPowerSaveMode(true);

        mController.updateHeaderPreference(mBatteryInfo);

        final String expectedResult = "Battery Saver on • " + TIME_LEFT;
        verify(mBatteryUsageProgressBarPref).setBottomSummary(expectedResult);
    }

    @Test
    public void updatePreference_triggerBatteryStatusUpdateTrue_updatePercentageAndUsageOnly() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ true);

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPref).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
    }

    @Test
    public void updatePreference_triggerBatteryStatusUpdateFalse_updateBatteryInfo() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ false);

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPref).setBottomSummary(mBatteryInfo.remainingLabel);
        verify(mBatteryUsageProgressBarPref).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
    }

    @Test
    public void updateBatteryStatus_nullLabel_updateSummaryOnly() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ false);

        mController.updateBatteryStatus(null, mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setBottomSummary(mBatteryInfo.remainingLabel);
    }

    @Test
    public void updateBatteryStatus_withLabel_showLabelText() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ false);

        final String label = "Update by battery status • " + TIME_LEFT;
        mController.updateBatteryStatus(label, mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setBottomSummary(label);
    }

    @Test
    public void updateHeaderByBatteryTips_lowBatteryTip_showLowBattery() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ false);
        BatteryTip lowBatteryTip =
                new LowBatteryTip(BatteryTip.StateType.NEW, /* powerSaveModeOn */ false);

        mController.updateHeaderByBatteryTips(lowBatteryTip, mBatteryInfo);

        final String expectedResult = "Low battery • " + TIME_LEFT;
        verify(mBatteryUsageProgressBarPref).setBottomSummary(expectedResult);
    }

    @Test
    public void updateHeaderByBatteryTips_notLowBatteryTip_showRemainingLabel() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ false);
        BatteryTip lowBatteryTip = new SmartBatteryTip(BatteryTip.StateType.NEW);

        mController.updateHeaderByBatteryTips(lowBatteryTip, mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setBottomSummary(mBatteryInfo.remainingLabel);
    }

    @Test
    public void updateHeaderByBatteryTips_noTip_noAction() {
        setChargingState(/* isDischarging */ true, /* updatedByStatusFeature */ false);

        mController.updateHeaderByBatteryTips(null, mBatteryInfo);

        verifyNoInteractions(mBatteryUsageProgressBarPref);
    }

    @Test
    public void updateHeaderByBatteryTips_noBatteryInfo_noAction() {
        BatteryTip lowBatteryTip =
                new LowBatteryTip(BatteryTip.StateType.NEW, /* powerSaveModeOn */ false);

        mController.updateHeaderByBatteryTips(lowBatteryTip, null);

        verifyNoInteractions(mBatteryUsageProgressBarPref);
    }

    @Test
    public void updatePreference_isBatteryDefender_showEmptyText() {
        mBatteryInfo.isBatteryDefender = true;

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref).setBottomSummary(null);
    }

    @Test
    public void updatePreference_incompatibleCharger_showNotChargingState() {
        BatteryTestUtils.setupIncompatibleEvent(mUsbPort, mUsbManager, mUsbPortStatus);

        mController.updateHeaderPreference(mBatteryInfo);

        verify(mBatteryUsageProgressBarPref)
                .setBottomSummary(
                        mContext.getString(
                                com.android.settingslib.R.string.battery_info_status_not_charging));
    }

    @Test
    public void quickUpdateHeaderPreference_onlyUpdateBatteryLevelAndChargingState() {
        mController.quickUpdateHeaderPreference();

        verify(mBatteryUsageProgressBarPref).setUsageSummary(formatBatteryPercentageText());
        verify(mBatteryUsageProgressBarPref).setPercent(BATTERY_LEVEL, BATTERY_MAX_LEVEL);
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

        assertThat(mBatteryUsageProgressBarPref.isVisible()).isFalse();
    }

    @Test
    public void displayPreference_init_showLoading() {
        mController.displayPreference(mPreferenceScreen);

        verify(mBatteryUsageProgressBarPref)
                .setBottomSummary(mContext.getString(R.string.settings_license_activity_loading));
    }

    private CharSequence formatBatteryPercentageText() {
        return TextUtils.expandTemplate(
                mContext.getText(R.string.battery_header_title_alternate),
                NumberFormat.getIntegerInstance().format(BATTERY_LEVEL));
    }

    private void setChargingState(boolean isDischarging, boolean updatedByStatusFeature) {
        mBatteryInfo.remainingLabel = TIME_LEFT;
        mBatteryInfo.statusLabel = BATTERY_STATUS;
        mBatteryInfo.discharging = isDischarging;

        when(mBatteryStatusFeatureProvider.triggerBatteryStatusUpdate(mController, mBatteryInfo))
                .thenReturn(updatedByStatusFeature);
    }
}
