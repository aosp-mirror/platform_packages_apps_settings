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

package com.android.settings;

import android.content.Context;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class DeviceAdminAddTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private FakeFeatureFactory mFeatureFactory;
    private DeviceAdminAdd mDeviceAdminAdd;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mFeatureFactory = (FakeFeatureFactory) FakeFeatureFactory.getFactory(mContext);
        mDeviceAdminAdd = Robolectric.buildActivity(DeviceAdminAdd.class).get();
    }

    @Test
    public void logSpecialPermissionChange() {
        mDeviceAdminAdd.logSpecialPermissionChange(true, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_ADMIN_ALLOW), eq("app"));

        mDeviceAdminAdd.logSpecialPermissionChange(false, "app");
        verify(mFeatureFactory.metricsFeatureProvider).action(any(Context.class),
                eq(MetricsProto.MetricsEvent.APP_SPECIAL_PERMISSION_ADMIN_DENY), eq("app"));
    }
}
