/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settingslib.applications.ApplicationsState.AppEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AppLocaleUtilTest {
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ActivityManager mActivityManager;
    @Mock
    private AppEntry mEntry;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private Resources mResources;

    private Context mContext;
    private String mDisallowedPackage = "com.disallowed.package";
    private String mAallowedPackage = "com.allowed.package";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(ActivityManager.class)).thenReturn(mActivityManager);
    }

    @Test
    public void isDisplayLocaleUi_showUI() throws PackageManager.NameNotFoundException {
        setTestAppEntry(mAallowedPackage);
        setDisallowedPackageName(mDisallowedPackage);
        setApplicationInfo(/*no platform key*/false);
        mEntry.hasLauncherEntry = true;

        assertTrue(AppLocaleUtil.canDisplayLocaleUi(mContext, mEntry));
    }

    @Test
    public void isDisplayLocaleUi_notShowUI_hasPlatformKey()
            throws PackageManager.NameNotFoundException {
        setTestAppEntry(mAallowedPackage);
        setDisallowedPackageName(mDisallowedPackage);
        setApplicationInfo(/*has platform key*/true);
        mEntry.hasLauncherEntry = true;

        assertFalse(AppLocaleUtil.canDisplayLocaleUi(mContext, mEntry));
    }

    @Test
    public void isDisplayLocaleUi_notShowUI_noLauncherEntry()
            throws PackageManager.NameNotFoundException {
        setTestAppEntry(mAallowedPackage);
        setDisallowedPackageName(mDisallowedPackage);
        setApplicationInfo(/*no platform key*/false);
        mEntry.hasLauncherEntry = false;

        assertFalse(AppLocaleUtil.canDisplayLocaleUi(mContext, mEntry));
    }

    @Test
    public void isDisplayLocaleUi_notShowUI_matchDisallowedPackageList()
            throws PackageManager.NameNotFoundException {
        setTestAppEntry(mDisallowedPackage);
        setDisallowedPackageName(mDisallowedPackage);
        setApplicationInfo(/*no platform key*/false);
        mEntry.hasLauncherEntry = false;

        assertFalse(AppLocaleUtil.canDisplayLocaleUi(mContext, mEntry));
    }

    private void setTestAppEntry(String packageName) {
        mEntry.info = mApplicationInfo;
        mApplicationInfo.packageName = packageName;
    }

    private void setDisallowedPackageName(String packageName) {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getStringArray(anyInt())).thenReturn(new String[]{packageName});
    }

    private void setApplicationInfo(boolean signedWithPlatformKey)
            throws PackageManager.NameNotFoundException {
        ApplicationInfo applicationInfo = new ApplicationInfo();
        if (signedWithPlatformKey) {
            applicationInfo.privateFlags = applicationInfo.privateFlags
                    | ApplicationInfo.PRIVATE_FLAG_SIGNED_WITH_PLATFORM_KEY;
        }

        PackageInfo packageInfo = new PackageInfo();
        packageInfo.applicationInfo = applicationInfo;
        when(mPackageManager.getPackageInfoAsUser(anyString(), anyInt(), anyInt())).thenReturn(
                packageInfo);
    }
}
