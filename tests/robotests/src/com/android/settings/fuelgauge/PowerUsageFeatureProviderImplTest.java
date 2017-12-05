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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;
import com.android.internal.os.BatterySipper;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.when;
import static com.google.common.truth.Truth.assertThat;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class PowerUsageFeatureProviderImplTest {
    private static final int UID_OTHER = Process.FIRST_APPLICATION_UID + 2;
    private static final int UID_CALENDAR = Process.FIRST_APPLICATION_UID + 3;
    private static final int UID_MEDIA = Process.FIRST_APPLICATION_UID + 4;
    private static final int UID_SYSTEMUI = Process.FIRST_APPLICATION_UID + 5;
    private static final String[] PACKAGES_CALENDAR = {"com.android.providers.calendar"};
    private static final String[] PACKAGES_MEDIA = {"com.android.providers.media"};
    private static final String[] PACKAGES_SYSTEMUI = {"com.android.systemui"};
    @Mock
    private Context mContext;
    @Mock
    private BatterySipper mBatterySipper;
    @Mock
    private PackageManager mPackageManager;
    private PowerUsageFeatureProviderImpl mPowerFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mPowerFeatureProvider = new PowerUsageFeatureProviderImpl(mContext);
        when(mPackageManager.getPackagesForUid(UID_CALENDAR)).thenReturn(PACKAGES_CALENDAR);
        when(mPackageManager.getPackagesForUid(UID_MEDIA)).thenReturn(PACKAGES_MEDIA);
        when(mPackageManager.getPackagesForUid(UID_SYSTEMUI)).thenReturn(PACKAGES_SYSTEMUI);
        mPowerFeatureProvider.mPackageManager = mPackageManager;
        mBatterySipper.uidObj = new FakeUid(UID_OTHER);
    }

    @Test
    public void testIsTypeSystem_uidRoot_returnTrue() {
        mBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mBatterySipper.getUid()).thenReturn(Process.ROOT_UID);

        assertThat(mPowerFeatureProvider.isTypeSystem(mBatterySipper)).isTrue();
    }

    @Test
    public void testIsTypeSystem_uidSystem_returnTrue() {
        mBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mBatterySipper.getUid()).thenReturn(Process.SYSTEM_UID);

        assertThat(mPowerFeatureProvider.isTypeSystem(mBatterySipper)).isTrue();
    }

    @Test
    public void testIsTypeSystem_uidMedia_returnTrue() {
        mBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mBatterySipper.getUid()).thenReturn(Process.MEDIA_UID);

        assertThat(mPowerFeatureProvider.isTypeSystem(mBatterySipper)).isTrue();
    }

    @Test
    public void testIsTypeSystem_appCalendar_returnTrue() {
        mBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mBatterySipper.getUid()).thenReturn(UID_CALENDAR);

        assertThat(mPowerFeatureProvider.isTypeSystem(mBatterySipper)).isTrue();
    }

    @Test
    public void testIsTypeSystem_appMedia_returnTrue() {
        mBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mBatterySipper.getUid()).thenReturn(UID_MEDIA);

        assertThat(mPowerFeatureProvider.isTypeSystem(mBatterySipper)).isTrue();
    }

    @Test
    public void testIsTypeSystem_appSystemUi_returnTrue() {
        mBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mBatterySipper.getUid()).thenReturn(UID_SYSTEMUI);

        assertThat(mPowerFeatureProvider.isTypeSystem(mBatterySipper)).isTrue();
    }

    @Test
    public void testIsTypeSystem_uidOther_returnFalse() {
        mBatterySipper.drainType = BatterySipper.DrainType.APP;
        when(mBatterySipper.getUid()).thenReturn(UID_OTHER);

        assertThat(mPowerFeatureProvider.isTypeSystem(mBatterySipper)).isFalse();
    }

    @Test
    public void testIsTypeSystem_uidObjNull_returnFalse() {
        mBatterySipper.drainType = BatterySipper.DrainType.APP;
        mBatterySipper.uidObj = null;

        assertThat(mPowerFeatureProvider.isTypeSystem(mBatterySipper)).isFalse();
    }

    @Test
    public void testIsAdvancedUiEnabled_returnTrue() {
        assertThat(mPowerFeatureProvider.isAdvancedUiEnabled()).isTrue();
    }

    @Test
    public void testIsPowerAccountingToggleEnabled_returnTrue() {
        assertThat(mPowerFeatureProvider.isPowerAccountingToggleEnabled()).isTrue();
    }
}
