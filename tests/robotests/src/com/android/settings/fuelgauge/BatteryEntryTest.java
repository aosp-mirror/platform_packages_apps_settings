/*
 * Copyright (C) 2016 The Android Open Source Project
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.BatteryConsumer;
import android.os.Handler;
import android.os.Process;
import android.os.UidBatteryConsumer;
import android.os.UserBatteryConsumer;
import android.os.UserManager;

import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryEntry.NameAndIcon;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

@RunWith(RobolectricTestRunner.class)
public class BatteryEntryTest {

    private static final int APP_UID = 123;
    private static final int SYSTEM_UID = Process.SYSTEM_UID;
    private static final String APP_DEFAULT_PACKAGE_NAME = "com.android.test";
    private static final String LABEL_PREFIX = "Label for ";
    private static final String HIGH_DRAIN_PACKAGE = "com.android.test.screen";
    private static final String ANDROID_PACKAGE = "android";

    @Rule public MockitoRule mocks = MockitoJUnit.rule();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mMockContext;
    private Context mContext;
    @Mock private Handler mockHandler;
    @Mock private PackageManager mockPackageManager;
    @Mock private UserManager mockUserManager;
    @Mock private UidBatteryConsumer mUidBatteryConsumer;

    @Before
    public void stubContextToReturnMockPackageManager() {
        mContext = spy(RuntimeEnvironment.application);
        when(mMockContext.getPackageManager()).thenReturn(mockPackageManager);
    }

    @Before
    public void stubPackageManagerToReturnAppPackageAndName() throws NameNotFoundException {
        when(mockPackageManager.getApplicationInfo(anyString(), eq(0) /* no flags */))
                .thenAnswer(invocation -> {
                    ApplicationInfo info = new ApplicationInfo();
                    info.packageName = invocation.getArgument(0);
                    return info;
                });
        when(mockPackageManager.getApplicationLabel(any(ApplicationInfo.class)))
                .thenAnswer(invocation -> LABEL_PREFIX
                        + ((ApplicationInfo) invocation.getArgument(0)).packageName);
    }

    private BatteryEntry createBatteryEntryForApp(String[] packages, String packageName,
            String highDrainPackage) {
        UidBatteryConsumer consumer = mock(UidBatteryConsumer.class);
        when(consumer.getUid()).thenReturn(APP_UID);
        when(consumer.getPackageWithHighestDrain()).thenReturn(highDrainPackage);
        return new BatteryEntry(mMockContext, mockHandler, mockUserManager,
                consumer, false, APP_UID, packages, packageName);
    }

    private BatteryEntry createAggregateBatteryEntry(int powerComponentId) {
        return new BatteryEntry(mMockContext, powerComponentId, 200, 100, 1000);
    }

    private BatteryEntry createCustomAggregateBatteryEntry(int powerComponentId) {
        return new BatteryEntry(mMockContext, powerComponentId, "CUSTOM", 200, 100);
    }

    private BatteryEntry createUserBatteryConsumer(int userId) {
        UserBatteryConsumer consumer = mock(UserBatteryConsumer.class);
        when(consumer.getUserId()).thenReturn(userId);
        return new BatteryEntry(mMockContext, mockHandler, mockUserManager,
                consumer, false, 0, null, null);
    }

    @Test
    public void batteryEntryForApp_shouldSetDefaultPackageNameAndLabel() throws Exception {
        BatteryEntry entry = createBatteryEntryForApp(null, APP_DEFAULT_PACKAGE_NAME,
                HIGH_DRAIN_PACKAGE);

        assertThat(entry.getDefaultPackageName()).isEqualTo(APP_DEFAULT_PACKAGE_NAME);
        assertThat(entry.getLabel()).isEqualTo(LABEL_PREFIX + APP_DEFAULT_PACKAGE_NAME);
    }

    @Test
    public void batteryEntryForApp_shouldSetLabelAsPackageName_whenPackageCannotBeFound()
            throws Exception {
        when(mockPackageManager.getApplicationInfo(APP_DEFAULT_PACKAGE_NAME, 0 /* no flags */))
                .thenThrow(new NameNotFoundException());

        BatteryEntry entry = createBatteryEntryForApp(null, APP_DEFAULT_PACKAGE_NAME, null);

        assertThat(entry.getLabel()).isEqualTo(APP_DEFAULT_PACKAGE_NAME);
    }

    @Test
    public void batteryEntryForApp_shouldSetHighestDrainPackage_whenPackagesCannotBeFoundForUid() {
        when(mockPackageManager.getPackagesForUid(APP_UID)).thenReturn(null);

        BatteryEntry entry = createBatteryEntryForApp(null, null, HIGH_DRAIN_PACKAGE);

        assertThat(entry.getLabel()).isEqualTo(LABEL_PREFIX + HIGH_DRAIN_PACKAGE);
    }

    @Test
    public void batteryEntryForApp_shouldSetHighestDrainPackage_whenMultiplePackagesFoundForUid() {
        BatteryEntry entry = createBatteryEntryForApp(
                new String[] {APP_DEFAULT_PACKAGE_NAME, "package2", "package3"}, null,
                HIGH_DRAIN_PACKAGE);

        assertThat(entry.getLabel()).isEqualTo(LABEL_PREFIX + HIGH_DRAIN_PACKAGE);
    }

    @Test
    public void batteryEntryForAOD_containCorrectInfo() {
        final BatteryEntry entry = new BatteryEntry(RuntimeEnvironment.application,
                BatteryConsumer.POWER_COMPONENT_AMBIENT_DISPLAY, 200, 100, 1000);

        assertThat(entry.iconId).isEqualTo(R.drawable.ic_settings_aod);
        assertThat(entry.name).isEqualTo("Ambient display");
    }

    @Test
    public void batteryEntryForCustomComponent_containCorrectInfo() {
        final BatteryEntry entry = new BatteryEntry(RuntimeEnvironment.application,
                BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID + 42, "ABC", 200, 100);

        assertThat(entry.iconId).isEqualTo(R.drawable.ic_power_system);
        assertThat(entry.name).isEqualTo("ABC");
    }

    @Test
    public void getTimeInForegroundMs_app() {
        when(mUidBatteryConsumer.getTimeInStateMs(UidBatteryConsumer.STATE_FOREGROUND))
                .thenReturn(100L);

        final BatteryEntry entry = new BatteryEntry(RuntimeEnvironment.application, mockHandler,
                mockUserManager, mUidBatteryConsumer, false, 0, null, null);

        assertThat(entry.getTimeInForegroundMs()).isEqualTo(100L);
    }

    @Test
    public void getTimeInForegroundMs_aggregateBatteryConsumer() {
        final BatteryEntry entry = new BatteryEntry(RuntimeEnvironment.application,
                BatteryConsumer.POWER_COMPONENT_BLUETOOTH, 10, 20, 100);

        assertThat(entry.getTimeInForegroundMs()).isEqualTo(100L);
    }

    @Test
    public void getTimeInBackgroundMs_app() {
        when(mUidBatteryConsumer.getTimeInStateMs(UidBatteryConsumer.STATE_BACKGROUND))
                .thenReturn(100L);

        final BatteryEntry entry = new BatteryEntry(RuntimeEnvironment.application, mockHandler,
                mockUserManager, mUidBatteryConsumer, false, 0, null, null);

        assertThat(entry.getTimeInBackgroundMs()).isEqualTo(100L);
    }

    @Test
    public void getTimeInBackgroundMs_systemConsumer() {
        final BatteryEntry entry = new BatteryEntry(RuntimeEnvironment.application,
                BatteryConsumer.POWER_COMPONENT_BLUETOOTH, 100, 200, 1000);

        assertThat(entry.getTimeInBackgroundMs()).isEqualTo(0);
    }

    @Test
    public void testUidCache_switchLocale_shouldCleanCache() {
        BatteryEntry.stopRequestQueue();

        Locale.setDefault(new Locale("en_US"));
        BatteryEntry.sUidCache.put(Integer.toString(APP_UID), null);
        assertThat(BatteryEntry.sUidCache).isNotEmpty();

        Locale.setDefault(new Locale("zh_TW"));
        createBatteryEntryForApp(null, null, HIGH_DRAIN_PACKAGE);
        assertThat(BatteryEntry.sUidCache).isEmpty(); // check if cache is clear
    }

    @Test
    public void getKey_UidBatteryConsumer() {
        final BatteryEntry entry = createBatteryEntryForApp(null, null, null);
        final String key = entry.getKey();
        assertThat(key).isEqualTo("123");
    }

    @Test
    public void getKey_AggregateBatteryConsumer_returnComponentId() {
        final BatteryEntry entry = createAggregateBatteryEntry(
                BatteryConsumer.POWER_COMPONENT_BLUETOOTH);
        final String key = entry.getKey();
        assertThat(key).isEqualTo("S|2");
    }

    @Test
    public void getKey_CustomAggregateBatteryConsumer_returnComponentId() {
        final BatteryEntry entry = createCustomAggregateBatteryEntry(
                BatteryConsumer.FIRST_CUSTOM_POWER_COMPONENT_ID + 42);
        final String key = entry.getKey();
        assertThat(key).isEqualTo("S|1042");
    }

    @Test
    public void getKey_UserBatteryConsumer_returnUserId() {
        doReturn(mockUserManager).when(mMockContext).getSystemService(UserManager.class);
        final BatteryEntry entry = createUserBatteryConsumer(2);
        final String key = entry.getKey();
        assertThat(key).isEqualTo("U|2");
    }

    @Test
    public void getNameAndIconFromUserId_nullUserInfo_returnDefaultNameAndIcon() {
        final int userId = 1001;
        doReturn(mockUserManager).when(mContext).getSystemService(UserManager.class);
        doReturn(null).when(mockUserManager).getUserInfo(userId);

        final NameAndIcon nameAndIcon = BatteryEntry.getNameAndIconFromUserId(
                mContext, userId);
        assertThat(nameAndIcon.name).isEqualTo(getString(
                R.string.running_process_item_removed_user_label));
        assertThat(nameAndIcon.icon).isNull();
    }

    @Test
    public void getNameAndIconFromUid_rerturnExpectedName() {
        final NameAndIcon nameAndIcon = BatteryEntry.getNameAndIconFromUid(
                mContext, /* name */ null, /* uid */ 0);
        assertThat(nameAndIcon.name).isEqualTo(getString(R.string.process_kernel_label));

        assertNameAndIcon("mediaserver", R.string.process_mediaserver_label);
        assertNameAndIcon("dex2oat32", R.string.process_dex2oat_label);
        assertNameAndIcon("dex2oat64", R.string.process_dex2oat_label);
        assertNameAndIcon("dex2oat", R.string.process_dex2oat_label);
    }

    @Test
    public void getNameAndIconFromPowerComponent_rerturnExpectedNameAndIcon() {
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_AMBIENT_DISPLAY,
                R.string.ambient_display_screen_title,
                R.drawable.ic_settings_aod);
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_BLUETOOTH,
                R.string.power_bluetooth,
                com.android.internal.R.drawable.ic_settings_bluetooth);
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_CAMERA,
                R.string.power_camera,
                R.drawable.ic_settings_camera);
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_MOBILE_RADIO,
                R.string.power_cell,
                R.drawable.ic_cellular_1_bar);
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_FLASHLIGHT,
                R.string.power_flashlight,
                R.drawable.ic_settings_display);
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_PHONE,
                R.string.power_phone,
                R.drawable.ic_settings_voice_calls);
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_SCREEN,
                R.string.power_screen,
                R.drawable.ic_settings_display);
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_WIFI,
                R.string.power_wifi,
                R.drawable.ic_settings_wireless);
        assertNameAndIcon(BatteryConsumer.POWER_COMPONENT_IDLE,
                R.string.power_idle,
                R.drawable.ic_settings_phone_idle);
    }

    private void assertNameAndIcon(String name, int stringId) {
        final NameAndIcon nameAndIcon = BatteryEntry.getNameAndIconFromUid(
                mContext, name, /* uid */ 1000);
        assertThat(nameAndIcon.name).isEqualTo(getString(stringId));
    }

    private void assertNameAndIcon(int powerComponentId, int stringId, int iconId) {
        final NameAndIcon nameAndIcon = BatteryEntry.getNameAndIconFromPowerComponent(
                mContext, powerComponentId);
        assertThat(nameAndIcon.name).isEqualTo(getString(stringId));
        assertThat(nameAndIcon.iconId).isEqualTo(iconId);
    }

    private String getString(int stringId) {
        return mContext.getResources().getString(stringId);
    }
}
