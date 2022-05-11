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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AppLocaleUtilTest {
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private ActivityManager mActivityManager;
    @Mock
    private ApplicationInfo mApplicationInfo;
    @Mock
    private Resources mResources;

    private Context mContext;
    private String mDisallowedPackage = "com.disallowed.package";
    private String mAllowedPackage = "com.allowed.package";
    private List<ResolveInfo> mListResolveInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(ActivityManager.class)).thenReturn(mActivityManager);
        setDisallowedPackageName(mDisallowedPackage);
    }

    @Test
    @Ignore("b/231904717")
    public void canDisplayLocaleUi_showUI() throws PackageManager.NameNotFoundException {
        setApplicationInfo(/*no platform key*/ false);
        setActivityInfo(mAllowedPackage);

        assertTrue(AppLocaleUtil.canDisplayLocaleUi(mContext, mAllowedPackage, mListResolveInfo));
    }

    @Test
    @Ignore("b/231904717")
    public void canDisplayLocaleUi_notShowUI_hasPlatformKey()
            throws PackageManager.NameNotFoundException {
        setApplicationInfo(/*has platform key*/ true);
        setActivityInfo(mAllowedPackage);

        assertFalse(AppLocaleUtil.canDisplayLocaleUi(mContext, mAllowedPackage, mListResolveInfo));
    }

    @Test
    @Ignore("b/231904717")
    public void canDisplayLocaleUi_notShowUI_noLauncherEntry()
            throws PackageManager.NameNotFoundException {
        setApplicationInfo(/*no platform key*/false);
        setActivityInfo("");

        assertFalse(AppLocaleUtil.canDisplayLocaleUi(mContext, mAllowedPackage, mListResolveInfo));
    }

    @Test
    @Ignore("b/231904717")
    public void canDisplayLocaleUi_notShowUI_matchDisallowedPackageList()
            throws PackageManager.NameNotFoundException {
        setApplicationInfo(/*no platform key*/false);
        setActivityInfo("");

        assertFalse(AppLocaleUtil
                .canDisplayLocaleUi(mContext, mDisallowedPackage, mListResolveInfo));
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

    private void setActivityInfo(String packageName) {
        ResolveInfo resolveInfo = mock(ResolveInfo.class);
        ActivityInfo activityInfo = mock(ActivityInfo.class);
        activityInfo.packageName = packageName;
        resolveInfo.activityInfo = activityInfo;
        mListResolveInfo = new ArrayList<>();
        mListResolveInfo.add(resolveInfo);
    }
}
