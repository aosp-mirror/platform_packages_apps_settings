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
package com.android.settings.fuelgauge.batteryusage;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
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
import com.android.settings.fuelgauge.BatteryUtils;

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

    private static final int UID = 100;
    private static final int UNINSTALLED_UID = 101;
    private static final String PACKAGE_NAME = "com.android.testing";
    private static final String UNINSTALLED_PACKAGE_NAME = "com.android.testing.uninstalled";
    private static final String UID_ZERO_PACKAGE_NAME = "com.android.testing.uid.zero";

    private Context mContext;

    @Mock private ApplicationInfo mMockAppInfo;
    @Mock private PackageManager mMockPackageManager;
    @Mock private UserManager mMockUserManager;
    @Mock private Drawable mMockDrawable;
    @Mock private Drawable mMockDrawable2;
    @Mock private Drawable mMockBadgedDrawable;
    @Mock private BatteryHistEntry mBatteryHistEntry;
    @Mock private PackageInfo mMockPackageInfo;
    @Mock private ConstantState mMockConstantState;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        ShadowUserHandle.reset();
        mContext = spy(RuntimeEnvironment.application);
        BatteryUtils.getInstance(mContext).reset();
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mMockUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(mMockPackageManager).when(mContext).getPackageManager();
        doReturn(UID)
                .when(mMockPackageManager)
                .getPackageUid(PACKAGE_NAME, PackageManager.GET_META_DATA);
        doReturn(BatteryUtils.UID_NULL)
                .when(mMockPackageManager)
                .getPackageUid(UNINSTALLED_PACKAGE_NAME, PackageManager.GET_META_DATA);
        doReturn(BatteryUtils.UID_ZERO)
                .when(mMockPackageManager)
                .getPackageUid(UID_ZERO_PACKAGE_NAME, PackageManager.GET_META_DATA);
        BatteryDiffEntry.clearCache();
    }

    @Test
    public void testSetTotalConsumePower_returnExpectedResult() {
        final BatteryDiffEntry entry =
                new BatteryDiffEntry(
                        mContext,
                        /* uid= */ 0,
                        /* userId= */ 0,
                        /* key= */ "key",
                        /* isHidden= */ false,
                        /* componentId= */ -1,
                        /* legacyPackageName= */ null,
                        /* legacyLabel= */ null,
                        /*consumerType*/ ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /* foregroundUsageTimeInMs= */ 10001L,
                        /* foregroundServiceUsageTimeInMs= */ 20002L,
                        /* backgroundUsageTimeInMs= */ 30003L,
                        /* screenOnTimeInMs= */ 40004L,
                        /* consumePower= */ 22.0,
                        /* foregroundUsageConsumePower= */ 10.0,
                        /* foregroundServiceUsageConsumePower= */ 10.0,
                        /* backgroundUsageConsumePower= */ 1.0,
                        /* cachedUsageConsumePower= */ 1.0);
        entry.setTotalConsumePower(100.0);

        assertThat(entry.getPercentage()).isEqualTo(22.0);
    }

    @Test
    public void testSetTotalConsumePower_setZeroValue_returnsZeroValue() {
        final BatteryDiffEntry entry =
                new BatteryDiffEntry(
                        mContext,
                        /* uid= */ 0,
                        /* userId= */ 0,
                        /* key= */ "key",
                        /* isHidden= */ false,
                        /* componentId= */ -1,
                        /* legacyPackageName= */ null,
                        /* legacyLabel= */ null,
                        /*consumerType*/ ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /* foregroundUsageTimeInMs= */ 10001L,
                        /* foregroundServiceUsageTimeInMs= */ 20002L,
                        /* backgroundUsageTimeInMs= */ 30003L,
                        /* screenOnTimeInMs= */ 40004L,
                        /* consumePower= */ 22.0,
                        /* foregroundUsageConsumePower= */ 10.0,
                        /* foregroundServiceUsageConsumePower= */ 10.0,
                        /* backgroundUsageConsumePower= */ 1.0,
                        /* cachedUsageConsumePower= */ 1.0);
        entry.setTotalConsumePower(0);

        assertThat(entry.getPercentage()).isEqualTo(0);
    }

    @Test
    public void testComparator_sortCollectionsInDescOrder() {
        final List<BatteryDiffEntry> entryList = new ArrayList<>();
        // Generates fake testing data.
        BatteryDiffEntry systemAppsBatteryDiffEntry =
                new BatteryDiffEntry(
                        mContext,
                        /* uid= */ 0,
                        /* userId= */ 0,
                        /* key= */ BatteryDiffEntry.SYSTEM_APPS_KEY,
                        /* isHidden= */ false,
                        /* componentId= */ -1,
                        /* legacyPackageName= */ null,
                        /* legacyLabel= */ BatteryDiffEntry.SYSTEM_APPS_KEY,
                        /*consumerType*/ ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0,
                        /* screenOnTimeInMs= */ 0,
                        /* consumePower= */ 0,
                        /* foregroundUsageConsumePower= */ 0,
                        /* foregroundServiceUsageConsumePower= */ 0,
                        /* backgroundUsageConsumePower= */ 0,
                        /* cachedUsageConsumePower= */ 0);
        systemAppsBatteryDiffEntry.mConsumePower = 16;
        systemAppsBatteryDiffEntry.setTotalConsumePower(100);
        entryList.add(systemAppsBatteryDiffEntry);
        entryList.add(createBatteryDiffEntry(30, mBatteryHistEntry));
        entryList.add(createBatteryDiffEntry(20, mBatteryHistEntry));
        entryList.add(createBatteryDiffEntry(10, mBatteryHistEntry));

        Collections.sort(entryList, BatteryDiffEntry.COMPARATOR);

        assertThat(entryList.get(0).getPercentage()).isEqualTo(30);
        assertThat(entryList.get(1).getPercentage()).isEqualTo(20);
        assertThat(entryList.get(2).getPercentage()).isEqualTo(10);
        assertThat(entryList.get(3).getPercentage()).isEqualTo(16);
    }

    @Test
    public void testLoadLabelAndIcon_forSystemBattery_returnExpectedResult() {
        final String expectedName = "Ambient display";
        // Generates fake testing data.
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        final BatteryInformation batteryInformation =
                BatteryInformation.newBuilder()
                        .setDrainType(BatteryConsumer.POWER_COMPONENT_AMBIENT_DISPLAY)
                        .build();
        values.put(
                BatteryHistEntry.KEY_BATTERY_INFORMATION,
                ConvertUtils.convertBatteryInformationToString(batteryInformation));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo(expectedName);
        assertThat(entry.getAppIconId()).isEqualTo(R.drawable.ic_settings_aod);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
                BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.mName).isEqualTo(expectedName);
        assertThat(nameAndIcon.mIconId).isEqualTo(R.drawable.ic_settings_aod);
        // Verifies the restrictable flag in the cache.
        assertThat(entry.mValidForRestriction).isFalse();
        assertThat(BatteryDiffEntry.sValidForRestriction.get(entry.getKey())).isFalse();
    }

    @Test
    public void testLoadLabelAndIcon_forUserBattery_returnExpectedResult() {
        final String expectedName = "Removed user";
        doReturn(null).when(mMockUserManager).getUserInfo(1001);
        // Generates fake testing data.
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_USER_BATTERY);
        values.put(BatteryHistEntry.KEY_USER_ID, Integer.valueOf(1001));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo(expectedName);
        assertThat(entry.getAppIcon()).isNull();
        assertThat(entry.getAppIconId()).isEqualTo(0);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
                BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.mName).isEqualTo(expectedName);
        assertThat(nameAndIcon.mIconId).isEqualTo(0);
        // Verifies the restrictable flag in the cache.
        assertThat(entry.mValidForRestriction).isFalse();
        assertThat(BatteryDiffEntry.sValidForRestriction.get(entry.getKey())).isFalse();
    }

    @Test
    public void testGetAppLabel_loadDataFromApplicationInfo() throws Exception {
        final String expectedAppLabel = "fake app label";
        final String fakePackageName = "com.fake.google.com";
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_UID, /*invalid uid*/ 10001);
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, fakePackageName);
        doReturn(mMockAppInfo).when(mMockPackageManager).getApplicationInfo(fakePackageName, 0);
        doReturn(expectedAppLabel).when(mMockPackageManager).getApplicationLabel(mMockAppInfo);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo(expectedAppLabel);
        assertThat(entry.getAppIconId()).isEqualTo(0);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
                BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.mName).isEqualTo(expectedAppLabel);
        // Verifies the restrictable flag in the cache.
        assertThat(entry.mValidForRestriction).isFalse();
        assertThat(BatteryDiffEntry.sValidForRestriction.get(entry.getKey())).isFalse();
    }

    @Test
    public void testGetAppLabel_loadDataFromPreDefinedNameAndUid() {
        final String expectedAppLabel = "Android OS";
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        assertThat(entry.getAppLabel()).isEqualTo(expectedAppLabel);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
                BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.mName).isEqualTo(expectedAppLabel);
    }

    @Test
    public void testGetAppLabel_nullAppLabel_returnAppLabelInBatteryHistEntry() {
        final String expectedAppLabel = "fake app label";
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        final BatteryInformation batteryInformation =
                BatteryInformation.newBuilder().setAppLabel(expectedAppLabel).build();
        values.put(
                BatteryHistEntry.KEY_BATTERY_INFORMATION,
                ConvertUtils.convertBatteryInformationToString(batteryInformation));
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        entry.mIsLoaded = true;
        assertThat(entry.getAppLabel()).isEqualTo(expectedAppLabel);
        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
    }

    @Test
    public void testGetAppIcon_nonUidConsumer_returnAppIconInBatteryDiffEntry() {
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY);
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);
        mockConstantState(mMockDrawable);

        final BatteryDiffEntry entry = createBatteryDiffEntry(10, batteryHistEntry);

        entry.mIsLoaded = true;
        entry.mAppIcon = mMockDrawable;
        assertThat(entry.getAppIcon()).isEqualTo(mMockDrawable);
        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
    }

    @Test
    public void testGetAppIcon_uidConsumerForNonOwner_returnDefaultActivityIconWithBadge()
            throws Exception {
        ShadowUserHandle.setUid(10);
        final BatteryDiffEntry entry = createBatteryDiffEntry(mMockDrawable);
        mockConstantState(mMockDrawable);
        mockConstantState(mMockBadgedDrawable);
        doReturn(mMockBadgedDrawable)
                .when(mMockUserManager)
                .getBadgedIconForUser(eq(mMockDrawable), any());

        entry.mAppIcon = null;
        assertThat(entry.getAppIcon()).isEqualTo(mMockBadgedDrawable);
    }

    @Test
    public void testGetAppIcon_uidConsumerWithNullIcon_returnDefaultActivityIcon()
            throws Exception {
        final BatteryDiffEntry entry = createBatteryDiffEntry(mMockDrawable);
        mockConstantState(mMockDrawable);

        entry.mAppIcon = null;
        assertThat(entry.getAppIcon()).isEqualTo(mMockDrawable);
        assertThat(BatteryDiffEntry.sResourceCache).hasSize(1);
        // Verifies the app label in the cache.
        final BatteryEntry.NameAndIcon nameAndIcon =
                BatteryDiffEntry.sResourceCache.get(entry.getKey());
        assertThat(nameAndIcon.mIcon).isEqualTo(mMockDrawable);
    }

    @Test
    public void testClearCache_clearDataForAllCaches() {
        BatteryDiffEntry.sResourceCache.put(
                "fake application key",
                new BatteryEntry.NameAndIcon("app label", null, /* iconId= */ 0));
        BatteryDiffEntry.sValidForRestriction.put("fake application key", Boolean.valueOf(false));
        BatteryDiffEntry.sPackageNameAndUidCache.put(PACKAGE_NAME, UID);

        BatteryDiffEntry.clearCache();

        assertThat(BatteryDiffEntry.sResourceCache).isEmpty();
        assertThat(BatteryDiffEntry.sValidForRestriction).isEmpty();
        assertThat(BatteryDiffEntry.sPackageNameAndUidCache).isEmpty();
    }

    @Test
    public void testClearCache_switchLocale_clearCacheIconAndLabel() throws Exception {
        final int userId = UserHandle.getUserId(1001);
        Locale.setDefault(new Locale("en_US"));
        final BatteryDiffEntry entry1 = createBatteryDiffEntry(mMockDrawable);
        mockConstantState(mMockDrawable);
        assertThat(entry1.getAppIcon()).isEqualTo(mMockDrawable);
        // Switch the locale into another one.
        Locale.setDefault(new Locale("zh_TW"));

        final BatteryDiffEntry entry2 = createBatteryDiffEntry(mMockDrawable2);

        // We should get new drawable without caching.
        mockConstantState(mMockDrawable2);
        assertThat(entry2.getAppIcon()).isEqualTo(mMockDrawable2);
        // Verifies the cache is updated into the new drawable.
        final BatteryEntry.NameAndIcon nameAndIcon =
                BatteryDiffEntry.sResourceCache.get(entry2.getKey());
        assertThat(nameAndIcon.mIcon).isEqualTo(mMockDrawable2);
    }

    @Test
    public void testIsSystemEntry_userBattery_returnTrue() {
        final BatteryDiffEntry entry =
                createBatteryDiffEntry(
                        ConvertUtils.CONSUMER_TYPE_USER_BATTERY,
                        /* uid= */ 0,
                        /* isHidden= */ false);
        assertThat(entry.isSystemEntry()).isTrue();
    }

    @Test
    public void testIsSystemEntry_systemBattery_returnTrue() {
        final BatteryDiffEntry entry =
                createBatteryDiffEntry(
                        ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY,
                        /* uid= */ 0,
                        /* isHidden= */ false);
        assertThat(entry.isSystemEntry()).isTrue();
    }

    @Test
    public void testIsSystemEntry_uidBattery_returnFalse() {
        final BatteryDiffEntry entry =
                createBatteryDiffEntry(
                        ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /* uid= */ 123,
                        /* isHidden= */ false);
        assertThat(entry.isSystemEntry()).isFalse();
    }

    @Test
    public void testIsSystemEntry_uidBatteryWithSystemProcess_returnFalse() {
        final BatteryDiffEntry entry =
                createBatteryDiffEntry(
                        ConvertUtils.CONSUMER_TYPE_UID_BATTERY,
                        /* uid= */ 1230,
                        /* isHidden= */ false);
        assertThat(entry.isSystemEntry()).isFalse();
    }

    @Test
    public void testIsUninstalledEntry_systemApp_returnFalse() {
        final BatteryDiffEntry entry =
                createBatteryDiffEntry(
                        ConvertUtils.CONSUMER_TYPE_SYSTEM_BATTERY,
                        /* uid= */ 0,
                        /* isHidden= */ false);
        assertThat(entry.isSystemEntry()).isTrue();
        assertThat(entry.isUninstalledEntry()).isFalse();
    }

    @Test
    public void testIsUninstalledEntry_installedApp_returnFalse() throws Exception {
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_UID, UID);
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, PACKAGE_NAME);
        final BatteryDiffEntry entry = createBatteryDiffEntry(10, new BatteryHistEntry(values));

        assertThat(entry.isSystemEntry()).isFalse();
        assertThat(BatteryDiffEntry.sPackageNameAndUidCache.containsKey(PACKAGE_NAME)).isFalse();
        assertThat(entry.isUninstalledEntry()).isFalse();
        assertThat(BatteryDiffEntry.sPackageNameAndUidCache.containsKey(PACKAGE_NAME)).isTrue();
        assertThat(BatteryDiffEntry.sPackageNameAndUidCache.get(PACKAGE_NAME)).isEqualTo(UID);
    }

    @Test
    public void testIsUninstalledEntry_uidZero_returnFalse() throws Exception {
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_UID, BatteryUtils.UID_ZERO);
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, PACKAGE_NAME);
        final BatteryDiffEntry entry = createBatteryDiffEntry(10, new BatteryHistEntry(values));

        assertThat(entry.isSystemEntry()).isFalse();
        assertThat(BatteryDiffEntry.sPackageNameAndUidCache.containsKey(PACKAGE_NAME)).isFalse();
        assertThat(entry.isUninstalledEntry()).isFalse();
        assertThat(BatteryDiffEntry.sPackageNameAndUidCache.containsKey(PACKAGE_NAME)).isFalse();
    }

    @Test
    public void testIsUninstalledEntry_uninstalledApp_returnTrue() throws Exception {
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_UID, UNINSTALLED_UID);
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, UNINSTALLED_PACKAGE_NAME);
        final BatteryDiffEntry entry = createBatteryDiffEntry(10, new BatteryHistEntry(values));

        assertThat(entry.isSystemEntry()).isFalse();
        assertThat(BatteryDiffEntry.sPackageNameAndUidCache.containsKey(UNINSTALLED_PACKAGE_NAME))
                .isFalse();
        assertThat(entry.isUninstalledEntry()).isTrue();
        assertThat(BatteryDiffEntry.sPackageNameAndUidCache.get(UNINSTALLED_PACKAGE_NAME))
                .isEqualTo(BatteryUtils.UID_NULL);
    }

    @Test
    public void testUpdateRestrictionFlagState_updateFlagAsExpected() throws Exception {
        final String expectedAppLabel = "fake app label";
        final String fakePackageName = "com.fake.google.com";
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_UID, /*invalid uid*/ 10001);
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, fakePackageName);
        final BatteryDiffEntry entry = createBatteryDiffEntry(10, new BatteryHistEntry(values));

        entry.updateRestrictionFlagState();
        // Sets false if the app entry cannot be found.
        assertThat(entry.mValidForRestriction).isFalse();

        doReturn(BatteryUtils.UID_NULL)
                .when(mMockPackageManager)
                .getPackageUid(entry.getPackageName(), PackageManager.GET_META_DATA);
        entry.updateRestrictionFlagState();
        // Sets false if the app is invalid package name.
        assertThat(entry.mValidForRestriction).isFalse();

        doReturn(1000)
                .when(mMockPackageManager)
                .getPackageUid(entry.getPackageName(), PackageManager.GET_META_DATA);
        entry.updateRestrictionFlagState();
        // Sets false if the app PackageInfo cannot be found.
        assertThat(entry.mValidForRestriction).isFalse();

        doReturn(mMockPackageInfo)
                .when(mMockPackageManager)
                .getPackageInfo(eq(entry.getPackageName()), anyInt());
        entry.updateRestrictionFlagState();
        // Sets true if package is valid and PackageInfo can be found.
        assertThat(entry.mValidForRestriction).isTrue();
    }

    @Test
    public void testGetPackageName_returnExpectedResult() {
        final String expectedPackageName = "com.fake.google.com";
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, expectedPackageName);
        final BatteryDiffEntry entry = createBatteryDiffEntry(10, new BatteryHistEntry(values));

        assertThat(entry.getPackageName()).isEqualTo(expectedPackageName);
    }

    @Test
    public void testGetPackageName_withProcessName_returnExpectedResult() {
        final String expectedPackageName = "com.fake.google.com";
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, expectedPackageName + ":privileged_process0");
        final BatteryDiffEntry entry = createBatteryDiffEntry(10, new BatteryHistEntry(values));

        assertThat(entry.getPackageName()).isEqualTo(expectedPackageName);
    }

    private BatteryDiffEntry createBatteryDiffEntry(int consumerType, long uid, boolean isHidden) {
        return new BatteryDiffEntry(
                mContext,
                /* uid= */ uid,
                /* userId= */ 0,
                /* key= */ "key",
                /* isHidden= */ isHidden,
                /* componentId= */ -1,
                /* legacyPackageName= */ null,
                /* legacyLabel= */ null,
                /*consumerType*/ consumerType,
                /* foregroundUsageTimeInMs= */ 0,
                /* foregroundServiceUsageTimeInMs= */ 0,
                /* backgroundUsageTimeInMs= */ 0,
                /* screenOnTimeInMs= */ 0,
                /* consumePower= */ 0,
                /* foregroundUsageConsumePower= */ 0,
                /* foregroundServiceUsageConsumePower= */ 0,
                /* backgroundUsageConsumePower= */ 0,
                /* cachedUsageConsumePower= */ 0);
    }

    private BatteryDiffEntry createBatteryDiffEntry(
            double consumePower, BatteryHistEntry batteryHistEntry) {
        final BatteryDiffEntry entry =
                new BatteryDiffEntry(
                        mContext,
                        batteryHistEntry.mUid,
                        batteryHistEntry.mUserId,
                        batteryHistEntry.getKey(),
                        batteryHistEntry.mIsHidden,
                        batteryHistEntry.mDrainType,
                        batteryHistEntry.mPackageName,
                        batteryHistEntry.mAppLabel,
                        batteryHistEntry.mConsumerType,
                        /* foregroundUsageTimeInMs= */ 0,
                        /* backgroundUsageTimeInMs= */ 0,
                        /* foregroundServiceUsageTimeInMs= */ 0,
                        /* screenOnTimeInMs= */ 0,
                        consumePower,
                        /* foregroundUsageConsumePower= */ 0,
                        /* foregroundServiceUsageConsumePower= */ 0,
                        /* backgroundUsageConsumePower= */ 0,
                        /* cachedUsageConsumePower= */ 0);
        entry.setTotalConsumePower(100.0);
        return entry;
    }

    private static ContentValues getContentValuesWithType(int consumerType) {
        final ContentValues values = new ContentValues();
        values.put(BatteryHistEntry.KEY_CONSUMER_TYPE, Integer.valueOf(consumerType));
        return values;
    }

    private BatteryDiffEntry createBatteryDiffEntry(Drawable drawable) throws Exception {
        final ContentValues values =
                getContentValuesWithType(ConvertUtils.CONSUMER_TYPE_UID_BATTERY);
        values.put(BatteryHistEntry.KEY_UID, 1001);
        values.put(BatteryHistEntry.KEY_PACKAGE_NAME, "com.a.b.c");
        final BatteryHistEntry batteryHistEntry = new BatteryHistEntry(values);
        doReturn(drawable).when(mMockPackageManager).getDefaultActivityIcon();
        doReturn(null).when(mMockPackageManager).getApplicationInfo("com.a.b.c", 0);
        doReturn(new String[] {"com.a.b.c"}).when(mMockPackageManager).getPackagesForUid(1001);
        return createBatteryDiffEntry(10, batteryHistEntry);
    }

    private void mockConstantState(Drawable drawable) {
        doReturn(mMockConstantState).when(drawable).getConstantState();
        doReturn(drawable).when(mMockConstantState).newDrawable();
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
