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

package com.android.settings.fuelgauge.anomaly.action;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import com.android.settings.TestConfig;
import com.android.settings.fuelgauge.anomaly.Anomaly;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowPermissionChecker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION, shadows =
        ShadowPermissionChecker.class)
public class LocationCheckActionTest {
    private static final String PACKAGE_NAME = "com.android.app";
    private static final int UID = 12345;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Context mContext;
    private Anomaly mAnomaly;
    private LocationCheckAction mLocationCheckAction;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest(mContext);
        mLocationCheckAction = new LocationCheckAction(mContext, null);
        mAnomaly = new Anomaly.Builder()
                .setType(Anomaly.AnomalyType.BLUETOOTH_SCAN)
                .setPackageName(PACKAGE_NAME)
                .setUid(UID)
                .build();
        ShadowPermissionChecker.clear();
    }

    @Test
    public void testIsActionActive_coarseLocationGranted_returnTrue() {
        ShadowPermissionChecker.addPermission(Manifest.permission.ACCESS_COARSE_LOCATION, -1, UID,
                PACKAGE_NAME, PackageManager.PERMISSION_GRANTED);

        assertThat(mLocationCheckAction.isActionActive(mAnomaly)).isTrue();
    }

    @Test
    public void testIsActionActive_fineLocationGranted_returnTrue() {
        ShadowPermissionChecker.addPermission(Manifest.permission.ACCESS_FINE_LOCATION, -1, UID,
                PACKAGE_NAME, PackageManager.PERMISSION_GRANTED);

        assertThat(mLocationCheckAction.isActionActive(mAnomaly)).isTrue();
    }

    @Test
    public void testIsActionActive_noLocationGranted_returnFalse() {
        assertThat(mLocationCheckAction.isActionActive(mAnomaly)).isFalse();
    }

}
