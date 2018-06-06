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

package com.android.settings.applications.defaultapps;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.os.UserManager;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.applications.DefaultAppInfo;
import com.android.settingslib.wrapper.PackageManagerWrapper;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.util.ReflectionHelpers;

@RunWith(SettingsRobolectricTestRunner.class)
public class DefaultBrowserPickerTest {

    private static final String TEST_APP_KEY = "";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;
    @Mock
    private PackageManagerWrapper mPackageManager;

    private DefaultBrowserPicker mPicker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();
        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mPicker = new DefaultBrowserPicker();
        mPicker.onAttach((Context) mActivity);

        ReflectionHelpers.setField(mPicker, "mPm", mPackageManager);
    }

    @Test
    public void setDefaultAppKey_shouldUpdateDefaultBrowser() {
        mPicker.setDefaultKey(TEST_APP_KEY);
        verify(mPackageManager).setDefaultBrowserPackageNameAsUser(eq(TEST_APP_KEY), anyInt());
    }

    @Test
    public void getDefaultAppKey_shouldReturnDefaultBrowser() {
        mPicker.getDefaultKey();
        verify(mPackageManager).getDefaultBrowserPackageNameAsUser(anyInt());
    }

    @Test
    public void getCandidates_shouldNotIncludeDuplicatePackageName() throws NameNotFoundException {
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        final String PACKAGE_ONE = "com.first.package";
        final String PACKAGE_TWO = "com.second.package";
        resolveInfos.add(createResolveInfo(PACKAGE_ONE));
        resolveInfos.add(createResolveInfo(PACKAGE_TWO));
        resolveInfos.add(createResolveInfo(PACKAGE_ONE));
        resolveInfos.add(createResolveInfo(PACKAGE_TWO));
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
            .thenReturn(resolveInfos);
        when(mPackageManager.getApplicationInfoAsUser(eq(PACKAGE_ONE), anyInt(), anyInt()))
            .thenReturn(createApplicationInfo(PACKAGE_ONE));
        when(mPackageManager.getApplicationInfoAsUser(eq(PACKAGE_TWO), anyInt(), anyInt()))
            .thenReturn(createApplicationInfo(PACKAGE_TWO));

        final List<DefaultAppInfo> defaultBrowserInfo = mPicker.getCandidates();

        assertThat(defaultBrowserInfo.size()).isEqualTo(2);
    }

    private ResolveInfo createResolveInfo(String packageName) {
        final ResolveInfo info = new ResolveInfo();
        info.handleAllWebDataURI = true;
        info.activityInfo = new ActivityInfo();
        info.activityInfo.packageName = packageName;
        return info;
    }

    private ApplicationInfo createApplicationInfo(String packageName) {
        final ApplicationInfo info = new ApplicationInfo();
        info.packageName = packageName;
        return info;
    }
}
