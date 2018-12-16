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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Handler;
import android.os.Process;
import android.os.UserManager;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.settings.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private static final String APP_LABEL = "Test App Name";
    private static final String HIGH_DRAIN_PACKAGE = "com.android.test.screen";
    private static final String ANDROID_PACKAGE = "android";
    private static final String[] SYSTEM_PACKAGES = {HIGH_DRAIN_PACKAGE, ANDROID_PACKAGE};

    @Rule public MockitoRule mocks = MockitoJUnit.rule();

    @Mock private Context mockContext;
    @Mock private Handler mockHandler;
    @Mock private PackageManager mockPackageManager;
    @Mock private UserManager mockUserManager;

    @Before
    public void stubContextToReturnMockPackageManager() {
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);
    }

    @Before
    public void stubPackageManagerToReturnAppPackageAndName() throws NameNotFoundException {
        when(mockPackageManager.getPackagesForUid(APP_UID))
            .thenReturn(new String[] {APP_DEFAULT_PACKAGE_NAME});

        ApplicationInfo appInfo = mock(ApplicationInfo.class);
        when(mockPackageManager.getApplicationInfo(APP_DEFAULT_PACKAGE_NAME, 0 /* no flags */))
            .thenReturn(appInfo);
        when(mockPackageManager.getApplicationLabel(appInfo)).thenReturn(APP_LABEL);
    }

    private BatteryEntry createBatteryEntryForApp() {
        return new BatteryEntry(mockContext, mockHandler, mockUserManager, createSipperForApp());
    }

    private BatterySipper createSipperForApp() {
        BatterySipper sipper =
            new BatterySipper(DrainType.APP, new FakeUid(APP_UID), 0 /* power use */);
        sipper.packageWithHighestDrain = HIGH_DRAIN_PACKAGE;
        return sipper;
    }

    private BatteryEntry createBatteryEntryForSystem() {
        return new BatteryEntry(mockContext, mockHandler, mockUserManager, createSipperForSystem());
    }

    private BatterySipper createSipperForSystem() {
        BatterySipper sipper =
                new BatterySipper(DrainType.APP, new FakeUid(SYSTEM_UID), 0 /* power use */);
        sipper.packageWithHighestDrain = HIGH_DRAIN_PACKAGE;
        sipper.mPackages = SYSTEM_PACKAGES;
        return sipper;
    }

    @Test
    public void batteryEntryForApp_shouldSetDefaultPackageNameAndLabel() throws Exception {
        BatteryEntry entry = createBatteryEntryForApp();

        assertThat(entry.defaultPackageName).isEqualTo(APP_DEFAULT_PACKAGE_NAME);
        assertThat(entry.getLabel()).isEqualTo(APP_LABEL);
    }

    @Test
    public void batteryEntryForApp_shouldSetLabelAsPackageName_whenPackageCannotBeFound()
        throws Exception {
      when(mockPackageManager.getApplicationInfo(APP_DEFAULT_PACKAGE_NAME, 0 /* no flags */))
          .thenThrow(new NameNotFoundException());

      BatteryEntry entry = createBatteryEntryForApp();

      assertThat(entry.getLabel()).isEqualTo(APP_DEFAULT_PACKAGE_NAME);
    }

    @Test
    public void batteryEntryForApp_shouldSetHighestDrainPackage_whenPackagesCannotBeFoundForUid() {
        when(mockPackageManager.getPackagesForUid(APP_UID)).thenReturn(null);

        BatteryEntry entry = createBatteryEntryForApp();

        assertThat(entry.getLabel()).isEqualTo(HIGH_DRAIN_PACKAGE);
    }

    @Test
    public void batteryEntryForApp_shouldSetHighestDrainPackage_whenMultiplePackagesFoundForUid() {
        when(mockPackageManager.getPackagesForUid(APP_UID))
            .thenReturn(new String[] {APP_DEFAULT_PACKAGE_NAME, "package2", "package3"});

        BatteryEntry entry = createBatteryEntryForApp();

        assertThat(entry.getLabel()).isEqualTo(HIGH_DRAIN_PACKAGE);
    }

    @Test
    public void batteryEntryForAOD_containCorrectInfo() {
        final BatterySipper batterySipper = mock(BatterySipper.class);
        batterySipper.drainType = DrainType.AMBIENT_DISPLAY;
        final BatteryEntry entry = new BatteryEntry(RuntimeEnvironment.application, mockHandler,
                mockUserManager, batterySipper);

        assertThat(entry.iconId).isEqualTo(R.drawable.ic_settings_aod);
        assertThat(entry.name).isEqualTo("Ambient display");
    }

    @Test
    public void extractPackageFromSipper_systemSipper_returnSystemPackage() {
        BatteryEntry entry = createBatteryEntryForSystem();

        assertThat(entry.extractPackagesFromSipper(entry.sipper))
            .isEqualTo(new String[] {ANDROID_PACKAGE});
    }

    @Test
    public void extractPackageFromSipper_normalSipper_returnDefaultPackage() {
        BatteryEntry entry = createBatteryEntryForApp();

        assertThat(entry.extractPackagesFromSipper(entry.sipper)).isEqualTo(entry.sipper.mPackages);
    }

    @Test
    public void testUidCache_switchLocale_shouldCleanCache() {
        BatteryEntry.stopRequestQueue();

        Locale.setDefault(new Locale("en_US"));
        BatteryEntry.sUidCache.put(Integer.toString(APP_UID), null);
        assertThat(BatteryEntry.sUidCache).isNotEmpty();

        Locale.setDefault(new Locale("zh_TW"));
        createBatteryEntryForApp();
        assertThat(BatteryEntry.sUidCache).isEmpty(); // check if cache is clear
    }
}
