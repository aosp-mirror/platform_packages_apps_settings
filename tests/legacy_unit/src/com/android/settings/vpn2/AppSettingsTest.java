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

import static com.android.settings.vpn2.AppManagementFragment.appHasVpnPermission;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Process;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

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
    public void testAppOpsRequiredToOpenFragment() {
        ApplicationInfo mockApp = createMockApp();

        final AppOpsManager.PackageOps[] blankOps = {
            new AppOpsManager.PackageOps(mockApp.packageName, mockApp.uid, new ArrayList<>()),
            new AppOpsManager.PackageOps(mockApp.packageName, mockApp.uid, new ArrayList<>())
        };

        // List with one package op
        when(mAppOps.getOpsForPackage(eq(mockApp.uid), eq(mockApp.packageName),
                any(int[].class))).thenReturn(Arrays.asList(
                        new AppOpsManager.PackageOps[] {blankOps[0]}));
        assertTrue(appHasVpnPermission(mContext, mockApp));

        // List with more than one package op
        when(mAppOps.getOpsForPackage(eq(mockApp.uid), eq(mockApp.packageName),
                any(int[].class))).thenReturn(Arrays.asList(blankOps));
        assertTrue(appHasVpnPermission(mContext, mockApp));

        // Empty list
        when(mAppOps.getOpsForPackage(eq(mockApp.uid), eq(mockApp.packageName),
                any(int[].class))).thenReturn(Collections.emptyList());
        assertFalse(appHasVpnPermission(mContext, mockApp));

        // Null list (may be returned in place of an empty list)
        when(mAppOps.getOpsForPackage(eq(mockApp.uid), eq(mockApp.packageName),
                any(int[].class))).thenReturn(null);
        assertFalse(appHasVpnPermission(mContext, mockApp));
    }

    private static ApplicationInfo createMockApp() {
        final ApplicationInfo app = new ApplicationInfo();
        app.packageName = "com.example.mockvpn";
        app.uid = Process.FIRST_APPLICATION_UID;
        return app;
    }
}
