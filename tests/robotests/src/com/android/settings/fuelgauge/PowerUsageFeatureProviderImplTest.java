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

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PowerUsageFeatureProviderImplTest {

    private static final int UID_OTHER = Process.FIRST_APPLICATION_UID + 2;
    private static final int UID_CALENDAR = Process.FIRST_APPLICATION_UID + 3;
    private static final int UID_MEDIA = Process.FIRST_APPLICATION_UID + 4;
    private static final int UID_SYSTEMUI = Process.FIRST_APPLICATION_UID + 5;
    private static final String[] PACKAGES_CALENDAR = {"com.android.providers.calendar"};
    private static final String[] PACKAGES_MEDIA = {"com.android.providers.media"};
    private static final String[] PACKAGES_SYSTEMUI = {"com.android.systemui"};

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;

    @Mock private PackageManager mPackageManager;
    private PowerUsageFeatureProviderImpl mPowerFeatureProvider;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getApplicationContext()).thenReturn(mContext);
        mPowerFeatureProvider = new PowerUsageFeatureProviderImpl(mContext);
        when(mPackageManager.getPackagesForUid(UID_CALENDAR)).thenReturn(PACKAGES_CALENDAR);
        when(mPackageManager.getPackagesForUid(UID_MEDIA)).thenReturn(PACKAGES_MEDIA);
        when(mPackageManager.getPackagesForUid(UID_SYSTEMUI)).thenReturn(PACKAGES_SYSTEMUI);
        mPowerFeatureProvider.mPackageManager = mPackageManager;
    }

    @Test
    public void testIsBatteryUsageEnabled_returnFalse() {
        assertThat(mPowerFeatureProvider.isBatteryUsageEnabled()).isTrue();
    }

    @Test
    public void testIsBatteryTipsEnabled_returnFalse() {
        assertThat(mPowerFeatureProvider.isBatteryTipsEnabled()).isFalse();
    }

    @Test
    public void testGetBatteryUsageListConsumePowerThreshold_return0() {
        assertThat(mPowerFeatureProvider.getBatteryUsageListConsumePowerThreshold()).isEqualTo(0.0);
    }

    @Test
    public void testIsTypeSystem_uidRoot_returnTrue() {
        assertThat(mPowerFeatureProvider.isTypeSystem(Process.ROOT_UID, null)).isTrue();
    }

    @Test
    public void testIsTypeSystem_uidSystem_returnTrue() {
        assertThat(mPowerFeatureProvider.isTypeSystem(Process.SYSTEM_UID, null)).isTrue();
    }

    @Test
    public void testIsTypeSystem_uidMedia_returnTrue() {
        assertThat(mPowerFeatureProvider.isTypeSystem(Process.MEDIA_UID, null)).isTrue();
    }

    @Test
    @Ignore
    public void testIsTypeSystem_appCalendar_returnTrue() {
        assertThat(mPowerFeatureProvider.isTypeSystem(UID_CALENDAR, null)).isTrue();
    }

    @Test
    @Ignore
    public void testIsTypeSystem_appMedia_returnTrue() {
        assertThat(mPowerFeatureProvider.isTypeSystem(UID_MEDIA, null)).isTrue();
    }

    @Test
    @Ignore
    public void testIsTypeSystem_appSystemUi_returnTrue() {
        assertThat(mPowerFeatureProvider.isTypeSystem(UID_SYSTEMUI, null)).isTrue();
    }

    @Test
    public void testIsTypeSystem_uidOther_returnFalse() {
        assertThat(mPowerFeatureProvider.isTypeSystem(UID_OTHER, null)).isFalse();
    }

    @Test
    public void testIsSmartBatterySupported_smartBatterySupported_returnTrue() {
        when(mContext.getResources()
                        .getBoolean(com.android.internal.R.bool.config_smart_battery_available))
                .thenReturn(true);

        assertThat(mPowerFeatureProvider.isSmartBatterySupported()).isTrue();
    }

    @Test
    public void testIsSmartBatterySupported_smartBatteryNotSupported_returnFalse() {
        when(mContext.getResources()
                        .getBoolean(com.android.internal.R.bool.config_smart_battery_available))
                .thenReturn(false);

        assertThat(mPowerFeatureProvider.isSmartBatterySupported()).isFalse();
    }

    @Test
    public void testGetResumeChargeIntentWithoutDockDefender_returnNull() {
        assertThat(mPowerFeatureProvider.getResumeChargeIntent(false)).isNull();
    }

    @Test
    public void testGetResumeChargeIntentWithDockDefender_returnNull() {
        assertThat(mPowerFeatureProvider.getResumeChargeIntent(true)).isNull();
    }
}
