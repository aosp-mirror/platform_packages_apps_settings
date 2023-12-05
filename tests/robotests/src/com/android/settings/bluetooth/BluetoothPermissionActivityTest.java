/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

@Ignore("b/313014781")
@RunWith(RobolectricTestRunner.class)
public class BluetoothPermissionActivityTest {

    private BluetoothPermissionActivity mActivity;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = mock(ContextWrapper.class);
        mActivity = new BluetoothPermissionActivity();
    }

    @Test
    public void sendBroadcastWithPermission() throws Exception {
        final ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        ReflectionHelpers.setField(mActivity, "mBase", mContext);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);

        final String btPkgName = "com.android.bluetooth";
        ActivityInfo btOppLauncherActivityInfo = new ActivityInfo();
        btOppLauncherActivityInfo.name = "com.android.bluetooth.opp.BluetoothOppLauncherActivity";

        PackageInfo btPkgInfo = new PackageInfo();
        btPkgInfo.activities = new ActivityInfo[] {btOppLauncherActivityInfo};

        PackageManager pm = mock(PackageManager.class);
        when(pm.getPackagesForUid(Process.BLUETOOTH_UID)).thenReturn(new String[] {btPkgName});
        when(pm.getPackageInfo(eq(btPkgName), anyInt())).thenReturn(btPkgInfo);
        when(mContext.getPackageManager()).thenReturn(pm);

        mActivity.sendReplyIntentToReceiver(true, true);

        verify(mContext).sendBroadcast(intentCaptor.capture(),
                eq("android.permission.BLUETOOTH_CONNECT"));
    }
}
