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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.LocaleConfig;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.LocaleList;
import android.util.FeatureFlagUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
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
    private Resources mResources;
    @Mock
    private LocaleConfig mLocaleConfig;

    private Context mContext;
    private String mDisallowedPackage = "com.disallowed.package";
    private String mAllowedPackage = "com.allowed.package";
    private List<ResolveInfo> mListResolveInfo;
    private ApplicationInfo mAppInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(ApplicationProvider.getApplicationContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(ActivityManager.class)).thenReturn(mActivityManager);

        FeatureFlagUtils.setEnabled(mContext, FeatureFlagUtils.SETTINGS_APP_LOCALE_OPT_IN_ENABLED,
                true);
        setDisallowedPackageName(mDisallowedPackage);
        mAppInfo = new ApplicationInfo();
        mLocaleConfig = mock(LocaleConfig.class);
    }

    @After
    public void tearDown() {
        AppLocaleUtil.sLocaleConfig = null;
    }

    @Test
    public void canDisplayLocaleUi_showUI() {
        when(mLocaleConfig.getStatus()).thenReturn(LocaleConfig.STATUS_SUCCESS);
        when(mLocaleConfig.getSupportedLocales()).thenReturn(LocaleList.forLanguageTags("en-US"));
        AppLocaleUtil.sLocaleConfig = mLocaleConfig;
        setActivityInfo(mAllowedPackage);
        mAppInfo.packageName = mAllowedPackage;

        assertTrue(AppLocaleUtil.canDisplayLocaleUi(mContext, mAppInfo, mListResolveInfo));
    }

    @Test
    public void canDisplayLocaleUi_notShowUI_hasPlatformKey() {
        setActivityInfo(mAllowedPackage);
        mAppInfo.privateFlags |= ApplicationInfo.PRIVATE_FLAG_SIGNED_WITH_PLATFORM_KEY;
        mAppInfo.packageName = mAllowedPackage;

        assertFalse(AppLocaleUtil.canDisplayLocaleUi(mContext, mAppInfo, mListResolveInfo));
    }

    @Test
    public void canDisplayLocaleUi_notShowUI_noLauncherEntry() {
        setActivityInfo("");

        assertFalse(AppLocaleUtil.canDisplayLocaleUi(mContext, mAppInfo, mListResolveInfo));
    }

    @Test
    public void canDisplayLocaleUi_notShowUI_matchDisallowedPackageList() {
        setActivityInfo("");
        mAppInfo.packageName = mDisallowedPackage;

        assertFalse(AppLocaleUtil
                .canDisplayLocaleUi(mContext, mAppInfo, mListResolveInfo));
    }

    @Test
    public void getPackageLocales_getLocales_success() {
        when(mLocaleConfig.getStatus()).thenReturn(LocaleConfig.STATUS_SUCCESS);
        when(mLocaleConfig.getSupportedLocales()).thenReturn(LocaleList.forLanguageTags("en-US"));

        LocaleList result = AppLocaleUtil.getPackageLocales(mLocaleConfig);

        assertFalse(result.isEmpty());
    }

    @Test
    public void getPackageLocales_getLocales_failed() {
        when(mLocaleConfig.getStatus()).thenReturn(LocaleConfig.STATUS_PARSING_FAILED);

        LocaleList result = AppLocaleUtil.getPackageLocales(mLocaleConfig);

        assertNull(result);
    }

    private void setDisallowedPackageName(String packageName) {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getStringArray(anyInt())).thenReturn(new String[]{packageName});
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
