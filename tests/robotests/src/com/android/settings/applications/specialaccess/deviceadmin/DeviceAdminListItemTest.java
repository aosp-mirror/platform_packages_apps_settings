/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.deviceadmin;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.app.admin.DeviceAdminInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DeviceAdminListItemTest {

    @Mock
    private DeviceAdminInfo mDeviceAdminInfo;
    private Context mContext;


    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
    }

    @Test
    public void newInstance_shouldLoadInfoFromDeviceAdminInfo() {
        final String label = "testlabel";
        final String description = "testdesc";
        final ComponentName cn = new ComponentName(mContext.getPackageName(), "test");
        when(mDeviceAdminInfo.getActivityInfo()).thenReturn(new ActivityInfo());
        mDeviceAdminInfo.getActivityInfo().applicationInfo = new ApplicationInfo();
        when(mDeviceAdminInfo.loadLabel(any(PackageManager.class))).thenReturn(label);
        when(mDeviceAdminInfo.loadDescription(any(PackageManager.class))).thenReturn(description);
        when(mDeviceAdminInfo.loadIcon(any(PackageManager.class)))
                .thenReturn(new ColorDrawable(Color.BLUE));
        when(mDeviceAdminInfo.getComponent()).thenReturn(cn);

        DeviceAdminListItem item = new DeviceAdminListItem(mContext, mDeviceAdminInfo);

        assertThat(item.getKey()).isEqualTo("0@" + cn.flattenToShortString());
        assertThat(item.getName()).isEqualTo(label);
        assertThat(item.getDescription()).isEqualTo(description);
    }
}
