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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.UserManager;

import androidx.preference.Preference;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DefaultBrowserPreferenceControllerTest {

    @Mock
    private Context mContext;
    @Mock
    private UserManager mUserManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PackageManager mPackageManager;

    private DefaultBrowserPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        mController = new DefaultBrowserPreferenceController(mContext);
        ReflectionHelpers.setField(mController, "mPackageManager", mPackageManager);
    }

    @Test
    public void isAvailable_noBrowser_shouldReturnFalse() {
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(null);
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasBrowser_shouldReturnTrue() {
        final ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.handleAllWebDataURI = true;
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
            .thenReturn(Collections.singletonList(info));
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void getSoleAppLabel_hasNoApp_shouldNotReturnLabel() {
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
                .thenReturn(null);
        final Preference pref = mock(Preference.class);

        mController.updateState(pref);
        verify(pref).setSummary(R.string.app_list_preference_none);
    }

    @Test
    public void getDefaultAppLabel_hasAppWithMultipleResolvedInfo_shouldReturnLabel()
            throws NameNotFoundException {
        DefaultBrowserPreferenceController spyController = spy(mController);
        doReturn(null).when(spyController).getDefaultAppIcon();
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        final CharSequence PACKAGE_NAME = "com.test.package";
        final ResolveInfo info1 = spy(createResolveInfo(PACKAGE_NAME.toString()));
        when(info1.loadLabel(mPackageManager)).thenReturn(PACKAGE_NAME);
        resolveInfos.add(info1);
        resolveInfos.add(createResolveInfo(PACKAGE_NAME.toString()));
        when(mPackageManager.getDefaultBrowserPackageNameAsUser(anyInt())).thenReturn(null);
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
            .thenReturn(resolveInfos);
        when(mPackageManager.getApplicationInfoAsUser(
            eq(PACKAGE_NAME.toString()), anyInt(), anyInt()))
            .thenReturn(createApplicationInfo(PACKAGE_NAME.toString()));

        assertThat(spyController.getDefaultAppLabel()).isEqualTo(PACKAGE_NAME);
    }

    @Test
    public void getDefaultApp_shouldGetDefaultBrowserPackage() {
        mController.getDefaultAppInfo();

        verify(mPackageManager).getDefaultBrowserPackageNameAsUser(anyInt());
    }

    @Test
    public void getDefaultApp_shouldGetApplicationInfoAsUser() throws NameNotFoundException {
        final String PACKAGE_NAME = "com.test.package";
        when(mPackageManager.getDefaultBrowserPackageNameAsUser(anyInt())).thenReturn(PACKAGE_NAME);

        mController.getDefaultAppInfo();

        verify(mPackageManager).getApplicationInfoAsUser(eq(PACKAGE_NAME), anyInt(), anyInt());
    }

    @Test
    public void isBrowserDefault_onlyApp_shouldReturnTrue() {
        when(mPackageManager.getDefaultBrowserPackageNameAsUser(anyInt())).thenReturn(null);
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        final String PACKAGE_ONE = "pkg";
        resolveInfos.add(createResolveInfo(PACKAGE_ONE));
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
            .thenReturn(resolveInfos);

        assertThat(mController.isBrowserDefault("pkg", 0)).isTrue();
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

        final List<ResolveInfo> defaultBrowserInfo =
            DefaultBrowserPreferenceController.getCandidates(mPackageManager, 0 /* userId */);

        assertThat(defaultBrowserInfo.size()).isEqualTo(2);
    }

    @Test
    public void getCandidates_shouldQueryActivityWithFlagsEquals0() {
        DefaultBrowserPreferenceController.getCandidates(mPackageManager, 0 /* userId */);

        verify(mPackageManager).queryIntentActivitiesAsUser(
            any(Intent.class), eq(0) /* flags */, eq(0) /* userId */);
    }

    @Test
    public void getOnlyAppIcon_shouldGetApplicationInfoAsUser() throws NameNotFoundException {
        final List<ResolveInfo> resolveInfos = new ArrayList<>();
        final String PACKAGE_NAME = "com.test.package";
        resolveInfos.add(createResolveInfo(PACKAGE_NAME));
        when(mPackageManager.queryIntentActivitiesAsUser(any(Intent.class), anyInt(), anyInt()))
            .thenReturn(resolveInfos);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mContext.getResources()).thenReturn(mock(Resources.class));

        mController.getOnlyAppIcon();

        verify(mPackageManager).getApplicationInfoAsUser(
            eq(PACKAGE_NAME), eq(0) /* flags */, eq(0) /* userId */);
    }

    @Test
    public void hasBrowserPreference_shouldQueryIntentActivitiesAsUser() {
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        DefaultBrowserPreferenceController
            .hasBrowserPreference("com.test.package", mContext, 0 /* userId */);

        verify(mPackageManager).queryIntentActivitiesAsUser(
            any(Intent.class), eq(0) /* flags */, eq(0) /* userId */);
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
