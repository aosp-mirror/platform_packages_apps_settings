/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.ContentValues;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.SystemBatteryConsumer;
import android.os.UserManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public final class BatteryDiffEntryTest {

    private Context mContext;

    @Mock private ApplicationInfo mockAppInfo;
    @Mock private PackageManager mockPackageManager;
    @Mock private UserManager mockUserManager;
    @Mock private Drawable mockDrawable;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mockUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mockPackageManager).when(mContext).getPackageManager();
    }

    @Test
    public void testSetTotalConsumePower_returnExpectedResult() {
        final BatteryDiffEntry entry =
            new BatteryDiffEntry(
                mContext,
                /*foregroundUsageTimeInMs=*/ 10001L,
                /*backgroundUsageTimeInMs=*/ 20002L,
                /*consumePower=*/ 22.0,
                /*batteryHistEntry=*/ null);
        entry.setTotalConsumePower(100.0);

        assertThat(entry.getPercentOfTotal()).isEqualTo(22.0);
    }

    @Test
    public void testSetTotalConsumePower_setZeroValue_returnsZeroValue() {
        final BatteryDiffEntry entry =
            new BatteryDiffEntry(
                mContext,
                /*foregroundUsageTimeInMs=*/ 10001L,
                /*backgroundUsageTimeInMs=*/ 20002L,
                /*consumePower=*/ 22.0,
                /*batteryHistEntry=*/ null);
        entry.setTotalConsumePower(0);

        assertThat(entry.getPercentOfTotal()).isEqualTo(0);
    }

    @Test
    public void testComparator_sortCollectionsInDescOrder() {
        final List<BatteryDiffEntry> entryList = new ArrayList<>();
        // Generates fake testing data.
        entryList.add(createBatteryDiffEntry(30, /*batteryHistEntry=*/ null));
        entryList.add(createBatteryDiffEntry(20, /*batteryHistEntry=*/ null));
        entryList.add(createBatteryDiffEntry(10, /*batteryHistEntry=*/ null));
        Collections.sort(entryList, BatteryDiffEntry.COMPARATOR);

        assertThat(entryList.get(0).getPercentOfTotal()).isEqualTo(30);
        assertThat(entryList.get(1).getPercentOfTotal()).isEqualTo(20);
        assertThat(entryList.get(2).getPercentOfTotal()).isEqualTo(10);
    }

    @Test
    public void testLoadLabelAndIcon_forSystemBattery_returnExpectedResult() {
        // Generates fake testing data.
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        values.put("drainType",
            Integer.valueOf(SystemBatteryConsumer.DRAIN_TYPE_AMBIENT_DISPLAY));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo("Ambient display");
    }

    @Test
    public void testLoadLabelAndIcon_forUserBattery_returnExpectedResult() {
        doReturn(null).when(mockUserManager).getUserInfo(1001);
        // Generates fake testing data.
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_USER_BATTERY);
        values.put("userId", Integer.valueOf(1001));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo("Removed user");
        assertThat(entry.getAppIcon()).isNull();
    }

    @Test
    public void testGetAppLabel_loadDataFromApplicationInfo() throws Exception {
        final String expectedAppLabel = "fake app label";
        final String fakePackageName = "com.fake.google.com";
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put("uid", /*invalid uid*/ 10001);
        values.put("packageName", fakePackageName);
        doReturn(mockAppInfo).when(mockPackageManager)
            .getApplicationInfo(fakePackageName, 0);
        doReturn(expectedAppLabel).when(mockPackageManager)
            .getApplicationLabel(mockAppInfo);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo(expectedAppLabel);
    }

    @Test
    public void testGetAppLabel_loadDataFromPreDefinedNameAndUid() {
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo("Android OS");
    }

    @Test
    public void testGetAppLabel_nullAppLabel_returnAppLabelInBatteryHistEntry() {
        final String expectedAppLabel = "fake app label";
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put("appLabel", expectedAppLabel);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        entry.mIsLoaded = true;
        assertThat(entry.getAppLabel()).isEqualTo(expectedAppLabel);
    }

    @Test
    public void testGetAppIcon_nonUidConsumer_returnAppIconInBatteryDiffEntry() {
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        entry.mIsLoaded = true;
        entry.mAppIcon = mockDrawable;
        assertThat(entry.getAppIcon()).isEqualTo(mockDrawable);
    }

    @Test
    public void testGetAppIcon_uidConsumerWithNullIcon_returnDefaultActivityIcon() {
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);
        doReturn(mockDrawable).when(mockPackageManager).getDefaultActivityIcon();

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        entry.mIsLoaded = true;
        entry.mAppIcon = null;
        assertThat(entry.getAppIcon()).isEqualTo(mockDrawable);
    }

    private BatteryDiffEntry createBatteryDiffEntry(
        double consumePower, BatteryHistEntry batteryHistEntry) {
        final BatteryDiffEntry entry = new BatteryDiffEntry(
            mContext,
            /*foregroundUsageTimeInMs=*/ 0,
            /*backgroundUsageTimeInMs=*/ 0,
            consumePower,
            batteryHistEntry);
        entry.setTotalConsumePower(100.0);
        return entry;
    }

    private static ContentValues getContentValuesWithType(int consumerType) {
        final ContentValues values = new ContentValues();
        values.put("consumerType", Integer.valueOf(consumerType));
        return values;
    }
}
