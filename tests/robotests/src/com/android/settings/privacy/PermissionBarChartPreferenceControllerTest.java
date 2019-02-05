/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.privacy;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.permission.RuntimePermissionUsageInfo;
import android.provider.DeviceConfig;

import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.shadow.ShadowDeviceConfig;
import com.android.settingslib.widget.BarChartInfo;
import com.android.settingslib.widget.BarChartPreference;
import com.android.settingslib.widget.BarViewInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowDeviceConfig.class})
public class PermissionBarChartPreferenceControllerTest {

    @Mock
    private PreferenceScreen mScreen;

    private PermissionBarChartPreferenceController mController;
    private BarChartPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context Context = RuntimeEnvironment.application;
        mController = new PermissionBarChartPreferenceController(Context, "test_key");
        mPreference = spy(new BarChartPreference(Context));
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn((BarChartPreference) mPreference);
    }

    @After
    public void tearDown() {
        ShadowDeviceConfig.reset();
    }

    @Test
    public void getAvailabilityStatus_permissionHubNotSet_shouldReturnUnsupported() {
        // We have not yet set the property to show the Permissions Hub.
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_permissionHubEnabled_shouldReturnAvailableUnsearchable() {
        DeviceConfig.setProperty(DeviceConfig.Privacy.NAMESPACE,
                DeviceConfig.Privacy.PROPERTY_PERMISSIONS_HUB_ENABLED, "true", true);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    public void displayPreference_shouldInitializeBarChart() {
        mController.displayPreference(mScreen);

        verify(mPreference).initializeBarChart(any(BarChartInfo.class));
    }

    @Test
    public void onPermissionUsageResult_differentPermissionResultSet_shouldSetBarViewInfos() {
        final List<RuntimePermissionUsageInfo> infos1 = new ArrayList<>();
        final RuntimePermissionUsageInfo info1 =
                new RuntimePermissionUsageInfo("permission 1", 10);
        infos1.add(info1);
        mController.displayPreference(mScreen);
        mController.onPermissionUsageResult(infos1);

        verify(mPreference).setBarViewInfos(any(BarViewInfo[].class));

        final List<RuntimePermissionUsageInfo> infos2 = new ArrayList<>();
        final RuntimePermissionUsageInfo info2 =
                new RuntimePermissionUsageInfo("permission 2", 20);
        infos2.add(info2);
        mController.onPermissionUsageResult(infos2);

        verify(mPreference, times(2)).setBarViewInfos(any(BarViewInfo[].class));
    }

    @Test
    public void onPermissionUsageResult_samePermissionResultSet_shouldNotSetBarViewInfos() {
        final List<RuntimePermissionUsageInfo> mInfos = new ArrayList<>();
        final RuntimePermissionUsageInfo info1 =
                new RuntimePermissionUsageInfo("permission 1", 10);
        mInfos.add(info1);
        mController.displayPreference(mScreen);
        mController.onPermissionUsageResult(mInfos);

        mController.onPermissionUsageResult(mInfos);

        verify(mPreference, times(1)).setBarViewInfos(any(BarViewInfo[].class));
    }
}
