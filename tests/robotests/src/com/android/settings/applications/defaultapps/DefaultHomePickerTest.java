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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Build;
import android.os.UserManager;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.applications.DefaultAppInfo;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DefaultHomePickerTest {

    private static final String TEST_APP_KEY = "com.android.settings/DefaultEmergencyPickerTest";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private PackageManager mPackageManager;

    private Context mContext;
    private DefaultHomePicker mPicker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        FakeFeatureFactory.setupForTest();

        when(mActivity.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        when(mActivity.getSystemService(Context.APP_OPS_SERVICE)).thenReturn(mAppOpsManager);
        when(mActivity.getApplicationContext()).thenReturn(RuntimeEnvironment.application);

        mPicker = spy(new DefaultHomePicker());
        mPicker.onAttach((Context) mActivity);

        ReflectionHelpers.setField(mPicker, "mPm", mPackageManager);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mContext).when(mPicker).getContext();
    }

    @Test
    public void setDefaultAppKey_shouldUpdateDefault() {
        assertThat(mPicker.setDefaultKey(TEST_APP_KEY)).isTrue();

        verify(mPackageManager).replacePreferredActivity(any(IntentFilter.class),
                anyInt(), any(ComponentName[].class), any(ComponentName.class));
        verify(mContext).startActivity(any());
    }

    @Test
    public void getDefaultAppKey_shouldReturnDefault() {
        final ComponentName cn = mock(ComponentName.class);
        when(mPackageManager.getHomeActivities(anyList())).thenReturn(cn);
        mPicker.getDefaultKey();
        verify(cn).flattenToString();
    }

    @Test
    public void getCandidates_allLaunchersAvailableIfNoManagedProfile() {
        addLaunchers();
        List<DefaultAppInfo> candidates = mPicker.getCandidates();
        assertThat(candidates.size()).isEqualTo(2);
        assertThat(candidates.get(0).summary).isNull();
        assertThat(candidates.get(0).enabled).isTrue();
        assertThat(candidates.get(1).summary).isNull();
        assertThat(candidates.get(1).enabled).isTrue();
    }

    @Test
    public void getCandidates_onlyLollipopPlusLaunchersAvailableIfManagedProfile() {
        List<UserInfo> profiles = new ArrayList<>();
        profiles.add(new UserInfo(/*id=*/ 10, "TestUserName", UserInfo.FLAG_MANAGED_PROFILE));
        when(mUserManager.getProfiles(anyInt())).thenReturn(profiles);

        addLaunchers();
        List<DefaultAppInfo> candidates = mPicker.getCandidates();
        assertThat(candidates.size()).isEqualTo(2);
        DefaultAppInfo lollipopPlusLauncher = candidates.get(0);
        assertThat(lollipopPlusLauncher.summary).isNull();
        assertThat(lollipopPlusLauncher.enabled).isTrue();

        DefaultAppInfo preLollipopLauncher = candidates.get(1);
        assertThat(preLollipopLauncher.summary).isNotNull();
        assertThat(preLollipopLauncher.enabled).isFalse();
    }

    private ResolveInfo createLauncher(
            String packageName, String className, int targetSdk) throws NameNotFoundException {
        ResolveInfo launcher = new ResolveInfo();
        launcher.activityInfo = new ActivityInfo();
        launcher.activityInfo.packageName = packageName;
        launcher.activityInfo.name = className;
        ApplicationInfo launcherAppInfo = new ApplicationInfo();
        launcherAppInfo.targetSdkVersion = targetSdk;
        when(mPackageManager.getApplicationInfo(eq(launcher.activityInfo.packageName), anyInt()))
                .thenReturn(launcherAppInfo);
        return launcher;
    }

    private void addLaunchers() {
        doAnswer(invocation -> {
                // The result of this method is stored in the first parameter...
                List<ResolveInfo> parameter = (List<ResolveInfo>) invocation.getArguments()[0];
                parameter.add(createLauncher(
                        "package.1", "LollipopPlusLauncher", Build.VERSION_CODES.LOLLIPOP));
                parameter.add(createLauncher(
                        "package.2", "PreLollipopLauncher", Build.VERSION_CODES.KITKAT));
                return null;
                })
                .when(mPackageManager).getHomeActivities(anyList());
    }
}
