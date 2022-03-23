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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.os.BatteryConsumer;
import android.os.UserHandle;
import android.os.UserManager;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {BatteryDiffEntryTest.ShadowUserHandle.class})
public final class BatteryDiffEntryTest {

    private Context mContext;

    @Mock private ApplicationInfo mockAppInfo;
    @Mock private PackageManager mockPackageManager;
    @Mock private UserManager mockUserManager;
    @Mock private Drawable mockDrawable;
    @Mock private Drawable mockDrawable2;
    @Mock private Drawable mockBadgedDrawable;
    @Mock private BatteryHistEntry mBatteryHistEntry;
    @Mock private PackageInfo mockPackageInfo;
    @Mock private ConstantState mockConstantState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUserHandle.reset();
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mockUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mockPackageManager).when(mContext).getPackageManager();
        BatteryDiffEntry.clearCache();
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
        entryList.add(createBatteryDiffEntry(30, mBatteryHistEntry));
        entryList.add(createBatteryDiffEntry(20, mBatteryHistEntry));
        entryList.add(createBatteryDiffEntry(10, mBatteryHistEntry));
        Collections.sort(entryList, BatteryDiffEntry.COMPARATOR);

        assertThat(entryList.get(0).getPercentOfTotal()).isEqualTo(30);
        assertThat(entryList.get(1).getPercentOfTotal()).isEqualTo(20);
        assertThat(entryList.get(2).getPercentOfTotal()).isEqualTo(10);
    }

    @Test
    public void testLoadLabelAndIcon_forSystemBattery_returnExpectedResult() {
        final String expectedName = "Ambient display";
        // Generates fake testing data.
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        values.put("drainType",
            Integer.valueOf(BatteryConsumer.POWER_COMPONENT_AMBIENT_DISPLAY));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo(expectedName);
        assertThat(entry.getAppIconId()).isEqualTo(R.drawable.ic_settings_aod);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
            BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.name).isEqualTo(expectedName);
        assertThat(nameAndIcon.iconId).isEqualTo(R.drawable.ic_settings_aod);
        // Verifies the restrictable flag in the cache.
        assertThat(entry.mValidForRestriction).isTrue();
        assertThat(BatteryDiffEntry.sValidForRestriction.get(entry.getKey())).isTrue();
    }

    @Test
    public void testLoadLabelAndIcon_forUserBattery_returnExpectedResult() {
        final String expectedName = "Removed user";
        doReturn(null).when(mockUserManager).getUserInfo(1001);
        // Generates fake testing data.
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_USER_BATTERY);
        values.put("userId", Integer.valueOf(1001));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo(expectedName);
        assertThat(entry.getAppIcon()).isNull();
        assertThat(entry.getAppIconId()).isEqualTo(0);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
            BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.name).isEqualTo(expectedName);
        assertThat(nameAndIcon.iconId).isEqualTo(0);
        // Verifies the restrictable flag in the cache.
        assertThat(entry.mValidForRestriction).isTrue();
        assertThat(BatteryDiffEntry.sValidForRestriction.get(entry.getKey())).isTrue();
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
        assertThat(entry.getAppIconId()).isEqualTo(0);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
            BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.name).isEqualTo(expectedAppLabel);
        // Verifies the restrictable flag in the cache.
        assertThat(entry.mValidForRestriction).isFalse();
        assertThat(BatteryDiffEntry.sValidForRestriction.get(entry.getKey())).isFalse();
    }

    @Test
    public void testGetAppLabel_loadDataFromPreDefinedNameAndUid() {
        final String expectedAppLabel = "Android OS";
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo(expectedAppLabel);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
            BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.name).isEqualTo(expectedAppLabel);
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
        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
    }

    @Test
    public void testGetAppIcon_nonUidConsumer_returnAppIconInBatteryDiffEntry() {
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);
        mockConstantState(mockDrawable);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        entry.mIsLoaded = true;
        entry.mAppIcon = mockDrawable;
        assertThat(entry.getAppIcon()).isEqualTo(mockDrawable);
        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
    }

    @Test
    public void testGetAppIcon_uidConsumerForNonOwner_returnDefaultActivityIconWithBadge()
            throws Exception {
        ShadowUserHandle.setUid(10);
        final BatteryDiffEntry entry = createBatteryDiffEntry(mockDrawable);
        mockConstantState(mockDrawable);
        mockConstantState(mockBadgedDrawable);
        doReturn(mockBadgedDrawable).when(mockUserManager)
            .getBadgedIconForUser(eq(mockDrawable), any());

        entry.mAppIcon = null;
        assertThat(entry.getAppIcon()).isEqualTo(mockBadgedDrawable);
    }

    @Test
    public void testGetAppIcon_uidConsumerWithNullIcon_returnDefaultActivityIcon()
            throws Exception {
        final BatteryDiffEntry entry = createBatteryDiffEntry(mockDrawable);
        mockConstantState(mockDrawable);

        entry.mAppIcon = null;
        assertThat(entry.getAppIcon()).isEqualTo(mockDrawable);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
            BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.icon).isEqualTo(mockDrawable);
    }

    @Test
    public void testClearCache_clearDataForResourcesAndFlags() {
        BatteryDiffEntry.sResourceCache.put(
            "fake application key",
            new BatteryEntry.NameAndIcon("app label", null, /*iconId=*/ 0));
        BatteryDiffEntry.sValidForRestriction.put(
            "fake application key", Boolean.valueOf(false));

        BatteryDiffEntry.clearCache();

        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
        assertThat(BatteryDiffEntry.sValidForRestriction).isEmpty();
    }

    @Test
    public void testClearCache_switchLocale_clearCacheIconAndLabel() throws Exception {
        final int userId = UserHandle.getUserId(1001);
        Locale.setDefault(new Locale("en_US"));
        final BatteryDiffEntry entry1 = createBatteryDiffEntry(mockDrawable);
        mockConstantState(mockDrawable);
        assertThat(entry1.getAppIcon()).isEqualTo(mockDrawable);
        // Switch the locale into another one.
        Locale.setDefault(new Locale("zh_TW"));

        final BatteryDiffEntry entry2 = createBatteryDiffEntry(mockDrawable2);

        // We should get new drawable without caching.
        mockConstantState(mockDrawable2);
        assertThat(entry2.getAppIcon()).isEqualTo(mockDrawable2);
        // Verifies the cache is updated into the new drawable.
        final BatteryEntry.NameAndIcon nameAndIcon =
            BatteryDiffEntry.sResourceCache.get(entry2.getKey());
        assertThat(nameAndIcon.icon).isEqualTo(mockDrawable2);
    }

    @Test
    public void testIsSystemEntry_userBattery_returnTrue() {
        final BatteryDiffEntry entry =
            createBatteryDiffEntry(
                ConvertUtils.CONSUMER_TYPE_USER_BATTERY,
                /*uid=*/ 0, /*isHidden=*/ false);
        assertThat(entry.isSystemEntry()).isTrue();
    }

    @Test
    public void testIsSystemEntry_systemBattery_returnTrue() {
        final BatteryDiffEntry entry =
            createBatteryDiffEntry(
                ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY,
                /*uid=*/ 0, /*isHidden=*/ false);
        assertThat(entry.isSystemEntry()).isTrue();
    }

    @Test
    public void testIsSystemEntry_uidBattery_returnFalse() {
        final BatteryDiffEntry entry =
            createBatteryDiffEntry(
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*uid=*/ 123, /*isHidden=*/ false);
        assertThat(entry.isSystemEntry()).isFalse();
    }

    @Test
    public void testIsSystemEntry_uidBatteryWithHiddenState_returnTrue() {
        final BatteryDiffEntry entry =
            createBatteryDiffEntry(
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*uid=*/ 123, /*isHidden=*/ true);
        assertThat(entry.isSystemEntry()).isTrue();
    }

    @Test
    public void testIsSystemEntry_uidBatteryWithSystemProcess_returnTrue() {
        final BatteryDiffEntry entry =
            createBatteryDiffEntry(
                ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                /*uid=*/ 1230, /*isHidden=*/ false);
        assertThat(entry.isSystemEntry()).isTrue();
    }

    @Test
    public void testUpdateRestrictionFlagState_updateFlagAsExpected() throws Exception {
        final String expectedAppLabel = "fake app label";
        final String fakePackageName = "com.fake.google.com";
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put("uid", /*invalid uid*/ 10001);
        values.put("packageName", fakePackageName);
        final BatteryDiffEntry entry =
            createBatteryDiffEntry(10, new BatteryHistEntry(values));

        entry.updateRestrictionFlagState();
        // Sets false if the app entry cannot be found.
        assertThat(entry.mValidForRestriction).isFalse();

        doReturn(BatteryUtils.UID_NULL).when(mockPackageManager).getPackageUid(
            entry.getPackageName(), PackageManager.GET_META_DATA);
        entry.updateRestrictionFlagState();
        // Sets false if the app is invalid package name.
        assertThat(entry.mValidForRestriction).isFalse();

        doReturn(1000).when(mockPackageManager).getPackageUid(
            entry.getPackageName(), PackageManager.GET_META_DATA);
        entry.updateRestrictionFlagState();
        // Sets false if the app PackageInfo cannot be found.
        assertThat(entry.mValidForRestriction).isFalse();

        doReturn(mockPackageInfo).when(mockPackageManager).getPackageInfo(
            eq(entry.getPackageName()), anyInt());
        entry.updateRestrictionFlagState();
        // Sets true if package is valid and PackageInfo can be found.
        assertThat(entry.mValidForRestriction).isTrue();
    }

    @Test
    public void testGetPackageName_returnExpectedResult() {
        final String expectedPackageName = "com.fake.google.com";
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put("packageName", expectedPackageName);
        final BatteryDiffEntry entry =
            createBatteryDiffEntry(10, new BatteryHistEntry(values));

        assertThat(entry.getPackageName()).isEqualTo(expectedPackageName);
    }

    @Test
    public void testGetPackageName_withProcessName_returnExpectedResult() {
        final String expectedPackageName = "com.fake.google.com";
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(
            "packageName",
            expectedPackageName + ":privileged_process0");
        final BatteryDiffEntry entry =
            createBatteryDiffEntry(10, new BatteryHistEntry(values));

        assertThat(entry.getPackageName()).isEqualTo(expectedPackageName);
    }

    private BatteryDiffEntry createBatteryDiffEntry(
            int consumerType, long uid, boolean isHidden) {
        final ContentValues values = getContentValuesWithType(consumerType);
        values.put("isHidden", isHidden);
        values.put("uid", uid);
        return new BatteryDiffEntry(
            mContext,
            /*foregroundUsageTimeInMs=*/ 0,
            /*backgroundUsageTimeInMs=*/ 0,
            /*consumePower=*/ 0,
            new BatteryHistEntry(values));
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

    private BatteryDiffEntry createBatteryDiffEntry(Drawable drawable) throws Exception {
        final ContentValues values = getContentValuesWithType(
            ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put("uid", 1001);
        values.put("packageName", "com.a.b.c");
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);
        doReturn(drawable).when(mockPackageManager).getDefaultActivityIcon();
        doReturn(null).when(mockPackageManager).getApplicationInfo("com.a.b.c", 0);
        doReturn(new String[] {"com.a.b.c"}).when(mockPackageManager)
            .getPackagesForUid(1001);
        return createBatteryDiffEntry(10, batteryHistEntry);
    }

    private void mockConstantState(Drawable drawable) {
        doReturn(mockConstantState).when(drawable).getConstantState();
        doReturn(drawable).when(mockConstantState).newDrawable();
    }

    @Implements(UserHandle.class)
    public static class ShadowUserHandle {
        // Sets the default as thte OWNER role.
        private static int sUid = 0;

        public static void setUid(int uid) {
            sUid = uid;
        }

        @Implementation
        public static int myUserId() {
            return sUid;
        }

        @Implementation
        public static int getUserId(int userId) {
            return sUid;
        }

        @Resetter
        public static void reset() {
            sUid = 0;
        }
    }
}
