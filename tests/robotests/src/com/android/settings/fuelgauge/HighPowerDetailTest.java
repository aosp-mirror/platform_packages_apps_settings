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
import com.android.settingslib.fuelgauge.PowerAllowlistBackend;

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
    @Mock private PowerAllowlistBackend mPowerAllowlistBackend;
    @Mock private BatteryUtils mBatteryUtils;

    @Before
    public void setUp() {
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFragment = spy(new HighPowerDetail());
        mFragment.mBackend = mPowerAllowlistBackend;
        mFragment.mBatteryUtils = mBatteryUtils;
        mFragment.mPackageUid = TEST_UID;
        mFragment.mPackageName = TEST_PACKAGE;
    }

    @Test
    public void logSpecialPermissionChange() {
        // Deny means app is allowlisted to opt out of power save restrictions
        HighPowerDetail.logSpecialPermissionChange(true, "app", RuntimeEnvironment.application);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_BATTERY_DENY),
                        eq("app"));

        // Allow means app is NOT allowlisted to opt out of power save restrictions
        HighPowerDetail.logSpecialPermissionChange(false, "app", RuntimeEnvironment.application);
        verify(mFeatureFactory.metricsFeatureProvider)
                .action(
                        any(Context.class),
                        eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_BATTERY_ALLOW),
                        eq("app"));
    }

    @Test
    public void onClick_appAddedToDozeAllowlist_getsUnrestricted() {
        mFragment.mIsEnabled = true;
        when(mPowerAllowlistBackend.isAllowlisted(TEST_PACKAGE, TEST_UID)).thenReturn(false);
        mFragment.onClick(null, DialogInterface.BUTTON_POSITIVE);
        verify(mBatteryUtils)
                .setForceAppStandby(TEST_UID, TEST_PACKAGE, AppOpsManager.MODE_ALLOWED);
    }

    @Test
    public void getSummary_defaultActivePackage_returnUnavailable() {
        doReturn(true).when(mPowerAllowlistBackend).isDefaultActiveApp(TEST_PACKAGE, TEST_UID);

        assertThat(
                        HighPowerDetail.getSummary(
                                mContext, mPowerAllowlistBackend, TEST_PACKAGE, TEST_UID))
                .isEqualTo(mContext.getString(R.string.high_power_system));
    }
}
