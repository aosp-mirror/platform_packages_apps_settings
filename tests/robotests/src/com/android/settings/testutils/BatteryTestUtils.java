/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.testutils;

import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.os.BatteryManager;
import android.os.UserManager;

import androidx.room.Room;

import com.android.settings.DisplaySettings;
import com.android.settings.display.ScreenTimeoutSettings;
import com.android.settings.fuelgauge.batteryusage.BatteryInformation;
import com.android.settings.fuelgauge.batteryusage.ConvertUtils;
import com.android.settings.fuelgauge.batteryusage.DeviceBatteryState;
import com.android.settings.fuelgauge.batteryusage.PowerAnomalyEvent;
import com.android.settings.fuelgauge.batteryusage.PowerAnomalyEventList;
import com.android.settings.fuelgauge.batteryusage.PowerAnomalyKey;
import com.android.settings.fuelgauge.batteryusage.PowerAnomalyType;
import com.android.settings.fuelgauge.batteryusage.WarningBannerInfo;
import com.android.settings.fuelgauge.batteryusage.WarningItemInfo;
import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventDao;
import com.android.settings.fuelgauge.batteryusage.db.AppUsageEventEntity;
import com.android.settings.fuelgauge.batteryusage.db.BatteryState;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDao;
import com.android.settings.fuelgauge.batteryusage.db.BatteryStateDatabase;

import com.google.common.collect.ImmutableList;

import org.robolectric.Shadows;

import java.util.ArrayList;
import java.util.List;

public class BatteryTestUtils {

    public static Intent getChargingIntent() {
        return getCustomBatteryIntent(
                BatteryManager.BATTERY_PLUGGED_AC,
                50 /* level */,
                100 /* scale */,
                BatteryManager.BATTERY_STATUS_CHARGING);
    }

    public static Intent getDischargingIntent() {
        return getCustomBatteryIntent(
                0 /* plugged */,
                10 /* level */,
                100 /* scale */,
                BatteryManager.BATTERY_STATUS_DISCHARGING);
    }

    /**
     * Sets the work profile mode.
     */
    public static void setWorkProfile(Context context) {
        final UserManager userManager = context.getSystemService(UserManager.class);
        Shadows.shadowOf(userManager).setManagedProfile(true);
        Shadows.shadowOf(userManager).setIsSystemUser(false);
    }

    /**
     * Creates and sets up the in-memory {@link BatteryStateDatabase}.
     */
    public static BatteryStateDatabase setUpBatteryStateDatabase(Context context) {
        final BatteryStateDatabase inMemoryDatabase =
                Room.inMemoryDatabaseBuilder(context, BatteryStateDatabase.class)
                        .allowMainThreadQueries()
                        .build();
        BatteryStateDatabase.setBatteryStateDatabase(inMemoryDatabase);
        return inMemoryDatabase;
    }

    /**
     * Inserts a fake data into the database for testing.
     */
    public static void insertDataToBatteryStateTable(
            Context context, long timestamp, String packageName) {
        insertDataToBatteryStateTable(
                context, timestamp, packageName, /*multiple=*/ false, /*isFullChargeStart=*/ false);
    }

    /**
     * Inserts a fake data into the database for testing.
     */
    public static void insertDataToBatteryStateTable(
            Context context, long timestamp, String packageName, boolean isFullChargeStart) {
        insertDataToBatteryStateTable(
                context, timestamp, packageName, /*multiple=*/ false, isFullChargeStart);
    }

    /**
     * Inserts a fake data into the database for testing.
     */
    public static void insertDataToBatteryStateTable(
            Context context, long timestamp, String packageName, boolean multiple,
            boolean isFullChargeStart) {
        DeviceBatteryState deviceBatteryState =
                DeviceBatteryState
                        .newBuilder()
                        .setBatteryLevel(31)
                        .setBatteryStatus(0)
                        .setBatteryHealth(0)
                        .build();
        BatteryInformation batteryInformation =
                BatteryInformation
                        .newBuilder()
                        .setDeviceBatteryState(deviceBatteryState)
                        .setIsHidden(true)
                        .setBootTimestamp(timestamp - 1)
                        .setZoneId("Europe/Paris")
                        .setAppLabel("Settings")
                        .setTotalPower(100f)
                        .setConsumePower(0.3f)
                        .setPercentOfTotal(10f)
                        .setDrainType(1)
                        .setForegroundUsageTimeInMs(60000)
                        .setBackgroundUsageTimeInMs(10000)
                        .setForegroundUsageConsumePower(0.1f)
                        .setForegroundServiceUsageConsumePower(0.05f)
                        .setBackgroundUsageConsumePower(0.1f)
                        .setCachedUsageConsumePower(0.05f)
                        .build();

        final BatteryState state =
                new BatteryState(
                        /*uid=*/ 1001L,
                        /*userId=*/ 100L,
                        packageName,
                        timestamp,
                        /*consumerType=*/ 2,
                        isFullChargeStart,
                        ConvertUtils.convertBatteryInformationToString(batteryInformation),
                        "");
        BatteryStateDao dao =
                BatteryStateDatabase.getInstance(context).batteryStateDao();
        if (multiple) {
            dao.insertAll(ImmutableList.of(state));
        } else {
            dao.insert(state);
        }
    }

    /**
     * Inserts a fake data into the database for testing.
     */
    public static void insertDataToAppUsageEventTable(
            Context context, long userId, long timestamp, String packageName) {
        insertDataToAppUsageEventTable(
                context, userId, timestamp, packageName, /*multiple=*/ false);
    }

