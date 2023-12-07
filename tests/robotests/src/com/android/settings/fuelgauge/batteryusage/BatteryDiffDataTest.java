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

package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.android.settingslib.applications.ApplicationsState;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
public class BatteryDiffDataTest {

    private Context mContext;

    @Mock private ApplicationsState mApplicationsState;
    @Mock private ApplicationsState.AppEntry mAppEntry;
    @Mock private ApplicationInfo mApplicationInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
    }

    @Test
    public void needsCombineInSystemApp_isHidden_returnTrue() {
        final BatteryDiffEntry hiddenDiffEntry =
                createBatteryDiffEntry(mContext, /* consumePower= */ 0, /* isHidden= */ true);

        final boolean needsCombineInSystemApp =
                BatteryDiffData.needsCombineInSystemApp(
                        hiddenDiffEntry, List.of(), Set.of(), Set.of());

        assertThat(needsCombineInSystemApp).isTrue();
    }

    @Test
    public void needsCombineInSystemApp_isSystemApp_returnTrue() {
        final BatteryDiffEntry batteryDiffEntry =
                createBatteryDiffEntry(mContext, /* consumePower= */ 0, /* isHidden= */ false);
        doReturn(mAppEntry).when(mApplicationsState).getEntry(anyString(), anyInt());
        mAppEntry.info = mApplicationInfo;
        mApplicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        final boolean needsCombineInSystemApp =
                BatteryDiffData.needsCombineInSystemApp(
                        batteryDiffEntry,
                        List.of(),
                        Set.of(ConvertUtils.FAKE_PACKAGE_NAME),
                        Set.of());

        assertThat(needsCombineInSystemApp).isTrue();
    }

    @Test
    public void needsCombineInSystemApp_notSystemApp_returnFalse() {
        final BatteryDiffEntry batteryDiffEntry =
                createBatteryDiffEntry(mContext, /* consumePower= */ 0, /* isHidden= */ false);
        doReturn(mAppEntry).when(mApplicationsState).getEntry(anyString(), anyInt());
        mAppEntry.info = mApplicationInfo;
        mApplicationInfo.flags = 0;

        final boolean needsCombineInSystemApp =
                BatteryDiffData.needsCombineInSystemApp(
                        batteryDiffEntry, List.of(), Set.of(), Set.of());

        assertThat(needsCombineInSystemApp).isFalse();
    }

    @Test
    public void processPercentsAndSort_sumLessThan100_adjustTo100() {
        List<BatteryDiffEntry> batteryDiffEntries = new ArrayList<>();
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 33.33, /* isHidden= */ false));
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 33.34, /* isHidden= */ false));
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 33.33, /* isHidden= */ false));

        BatteryDiffData.processAndSortEntries(batteryDiffEntries);

        assertThat(batteryDiffEntries.get(0).getPercentage()).isEqualTo(33.34);
        assertThat(batteryDiffEntries.get(0).getAdjustPercentageOffset()).isEqualTo(1);
        assertThat(batteryDiffEntries.get(1).getPercentage()).isEqualTo(33.33);
        assertThat(batteryDiffEntries.get(1).getAdjustPercentageOffset()).isEqualTo(0);
        assertThat(batteryDiffEntries.get(2).getPercentage()).isEqualTo(33.33);
        assertThat(batteryDiffEntries.get(2).getAdjustPercentageOffset()).isEqualTo(0);
    }

    @Test
    public void processPercentsAndSort_sumGreaterThan100_adjustTo100() {
        List<BatteryDiffEntry> batteryDiffEntries = new ArrayList<>();
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 48.5, /* isHidden= */ false));
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 3, /* isHidden= */ false));
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 48.5, /* isHidden= */ false));

        BatteryDiffData.processAndSortEntries(batteryDiffEntries);

        assertThat(batteryDiffEntries.get(0).getPercentage()).isEqualTo(48.5);
        assertThat(batteryDiffEntries.get(0).getAdjustPercentageOffset()).isEqualTo(0);
        assertThat(batteryDiffEntries.get(1).getPercentage()).isEqualTo(48.5);
        assertThat(batteryDiffEntries.get(1).getAdjustPercentageOffset()).isEqualTo(-1);
        assertThat(batteryDiffEntries.get(2).getPercentage()).isEqualTo(3);
        assertThat(batteryDiffEntries.get(2).getAdjustPercentageOffset()).isEqualTo(0);
    }

    @Test
    public void processPercentsAndSort_uninstalledApps_sortAsExpected() {
        List<BatteryDiffEntry> batteryDiffEntries = new ArrayList<>();
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 28.5, /* key= */ "APP_1"));
        batteryDiffEntries.add(
                createBatteryDiffEntry(
                        mContext, /* consumePower= */ 20, BatteryDiffEntry.UNINSTALLED_APPS_KEY));
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 3, /* key= */ "APP_2"));
        batteryDiffEntries.add(
                createBatteryDiffEntry(
                        mContext, /* consumePower= */ 28.5, BatteryDiffEntry.SYSTEM_APPS_KEY));
        batteryDiffEntries.add(
                createBatteryDiffEntry(mContext, /* consumePower= */ 20, /* key= */ "APP_3"));

        BatteryDiffData.processAndSortEntries(batteryDiffEntries);

        assertThat(batteryDiffEntries.get(0).getKey()).isEqualTo("APP_1");
        assertThat(batteryDiffEntries.get(1).getKey()).isEqualTo("APP_3");
        assertThat(batteryDiffEntries.get(2).getKey()).isEqualTo("APP_2");
        assertThat(batteryDiffEntries.get(3).getKey())
                .isEqualTo(BatteryDiffEntry.UNINSTALLED_APPS_KEY);
        assertThat(batteryDiffEntries.get(4).getKey()).isEqualTo(BatteryDiffEntry.SYSTEM_APPS_KEY);
    }

    private static BatteryDiffEntry createBatteryDiffEntry(
            Context context, double consumePower, boolean isHidden) {
        return createBatteryDiffEntry(context, consumePower, isHidden, /* key= */ null);
    }

    private static BatteryDiffEntry createBatteryDiffEntry(
            Context context, double consumePower, String key) {
        return createBatteryDiffEntry(context, consumePower, /* isHidden= */ false, key);
    }

    private static BatteryDiffEntry createBatteryDiffEntry(
            Context context, double consumePower, boolean isHidden, String key) {
        final int currentUserId = context.getUserId();
        final BatteryHistEntry batteryHistEntry =
                createBatteryHistEntry(
                        ConvertUtils.FAKE_PACKAGE_NAME,
                        "fake_label",
                        consumePower,
                        /* foregroundUsageConsumePower= */ 0,
                        /* foregroundServiceUsageConsumePower= */ 0,
                        /* backgroundUsageConsumePower= */ 0,
                        /* cachedUsageConsumePower= */ 0,
                        /* uid= */ 0L,
                        currentUserId,
                        ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /* foregroundUsageTimeInMs= */ 0L,
                        /* backgroundUsageTimeInMs= */ 0L,
                        isHidden);
        return new BatteryDiffEntry(
                context,
                batteryHistEntry.mUid,
                batteryHistEntry.mUserId,
                key == null ? batteryHistEntry.getKey() : key,
                batteryHistEntry.mIsHidden,
                batteryHistEntry.mDrainType,
                batteryHistEntry.mPackageName,
                batteryHistEntry.mAppLabel,
                batteryHistEntry.mConsumerType,
                /* foregroundUsageTimeInMs= */ 0,
                /* foregroundServiceUsageTimeInMs= */ 0,
                /* backgroundUsageTimeInMs= */ 0,
                /* screenOnTimeInMs= */ 0,
                consumePower,
                /* foregroundUsageConsumePower= */ 0,
                /* foregroundServiceUsageConsumePower= */ 0,
                /* backgroundUsageConsumePower= */ 0,
                /* cachedUsageConsumePower= */ 0);
    }

    private static BatteryHistEntry createBatteryHistEntry(
            final String packageName,
            final String appLabel,
            final double consumePower,
            final double foregroundUsageConsumePower,
            final double foregroundServiceUsageConsumePower,
            final double backgroundUsageConsumePower,
            final double cachedUsageConsumePower,
            final long uid,
            final long userId,
            final int consumerType,
            final long foregroundUsageTimeInMs,
            final long backgroundUsageTimeInMs,
            final boolean isHidden) {
        // Only insert required fields.
        final BatteryInformation batteryInformation =
                BatteryInformation.newBuilder()
                        .setAppLabel(appLabel)
                        .setConsumePower(consumePower)
                        .setForegroundUsageConsumePower(foregroundUsageConsumePower)
                        .setForegroundServiceUsageConsumePower(foregroundServiceUsageConsumePower)
                        .setBackgroundUsageConsumePower(backgroundUsageConsumePower)
                        .setCachedUsageConsumePower(cachedUsageConsumePower)
                        .setForegroundUsageTimeInMs(foregroundUsageTimeInMs)
                        .setBackgroundUsageTimeInMs(backgroundUsageTimeInMs)
                        .setIsHidden(isHidden)
                        .build();
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, packageName);
        values.put(BatteryHistEntry.KEY_UID, uid);
        values.put(BatteryHistEntry.KEY_USER_ID, userId);
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE, consumerType);
        values.put(
                BatteryHistEntry.KEY_BATTERY_INFORMATION,
                ConvertUtils.convertBatteryInformationToString(batteryInformation));
        return new BatteryHistEntry(values);
    }
}
