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
 * limitations under the License
 */

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.fuelgauge.PowerWhitelistBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class HighPowerDetailTest {
    private static final int TEST_UID = 12000;
    private static final String TEST_PACKAGE = "com.test.package";

    private FakeFeatureFactory mFeatureFactory;
    private HighPowerDetail mFragment;

    private Context mContext;
    @Mock
    private PowerWhitelistBackend mPowerWhitelistBackend;
    @Mock
    private BatteryUtils mBatteryUtils;

    @Before
    public void setUp() {
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new HighPowerDetail());
        mFragment.mBackend = mPowerWhitelistBackend;
        mFragment.mBatteryUtils = mBatteryUtils;
        mFragment.mPackageUid = TEST_UID;
        mFragment.mPackageName = TEST_PACKAGE;
    }

    @Test
    public void logSpecialPermissionChange() {
        // Deny means app is whitelisted to opt out of power save restrictions
        HighPowerDetail.logSpecialPermissionChange(true, "app", RuntimeEnvironment.application);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_BATTERY_DENY), eq("app"));

        // Allow means app is NOT whitelisted to opt out of power save restrictions
        HighPowerDetail.logSpecialPermissionChange(false, "app", RuntimeEnvironment.application);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_BATTERY_ALLOW), eq("app"));
    }

    @Test
    public void onClick_appAddedToDozeWhitelist_getsUnrestricted() {
        mFragment.mIsEnabled = true;
        when(mPowerWhitelistBackend.isWhitelisted(TEST_PACKAGE)).thenReturn(false);
        mFragment.onClick(null, DialogInterface.BUTTON_POSITIVE);
        verify(mBatteryUtils).setForceAppStandby(TEST_UID, TEST_PACKAGE,
                AppOpsManager.MODE_ALLOWED);
    }

    @Test
    public void getSummary_defaultActivePackage_returnUnavailable() {
        doReturn(true).when(mPowerWhitelistBackend).isDefaultActiveApp(TEST_PACKAGE);

        assertThat(HighPowerDetail.getSummary(mContext, mPowerWhitelistBackend, TEST_PACKAGE))
                .isEqualTo(mContext.getString(R.string.high_power_system));
    }
}