    /**
     * Inserts a fake data into the database for testing.
     */
    public static void insertDataToAppUsageEventTable(
            Context context, long userId, long timestamp, String packageName, boolean multiple) {
        final AppUsageEventEntity entity =
                new AppUsageEventEntity(
                        /*uid=*/ 101L,
                        userId,
                        timestamp,
                        /*appUsageEventType=*/ 2,
                        packageName,
                        /*instanceId=*/ 10001,
                        /*taskRootPackageName=*/ "com.android.settings");
        AppUsageEventDao dao =
                BatteryStateDatabase.getInstance(context).appUsageEventDao();
        if (multiple) {
            dao.insertAll(ImmutableList.of(entity));
        } else {
            dao.insert(entity);
        }
    }

    /**
     * Gets customized battery changed intent.
     */
    public static Intent getCustomBatteryIntent(int plugged, int level, int scale, int status) {
        Intent intent = new Intent();
        intent.putExtra(BatteryManager.EXTRA_PLUGGED, plugged);
        intent.putExtra(BatteryManager.EXTRA_LEVEL, level);
        intent.putExtra(BatteryManager.EXTRA_SCALE, scale);
        intent.putExtra(BatteryManager.EXTRA_STATUS, status);

        return intent;
    }

    /**
     * Configures the incompatible charger environment.
     */
    public static void setupIncompatibleEvent(
            UsbPort mockUsbPort, UsbManager mockUsbManager, UsbPortStatus mockUsbPortStatus) {
        final List<UsbPort> usbPorts = new ArrayList<>();
        usbPorts.add(mockUsbPort);
        when(mockUsbManager.getPorts()).thenReturn(usbPorts);
        when(mockUsbPort.getStatus()).thenReturn(mockUsbPortStatus);
        when(mockUsbPort.supportsComplianceWarnings()).thenReturn(true);
        when(mockUsbPortStatus.isConnected()).thenReturn(true);
        when(mockUsbPortStatus.getComplianceWarnings())
                .thenReturn(new int[]{UsbPortStatus.COMPLIANCE_WARNING_DEBUG_ACCESSORY});
    }

    /**
     * Create an empty power anomaly event list proto.
     */
    public static PowerAnomalyEventList createEmptyPowerAnomalyEventList() {
        return PowerAnomalyEventList.getDefaultInstance();
    }

    /**
     * Create an non-empty power anomaly event list proto.
     */
    public static PowerAnomalyEventList createNonEmptyPowerAnomalyEventList() {
        return PowerAnomalyEventList.newBuilder()
                .addPowerAnomalyEvents(0, createAdaptiveBrightnessAnomalyEvent())
                .addPowerAnomalyEvents(1, createScreenTimeoutAnomalyEvent())
                .build();
    }

    /**
     * Create a power anomaly event proto of adaptive brightness.
     */
    public static PowerAnomalyEvent createAdaptiveBrightnessAnomalyEvent() {
        return PowerAnomalyEvent.newBuilder()
                .setEventId("BrightnessAnomaly")
                .setType(PowerAnomalyType.TYPE_SETTINGS_BANNER)
                .setKey(PowerAnomalyKey.KEY_BRIGHTNESS)
                .setDismissRecordKey(PowerAnomalyKey.KEY_BRIGHTNESS.name())
                .setScore(1.2f)
                .setWarningBannerInfo(WarningBannerInfo.newBuilder()
                        .setMainButtonDestination(DisplaySettings.class.getName())
                        .setMainButtonSourceMetricsCategory(SettingsEnums.DISPLAY)
                        .setMainButtonSourceHighlightKey("auto_brightness_entry")
                        .build())
                .build();
    }

    /**
     * Create a power anomaly event proto of screen timeout.
     */
    public static PowerAnomalyEvent createScreenTimeoutAnomalyEvent() {
        return PowerAnomalyEvent.newBuilder()
                .setEventId("ScreenTimeoutAnomaly")
                .setType(PowerAnomalyType.TYPE_SETTINGS_BANNER)
                .setKey(PowerAnomalyKey.KEY_SCREEN_TIMEOUT)
                .setDismissRecordKey(PowerAnomalyKey.KEY_SCREEN_TIMEOUT.name())
                .setScore(1.1f)
                .setWarningBannerInfo(WarningBannerInfo.newBuilder()
                        .setMainButtonDestination(ScreenTimeoutSettings.class.getName())
                        .setMainButtonSourceMetricsCategory(SettingsEnums.SCREEN_TIMEOUT)
                        .setMainButtonSourceHighlightKey("60000")
                        .build())
                .build();
    }

    /**
     * Create a power anomaly event proto of app anomaly.
     */
    public static PowerAnomalyEvent createAppAnomalyEvent() {
        return PowerAnomalyEvent.newBuilder()
                .setEventId("AppAnomaly")
                .setType(PowerAnomalyType.TYPE_APPS_ITEM)
                .setKey(PowerAnomalyKey.KEY_APP_TOTAL_HIGHER_THAN_USUAL)
                .setDismissRecordKey("KEY_APP_1")
                .setScore(2.0f)
                .setWarningItemInfo(WarningItemInfo.newBuilder()
                        .setStartTimestamp(1694361600000L)  // 2023-09-11 00:00:00
                        .setEndTimestamp(1694368800000L)    // 2023-09-11 02:00:00
                        .build())
                .build();
    }
}
