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
        final int currentUserId = mContext.getUserId();
        final BatteryHistEntry hiddenHistEntry = createBatteryHistEntry(
                ConvertUtils.FAKE_PACKAGE_NAME, "fake_label", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 0L, currentUserId, ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*foregroundUsageTimeInMs=*/ 0L,  /*backgroundUsageTimeInMs=*/ 0L, true);
        final BatteryDiffEntry hiddenDiffEntry = new BatteryDiffEntry(
                mContext,
                /*foregroundUsageTimeInMs=*/ 0,
                /*backgroundUsageTimeInMs=*/ 0,
                /*screenOnTimeInMs=*/ 0,
                /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0,
                /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0,
                /*cachedUsageConsumePower=*/ 0,
                hiddenHistEntry);

        boolean needsCombineInSystemApp = BatteryDiffData.needsCombineInSystemApp(
                hiddenDiffEntry, List.of(), Set.of());

        assertThat(needsCombineInSystemApp).isTrue();
    }

    @Test
    public void needsCombineInSystemApp_isSystemApp_returnTrue() {
        final int currentUserId = mContext.getUserId();
        final BatteryHistEntry batteryHistEntry = createBatteryHistEntry(
                ConvertUtils.FAKE_PACKAGE_NAME, "fake_label", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 0L, currentUserId, ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*foregroundUsageTimeInMs=*/ 0L,  /*backgroundUsageTimeInMs=*/ 0L, false);
        final BatteryDiffEntry batteryDiffEntry = new BatteryDiffEntry(
                mContext,
                /*foregroundUsageTimeInMs=*/ 0,
                /*backgroundUsageTimeInMs=*/ 0,
                /*screenOnTimeInMs=*/ 0,
                /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0,
                /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0,
                /*cachedUsageConsumePower=*/ 0,
                batteryHistEntry);
        doReturn(mAppEntry).when(mApplicationsState).getEntry(anyString(), anyInt());
        mAppEntry.info = mApplicationInfo;
        mApplicationInfo.flags = ApplicationInfo.FLAG_SYSTEM;

        boolean needsCombineInSystemApp = BatteryDiffData.needsCombineInSystemApp(
                batteryDiffEntry, List.of(), Set.of(ConvertUtils.FAKE_PACKAGE_NAME));

        assertThat(needsCombineInSystemApp).isTrue();
    }

    @Test
    public void needsCombineInSystemApp_notSystemApp_returnFalse() {
        final int currentUserId = mContext.getUserId();
        final BatteryHistEntry batteryHistEntry = createBatteryHistEntry(
                ConvertUtils.FAKE_PACKAGE_NAME, "fake_label", /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0, /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0, /*cachedUsageConsumePower=*/ 0,
                /*uid=*/ 0L, currentUserId, ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*foregroundUsageTimeInMs=*/ 0L,  /*backgroundUsageTimeInMs=*/ 0L, false);
        final BatteryDiffEntry batteryDiffEntry = new BatteryDiffEntry(
                mContext,
                /*foregroundUsageTimeInMs=*/ 0,
                /*backgroundUsageTimeInMs=*/ 0,
                /*screenOnTimeInMs=*/ 0,
                /*consumePower=*/ 0,
                /*foregroundUsageConsumePower=*/ 0,
                /*foregroundServiceUsageConsumePower=*/ 0,
                /*backgroundUsageConsumePower=*/ 0,
                /*cachedUsageConsumePower=*/ 0,
                batteryHistEntry);
        doReturn(mAppEntry).when(mApplicationsState).getEntry(anyString(), anyInt());
        mAppEntry.info = mApplicationInfo;
        mApplicationInfo.flags = 0;

        boolean needsCombineInSystemApp = BatteryDiffData.needsCombineInSystemApp(
                batteryDiffEntry, List.of(), Set.of());

        assertThat(needsCombineInSystemApp).isFalse();
    }

    private static BatteryHistEntry createBatteryHistEntry(
            final String packageName, final String appLabel, final double consumePower,
            final double foregroundUsageConsumePower,
            final double foregroundServiceUsageConsumePower,
            final double backgroundUsageConsumePower, final double cachedUsageConsumePower,
            final long uid, final long userId, final int consumerType,
            final long foregroundUsageTimeInMs, final long backgroundUsageTimeInMs,
            final boolean isHidden) {
        // Only insert required fields.
        final BatteryInformation batteryInformation =
                BatteryInformation
                        .newBuilder()
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
        values.put(BatteryHistEntry.KEY_BATTERY_INFORMATION,
                ConvertUtils.convertBatteryInformationToString(batteryInformation));
        return new BatteryHistEntry(values);
    }
}
