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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.TestConfig;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class HighPowerDetailTest {

    private Context mContext;

    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void logSpecialPermissionChange() {
        // Deny means app is whitelisted to opt out of power save restrictions
        HighPowerDetail.logSpecialPermissionChange(true, "app", mContext);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_BATTERY_DENY), eq("app"));

        // Allow means app is NOT whitelisted to opt out of power save restrictions
        HighPowerDetail.logSpecialPermissionChange(false, "app", mContext);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_BATTERY_ALLOW), eq("app"));
    }
}
