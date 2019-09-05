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
 * limitations under the License
 */

package com.android.settings.homepage.contextualcards.slices;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.android.settings.notification.NotificationBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class NotificationMultiChannelAppRowTest {

    @Mock
    private NotificationBackend mNotificationBackend;
    private Context mContext;
    private NotificationMultiChannelAppRow mNotificationMultiChannelAppRow;
    private PackageInfo mPackageInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
        mPackageInfo = new PackageInfo();
        mPackageInfo.applicationInfo = new ApplicationInfo();
        mPackageInfo.applicationInfo.packageName = "com.android.test";
        mNotificationMultiChannelAppRow = new NotificationMultiChannelAppRow(mContext,
                mNotificationBackend, mPackageInfo);
    }

    @Test
    public void call_isMultiChannel_shouldLoadAppRow() throws Exception {
        doReturn(3).when(mNotificationBackend).getChannelCount(any(String.class),
                any(int.class));

        mNotificationMultiChannelAppRow.call();

        verify(mNotificationBackend).loadAppRow(any(Context.class), any(PackageManager.class),
                any(RoleManager.class), any(PackageInfo.class));
    }

    @Test
    public void call_isNotMultiChannel_shouldNotLoadAppRow() throws Exception {
        doReturn(1).when(mNotificationBackend).getChannelCount(any(String.class),
                any(int.class));

        mNotificationMultiChannelAppRow.call();

        verify(mNotificationBackend, never()).loadAppRow(any(Context.class),
                any(PackageManager.class), any(RoleManager.class), any(PackageInfo.class));
    }
}
