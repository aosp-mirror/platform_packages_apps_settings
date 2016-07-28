/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.vpn2;

import static com.android.settings.vpn2.AppManagementFragment.isAlwaysOnSupportedByApp;
import static com.android.settings.vpn2.AppManagementFragment.appHasVpnPermission;
import static org.mockito.Mockito.*;

import android.app.AppOpsManager;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Process;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;
import android.content.Context;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AppSettingsTest extends AndroidTestCase {
    private static final String TAG = AppSettingsTest.class.getSimpleName();

    @Mock private Context mContext;
    @Mock private AppOpsManager mAppOps;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(eq(Context.APP_OPS_SERVICE))).thenReturn(mAppOps);
    }

    @SmallTest
    public void testAlwaysOnVersionRestriction() {
        ApplicationInfo mockApp = createMockApp();

        // API 23 (MNC) = not supported
        mockApp.targetSdkVersion = Build.VERSION_CODES.M;
        assertFalse(isAlwaysOnSupportedByApp(mockApp));

        // API 24 (NYC) = supported
        mockApp.targetSdkVersion = Build.VERSION_CODES.N;
        assertTrue(isAlwaysOnSupportedByApp(mockApp));

        // API 25 (NYC MR1) = supported
        mockApp.targetSdkVersion = Build.VERSION_CODES.N_MR1;
        assertTrue(isAlwaysOnSupportedByApp(mockApp));
    }

    @SmallTest
    public void testAppOpsRequiredToOpenFragment() {
        ApplicationInfo mockApp = createMockApp();

        final AppOpsManager.PackageOps[] blankOps = {
            new AppOpsManager.PackageOps(mockApp.packageName, mockApp.uid, new ArrayList<>()),
            new AppOpsManager.PackageOps(mockApp.packageName, mockApp.uid, new ArrayList<>())
        };

        // List with one package op
        when(mAppOps.getOpsForPackage(eq(mockApp.uid), eq(mockApp.packageName), any()))
                .thenReturn(Arrays.asList(new AppOpsManager.PackageOps[] {blankOps[0]}));
        assertTrue(appHasVpnPermission(mContext, mockApp));

        // List with more than one package op
        when(mAppOps.getOpsForPackage(eq(mockApp.uid), eq(mockApp.packageName), any()))
                .thenReturn(Arrays.asList(blankOps));
        assertTrue(appHasVpnPermission(mContext, mockApp));

        // Empty list
        when(mAppOps.getOpsForPackage(eq(mockApp.uid), eq(mockApp.packageName), any()))
                .thenReturn(Collections.emptyList());
        assertFalse(appHasVpnPermission(mContext, mockApp));

        // Null list (may be returned in place of an empty list)
        when(mAppOps.getOpsForPackage(eq(mockApp.uid), eq(mockApp.packageName), any()))
                .thenReturn(null);
        assertFalse(appHasVpnPermission(mContext, mockApp));
    }

    private static ApplicationInfo createMockApp() {
        final ApplicationInfo app = new ApplicationInfo();
        app.packageName = "com.example.mockvpn";
        app.uid = Process.FIRST_APPLICATION_UID;
        return app;
    }
}
